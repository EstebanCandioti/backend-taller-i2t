package com.judicial.mesadeayuda.Controller;

import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.AuditLogResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Service.AuditLogService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller de Auditoría.
 * Solo lectura: consulta el registro inmutable de acciones del sistema.
 *
 * Endpoints:
 *   GET /api/audit/{entidad}/{registroId}    → Historial de un registro específico
 *   GET /api/audit/usuario/{usuarioId}       → Acciones de un usuario
 *   GET /api/audit?desde=...&hasta=...       → Filtro por rango de fechas
 *   GET /api/audit/filtro?entidad=...&accion=...&desde=...&hasta=...  → Filtro completo
 *
 * Autorización: solo Admin (definido en SecurityConfig).
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Historial completo de una entidad específica.
     * Ej: GET /api/audit/Ticket/5 → todos los cambios del ticket 5
     */
    @GetMapping("/{entidad}/{registroId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> obtenerPorEntidad(
            @PathVariable String entidad,
            @PathVariable Integer registroId) {

        List<AuditLogResponseDTO> logs = auditLogService.obtenerPorEntidad(entidad, registroId);
        return ResponseEntity.ok(ApiResponse.success("Historial de auditoría obtenido", logs));
    }

    /**
     * Todas las acciones realizadas por un usuario.
     * Ej: GET /api/audit/usuario/3
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> obtenerPorUsuario(
            @PathVariable Integer usuarioId) {

        List<AuditLogResponseDTO> logs = auditLogService.obtenerPorUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.success("Auditoría del usuario obtenida", logs));
    }

    /**
     * Filtro por rango de fechas.
     * Ej: GET /api/audit?desde=2026-01-01T00:00:00&hasta=2026-01-31T23:59:59
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> listarPorFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        List<AuditLogResponseDTO> logs = auditLogService.listarPorFechas(desde, hasta);
        return ResponseEntity.ok(ApiResponse.success("Registros de auditoría obtenidos", logs));
    }

    /**
     * Filtro completo: entidad + acción + rango de fechas.
     * Ej: GET /api/audit/filtro?entidad=Ticket&accion=CREATE&desde=...&hasta=...
     */
    @GetMapping("/filtro")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> listarConFiltros(
            @RequestParam String entidad,
            @RequestParam AuditLog.Accion accion,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {

        List<AuditLogResponseDTO> logs = auditLogService.listarConFiltros(entidad, accion, desde, hasta);
        return ResponseEntity.ok(ApiResponse.success("Registros de auditoría filtrados", logs));
    }
}