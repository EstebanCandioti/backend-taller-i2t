package com.judicial.mesadeayuda.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.SoftwareRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.PaginatedResponse;
import com.judicial.mesadeayuda.DTO.Response.SoftwareResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Contrato;
import com.judicial.mesadeayuda.Entities.Hardware;
import com.judicial.mesadeayuda.Entities.Juzgado;
import com.judicial.mesadeayuda.Entities.Software;
import com.judicial.mesadeayuda.Entities.SoftwareHardware;
import com.judicial.mesadeayuda.Entities.SoftwareJuzgado;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.SoftwareMapper;
import com.judicial.mesadeayuda.Repositories.ContratoRepository;
import com.judicial.mesadeayuda.Repositories.HardwareRepository;
import com.judicial.mesadeayuda.Repositories.JuzgadoRepository;
import com.judicial.mesadeayuda.Repositories.SoftwareHardwareRepository;
import com.judicial.mesadeayuda.Repositories.SoftwareJuzgadoRepository;
import com.judicial.mesadeayuda.Repositories.SoftwareRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

import jakarta.persistence.EntityManager;

@Service
@Transactional
public class SoftwareService {

    private final SoftwareRepository softwareRepository;
    private final ContratoRepository contratoRepository;
    private final JuzgadoRepository juzgadoRepository;
    private final HardwareRepository hardwareRepository;
    private final SoftwareHardwareRepository softwareHardwareRepository;
    private final SoftwareJuzgadoRepository softwareJuzgadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;

    public SoftwareService(SoftwareRepository softwareRepository,
            ContratoRepository contratoRepository,
            JuzgadoRepository juzgadoRepository,
            HardwareRepository hardwareRepository,
            SoftwareHardwareRepository softwareHardwareRepository,
            SoftwareJuzgadoRepository softwareJuzgadoRepository,
            UsuarioRepository usuarioRepository,
            AuditLogService auditLogService,
            EntityManager entityManager) {
        this.softwareRepository = softwareRepository;
        this.contratoRepository = contratoRepository;
        this.juzgadoRepository = juzgadoRepository;
        this.hardwareRepository = hardwareRepository;
        this.softwareHardwareRepository = softwareHardwareRepository;
        this.softwareJuzgadoRepository = softwareJuzgadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SoftwareResponseDTO> listar(Integer contratoId, Integer juzgadoId,
                                                          String proveedor, Pageable pageable) {
        Page<Software> page = softwareRepository.findConFiltros(contratoId, juzgadoId, proveedor, pageable);
        return PaginatedResponse.from(page.map(SoftwareMapper::toDTO));
    }

    @Transactional(readOnly = true)
    public SoftwareResponseDTO obtenerPorId(Integer id) {
        return SoftwareMapper.toDTO(buscarSoftware(id));
    }

    @Transactional(readOnly = true)
    public List<SoftwareResponseDTO> proximasAVencer(int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(dias);
        return softwareRepository.findProximasAVencer(hoy, fechaLimite).stream()
                .map(SoftwareMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.CREATE)
    public SoftwareResponseDTO crear(SoftwareRequestDTO dto) {
        Contrato contrato = buscarContrato(dto.getContratoId());
        List<Juzgado> juzgados = resolverJuzgados(dto.getJuzgadoIds());
        List<Hardware> hardware = resolverHardware(dto.getHardwareIds());

        int licenciasEnUso = calcularLicenciasEnUso(hardware.size());
        validarLicenciasDisponibles(dto.getCantidadLicencias(), licenciasEnUso);

        Software software = Software.builder()
                .nombre(dto.getNombre())
                .proveedor(dto.getProveedor())
                .cantidadLicencias(dto.getCantidadLicencias())
                .licenciasEnUso(licenciasEnUso)
                .fechaVencimiento(dto.getFechaVencimiento())
                .contrato(contrato)
                .observaciones(dto.getObservaciones())
                .build();

        software = softwareRepository.saveAndFlush(software);

        sincronizarHardware(software, hardware);
        sincronizarJuzgados(software, juzgados);

        return SoftwareMapper.toDTO(recargarSoftware(software.getId()));
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.UPDATE)
    public SoftwareResponseDTO editar(Integer id, SoftwareRequestDTO dto) {
        Software software = buscarSoftware(id);
        Contrato contrato = buscarContrato(dto.getContratoId());
        List<Juzgado> juzgados = resolverJuzgados(dto.getJuzgadoIds());
        List<Hardware> hardware = resolverHardware(dto.getHardwareIds());

        int licenciasEnUso = calcularLicenciasEnUso(hardware.size());
        validarLicenciasDisponibles(dto.getCantidadLicencias(), licenciasEnUso);

        software.setNombre(dto.getNombre());
        software.setProveedor(dto.getProveedor());
        software.setCantidadLicencias(dto.getCantidadLicencias());
        software.setLicenciasEnUso(licenciasEnUso);
        software.setFechaVencimiento(dto.getFechaVencimiento());
        software.setContrato(contrato);
        software.setObservaciones(dto.getObservaciones());
        softwareRepository.save(software);

        sincronizarHardware(software, hardware);
        sincronizarJuzgados(software, juzgados);

        return SoftwareMapper.toDTO(recargarSoftware(software.getId()));
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.UPDATE)
    public SoftwareResponseDTO actualizarHardware(Integer id, List<Integer> hardwareIds) {
        Software software = buscarSoftware(id);
        List<Hardware> hardware = resolverHardware(hardwareIds);

        int licenciasEnUso = calcularLicenciasEnUso(hardware.size());
        validarLicenciasDisponibles(software.getCantidadLicencias(), licenciasEnUso);

        software.setLicenciasEnUso(licenciasEnUso);
        softwareRepository.save(software);

        sincronizarHardware(software, hardware);

        return SoftwareMapper.toDTO(recargarSoftware(software.getId()));
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.UPDATE)
    public SoftwareResponseDTO actualizarJuzgados(Integer id, List<Integer> juzgadoIds) {
        Software software = buscarSoftware(id);
        List<Juzgado> juzgados = resolverJuzgados(juzgadoIds);
        sincronizarJuzgados(software, juzgados);
        return SoftwareMapper.toDTO(recargarSoftware(software.getId()));
    }

    @Auditable(entidad = "Software", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Software software = buscarSoftware(id);

        for (SoftwareHardware vinculo : softwareHardwareRepository.findBySoftwareId(id)) {
            softDeleteVinculoHardware(vinculo);
        }
        for (SoftwareJuzgado vinculo : softwareJuzgadoRepository.findBySoftwareIdAndEliminadoFalse(id)) {
            softDeleteVinculoJuzgado(vinculo);
        }

        software.setLicenciasEnUso(0);
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

    private Software buscarSoftware(Integer id) {
        return softwareRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Software", id));
    }

    private Software recargarSoftware(Integer id) {
        softwareRepository.flush();
        entityManager.clear();
        return buscarSoftware(id);
    }

    private Contrato buscarContrato(Integer id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contrato", id));
    }

    private List<Juzgado> resolverJuzgados(List<Integer> juzgadoIds) {
        if (juzgadoIds == null || juzgadoIds.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> idsUnicos = new LinkedHashSet<>(juzgadoIds);
        return idsUnicos.stream()
                .map(juzgadoId -> juzgadoRepository.findById(juzgadoId)
                        .orElseThrow(() -> new NotFoundException("Juzgado", juzgadoId)))
                .collect(Collectors.toList());
    }

    private List<Hardware> resolverHardware(List<Integer> hardwareIds) {
        if (hardwareIds == null || hardwareIds.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> idsUnicos = new LinkedHashSet<>(hardwareIds);
        return idsUnicos.stream()
                .map(hardwareId -> hardwareRepository.findById(hardwareId)
                        .orElseThrow(() -> new NotFoundException("Hardware", hardwareId)))
                .collect(Collectors.toList());
    }

    private void validarLicenciasDisponibles(Integer cantidadLicencias, int licenciasEnUso) {
        if (licenciasEnUso > cantidadLicencias) {
            throw new BusinessException("No hay licencias disponibles");
        }
    }

    private int calcularLicenciasEnUso(int hardwareAsignado) {
        return hardwareAsignado;
    }

    private void sincronizarHardware(Software software, List<Hardware> hardwareNuevo) {
        Set<Integer> hardwareIdsNuevo = hardwareNuevo.stream()
                .map(Hardware::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SoftwareHardware> vinculosActivos = softwareHardwareRepository.findBySoftwareId(software.getId());
        Set<Integer> hardwareIdsActual = vinculosActivos.stream()
                .map(vinculo -> vinculo.getHardware().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (SoftwareHardware vinculo : vinculosActivos) {
            if (!hardwareIdsNuevo.contains(vinculo.getHardware().getId())) {
                softDeleteVinculoHardware(vinculo);
            }
        }

        for (Hardware equipo : hardwareNuevo) {
            if (!hardwareIdsActual.contains(equipo.getId())) {
                crearOReactivarVinculoHardware(software, equipo);
            }
        }
    }

    private void sincronizarJuzgados(Software software, List<Juzgado> juzgadosNuevos) {
        Set<Integer> juzgadoIdsNuevo = juzgadosNuevos.stream()
                .map(Juzgado::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SoftwareJuzgado> vinculosActivos = softwareJuzgadoRepository
                .findBySoftwareIdAndEliminadoFalse(software.getId());
        Set<Integer> juzgadoIdsActual = vinculosActivos.stream()
                .map(vinculo -> vinculo.getJuzgado().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (SoftwareJuzgado vinculo : vinculosActivos) {
            if (!juzgadoIdsNuevo.contains(vinculo.getJuzgado().getId())) {
                softDeleteVinculoJuzgado(vinculo);
            }
        }

        for (Juzgado juzgado : juzgadosNuevos) {
            if (!juzgadoIdsActual.contains(juzgado.getId())) {
                crearOReactivarVinculoJuzgado(software, juzgado);
            }
        }
    }

    private void crearOReactivarVinculoHardware(Software software, Hardware hardware) {
        SoftwareHardware vinculo = softwareHardwareRepository
                .findAnyBySoftwareIdAndHardwareId(software.getId(), hardware.getId())
                .orElse(null);

        if (vinculo == null) {
            SoftwareHardware nuevoVinculo = SoftwareHardware.builder()
                    .software(software)
                    .hardware(hardware)
                    .build();
            nuevoVinculo = softwareHardwareRepository.save(nuevoVinculo);
            auditLogService.registrar("SoftwareHardware", AuditLog.Accion.CREATE, nuevoVinculo.getId(),
                    null, serializarVinculoHardware(nuevoVinculo));
            return;
        }

        if (!vinculo.isEliminado()) {
            return;
        }

        String valorAnterior = serializarVinculoHardware(vinculo);
        vinculo.setEliminado(false);
        vinculo.setFechaEliminacion(null);
        vinculo.setEliminadoPor(null);
        vinculo.setFechaAsignacion(LocalDateTime.now());
        softwareHardwareRepository.save(vinculo);
        auditLogService.registrar("SoftwareHardware", AuditLog.Accion.RESTORE, vinculo.getId(),
                valorAnterior, serializarVinculoHardware(vinculo));
    }

    private void crearOReactivarVinculoJuzgado(Software software, Juzgado juzgado) {
        SoftwareJuzgado vinculo = softwareJuzgadoRepository
                .findAnyBySoftwareIdAndJuzgadoId(software.getId(), juzgado.getId())
                .orElse(null);

        if (vinculo == null) {
            SoftwareJuzgado nuevoVinculo = SoftwareJuzgado.builder()
                    .software(software)
                    .juzgado(juzgado)
                    .build();
            nuevoVinculo = softwareJuzgadoRepository.save(nuevoVinculo);
            auditLogService.registrar("SoftwareJuzgado", AuditLog.Accion.CREATE, nuevoVinculo.getId(),
                    null, serializarVinculoJuzgado(nuevoVinculo));
            return;
        }

        if (!vinculo.isEliminado()) {
            return;
        }

        String valorAnterior = serializarVinculoJuzgado(vinculo);
        vinculo.setEliminado(false);
        vinculo.setFechaEliminacion(null);
        vinculo.setEliminadoPor(null);
        vinculo.setFechaAsignacion(LocalDateTime.now());
        softwareJuzgadoRepository.save(vinculo);
        auditLogService.registrar("SoftwareJuzgado", AuditLog.Accion.RESTORE, vinculo.getId(),
                valorAnterior, serializarVinculoJuzgado(vinculo));
    }

    private void softDeleteVinculoHardware(SoftwareHardware vinculo) {
        String valorAnterior = serializarVinculoHardware(vinculo);
        vinculo.setEliminado(true);
        vinculo.setFechaEliminacion(LocalDateTime.now());
        vinculo.setEliminadoPor(obtenerUsuarioActual());
        softwareHardwareRepository.save(vinculo);
        auditLogService.registrar("SoftwareHardware", AuditLog.Accion.DELETE, vinculo.getId(),
                valorAnterior, null);
    }

    private void softDeleteVinculoJuzgado(SoftwareJuzgado vinculo) {
        String valorAnterior = serializarVinculoJuzgado(vinculo);
        vinculo.setEliminado(true);
        vinculo.setFechaEliminacion(LocalDateTime.now());
        vinculo.setEliminadoPor(obtenerUsuarioActual());
        softwareJuzgadoRepository.save(vinculo);
        auditLogService.registrar("SoftwareJuzgado", AuditLog.Accion.DELETE, vinculo.getId(),
                valorAnterior, null);
    }

    private String serializarVinculoHardware(SoftwareHardware vinculo) {
        return String.format(
                "{\"id\":%d,\"softwareId\":%d,\"hardwareId\":%d,\"eliminado\":%s}",
                vinculo.getId(),
                vinculo.getSoftware().getId(),
                vinculo.getHardware().getId(),
                vinculo.isEliminado());
    }

    private String serializarVinculoJuzgado(SoftwareJuzgado vinculo) {
        return String.format(
                "{\"id\":%d,\"softwareId\":%d,\"juzgadoId\":%d,\"eliminado\":%s}",
                vinculo.getId(),
                vinculo.getSoftware().getId(),
                vinculo.getJuzgado().getId(),
                vinculo.isEliminado());
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}
