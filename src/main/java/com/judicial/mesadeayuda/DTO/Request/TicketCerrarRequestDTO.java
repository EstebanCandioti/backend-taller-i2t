package com.judicial.mesadeayuda.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO específico para el cierre de un ticket.
 * Endpoint: PUT /api/tickets/{id}/cerrar
 */
@Getter
@Setter
public class TicketCerrarRequestDTO {

    @NotBlank(message = "La resolución es obligatoria para cerrar el ticket")
    private String resolucion;
}