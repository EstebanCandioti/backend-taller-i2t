package com.judicial.mesadeayuda.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.TicketAsignarRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.TicketCerrarRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.TicketRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.PaginatedResponse;
import com.judicial.mesadeayuda.DTO.Response.TicketResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Hardware;
import com.judicial.mesadeayuda.Entities.Juzgado;
import com.judicial.mesadeayuda.Entities.Ticket;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.TicketMapper;
import com.judicial.mesadeayuda.Repositories.HardwareRepository;
import com.judicial.mesadeayuda.Repositories.JuzgadoRepository;
import com.judicial.mesadeayuda.Repositories.TicketRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

/**
 * Service principal de Tickets.
 *
 * FLUJO DE ESTADOS: SOLICITADO → ASIGNADO → EN_CURSO → CERRADO
 *
 * REGLAS:
 *   - Solo Admin/Operario crean, editan, asignan y cierran tickets.
 *   - Técnico solo ve sus tickets asignados (solo lectura).
 *   - Al asignar técnico → estado cambia a ASIGNADO + email al técnico.
 *   - Al cerrar → resolución obligatoria.
 *   - Soft-delete solo si está en SOLICITADO y sin técnico.
 */
@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final JuzgadoRepository juzgadoRepository;
    private final HardwareRepository hardwareRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final NotificationWebSocketService notificationWsService;

    public TicketService(TicketRepository ticketRepository,
                         JuzgadoRepository juzgadoRepository,
                         HardwareRepository hardwareRepository,
                         UsuarioRepository usuarioRepository,
                         EmailService emailService,
                         NotificationWebSocketService notificationWsService) {
        this.ticketRepository = ticketRepository;
        this.juzgadoRepository = juzgadoRepository;
        this.hardwareRepository = hardwareRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.notificationWsService = notificationWsService;
    }

    // ── LISTAR (con filtros y restricción por rol) ────────────

    /**
     * Lista tickets según el rol del usuario autenticado.
     * - Admin/Operario: ven todos con filtros opcionales.
     * - Técnico: solo ve sus tickets asignados.
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<TicketResponseDTO> listar(List<Ticket.Estado> estados, Ticket.Prioridad prioridad,
                                                        Integer juzgadoId, Integer tecnicoId,
                                                        String q, Pageable pageable) {
        CustomUserDetails user = getUsuarioAutenticado();
        String rol = getRolUsuario(user);

        Page<Ticket> page;

        if ("Técnico".equals(rol)) {
            page = ticketRepository.findByTecnicoId(user.getId(), pageable);
        } else {
            page = ticketRepository.findConFiltros(estados, prioridad, juzgadoId, tecnicoId, q, pageable);
        }

        return PaginatedResponse.from(page.map(TicketMapper::toDTO));
    }

    /**
     * Obtiene un ticket por ID.
     * Técnico solo puede ver sus propios tickets.
     */
    @Transactional(readOnly = true)
    public TicketResponseDTO obtenerPorId(Integer id) {
        Ticket ticket = buscarTicket(id);
        validarAccesoTecnico(ticket);
        return TicketMapper.toDTO(ticket);
    }

    // ── CREAR ─────────────────────────────────────────────────

    /**
     * Crea un nuevo ticket en estado SOLICITADO.
     * El creador se obtiene del contexto JWT.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.CREATE)
    public TicketResponseDTO crear(TicketRequestDTO dto) {
        Juzgado juzgado = juzgadoRepository.findById(dto.getJuzgadoId())
                .orElseThrow(() -> new NotFoundException("Juzgado", dto.getJuzgadoId()));

        Hardware hardware = null;
        if (dto.getHardwareId() != null) {
            hardware = hardwareRepository.findById(dto.getHardwareId())
                    .orElseThrow(() -> new NotFoundException("Hardware", dto.getHardwareId()));
        }

        Usuario creador = obtenerUsuarioActual();

        // Parsear prioridad con default MEDIA
        Ticket.Prioridad prioridad = Ticket.Prioridad.MEDIA;
        if (dto.getPrioridad() != null && !dto.getPrioridad().isBlank()) {
            try {
                prioridad = Ticket.Prioridad.valueOf(dto.getPrioridad().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        "Prioridad inválida: " + dto.getPrioridad() + ". Valores válidos: BAJA, MEDIA, ALTA, CRITICA");
            }
        }

        Ticket ticket = Ticket.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .prioridad(prioridad)
                .estado(Ticket.Estado.SOLICITADO)
                .tipoRequerimiento(dto.getTipoRequerimiento())
                .juzgado(juzgado)
                .hardware(hardware)
                .referenteNombre(dto.getReferenteNombre())
                .referenteTelefono(dto.getReferenteTelefono())
                .creadoPor(creador)
                .build();

        ticket = ticketRepository.save(ticket);
        return TicketMapper.toDTO(ticket);
    }

    // ── EDITAR ────────────────────────────────────────────────

    /**
     * Edita un ticket existente.
     * Solo se puede editar en estados SOLICITADO o ASIGNADO.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.UPDATE)
    public TicketResponseDTO editar(Integer id, TicketRequestDTO dto) {
        Ticket ticket = buscarTicket(id);

        if (ticket.getEstado() == Ticket.Estado.CERRADO) {
            throw new BusinessException("No se puede editar un ticket cerrado");
        }

        Juzgado juzgado = juzgadoRepository.findById(dto.getJuzgadoId())
                .orElseThrow(() -> new NotFoundException("Juzgado", dto.getJuzgadoId()));

        Hardware hardware = null;
        if (dto.getHardwareId() != null) {
            hardware = hardwareRepository.findById(dto.getHardwareId())
                    .orElseThrow(() -> new NotFoundException("Hardware", dto.getHardwareId()));
        }

        if (dto.getPrioridad() != null && !dto.getPrioridad().isBlank()) {
            try {
                ticket.setPrioridad(Ticket.Prioridad.valueOf(dto.getPrioridad().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        "Prioridad inválida: " + dto.getPrioridad() + ". Valores válidos: BAJA, MEDIA, ALTA, CRITICA");
            }
        }

        ticket.setTitulo(dto.getTitulo());
        ticket.setDescripcion(dto.getDescripcion());
        ticket.setTipoRequerimiento(dto.getTipoRequerimiento());
        ticket.setJuzgado(juzgado);
        ticket.setHardware(hardware);
        ticket.setReferenteNombre(dto.getReferenteNombre());
        ticket.setReferenteTelefono(dto.getReferenteTelefono());
        ticket.setFechaActualizacion(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);
        return TicketMapper.toDTO(ticket);
    }

    // ── ASIGNAR TÉCNICO ───────────────────────────────────────

    /**
     * Asigna un técnico al ticket.
     * Cambia estado a ASIGNADO y envía email al técnico.
     *
     * Reglas:
     *   - Solo se puede asignar si está en SOLICITADO.
     *   - El técnico debe existir, estar activo y tener rol Técnico.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.ASSIGN)
    public TicketResponseDTO asignar(Integer id, TicketAsignarRequestDTO dto) {
        Ticket ticket = buscarTicket(id);

        if (!ticket.puedeAsignarse()) {
            throw new BusinessException(
                    "El ticket solo puede asignarse en estado SOLICITADO. Estado actual: " + ticket.getEstado(),
                    HttpStatus.CONFLICT);
        }

        Usuario tecnico = validarTecnico(dto.getTecnicoId());

        ticket.setTecnico(tecnico);
        ticket.setEstado(Ticket.Estado.ASIGNADO);
        ticket.setFechaAsignacion(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        // Enviar email al técnico (asíncrono, no bloquea)
        emailService.enviarNotificacionAsignacion(ticket);

        notificationWsService.notificarUsuario(
                tecnico.getEmail(),
                "TICKET_ASIGNADO",
                "Ticket",
                ticket.getId(),
                "Se te asignó el ticket: " + ticket.getTitulo()
        );

        notificationWsService.notificarPorRol(
                List.of("Admin", "Operario"),
                "TICKET_ASIGNADO",
                "Ticket",
                ticket.getId(),
                "Ticket asignado a " + tecnico.getNombre() + " " + tecnico.getApellido()
                        + ": " + ticket.getTitulo()
        );

        return TicketMapper.toDTO(ticket);
    }

    // ── REASIGNAR TÉCNICO ─────────────────────────────────────

    /**
     * Reasigna un ticket a un técnico diferente.
     *
     * Reglas:
     *   - Solo se puede reasignar si está en ASIGNADO o EN_CURSO.
     *   - El nuevo técnico debe existir, estar activo y tener rol Técnico.
     *   - El nuevo técnico debe ser diferente al actual.
     *   - Actualiza la fecha de asignación.
     *   - Envía email al nuevo técnico.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.ASSIGN)
    public TicketResponseDTO reasignar(Integer id, TicketAsignarRequestDTO dto) {
        Ticket ticket = buscarTicket(id);

        // Solo se puede reasignar si tiene técnico asignado (ASIGNADO o EN_CURSO)
        if (ticket.getTecnico() == null) {
            throw new BusinessException(
                    "El ticket no tiene técnico asignado. Use el endpoint de asignar.",
                    HttpStatus.CONFLICT);
        }

        if (ticket.getEstado() == Ticket.Estado.CERRADO) {
            throw new BusinessException(
                    "No se puede reasignar un ticket cerrado",
                    HttpStatus.CONFLICT);
        }

        if (ticket.getEstado() == Ticket.Estado.SOLICITADO) {
            throw new BusinessException(
                    "El ticket está en estado SOLICITADO. Use el endpoint de asignar.",
                    HttpStatus.CONFLICT);
        }

        Usuario nuevoTecnico = validarTecnico(dto.getTecnicoId());

        // Validar que sea un técnico diferente
        if (ticket.getTecnico().getId().equals(nuevoTecnico.getId())) {
            throw new BusinessException("El técnico seleccionado ya está asignado a este ticket");
        }

        // Reasignar
        ticket.setTecnico(nuevoTecnico);
        ticket.setFechaAsignacion(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        // Enviar email al nuevo técnico
        emailService.enviarNotificacionAsignacion(ticket);

        notificationWsService.notificarUsuario(
                nuevoTecnico.getEmail(),
                "TICKET_ASIGNADO",
                "Ticket",
                ticket.getId(),
                "Se te asignó el ticket: " + ticket.getTitulo()
        );

        notificationWsService.notificarPorRol(
                List.of("Admin", "Operario"),
                "TICKET_ASIGNADO",
                "Ticket",
                ticket.getId(),
                "Ticket reasignado a " + nuevoTecnico.getNombre() + " " + nuevoTecnico.getApellido()
                        + ": " + ticket.getTitulo()
        );

        return TicketMapper.toDTO(ticket);
    }

    // ── CAMBIAR ESTADO ────────────────────────────────────────

    /**
     * Cambia el estado del ticket a EN_CURSO.
     * Solo válido desde estado ASIGNADO.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.UPDATE)
    public TicketResponseDTO pasarAEnCurso(Integer id) {
        Ticket ticket = buscarTicket(id);

        if (ticket.getEstado() != Ticket.Estado.ASIGNADO) {
            throw new BusinessException(
                    "Solo se puede pasar a EN_CURSO desde estado ASIGNADO. Estado actual: " + ticket.getEstado(),
                    HttpStatus.CONFLICT);
        }

        ticket.setEstado(Ticket.Estado.EN_CURSO);
        ticket = ticketRepository.save(ticket);

        notificationWsService.notificarPorRol(
                List.of("Admin", "Operario"),
                "TICKET_EN_CURSO",
                "Ticket",
                ticket.getId(),
                "Ticket pasó a EN_CURSO: " + ticket.getTitulo()
        );

        return TicketMapper.toDTO(ticket);
    }

    // ── VOLVER A ASIGNADO ─────────────────────────────────────

    /**
     * Retrocede el ticket de EN_CURSO a ASIGNADO.
     * Útil si el técnico no pudo continuar con la resolución.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.UPDATE)
    public TicketResponseDTO volverAAsignado(Integer id) {
        Ticket ticket = buscarTicket(id);

        if (ticket.getEstado() != Ticket.Estado.EN_CURSO) {
            throw new BusinessException(
                    "Solo se puede volver a ASIGNADO desde estado EN_CURSO. Estado actual: " + ticket.getEstado(),
                    HttpStatus.CONFLICT);
        }

        ticket.setEstado(Ticket.Estado.ASIGNADO);
        ticket = ticketRepository.save(ticket);

        notificationWsService.notificarPorRol(
                List.of("Admin", "Operario"),
                "TICKET_ASIGNADO",
                "Ticket",
                ticket.getId(),
                "Ticket volvió a ASIGNADO: " + ticket.getTitulo()
        );

        return TicketMapper.toDTO(ticket);
    }

    // ── CERRAR ────────────────────────────────────────────────

    /**
     * Cierra el ticket con resolución obligatoria.
     * Solo válido desde estados ASIGNADO o EN_CURSO.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.CLOSE)
    public TicketResponseDTO cerrar(Integer id, TicketCerrarRequestDTO dto) {
        Ticket ticket = buscarTicket(id);

        if (!ticket.puedeCerrarse()) {
            throw new BusinessException(
                    "El ticket solo puede cerrarse en estado ASIGNADO o EN_CURSO. Estado actual: " + ticket.getEstado(),
                    HttpStatus.CONFLICT);
        }

        ticket.setEstado(Ticket.Estado.CERRADO);
        ticket.setResolucion(dto.getResolucion());
        ticket.setFechaCierre(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        notificationWsService.notificarPorRol(
                List.of("Admin", "Operario"),
                "TICKET_CERRADO",
                "Ticket",
                ticket.getId(),
                "Ticket cerrado: " + ticket.getTitulo()
        );

        return TicketMapper.toDTO(ticket);
    }

    // ── SOFT-DELETE ────────────────────────────────────────────

    /**
     * Eliminación lógica del ticket.
     * Solo permitido en estado SOLICITADO y sin técnico asignado.
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Ticket ticket = buscarTicket(id);

        if (!ticket.puedeEliminarse()) {
            throw new BusinessException(
                    "Solo se puede eliminar un ticket en estado SOLICITADO sin técnico asignado",
                    HttpStatus.CONFLICT);
        }

        ticket.setEliminado(true);
        ticket.setFechaEliminacion(LocalDateTime.now());
        ticket.setEliminadoPor(obtenerUsuarioActual());
        ticketRepository.save(ticket);
    }

    // ── RESTORE ───────────────────────────────────────────────

    /**
     * Restaura un ticket eliminado con soft-delete.
     * Usa query nativa para bypassear @SQLRestriction("eliminado = 0").
     */
    @Auditable(entidad = "Ticket", accion = AuditLog.Accion.RESTORE)
    public TicketResponseDTO restaurar(Integer id) {
        // findEliminadoById bypasea el filtro de @SQLRestriction
        Ticket ticket = ticketRepository.findEliminadoById(id)
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró un ticket eliminado con id: " + id));

        if (!ticket.isEliminado()) {
            throw new BusinessException("El ticket no está eliminado");
        }

        ticket.setEliminado(false);
        ticket.setFechaEliminacion(null);
        ticket.setEliminadoPor(null);

        ticket = ticketRepository.save(ticket);
        return TicketMapper.toDTO(ticket);
    }

    // ── HELPERS PRIVADOS ──────────────────────────────────────

    private Ticket buscarTicket(Integer id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket", id));
    }

    /**
     * Valida que un usuario sea técnico activo.
     * Reutilizado en asignar() y reasignar().
     */
    private Usuario validarTecnico(Integer tecnicoId) {
        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new NotFoundException("Técnico", tecnicoId));

        if (!"Técnico".equals(tecnico.getRol().getNombre())) {
            throw new BusinessException("El usuario seleccionado no tiene rol Técnico");
        }
        if (!tecnico.isActivo()) {
            throw new BusinessException("El técnico seleccionado está desactivado");
        }

        return tecnico;
    }

    /**
     * Valida que un técnico solo pueda acceder a sus propios tickets.
     */
    private void validarAccesoTecnico(Ticket ticket) {
        CustomUserDetails user = getUsuarioAutenticado();
        String rol = getRolUsuario(user);

        if ("Técnico".equals(rol) &&
                (ticket.getTecnico() == null || !ticket.getTecnico().getId().equals(user.getId()))) {
            throw new BusinessException("No tiene acceso a este ticket", HttpStatus.FORBIDDEN);
        }
    }

    private CustomUserDetails getUsuarioAutenticado() {
        return (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    private String getRolUsuario(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = getUsuarioAutenticado();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}

