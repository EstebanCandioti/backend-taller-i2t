package com.judicial.mesadeayuda.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HardwareRequestDTO {

    @NotBlank(message = "El número de inventario es obligatorio")
    @Size(max = 50, message = "El nro de inventario no puede superar los 50 caracteres")
    private String nroInventario;

    @NotBlank(message = "La clase es obligatoria")
    @Size(max = 100, message = "La clase no puede superar los 100 caracteres")
    private String clase; // "PC Desktop", "Impresora", "Scanner", "Monitor"

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 150, message = "El modelo no puede superar los 150 caracteres")
    private String modelo;

    @Size(max = 100, message = "El nro de serie no puede superar los 100 caracteres")
    private String nroSerie;

    @NotBlank(message = "La ubicación física es obligatoria")
    @Size(max = 200, message = "La ubicación física no puede superar los 200 caracteres")
    private String ubicacionFisica;

    @NotNull(message = "El juzgado es obligatorio")
    private Integer juzgadoId;

    /**
     * OPCIONAL. Contrato que cubre este equipo.
     */
    private Integer contratoId;

    private String observaciones;
}






