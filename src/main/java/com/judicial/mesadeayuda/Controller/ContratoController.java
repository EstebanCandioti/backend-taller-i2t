package com.judicial.mesadeayuda.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.judicial.mesadeayuda.DTO.Request.ContratoRenovarRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.ContratoRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.ContratoResponseDTO;
import com.judicial.mesadeayuda.Service.ContratoService;

import java.util.List;

/**
 * Controller de Contratos.
 *
 * Endpoints:
 *   GET    /api/contratos                    → Listar todos
 *   GET    /api/contratos/{id}               → Obtener detalle
 *   GET    /api/contratos/proximos-vencer    → Próximos a vencer
 *   GET    /api/contratos/vencidos           → Ya vencidos
 *   POST   /api/contratos                    → Crear contrato
 *   POST   /api/contratos/{id}/renovar       → Renovar contrato
 *   PUT    /api/contratos/{id}               → Editar contrato
 *   DELETE /api/contratos/{id}               → Soft-delete
 *
 * Autorización: solo Admin y Operario.
 */
@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContratoResponseDTO>>> listarTodos() {
        List<ContratoResponseDTO> contratos = contratoService.listarTodos();
        return ResponseEntity.ok(ApiResponse.success("Contratos obtenidos", contratos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContratoResponseDTO>> obtenerPorId(@PathVariable Integer id) {
        ContratoResponseDTO contrato = contratoService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Contrato obtenido", contrato));
    }

    @GetMapping("/proximos-vencer")
    public ResponseEntity<ApiResponse<List<ContratoResponseDTO>>> proximosAVencer() {
        List<ContratoResponseDTO> contratos = contratoService.proximosAVencer();
        return ResponseEntity.ok(ApiResponse.success("Contratos próximos a vencer", contratos));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<ApiResponse<List<ContratoResponseDTO>>> vencidos() {
        List<ContratoResponseDTO> contratos = contratoService.vencidos();
        return ResponseEntity.ok(ApiResponse.success("Contratos vencidos", contratos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContratoResponseDTO>> crear(
            @Valid @RequestBody ContratoRequestDTO dto) {
        ContratoResponseDTO contrato = contratoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contrato creado exitosamente", contrato));
    }

    @PostMapping("/{id}/renovar")
    public ResponseEntity<ApiResponse<ContratoResponseDTO>> renovar(
            @PathVariable Integer id,
            @Valid @RequestBody ContratoRenovarRequestDTO dto) {
        ContratoResponseDTO contrato = contratoService.renovar(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contrato renovado exitosamente", contrato));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContratoResponseDTO>> editar(
            @PathVariable Integer id,
            @Valid @RequestBody ContratoRequestDTO dto) {
        ContratoResponseDTO contrato = contratoService.editar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Contrato actualizado", contrato));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        contratoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.success("Contrato eliminado"));
    }
}