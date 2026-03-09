package com.judicial.mesadeayuda.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class JuzgadoResponseDTO {

    private Integer id;
    private String nombre;
    private String fuero;
    private String ciudad;
    private String edificio;

    // Circunscripción (datos embebidos)
    private Integer circunscripcionId;
    private String circunscripcionNombre;
}