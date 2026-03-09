package com.judicial.mesadeayuda.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura estándar de respuesta HTTP para TODOS los endpoints.
 *
 * Uso en Controllers:
 *   return ResponseEntity.ok(ApiResponse.success("Ticket creado", ticketDTO));
 *   return ResponseEntity.status(404).body(ApiResponse.error("No encontrado"));
 *
 * @param <T> Tipo del dato en la respuesta (DTO, lista, etc.)
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // No incluir campos null en el JSON
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Map<String, String> errors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Factory methods para respuestas exitosas ──────────────────

    /**
     * Respuesta exitosa con datos.
     * Uso: ApiResponse.success("Ticket creado", ticketResponseDTO)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Respuesta exitosa sin datos (ej: DELETE exitoso).
     * Uso: ApiResponse.success("Ticket eliminado")
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ── Factory methods para respuestas de error ──────────────────

    /**
     * Error simple (sin detalle de campos).
     * Uso: ApiResponse.error("Recurso no encontrado")
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Error con detalle de validación por campo.
     * Uso: ApiResponse.validationError("Error de validación", errorsMap)
     */
    public static <T> ApiResponse<T> validationError(String message, Map<String, String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}