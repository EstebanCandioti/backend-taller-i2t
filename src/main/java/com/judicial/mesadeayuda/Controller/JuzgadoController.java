package com.judicial.mesadeayuda.Controller;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.judicial.mesadeayuda.DTO.Request.JuzgadoRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.JuzgadoResponseDTO;
import com.judicial.mesadeayuda.Service.JuzgadoService;

import java.util.List;

/**
 * Controller de Juzgados.
 *
 * Endpoints:
 *   GET    /api/juzgados             → Listar con filtros
 *   GET    /api/juzgados/{id}        → Obtener detalle
 *   POST   /api/juzgados             → Crear juzgado
 *   PUT    /api/juzgados/{id}        → Editar juzgado
 *   DELETE /api/juzgados/{id}        → Soft-delete
 *
 * Autorización: solo Admin y Operario.
 */
@RestController
@RequestMapping("/api/juzgados")
public class JuzgadoController {

    private final JuzgadoService juzgadoService;

    public JuzgadoController(JuzgadoService juzgadoService) {
        this.juzgadoService = juzgadoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JuzgadoResponseDTO>>> listar(
            @RequestParam(required = false) Integer circunscripcionId,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String fuero) {

        List<JuzgadoResponseDTO> juzgados = juzgadoService.listar(circunscripcionId, ciudad, fuero);
        return ResponseEntity.ok(ApiResponse.success("Juzgados obtenidos", juzgados));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JuzgadoResponseDTO>> obtenerPorId(@PathVariable Integer id) {
        JuzgadoResponseDTO juzgado = juzgadoService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Juzgado obtenido", juzgado));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JuzgadoResponseDTO>> crear(
            @Valid @RequestBody JuzgadoRequestDTO dto) {
        JuzgadoResponseDTO juzgado = juzgadoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Juzgado creado exitosamente", juzgado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JuzgadoResponseDTO>> editar(
            @PathVariable Integer id,
            @Valid @RequestBody JuzgadoRequestDTO dto) {
        JuzgadoResponseDTO juzgado = juzgadoService.editar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Juzgado actualizado", juzgado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        juzgadoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.success("Juzgado eliminado"));
    }
}