package com.judicial.mesadeayuda.Mapper;

import com.judicial.mesadeayuda.DTO.Response.UsuarioResponseDTO;
import com.judicial.mesadeayuda.Entities.Usuario;

public class UsuarioMapper {

    private UsuarioMapper() {}

    public static UsuarioResponseDTO toDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .activo(usuario.isActivo())
                .fechaAlta(usuario.getFechaAlta())
                .rolId(usuario.getRol().getId())
                .rolNombre(usuario.getRol().getNombre())
                .build();
    }
}