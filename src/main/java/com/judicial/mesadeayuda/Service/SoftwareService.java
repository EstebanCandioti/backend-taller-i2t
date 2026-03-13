package com.judicial.mesadeayuda.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.SoftwareRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.SoftwareResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Contrato;
import com.judicial.mesadeayuda.Entities.Hardware;
import com.judicial.mesadeayuda.Entities.Juzgado;
import com.judicial.mesadeayuda.Entities.Software;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.SoftwareMapper;
import com.judicial.mesadeayuda.Repositories.ContratoRepository;
import com.judicial.mesadeayuda.Repositories.HardwareRepository;
import com.judicial.mesadeayuda.Repositories.JuzgadoRepository;
import com.judicial.mesadeayuda.Repositories.SoftwareRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

@Service
@Transactional
public class SoftwareService {

    private final SoftwareRepository softwareRepository;
    private final ContratoRepository contratoRepository;
    private final JuzgadoRepository juzgadoRepository;
    private final HardwareRepository hardwareRepository;
    private final UsuarioRepository usuarioRepository;

    public SoftwareService(SoftwareRepository softwareRepository,
            ContratoRepository contratoRepository,
            JuzgadoRepository juzgadoRepository,
            HardwareRepository hardwareRepository,
            UsuarioRepository usuarioRepository) {
        this.softwareRepository = softwareRepository;
        this.contratoRepository = contratoRepository;
        this.juzgadoRepository = juzgadoRepository;
        this.hardwareRepository = hardwareRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<SoftwareResponseDTO> listar(Integer contratoId, Integer juzgadoId, String proveedor) {
        return softwareRepository.findConFiltros(contratoId, juzgadoId, proveedor).stream()
                .map(SoftwareMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SoftwareResponseDTO obtenerPorId(Integer id) {
        return SoftwareMapper.toDTO(buscarSoftware(id));
    }

    /**
     * Licencias próximas a vencer en los próximos N días.
     */
    @Transactional(readOnly = true)
    public List<SoftwareResponseDTO> proximasAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(dias);
        return softwareRepository.findProximasAVencer(hoy, fechaLimite).stream()
                .map(SoftwareMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea software. Contrato es OBLIGATORIO.
     */
    @Auditable(entidad = "Software", accion = AuditLog.Accion.CREATE)
    public SoftwareResponseDTO crear(SoftwareRequestDTO dto) {
        Contrato contrato = contratoRepository.findById(dto.getContratoId())
                .orElseThrow(() -> new NotFoundException("Contrato", dto.getContratoId()));

        Juzgado juzgado = null;
        if (dto.getJuzgadoId() != null) {
            juzgado = juzgadoRepository.findById(dto.getJuzgadoId())
                    .orElseThrow(() -> new NotFoundException("Juzgado", dto.getJuzgadoId()));
        }

        Hardware hardware = null;
        if (dto.getHardwareId() != null) {
            hardware = hardwareRepository.findById(dto.getHardwareId())
                    .orElseThrow(() -> new NotFoundException("Hardware", dto.getHardwareId()));
        }

        Software software = Software.builder()
                .nombre(dto.getNombre())
                .proveedor(dto.getProveedor())
                .cantidadLicencias(dto.getCantidadLicencias())
                .licenciasEnUso(0)
                .fechaVencimiento(dto.getFechaVencimiento())
                .contrato(contrato)
                .juzgado(juzgado)
                .hardware(hardware)
                .observaciones(dto.getObservaciones())
                .build();

        software = softwareRepository.save(software);
        return SoftwareMapper.toDTO(software);
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.UPDATE)
    public SoftwareResponseDTO editar(Integer id, SoftwareRequestDTO dto) {
        Software software = buscarSoftware(id);

        Contrato contrato = contratoRepository.findById(dto.getContratoId())
                .orElseThrow(() -> new NotFoundException("Contrato", dto.getContratoId()));

        Juzgado juzgado = null;
        if (dto.getJuzgadoId() != null) {
            juzgado = juzgadoRepository.findById(dto.getJuzgadoId())
                    .orElseThrow(() -> new NotFoundException("Juzgado", dto.getJuzgadoId()));
        }

        Hardware hardware = null;
        if (dto.getHardwareId() != null) {
            hardware = hardwareRepository.findById(dto.getHardwareId())
                    .orElseThrow(() -> new NotFoundException("Hardware", dto.getHardwareId()));
        }

        // Validar que las licencias en uso no superen la nueva cantidad
        if (dto.getCantidadLicencias() < software.getLicenciasEnUso()) {
            throw new BusinessException(
                    "La cantidad de licencias (" + dto.getCantidadLicencias() +
                            ") no puede ser menor a las licencias en uso (" + software.getLicenciasEnUso() + ")");
        }

        // ── Control de licenciasEnUso por cambio de asignación ──
        boolean teniaAsignacion = software.getHardware() != null || software.getJuzgado() != null;
        boolean tieneAsignacion = hardware != null || dto.getJuzgadoId() != null;

        if (!teniaAsignacion && tieneAsignacion) {
            // No tenía asignación → ahora sí: incrementar
            int nuevasEnUso = software.getLicenciasEnUso() + 1;
            if (nuevasEnUso > software.getCantidadLicencias()) {
                throw new BusinessException("No hay licencias disponibles");
            }
            software.setLicenciasEnUso(nuevasEnUso);
        } else if (teniaAsignacion && !tieneAsignacion) {
            // Tenía asignación → ahora no: decrementar
            software.setLicenciasEnUso(Math.max(0, software.getLicenciasEnUso() - 1));
        }
        // Si cambió de una asignación a otra, o no cambió: no tocar licenciasEnUso

        software.setNombre(dto.getNombre());
        software.setProveedor(dto.getProveedor());
        software.setCantidadLicencias(dto.getCantidadLicencias());
        software.setFechaVencimiento(dto.getFechaVencimiento());
        software.setContrato(contrato);
        software.setJuzgado(juzgado);
        software.setHardware(hardware);
        software.setObservaciones(dto.getObservaciones());

        software = softwareRepository.save(software);
        return SoftwareMapper.toDTO(software);
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Software software = buscarSoftware(id);

        software.setEliminado(true);
        software.setFechaEliminacion(LocalDateTime.now());
        software.setEliminadoPor(obtenerUsuarioActual());
        softwareRepository.save(software);
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.RESTORE)
    public SoftwareResponseDTO restaurar(Integer id) {
        Software software = softwareRepository.findEliminadoById(id)
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró un software eliminado con id: " + id));

        if (!software.isEliminado()) {
            throw new BusinessException("El software no está eliminado");
        }

        software.setEliminado(false);
        software.setFechaEliminacion(null);
        software.setEliminadoPor(null);

        software = softwareRepository.save(software);
        return SoftwareMapper.toDTO(software);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private Software buscarSoftware(Integer id) {
        return softwareRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Software", id));
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}