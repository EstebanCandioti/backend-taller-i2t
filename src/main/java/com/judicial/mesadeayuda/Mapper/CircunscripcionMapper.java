package com.judicial.mesadeayuda.Mapper;

import com.judicial.mesadeayuda.DTO.Response.CircunscripcionResponseDTO;
import com.judicial.mesadeayuda.Entities.Circunscripcion;

public class CircunscripcionMapper {

    private CircunscripcionMapper() {}

    public static CircunscripcionResponseDTO toDTO(Circunscripcion circ) {
        return CircunscripcionResponseDTO.builder()
                .id(circ.getId())
                .nombre(circ.getNombre())
                .distritoJudicial(circ.getDistritoJudicial())
                .build();
    }
}