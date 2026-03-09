package com.judicial.mesadeayuda.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.CircunscripcionRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.CircunscripcionResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Circunscripcion;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.CircunscripcionMapper;
import com.judicial.mesadeayuda.Repositories.CircunscripcionRepository;
import com.judicial.mesadeayuda.Repositories.JuzgadoRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

@Service
@Transactional
public class CircunscripcionService {

    private final CircunscripcionRepository circunscripcionRepository;
    private final JuzgadoRepository juzgadoRepository;
    private final UsuarioRepository usuarioRepository;

    public CircunscripcionService(CircunscripcionRepository circunscripcionRepository,
                                  JuzgadoRepository juzgadoRepository,
                                  UsuarioRepository usuarioRepository) {
        this.circunscripcionRepository = circunscripcionRepository;
        this.juzgadoRepository = juzgadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<CircunscripcionResponseDTO> listarTodas() {
        return circunscripcionRepository.findAll().stream()
                .map(CircunscripcionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CircunscripcionResponseDTO obtenerPorId(Integer id) {
        return CircunscripcionMapper.toDTO(buscarCircunscripcion(id));
    }

    @Auditable(entidad = "Circunscripcion", accion = AuditLog.Accion.CREATE)
    public CircunscripcionResponseDTO crear(CircunscripcionRequestDTO dto) {
        if (circunscripcionRepository.existsByNombre(dto.getNombre())) {
            throw new BusinessException("Ya existe una circunscripción con el nombre: " + dto.getNombre(),
                    HttpStatus.CONFLICT);
        }

        Circunscripcion circ = Circunscripcion.builder()
                .nombre(dto.getNombre())
                .distritoJudicial(dto.getDistritoJudicial())
                .build();

        circ = circunscripcionRepository.save(circ);
        return CircunscripcionMapper.toDTO(circ);
    }

    @Auditable(entidad = "Circunscripcion", accion = AuditLog.Accion.UPDATE)
    public CircunscripcionResponseDTO editar(Integer id, CircunscripcionRequestDTO dto) {
        Circunscripcion circ = buscarCircunscripcion(id);

        // Validar nombre único (si cambió)
        if (!circ.getNombre().equals(dto.getNombre())
                && circunscripcionRepository.existsByNombre(dto.getNombre())) {
            throw new BusinessException("Ya existe una circunscripción con el nombre: " + dto.getNombre(),
                    HttpStatus.CONFLICT);
        }

        circ.setNombre(dto.getNombre());
        circ.setDistritoJudicial(dto.getDistritoJudicial());

        circ = circunscripcionRepository.save(circ);
        return CircunscripcionMapper.toDTO(circ);
    }

    /**
     * Soft-delete. No se puede eliminar si tiene juzgados asociados.
     */
    @Auditable(entidad = "Circunscripcion", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Circunscripcion circ = buscarCircunscripcion(id);

        // Verificar si tiene juzgados
        if (!juzgadoRepository.findConFiltros(id, null, null).isEmpty()) {
            throw new BusinessException(
                    "No se puede eliminar la circunscripción porque tiene juzgados asociados",
                    HttpStatus.CONFLICT);
        }

        circ.setEliminado(true);
        circ.setFechaEliminacion(LocalDateTime.now());
        circ.setEliminadoPor(obtenerUsuarioActual());
        circunscripcionRepository.save(circ);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private Circunscripcion buscarCircunscripcion(Integer id) {
        return circunscripcionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Circunscripción", id));
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}