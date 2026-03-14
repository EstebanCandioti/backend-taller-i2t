package com.judicial.mesadeayuda.DTO.Request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoftwareRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "El proveedor es obligatorio")
    @Size(max = 150, message = "El proveedor no puede superar los 150 caracteres")
    private String proveedor;

    @NotNull(message = "La cantidad de licencias es obligatoria")
    @Min(value = 1, message = "La cantidad de licencias debe ser al menos 1")
    private Integer cantidadLicencias;

    private LocalDate fechaVencimiento;

    @NotNull(message = "El contrato es obligatorio")
    private Integer contratoId;

    private List<Integer> juzgadoIds;

    /**
     * OPCIONAL. Equipo en el que se instala el software.
     */
    private List<Integer> hardwareIds;

    private String observaciones;
}
