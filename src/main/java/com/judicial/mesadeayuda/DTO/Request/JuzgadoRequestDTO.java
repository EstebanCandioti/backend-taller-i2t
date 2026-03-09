package com.judicial.mesadeayuda.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JuzgadoRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "El fuero es obligatorio")
    @Size(max = 100, message = "El fuero no puede superar los 100 caracteres")
    private String fuero;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
    private String ciudad;

    @Size(max = 150, message = "El edificio no puede superar los 150 caracteres")
    private String edificio;

    @NotNull(message = "La circunscripción es obligatoria")
    private Integer circunscripcionId;
}






