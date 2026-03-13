package com.judicial.mesadeayuda.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.ContratoRenovarRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.ContratoRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ContratoResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Contrato;
import com.judicial.mesadeayuda.Entities.Hardware;
import com.judicial.mesadeayuda.Entities.Software;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.ContratoMapper;
import com.judicial.mesadeayuda.Repositories.ContratoRepository;
import com.judicial.mesadeayuda.Repositories.HardwareRepository;
import com.judicial.mesadeayuda.Repositories.SoftwareRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

@Service
@Transactional
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final HardwareRepository hardwareRepository;
    private final SoftwareRepository softwareRepository;
    private final UsuarioRepository usuarioRepository;

    public ContratoService(ContratoRepository contratoRepository,
            HardwareRepository hardwareRepository,
            SoftwareRepository softwareRepository,
            UsuarioRepository usuarioRepository) {
        this.contratoRepository = contratoRepository;
        this.hardwareRepository = hardwareRepository;
        this.softwareRepository = softwareRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listarTodos() {
        return contratoRepository.findAll().stream()
                .map(ContratoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContratoResponseDTO obtenerPorId(Integer id) {
        return ContratoMapper.toDTO(buscarContrato(id));
    }

    /**
     * Contratos próximos a vencer según su configuración individual de días de
     * alerta.
     */
    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> proximosAVencer() {
        return contratoRepository.findProximosAVencerV2(LocalDate.now()).stream()
                .map(ContratoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Contratos ya vencidos.
     */
    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> vencidos() {
        return contratoRepository.findByFechaFinBeforeOrderByFechaFinDesc(LocalDate.now()).stream()
                .map(ContratoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Auditable(entidad = "Contrato", accion = AuditLog.Accion.CREATE)
    public ContratoResponseDTO crear(ContratoRequestDTO dto) {
        validarFechas(dto.getFechaInicio(), dto.getFechaFin());

        Contrato contrato = Contrato.builder()
                .nombre(dto.getNombre())
                .proveedor(dto.getProveedor())
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .cobertura(dto.getCobertura())
                .monto(dto.getMonto())
                .diasAlertaVencimiento(
                        dto.getDiasAlertaVencimiento() != null ? dto.getDiasAlertaVencimiento() : 30)
                .observaciones(dto.getObservaciones())
                .hardware(resolverHardware(dto.getHardwareIds()))
                .softwareLicencias(resolverSoftware(dto.getSoftwareIds()))
                .build();

        contrato = contratoRepository.save(contrato);
        return ContratoMapper.toDTO(contrato);
    }

    @Auditable(entidad = "Contrato", accion = AuditLog.Accion.UPDATE)
    public ContratoResponseDTO editar(Integer id, ContratoRequestDTO dto) {
        Contrato contrato = buscarContrato(id);
        validarFechas(dto.getFechaInicio(), dto.getFechaFin());

        contrato.setNombre(dto.getNombre());
        contrato.setProveedor(dto.getProveedor());
        contrato.setFechaInicio(dto.getFechaInicio());
        contrato.setFechaFin(dto.getFechaFin());
        contrato.setCobertura(dto.getCobertura());
        contrato.setMonto(dto.getMonto());
        if (dto.getDiasAlertaVencimiento() != null) {
            contrato.setDiasAlertaVencimiento(dto.getDiasAlertaVencimiento());
        }
        contrato.setObservaciones(dto.getObservaciones());
        contrato.setHardware(resolverHardware(dto.getHardwareIds()));
        contrato.setSoftwareLicencias(resolverSoftware(dto.getSoftwareIds()));

        contrato = contratoRepository.save(contrato);
        return ContratoMapper.toDTO(contrato);
    }

    // ── RENOVAR ───────────────────────────────────────────────

    /**
     * Renueva un contrato existente.
     *
     * Crea un nuevo contrato copiando los datos del original (proveedor, cobertura,
     * diasAlertaVencimiento) y sobreescribiendo fechas, monto y observaciones
     * con los del DTO de renovación.
     *
     * Reasigna automáticamente todo el hardware y software vinculado
     * al contrato original hacia el contrato nuevo.
     */
    @Auditable(entidad = "Contrato", accion = AuditLog.Accion.CREATE)
    public ContratoResponseDTO renovar(Integer id, ContratoRenovarRequestDTO dto) {
        Contrato original = buscarContrato(id);

        validarFechas(dto.getFechaInicio(), dto.getFechaFin());

        if (dto.getFechaInicio().isBefore(original.getFechaFin())) {
            throw new BusinessException(
                    "La fecha de inicio de la renovación no puede ser anterior a la fecha de fin del contrato original ("
                            + original.getFechaFin() + ")");
        }

        Contrato renovado = Contrato.builder()
                .nombre(original.getNombre() + " (Renovación)")
                .proveedor(original.getProveedor())
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .cobertura(original.getCobertura())
                .monto(dto.getMonto() != null ? dto.getMonto() : original.getMonto())
                .diasAlertaVencimiento(original.getDiasAlertaVencimiento())
                .observaciones(dto.getObservaciones() != null
                        ? dto.getObservaciones()
                        : "Renovación del contrato #" + original.getId() + " - " + original.getNombre())
                .build();

        renovado = contratoRepository.save(renovado);

        // Reasignar hardware por contrato_id directo
        List<Hardware> hardwareOriginal = hardwareRepository.findByContratoId(original.getId());
        if (hardwareOriginal != null && !hardwareOriginal.isEmpty()) {
            for (Hardware hardware : hardwareOriginal) {
                hardware.setContrato(renovado);
                hardwareRepository.save(hardware);
            }
        }

        // Reasignar software por contrato_id directo
        List<Software> softwareOriginal = softwareRepository.findByContratoId(original.getId());
        if (softwareOriginal != null && !softwareOriginal.isEmpty()) {
            for (Software software : softwareOriginal) {
                software.setContrato(renovado);
                softwareRepository.save(software);
            }
        }

        return ContratoMapper.toDTO(renovado);
    }

    /**
     * Soft-delete. No se puede eliminar un contrato con software activo vinculado.
     */
    @Auditable(entidad = "Contrato", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Contrato contrato = buscarContrato(id);

        if (contratoRepository.tieneSoftwareActivo(id)) {
            throw new BusinessException(
                    "No se puede eliminar el contrato porque tiene software activo vinculado. " +
                            "Reasigne o elimine el software primero.",
                    HttpStatus.CONFLICT);
        }

        contrato.setEliminado(true);
        contrato.setFechaEliminacion(LocalDateTime.now());
        contrato.setEliminadoPor(obtenerUsuarioActual());
        contratoRepository.save(contrato);
    }

    @Auditable(entidad = "Contrato", accion = AuditLog.Accion.RESTORE)
    public ContratoResponseDTO restaurar(Integer id) {
        Contrato contrato = contratoRepository.findEliminadoById(id)
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró un contrato eliminado con id: " + id));

        if (!contrato.isEliminado()) {
            throw new BusinessException("El contrato no está eliminado");
        }

        contrato.setEliminado(false);
        contrato.setFechaEliminacion(null);
        contrato.setEliminadoPor(null);

        contrato = contratoRepository.save(contrato);
        return ContratoMapper.toDTO(contrato);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private Contrato buscarContrato(Integer id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contrato", id));
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private List<Hardware> resolverHardware(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return new ArrayList<>();
        return ids.stream()
                .map(hwId -> hardwareRepository.findById(hwId)
                        .orElseThrow(() -> new NotFoundException("Hardware", hwId)))
                .collect(Collectors.toList());
    }

    private List<Software> resolverSoftware(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return new ArrayList<>();
        return ids.stream()
                .map(swId -> softwareRepository.findById(swId)
                        .orElseThrow(() -> new NotFoundException("Software", swId)))
                .collect(Collectors.toList());
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}