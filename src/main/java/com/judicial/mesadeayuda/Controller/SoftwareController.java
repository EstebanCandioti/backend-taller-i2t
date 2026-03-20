package com.judicial.mesadeayuda.Controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.judicial.mesadeayuda.DTO.Request.SoftwareHardwareRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.SoftwareJuzgadoRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.SoftwareRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.PaginatedResponse;
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
    public ResponseEntity<ApiResponse<PaginatedResponse<SoftwareResponseDTO>>> listar(
            @RequestParam(required = false) Integer contratoId,
            @RequestParam(required = false) Integer juzgadoId,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre,asc") String sort) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PaginatedResponse<SoftwareResponseDTO> software = softwareService.listar(contratoId, juzgadoId, proveedor, q, pageable);
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

@PutMapping("/{id}/hardware")
public ResponseEntity<ApiResponse<SoftwareResponseDTO>> actualizarHardware(
        @PathVariable Integer id,
        @RequestBody SoftwareHardwareRequestDTO dto) {
    SoftwareResponseDTO response = softwareService.actualizarHardware(id, dto.getHardwareIds());
    return ResponseEntity.ok(ApiResponse.success("Hardware actualizado correctamente", response));
}

@PutMapping("/{id}/juzgados")
public ResponseEntity<ApiResponse<SoftwareResponseDTO>> actualizarJuzgados(
        @PathVariable Integer id,
        @RequestBody SoftwareJuzgadoRequestDTO dto) {
    SoftwareResponseDTO response = softwareService.actualizarJuzgados(id, dto.getJuzgadoIds());
    return ResponseEntity.ok(ApiResponse.success("Juzgados actualizados correctamente", response));
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
