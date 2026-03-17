package com.judicial.mesadeayuda.Controller;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.judicial.mesadeayuda.DTO.Request.CircunscripcionRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.CircunscripcionResponseDTO;
import com.judicial.mesadeayuda.Service.CircunscripcionService;

import java.util.List;

/**
 * Controller de Circunscripciones.
 *
 * Endpoints:
 *   GET    /api/circunscripciones             → Listar todas
 *   GET    /api/circunscripciones/{id}        → Obtener detalle
 *   POST   /api/circunscripciones             → Crear circunscripción
 *   PUT    /api/circunscripciones/{id}        → Editar circunscripción
 *   DELETE /api/circunscripciones/{id}        → Soft-delete
 *
 * Autorización: solo Admin y Operario.
 */
@RestController
@RequestMapping("/api/circunscripciones")
public class CircunscripcionController {

    private final CircunscripcionService circunscripcionService;

    public CircunscripcionController(CircunscripcionService circunscripcionService) {
        this.circunscripcionService = circunscripcionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CircunscripcionResponseDTO>>> listarTodas() {
        List<CircunscripcionResponseDTO> circunscripciones = circunscripcionService.listarTodas();
        return ResponseEntity.ok(ApiResponse.success("Circunscripciones obtenidas", circunscripciones));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CircunscripcionResponseDTO>> obtenerPorId(
            @PathVariable Integer id) {
        CircunscripcionResponseDTO circ = circunscripcionService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Circunscripción obtenida", circ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CircunscripcionResponseDTO>> crear(
            @Valid @RequestBody CircunscripcionRequestDTO dto) {
        CircunscripcionResponseDTO circ = circunscripcionService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Circunscripción creada exitosamente", circ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CircunscripcionResponseDTO>> editar(
            @PathVariable Integer id,
            @Valid @RequestBody CircunscripcionRequestDTO dto) {
        CircunscripcionResponseDTO circ = circunscripcionService.editar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Circunscripción actualizada", circ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        circunscripcionService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.success("Circunscripción eliminada"));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<CircunscripcionResponseDTO>> restaurar(@PathVariable Integer id) {
        CircunscripcionResponseDTO circ = circunscripcionService.restaurar(id);
        return ResponseEntity.ok(ApiResponse.success("Circunscripción restaurada exitosamente", circ));
    }
}