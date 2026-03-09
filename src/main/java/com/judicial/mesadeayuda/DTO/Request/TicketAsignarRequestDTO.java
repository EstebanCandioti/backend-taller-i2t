package com.judicial.mesadeayuda.DTO.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO específico para la asignación de técnico a un ticket.
 * Endpoint: PUT /api/tickets/{id}/asignar
 */
@Getter
@Setter
public class TicketAsignarRequestDTO {

    @NotNull(message = "El técnico es obligatorio")
    private Integer tecnicoId;
}