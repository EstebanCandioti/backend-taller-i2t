package com.judicial.mesadeayuda.DTO.Request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoftwareHardwareRequestDTO {

    @NotNull(message = "La lista de hardware es obligatoria")
    private List<Integer> hardwareIds;
}
