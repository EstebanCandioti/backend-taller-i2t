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

import com.judicial.mesadeayuda.DTO.Request.TicketAsignarRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.TicketCerrarRequestDTO;
import com.judicial.mesadeayuda.DTO.Request.TicketRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.PaginatedResponse;
import com.judicial.mesadeayuda.DTO.Response.TicketResponseDTO;
import com.judicial.mesadeayuda.Entities.Ticket;
import com.judicial.mesadeayuda.Service.TicketService;

import jakarta.validation.Valid;

/**
 * Controller de Tickets.
 *
 * Endpoints:
 * GET /api/tickets → Listar con filtros (por rol)
 * GET /api/tickets/{id} → Obtener detalle
 * POST /api/tickets → Crear ticket
 * PUT /api/tickets/{id} → Editar ticket
 * PUT /api/tickets/{id}/asignar → Asignar técnico
 * PUT /api/tickets/{id}/reasignar → Reasignar a otro técnico
 * PUT /api/tickets/{id}/estado → Pasar a EN_CURSO
 * PUT /api/tickets/{id}/cerrar → Cerrar con resolución
 * DELETE /api/tickets/{id} → Soft-delete
 *
 * Autorización definida en SecurityConfig:
 * - GET: todos los roles autenticados (filtro por rol en Service)
 * - POST/PUT/DELETE: solo Admin y Operario
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<TicketResponseDTO>>> listar(
            @RequestParam(required = false) Ticket.Estado estado,
            @RequestParam(required = false) Ticket.Prioridad prioridad,
            @RequestParam(required = false) Integer juzgadoId,
            @RequestParam(required = false) Integer tecnicoId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaCreacion,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        PaginatedResponse<TicketResponseDTO> tickets = ticketService.listar(estado, prioridad, juzgadoId, tecnicoId, q, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tickets obtenidos", tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> obtenerPorId(@PathVariable Integer id) {
        TicketResponseDTO ticket = ticketService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket obtenido", ticket));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponseDTO>> crear(
            @Valid @RequestBody TicketRequestDTO dto) {
        TicketResponseDTO ticket = ticketService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket creado exitosamente", ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> editar(
            @PathVariable Integer id,
            @Valid @RequestBody TicketRequestDTO dto) {
        TicketResponseDTO ticket = ticketService.editar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Ticket actualizado", ticket));
    }

    @PutMapping("/{id}/asignar")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> asignar(
            @PathVariable Integer id,
            @Valid @RequestBody TicketAsignarRequestDTO dto) {
        TicketResponseDTO ticket = ticketService.asignar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Técnico asignado exitosamente", ticket));
    }

    @PutMapping("/{id}/reasignar")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> reasignar(
            @PathVariable Integer id,
            @Valid @RequestBody TicketAsignarRequestDTO dto) {
        TicketResponseDTO ticket = ticketService.reasignar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Técnico reasignado exitosamente", ticket));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> pasarAEnCurso(
            @PathVariable Integer id) {
        TicketResponseDTO ticket = ticketService.pasarAEnCurso(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket pasado a EN_CURSO", ticket));
    }

    @PutMapping("/{id}/cerrar")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> cerrar(
            @PathVariable Integer id,
            @Valid @RequestBody TicketCerrarRequestDTO dto) {
        TicketResponseDTO ticket = ticketService.cerrar(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Ticket cerrado exitosamente", ticket));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Integer id) {
        ticketService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket eliminado"));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<TicketResponseDTO>> restaurar(@PathVariable Integer id) {
        TicketResponseDTO ticket = ticketService.restaurar(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket restaurado exitosamente", ticket));
    }
}