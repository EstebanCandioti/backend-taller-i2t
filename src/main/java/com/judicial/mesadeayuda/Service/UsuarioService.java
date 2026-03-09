package com.judicial.mesadeayuda.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.judicial.mesadeayuda.Audit.Auditable;
import com.judicial.mesadeayuda.DTO.Request.UsuarioRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.UsuarioResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Entities.Rol;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Exceptions.BusinessException;
import com.judicial.mesadeayuda.Exceptions.NotFoundException;
import com.judicial.mesadeayuda.Mapper.UsuarioMapper;
import com.judicial.mesadeayuda.Repositories.RolRepository;
import com.judicial.mesadeayuda.Repositories.TicketRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Security.CustomUserDetails;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          TicketRepository ticketRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Integer id) {
        return UsuarioMapper.toDTO(buscarUsuario(id));
    }

    /**
     * Lista técnicos activos para el selector de asignación de tickets.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTecnicosActivos() {
        return usuarioRepository.findTecnicosActivos().stream()
                .map(UsuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Búsqueda libre por nombre, apellido o email.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> buscar(String q) {
        return usuarioRepository.buscarPorTexto(q).stream()
                .map(UsuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo usuario. Password es obligatorio al crear.
     */
    @Auditable(entidad = "Usuario", accion = AuditLog.Accion.CREATE)
    public UsuarioResponseDTO crear(UsuarioRequestDTO dto) {
        // Password obligatorio al crear
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("La contraseña es obligatoria al crear un usuario");
        }

        // Email único
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Ya existe un usuario con el email: " + dto.getEmail(),
                    HttpStatus.CONFLICT);
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new NotFoundException("Rol", dto.getRolId()));

        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .telefono(dto.getTelefono())
                .rol(rol)
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);
        return UsuarioMapper.toDTO(usuario);
    }

    /**
     * Edita un usuario. Si no envía password, no se cambia.
     */
    @Auditable(entidad = "Usuario", accion = AuditLog.Accion.UPDATE)
    public UsuarioResponseDTO editar(Integer id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarUsuario(id);

        // Email único (si cambió)
        if (!usuario.getEmail().equals(dto.getEmail())
                && usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Ya existe un usuario con el email: " + dto.getEmail(),
                    HttpStatus.CONFLICT);
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new NotFoundException("Rol", dto.getRolId()));

        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefono(dto.getTelefono());
        usuario.setRol(rol);

        // Solo cambiar password si se envía uno nuevo
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        usuario = usuarioRepository.save(usuario);
        return UsuarioMapper.toDTO(usuario);
    }

    /**
     * Activa un usuario desactivado.
     */
    @Auditable(entidad = "Usuario", accion = AuditLog.Accion.ACTIVATE)
    public UsuarioResponseDTO activar(Integer id) {
        Usuario usuario = buscarUsuario(id);
        if (usuario.isActivo()) {
            throw new BusinessException("El usuario ya está activo");
        }
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);
        return UsuarioMapper.toDTO(usuario);
    }

    /**
     * Desactiva un usuario. No se puede desactivar un técnico con tickets activos.
     */
    @Auditable(entidad = "Usuario", accion = AuditLog.Accion.DEACTIVATE)
    public UsuarioResponseDTO desactivar(Integer id) {
        Usuario usuario = buscarUsuario(id);

        if (!usuario.isActivo()) {
            throw new BusinessException("El usuario ya está desactivado");
        }

        // No desactivar técnico con tickets activos
        if ("Técnico".equals(usuario.getRol().getNombre())
                && ticketRepository.tieneTicketsActivosByTecnico(id)) {
            throw new BusinessException(
                    "No se puede desactivar al técnico porque tiene tickets activos asignados. " +
                    "Reasigne los tickets primero.",
                    HttpStatus.CONFLICT);
        }

        usuario.setActivo(false);
        usuario = usuarioRepository.save(usuario);
        return UsuarioMapper.toDTO(usuario);
    }

    /**
     * Soft-delete. No se puede eliminar un técnico con tickets activos.
     */
    @Auditable(entidad = "Usuario", accion = AuditLog.Accion.DELETE)
    public void eliminar(Integer id) {
        Usuario usuario = buscarUsuario(id);

        // No eliminar técnico con tickets activos
        if ("Técnico".equals(usuario.getRol().getNombre())
                && ticketRepository.tieneTicketsActivosByTecnico(id)) {
            throw new BusinessException(
                    "No se puede eliminar al técnico porque tiene tickets activos asignados",
                    HttpStatus.CONFLICT);
        }

        // No eliminarse a sí mismo
        CustomUserDetails currentUser = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (currentUser.getId().equals(id)) {
            throw new BusinessException("No puede eliminarse a sí mismo");
        }

        usuario.setEliminado(true);
        usuario.setActivo(false);
        usuario.setFechaEliminacion(LocalDateTime.now());
        usuario.setEliminadoPor(obtenerUsuarioActual());
        usuarioRepository.save(usuario);
    }

    //-- METODO PARA RESTAURAR -------------
    @Auditable(entidad = "Usuario", accion = AuditLog.Accion.RESTORE)
public UsuarioResponseDTO restaurar(Integer id) {
    usuarioRepository.findEliminadoById(id)
            .orElseThrow(() -> new NotFoundException(
                    "No se encontró un usuario eliminado con id: " + id));
    int updated = usuarioRepository.restore(id);
    if (updated == 0) {
        throw new BusinessException("No se pudo restaurar el usuario");
    }
    return UsuarioMapper.toDTO(usuarioRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuario", id)));
}

    // ── HELPERS ───────────────────────────────────────────────

    private Usuario buscarUsuario(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario", id));
    }

    private Usuario obtenerUsuarioActual() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return usuarioRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario", user.getId()));
    }
}