package com.judicial.mesadeayuda.Controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.judicial.mesadeayuda.DTO.Request.UsuarioRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.UsuarioResponseDTO;
import com.judicial.mesadeayuda.Service.UsuarioService;

import jakarta.validation.Valid;

/**
 * Controller de Usuarios.
 *
 * Endpoints:
 *   GET    /api/usuarios                     → Listar todos
 *   GET    /api/usuarios/{id}                → Obtener detalle
 *   GET    /api/usuarios/tecnicos-activos    → Técnicos para selector de asignación
 *   GET    /api/usuarios/buscar?q=texto      → Búsqueda libre
 *   POST   /api/usuarios                     → Crear usuario (solo Admin)
 *   PUT    /api/usuarios/{id}                → Editar usuario (solo Admin)
 *   PUT    /api/usuarios/{id}/activar        → Activar usuario (solo Admin)
 *   PUT    /api/usuarios/{id}/desactivar     → Desactivar usuario (solo Admin)
 *   DELETE /api/usuarios/{id}                → Soft-delete (solo Admin)
 *
 * Autorización:
 *   - GET: Admin y Operario (Operario solo lectura)
 *   - POST/PUT/DELETE: solo Admin
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioResponseDTO>>> listarTodos() {
        List<UsuarioResponseDTO> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(ApiResponse.success("Usuarios obtenidos", usuarios));
    }


    /**
     * Lista técnicos activos para el selector de asignación de tickets.
     */
    @GetMapping("/tecnicos-activos")
    public ResponseEntity<ApiResponse<List<UsuarioResponseDTO>>> listarTecnicosActivos() {
        List<UsuarioResponseDTO> tecnicos = usuarioService.listarTecnicosActivos();
        return ResponseEntity.ok(ApiResponse.success("Técnicos activos obtenidos", tecnicos));
    }

    /**
     * Búsqueda libre por nombre, apellido o email.
     */
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<UsuarioResponseDTO>>> buscar(
            @RequestParam String q) {
        List<UsuarioResponseDTO> usuarios = usuarioService.buscar(q);
        return ResponseEntity.ok(ApiResponse.success("Resultados de búsqueda", usuarios));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> obtenerPorId(@PathVariable Integer id) {
        UsuarioResponseDTO usuario = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario obtenido", usuario));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> crear(
            @Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO usuario = usuarioService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario creado exitosamente", usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> editar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO usuario = usuarioService.editar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Usuario actualizado", usuario));
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> activar(@PathVariable Integer id) {
        UsuarioResponseDTO usuario = usuarioService.activar(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario activado", usuario));
    }

    @PutMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> desactivar(@PathVariable Integer id) {
        UsuarioResponseDTO usuario = usuarioService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario desactivado", usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.success("Usuario eliminado"));
    }

    @PutMapping("/{id}/restore")
public ResponseEntity<ApiResponse<UsuarioResponseDTO>> restaurar(@PathVariable Integer id) {
    UsuarioResponseDTO usuario = usuarioService.restaurar(id);
    return ResponseEntity.ok(ApiResponse.success("Usuario restaurado exitosamente", usuario));
}
}
