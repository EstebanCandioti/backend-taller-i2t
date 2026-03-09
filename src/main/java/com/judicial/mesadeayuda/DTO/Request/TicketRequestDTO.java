package com.judicial.mesadeayuda.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar los 200 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    /**
     * Valores válidos: BAJA, MEDIA, ALTA, CRITICA. Default: MEDIA.
     */
    private String prioridad;

    @Size(max = 100, message = "El tipo de requerimiento no puede superar los 100 caracteres")
    private String tipoRequerimiento;

    @NotNull(message = "El juzgado es obligatorio")
    private Integer juzgadoId;

    /**
     * OPCIONAL. Equipo relacionado al problema.
     */
    private Integer hardwareId;

    @Size(max = 150, message = "El nombre del referente no puede superar los 150 caracteres")
    private String referenteNombre;

    @Size(max = 30, message = "El teléfono del referente no puede superar los 30 caracteres")
    private String referenteTelefono;
}