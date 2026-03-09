package com.judicial.mesadeayuda.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CircunscripcionRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El distrito judicial es obligatorio")
    @Size(max = 100, message = "El distrito judicial no puede superar los 100 caracteres")
    private String distritoJudicial;
}