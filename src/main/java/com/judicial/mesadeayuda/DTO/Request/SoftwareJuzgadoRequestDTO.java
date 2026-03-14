package com.judicial.mesadeayuda.DTO.Request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoftwareJuzgadoRequestDTO {

    @NotNull(message = "La lista de juzgados es obligatoria")
    private List<Integer> juzgadoIds;
}
