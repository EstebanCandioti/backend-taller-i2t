package com.judicial.mesadeayuda.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String token;
    private String tipo; // "Bearer"
    private Integer usuarioId;
    private String nombreCompleto;
    private String email;
    private String rol;
}






