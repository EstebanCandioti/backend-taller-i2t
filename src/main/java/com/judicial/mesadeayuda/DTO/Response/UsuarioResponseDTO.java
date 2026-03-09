package com.judicial.mesadeayuda.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class UsuarioResponseDTO {

    private Integer id;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String email;
    private String telefono;
    private boolean activo;
    private LocalDateTime fechaAlta;

    // Rol embebido (evita relación circular)
    private Integer rolId;
    private String rolNombre;
}
