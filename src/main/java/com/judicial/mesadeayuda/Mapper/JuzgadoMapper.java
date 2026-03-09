package com.judicial.mesadeayuda.Mapper;

import com.judicial.mesadeayuda.DTO.Response.JuzgadoResponseDTO;
import com.judicial.mesadeayuda.Entities.Juzgado;

public class JuzgadoMapper {

    private JuzgadoMapper() {}

    public static JuzgadoResponseDTO toDTO(Juzgado juzgado) {
        return JuzgadoResponseDTO.builder()
                .id(juzgado.getId())
                .nombre(juzgado.getNombre())
                .fuero(juzgado.getFuero())
                .ciudad(juzgado.getCiudad())
                .edificio(juzgado.getEdificio())
                .circunscripcionId(juzgado.getCircunscripcion().getId())
                .circunscripcionNombre(juzgado.getCircunscripcion().getNombre())
                .build();
    }
}