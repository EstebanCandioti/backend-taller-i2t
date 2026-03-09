package com.judicial.mesadeayuda.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Estructura estándar para respuestas paginadas.
 * Se usa como el campo "data" dentro de ApiResponse<PaginatedResponse<T>>.
 *
 * Ejemplo de respuesta final:
 * {
 *   "success": true,
 *   "message": "Tickets obtenidos",
 *   "data": {
 *     "content": [...],
 *     "totalElements": 500,
 *     "totalPages": 25,
 *     "currentPage": 0,
 *     "pageSize": 20
 *   },
 *   "timestamp": "2026-03-01T14:33:00"
 * }
 *
 * Uso en Service/Controller:
 *   Page<TicketResponseDTO> page = ...;
 *   PaginatedResponse<TicketResponseDTO> paginated = PaginatedResponse.from(page);
 *   return ResponseEntity.ok(ApiResponse.success("Tickets obtenidos", paginated));
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class PaginatedResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    /**
     * Factory method que convierte un Page<T> de Spring Data
     * en nuestra estructura estándar PaginatedResponse<T>.
     */
    public static <T> PaginatedResponse<T> from(Page<T> page) {
        return PaginatedResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .build();
    }
}