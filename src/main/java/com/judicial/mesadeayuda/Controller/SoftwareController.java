package com.judicial.mesadeayuda.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.judicial.mesadeayuda.DTO.Request.SoftwareRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.SoftwareResponseDTO;
import com.judicial.mesadeayuda.Service.SoftwareService;

import java.util.List;

/**
 * Controller de Software.
 *
 * Endpoints:
 *   GET    /api/software                   → Listar con filtros
 *   GET    /api/software/{id}              → Obtener detalle
 *   GET    /api/software/vencimientos      → Licencias próximas a vencer
 *   POST   /api/software                   → Crear software (requiere contrato)
 *   PUT    /api/software/{id}              → Editar software
 *   DELETE /api/software/{id}              → Soft-delete
 *
 * Autorización: solo Admin y Operario.
 */
@RestController
@RequestMapping("/api/software")
public class SoftwareController {

    private final SoftwareService softwareService;

    public SoftwareController(SoftwareService softwareService) {
        this.softwareService = softwareService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SoftwareResponseDTO>>> listar(
            @RequestParam(required = false) Integer contratoId,
            @RequestParam(required = false) Integer juzgadoId,
            @RequestParam(required = false) String proveedor) {

        List<SoftwareResponseDTO> software = softwareService.listar(contratoId, juzgadoId, proveedor);
        return ResponseEntity.ok(ApiResponse.success("Software obtenido", software));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SoftwareResponseDTO>> obtenerPorId(@PathVariable Integer id) {
        SoftwareResponseDTO software = softwareService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Software obtenido", software));
    }

    @GetMapping("/vencimientos")
    public ResponseEntity<ApiResponse<List<SoftwareResponseDTO>>> proximasAVencer(
            @RequestParam(defaultValue = "30") int dias) {

        List<SoftwareResponseDTO> software = softwareService.proximasAVencer(dias);
        return ResponseEntity.ok(ApiResponse.success("Licencias próximas a vencer", software));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SoftwareResponseDTO>> crear(
            @Valid @RequestBody SoftwareRequestDTO dto) {
        SoftwareResponseDTO software = softwareService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Software creado exitosamente", software));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SoftwareResponseDTO>> editar(
            @PathVariable Integer id,
            @Valid @RequestBody SoftwareRequestDTO dto) {
        SoftwareResponseDTO software = softwareService.editar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Software actualizado", software));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<SoftwareResponseDTO>> restaurar(@PathVariable Integer id) {
        SoftwareResponseDTO software = softwareService.restaurar(id);
        return ResponseEntity.ok(ApiResponse.success("Software restaurado exitosamente", software));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        softwareService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.success("Software eliminado"));
    }
}