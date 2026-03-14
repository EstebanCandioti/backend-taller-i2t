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
                .map(this::mapearContratoResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContratoResponseDTO obtenerPorId(Integer id) {
        return mapearContratoResponse(buscarContrato(id));
    }

    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> proximosAVencer() {
        return contratoRepository.findProximosAVencerV2(LocalDate.now()).stream()
                .map(this::mapearContratoResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> vencidos() {
        return contratoRepository.findByFechaFinBeforeOrderByFechaFinDesc(LocalDate.now()).stream()
                .map(this::mapearContratoResponse)
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
        sincronizarAsignacionesDirectas(contrato, dto.getHardwareIds(), dto.getSoftwareIds());
        return mapearContratoResponse(contrato);
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
        sincronizarAsignacionesDirectas(contrato, dto.getHardwareIds(), dto.getSoftwareIds());
        return mapearContratoResponse(contrato);
    }

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
        renovado.setHardware(
                original.getHardware() != null ? new ArrayList<>(original.getHardware()) : new ArrayList<>());
        renovado.setSoftwareLicencias(
                original.getSoftwareLicencias() != null ? new ArrayList<>(original.getSoftwareLicencias())
                        : new ArrayList<>());
        renovado = contratoRepository.save(renovado);

        List<Hardware> hardwareOriginal = hardwareRepository.findByContratoId(original.getId());
        if (hardwareOriginal != null && !hardwareOriginal.isEmpty()) {
            for (Hardware hardware : hardwareOriginal) {
                hardware.setContrato(renovado);
                hardwareRepository.save(hardware);
            }
        }

        List<Software> softwareOriginal = softwareRepository.findByContratoId(original.getId());
        if (softwareOriginal != null && !softwareOriginal.isEmpty()) {
            for (Software software : softwareOriginal) {
                software.setContrato(renovado);
                softwareRepository.save(software);
            }
        }

        return mapearContratoResponse(renovado);
    }

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
        return mapearContratoResponse(contrato);
    }

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
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream()
                .map(hwId -> hardwareRepository.findById(hwId)
                        .orElseThrow(() -> new NotFoundException("Hardware", hwId)))
                .collect(Collectors.toList());
    }

    private List<Software> resolverSoftware(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return ids.stream()
                .map(swId -> softwareRepository.findById(swId)
                        .orElseThrow(() -> new NotFoundException("Software", swId)))
                .collect(Collectors.toList());
    }

    private void sincronizarAsignacionesDirectas(Contrato contrato, List<Integer> hardwareIds, List<Integer> softwareIds) {
        List<Integer> hardwareSeleccionado = hardwareIds != null ? hardwareIds : new ArrayList<>();
        List<Integer> softwareSeleccionado = softwareIds != null ? softwareIds : new ArrayList<>();

        for (Hardware hardwareActual : hardwareRepository.findByContratoId(contrato.getId())) {
            if (!hardwareSeleccionado.contains(hardwareActual.getId())) {
                hardwareActual.setContrato(null);
                hardwareRepository.save(hardwareActual);
            }
        }

        for (Integer hardwareId : hardwareSeleccionado) {
            Hardware hardware = hardwareRepository.findById(hardwareId)
                    .orElseThrow(() -> new NotFoundException("Hardware", hardwareId));
            hardware.setContrato(contrato);
            hardwareRepository.save(hardware);
        }

        for (Integer softwareId : softwareSeleccionado) {
            Software software = softwareRepository.findById(softwareId)
                    .orElseThrow(() -> new NotFoundException("Software", softwareId));
            software.setContrato(contrato);
            softwareRepository.save(software);
        }
    }

    private ContratoResponseDTO mapearContratoResponse(Contrato contrato) {
        ContratoResponseDTO dto = ContratoMapper.toDTO(contrato);

        dto.setHardware(hardwareRepository.findByContratoId(contrato.getId()).stream()
                .map(hw -> ContratoResponseDTO.HardwareSimpleDTO.builder()
                        .id(hw.getId())
                        .nroInventario(hw.getNroInventario())
                        .clase(hw.getClase())
                        .marca(hw.getMarca())
                        .modelo(hw.getModelo())
                        .build())
                .collect(Collectors.toList()));

        dto.setSoftware(softwareRepository.findByContratoId(contrato.getId()).stream()
                .map(sw -> ContratoResponseDTO.SoftwareSimpleDTO.builder()
                        .id(sw.getId())
                        .nombre(sw.getNombre())
                        .proveedor(sw.getProveedor())
                        .cantidadLicencias(sw.getCantidadLicencias())
                        .build())
                .collect(Collectors.toList()));

        return dto;
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}
