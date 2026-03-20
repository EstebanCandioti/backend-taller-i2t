package com.judicial.mesadeayuda.DTO.Request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HardwareSoftwareRequestDTO {

    @NotNull(message = "La lista de software es obligatoria")
    private List<Integer> softwareIds;
}
