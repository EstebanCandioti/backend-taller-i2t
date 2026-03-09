package com.judicial.mesadeayuda.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.JuzgadoRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.JuzgadoResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Circunscripcion;
import com.judicial.mesadeayuda.Entities.Juzgado;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.JuzgadoMapper;
import com.judicial.mesadeayuda.Repositories.CircunscripcionRepository;
import com.judicial.mesadeayuda.Repositories.JuzgadoRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

@Service
@Transactional
public class JuzgadoService {

    private final JuzgadoRepository juzgadoRepository;
    private final CircunscripcionRepository circunscripcionRepository;
    private final UsuarioRepository usuarioRepository;

    public JuzgadoService(JuzgadoRepository juzgadoRepository,
                          CircunscripcionRepository circunscripcionRepository,
                          UsuarioRepository usuarioRepository) {
        this.juzgadoRepository = juzgadoRepository;
        this.circunscripcionRepository = circunscripcionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<JuzgadoResponseDTO> listar(Integer circunscripcionId, String ciudad, String fuero) {
        return juzgadoRepository.findConFiltros(circunscripcionId, ciudad, fuero).stream()
                .map(JuzgadoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JuzgadoResponseDTO obtenerPorId(Integer id) {
        return JuzgadoMapper.toDTO(buscarJuzgado(id));
    }

    @Auditable(entidad = "Juzgado", accion = AuditLog.Accion.CREATE)
    public JuzgadoResponseDTO crear(JuzgadoRequestDTO dto) {
        Circunscripcion circunscripcion = circunscripcionRepository.findById(dto.getCircunscripcionId())
                .orElseThrow(() -> new NotFoundException("Circunscripción", dto.getCircunscripcionId()));

        Juzgado juzgado = Juzgado.builder()
                .nombre(dto.getNombre())
                .fuero(dto.getFuero())
                .ciudad(dto.getCiudad())
                .edificio(dto.getEdificio())
                .circunscripcion(circunscripcion)
                .build();

        juzgado = juzgadoRepository.save(juzgado);
        return JuzgadoMapper.toDTO(juzgado);
    }

    @Auditable(entidad = "Juzgado", accion = AuditLog.Accion.UPDATE)
    public JuzgadoResponseDTO editar(Integer id, JuzgadoRequestDTO dto) {
        Juzgado juzgado = buscarJuzgado(id);

        Circunscripcion circunscripcion = circunscripcionRepository.findById(dto.getCircunscripcionId())
                .orElseThrow(() -> new NotFoundException("Circunscripción", dto.getCircunscripcionId()));

        juzgado.setNombre(dto.getNombre());
        juzgado.setFuero(dto.getFuero());
        juzgado.setCiudad(dto.getCiudad());
        juzgado.setEdificio(dto.getEdificio());
        juzgado.setCircunscripcion(circunscripcion);

        juzgado = juzgadoRepository.save(juzgado);
        return JuzgadoMapper.toDTO(juzgado);
    }

    /**
     * Soft-delete. No se puede eliminar un juzgado con hardware, software o tickets activos.
     */
    @Auditable(entidad = "Juzgado", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Juzgado juzgado = buscarJuzgado(id);

        if (juzgadoRepository.tieneHardwareActivo(id)) {
            throw new BusinessException(
                    "No se puede eliminar el juzgado porque tiene hardware asociado",
                    HttpStatus.CONFLICT);
        }
        if (juzgadoRepository.tieneSoftwareActivo(id)) {
            throw new BusinessException(
                    "No se puede eliminar el juzgado porque tiene software asociado",
                    HttpStatus.CONFLICT);
        }
        if (juzgadoRepository.tieneTicketsActivos(id)) {
            throw new BusinessException(
                    "No se puede eliminar el juzgado porque tiene tickets activos",
                    HttpStatus.CONFLICT);
        }

        juzgado.setEliminado(true);
        juzgado.setFechaEliminacion(LocalDateTime.now());
        juzgado.setEliminadoPor(obtenerUsuarioActual());
        juzgadoRepository.save(juzgado);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private Juzgado buscarJuzgado(Integer id) {
        return juzgadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Juzgado", id));
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}