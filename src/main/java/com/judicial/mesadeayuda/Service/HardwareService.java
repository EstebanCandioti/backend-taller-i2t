package com.judicial.mesadeayuda.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.HardwareRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.HardwareResponseDTO;
import com.judicial.mesadeayuda.DTO.Response.TicketResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Contrato;
import com.judicial.mesadeayuda.Entities.Hardware;
import com.judicial.mesadeayuda.Entities.Juzgado;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.HardwareMapper;
import com.judicial.mesadeayuda.Mapper.TicketMapper;
import com.judicial.mesadeayuda.Repositories.ContratoRepository;
import com.judicial.mesadeayuda.Repositories.HardwareRepository;
import com.judicial.mesadeayuda.Repositories.JuzgadoRepository;
import com.judicial.mesadeayuda.Repositories.TicketRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

@Service
@Transactional
public class HardwareService {

    private final HardwareRepository hardwareRepository;
    private final JuzgadoRepository juzgadoRepository;
    private final ContratoRepository contratoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketRepository ticketRepository;

    public HardwareService(HardwareRepository hardwareRepository,
                           JuzgadoRepository juzgadoRepository,
                           ContratoRepository contratoRepository,
                           UsuarioRepository usuarioRepository,
                           TicketRepository ticketRepository) {
        this.hardwareRepository = hardwareRepository;
        this.juzgadoRepository = juzgadoRepository;
        this.contratoRepository = contratoRepository;
        this.usuarioRepository = usuarioRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<HardwareResponseDTO> listar(Integer juzgadoId, String clase, String modelo, String ubicacion) {
        return hardwareRepository.findConFiltros(juzgadoId, clase, modelo, ubicacion).stream()
                .map(HardwareMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HardwareResponseDTO obtenerPorId(Integer id) {
        return HardwareMapper.toDTO(buscarHardware(id));
    }

    /**
     * Lista tickets asociados a un equipo de hardware.
     */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> listarTickets(Integer hardwareId) {
        // Verificar que el hardware existe
        buscarHardware(hardwareId);
        return ticketRepository.findByHardwareId(hardwareId).stream()
                .map(TicketMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Auditable(entidad = "Hardware", accion = AuditLog.Accion.CREATE)
    public HardwareResponseDTO crear(HardwareRequestDTO dto) {
        // Validar nro inventario único
        if (hardwareRepository.existsByNroInventario(dto.getNroInventario())) {
            throw new BusinessException("Ya existe un equipo con nro de inventario: " + dto.getNroInventario(),
                    HttpStatus.CONFLICT);
        }

        Juzgado juzgado = juzgadoRepository.findById(dto.getJuzgadoId())
                .orElseThrow(() -> new NotFoundException("Juzgado", dto.getJuzgadoId()));

        Contrato contrato = null;
        if (dto.getContratoId() != null) {
            contrato = contratoRepository.findById(dto.getContratoId())
                    .orElseThrow(() -> new NotFoundException("Contrato", dto.getContratoId()));
        }

        Hardware hardware = Hardware.builder()
                .nroInventario(dto.getNroInventario())
                .clase(dto.getClase())
                .marca(dto.getMarca())
                .modelo(dto.getModelo())
                .nroSerie(dto.getNroSerie())
                .ubicacionFisica(dto.getUbicacionFisica())
                .juzgado(juzgado)
                .contrato(contrato)
                .fechaAlta(LocalDate.now())
                .observaciones(dto.getObservaciones())
                .build();

        hardware = hardwareRepository.save(hardware);
        return HardwareMapper.toDTO(hardware);
    }

    @Auditable(entidad = "Hardware", accion = AuditLog.Accion.UPDATE)
    public HardwareResponseDTO editar(Integer id, HardwareRequestDTO dto) {
        Hardware hardware = buscarHardware(id);

        // Validar nro inventario único (si cambió)
        if (!hardware.getNroInventario().equals(dto.getNroInventario())
                && hardwareRepository.existsByNroInventario(dto.getNroInventario())) {
            throw new BusinessException("Ya existe un equipo con nro de inventario: " + dto.getNroInventario(),
                    HttpStatus.CONFLICT);
        }

        Juzgado juzgado = juzgadoRepository.findById(dto.getJuzgadoId())
                .orElseThrow(() -> new NotFoundException("Juzgado", dto.getJuzgadoId()));

        Contrato contrato = null;
        if (dto.getContratoId() != null) {
            contrato = contratoRepository.findById(dto.getContratoId())
                    .orElseThrow(() -> new NotFoundException("Contrato", dto.getContratoId()));
        }

        hardware.setNroInventario(dto.getNroInventario());
        hardware.setClase(dto.getClase());
        hardware.setMarca(dto.getMarca());
        hardware.setModelo(dto.getModelo());
        hardware.setNroSerie(dto.getNroSerie());
        hardware.setUbicacionFisica(dto.getUbicacionFisica());
        hardware.setJuzgado(juzgado);
        hardware.setContrato(contrato);
        hardware.setObservaciones(dto.getObservaciones());

        hardware = hardwareRepository.save(hardware);
        return HardwareMapper.toDTO(hardware);
    }

    /**
     * Soft-delete. No se puede eliminar hardware con tickets activos.
     */
    @Auditable(entidad = "Hardware", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Hardware hardware = buscarHardware(id);

        if (hardwareRepository.tieneTicketsActivos(id)) {
            throw new BusinessException(
                    "No se puede eliminar el equipo porque tiene tickets activos asociados",
                    HttpStatus.CONFLICT);
        }

        hardware.setEliminado(true);
        hardware.setFechaEliminacion(LocalDateTime.now());
        hardware.setEliminadoPor(obtenerUsuarioActual());
        hardwareRepository.save(hardware);
    }

    // ── RESTORE ───────────────────────────────────────────────

    /**
     * Restaura un hardware eliminado con soft-delete.
     * Usa query nativa para bypassear @SQLRestriction("eliminado = 0").
     */
    @Auditable(entidad = "Hardware", accion = AuditLog.Accion.RESTORE)
    public HardwareResponseDTO restaurar(Integer id) {
        Hardware hardware = hardwareRepository.findEliminadoById(id)
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró un equipo eliminado con id: " + id));

        if (!hardware.isEliminado()) {
            throw new BusinessException("El equipo no está eliminado");
        }

        hardware.setEliminado(false);
        hardware.setFechaEliminacion(null);
        hardware.setEliminadoPor(null);

        hardware = hardwareRepository.save(hardware);
        return HardwareMapper.toDTO(hardware);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private Hardware buscarHardware(Integer id) {
        return hardwareRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hardware", id));
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}