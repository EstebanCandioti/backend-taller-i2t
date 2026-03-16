package com.judicial.mesadeayuda.Controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import com.judicial.mesadeayuda.DTO.Request.HardwareRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.HardwareResponseDTO;
import com.judicial.mesadeayuda.DTO.Response.PaginatedResponse;
import com.judicial.mesadeayuda.DTO.Response.TicketResponseDTO;
import com.judicial.mesadeayuda.Service.HardwareService;

import jakarta.validation.Valid;

/**
 * Controller de Hardware.
 *
 * Endpoints:
 * GET /api/hardware → Listar con filtros
 * GET /api/hardware/{id} → Obtener detalle
 * POST /api/hardware → Crear equipo
 * PUT /api/hardware/{id} → Editar equipo
 * DELETE /api/hardware/{id} → Soft-delete
 *
 * Autorización: solo Admin y Operario (definido en SecurityConfig).
 */
@RestController
@RequestMapping("/api/hardware")
public class HardwareController {

    private final HardwareService hardwareService;

    public HardwareController(HardwareService hardwareService) {
        this.hardwareService = hardwareService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<HardwareResponseDTO>>> listar(
            @RequestParam(required = false) Integer juzgadoId,
            @RequestParam(required = false) String clase,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String ubicacion,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaAlta,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PaginatedResponse<HardwareResponseDTO> hardware = hardwareService.listar(juzgadoId, clase, modelo, ubicacion, q, pageable);
        return ResponseEntity.ok(ApiResponse.success("Hardware obtenido", hardware));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HardwareResponseDTO>> obtenerPorId(@PathVariable Integer id) {
        HardwareResponseDTO hardware = hardwareService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Hardware obtenido", hardware));
    }

    @GetMapping("/{id}/tickets")
    public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> listarTickets(@PathVariable Integer id) {
        List<TicketResponseDTO> tickets = hardwareService.listarTickets(id);
        return ResponseEntity.ok(ApiResponse.success("Tickets del hardware obtenidos", tickets));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HardwareResponseDTO>> crear(
            @Valid @RequestBody HardwareRequestDTO dto) {
        HardwareResponseDTO hardware = hardwareService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hardware creado exitosamente", hardware));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HardwareResponseDTO>> editar(
            @PathVariable Integer id,
            @Valid @RequestBody HardwareRequestDTO dto) {
        HardwareResponseDTO hardware = hardwareService.editar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Hardware actualizado", hardware));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        hardwareService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.success("Hardware eliminado"));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<HardwareResponseDTO>> restaurar(@PathVariable Integer id) {
        HardwareResponseDTO hardware = hardwareService.restaurar(id);
        return ResponseEntity.ok(ApiResponse.success("Hardware restaurado exitosamente", hardware));
    }
}