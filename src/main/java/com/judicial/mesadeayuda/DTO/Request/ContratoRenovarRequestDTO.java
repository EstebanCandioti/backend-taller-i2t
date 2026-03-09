package com.judicial.mesadeayuda.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO para la renovación de un contrato existente.
 *
 * Uso: POST /api/contratos/{id}/renovar
 *
 * La renovación crea un nuevo período para el contrato,
 * actualizando fechas, monto y opcionalmente observaciones.
 */
@Getter
@Setter
public class ContratoRenovarRequestDTO {

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    /**
     * Nuevo monto del contrato renovado. OPCIONAL.
     * Si no se envía, se mantiene el monto anterior.
     */
    private BigDecimal monto;

    /**
     * Observaciones sobre la renovación. OPCIONAL.
     */
    @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
    private String observaciones;
}