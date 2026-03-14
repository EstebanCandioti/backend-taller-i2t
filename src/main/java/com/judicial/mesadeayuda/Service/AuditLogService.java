package com.judicial.mesadeayuda.Service;

import com.judicial.mesadeayuda.DTO.Response.AuditLogResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Repositories.AuditLogRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Mapper.AuditLogMapper;
import com.judicial.mesadeayuda.Security.CustomUserDetails;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service para el módulo de Auditoría.
 *
 * Dos responsabilidades:
 *   1. CONSULTA: listar registros de auditoría con filtros (GET /api/audit)
 *   2. INSERCIÓN: registrar acciones (llamado desde AuditAspect via AOP)
 *
 * NUNCA modifica ni elimina registros: audit_log es inmutable.
 */
@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UsuarioRepository usuarioRepository) {
        this.auditLogRepository = auditLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ── CONSULTA ──────────────────────────────────────────────

    /**
     * Historial de una entidad específica.
     * Ej: todos los cambios del Ticket con id=5
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> obtenerPorEntidad(String entidad, Integer registroId) {
        return auditLogRepository
                .findByEntidadAndRegistroIdOrderByFechaDesc(entidad, registroId).stream()
                .map(AuditLogMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Acciones realizadas por un usuario.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> obtenerPorUsuario(Integer usuarioId) {
        return auditLogRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(AuditLogMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Filtro general con rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> listarPorFechas(LocalDateTime desde, LocalDateTime hasta) {
        return listarConFiltros(null, null, desde, hasta);
    }

    /**
     * Filtro completo: entidad + acción + rango de fechas.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> listarConFiltros(String entidad, AuditLog.Accion accion,
                                                      LocalDateTime desde, LocalDateTime hasta) {
        Specification<AuditLog> specification = crearSpecification(entidad, accion, desde, hasta);

        return auditLogRepository
                .findAll(specification, Sort.by(Sort.Direction.DESC, "fecha"))
                .stream()
                .map(AuditLogMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseDTO> listarConFiltrosOpcionales(String entidad, AuditLog.Accion accion,
                                                                LocalDate desde, LocalDate hasta) {
        LocalDateTime fechaDesde = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime fechaHasta = hasta != null ? hasta.atTime(LocalTime.MAX) : null;
        return listarConFiltros(entidad, accion, fechaDesde, fechaHasta);
    }

    // ── INSERCIÓN (llamada desde AuditAspect) ─────────────────

    /**
     * Registra una acción en el audit log.
     * Llamado por el AuditAspect (AOP) después de cada operación CUD.
     *
     * @param entidad     nombre de la entidad (ej: "Ticket")
     * @param accion      tipo de acción (CREATE, UPDATE, DELETE, etc.)
     * @param registroId  ID del registro afectado
     * @param valorAnterior estado anterior en JSON (null para CREATE)
     * @param valorNuevo    estado nuevo en JSON (null para DELETE)
     */
    public void registrar(String entidad, AuditLog.Accion accion, Integer registroId,
                          String valorAnterior, String valorNuevo) {

        Usuario usuario = obtenerUsuarioActualONull();

        AuditLog log = AuditLog.builder()
                .entidad(entidad)
                .accion(accion)
                .registroId(registroId)
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .usuario(usuario)
                .build();

        auditLogRepository.save(log);
    }

    // ── HELPERS ───────────────────────────────────────────────

    /**
     * Obtiene el usuario actual o null si no hay autenticación (ej: job del sistema).
     */
    private Usuario obtenerUsuarioActualONull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            return null; // Acción del sistema (job, startup)
        }
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return usuarioRepository.findById(user.getId()).orElse(null);
    }
    private Specification<AuditLog> crearSpecification(String entidad, AuditLog.Accion accion,
                                                        LocalDateTime desde, LocalDateTime hasta) {
        List<Specification<AuditLog>> specs = new ArrayList<>();

        if (entidad != null && !entidad.isBlank()) {
            String entidadNormalizada = entidad.trim().toLowerCase();
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("entidad")), entidadNormalizada));
        }

        if (accion != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("accion"), accion));
        }

        if (desde != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), desde));
        }

        if (hasta != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), hasta));
        }

        return specs.stream().reduce(Specification.where(null), Specification::and);
    }
}
