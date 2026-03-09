package com.judicial.mesadeayuda.Mapper;

import com.judicial.mesadeayuda.DTO.Response.AuditLogResponseDTO;
import com.judicial.mesadeayuda.Entities.AuditLog;

public class AuditLogMapper {

    private AuditLogMapper() {}

    public static AuditLogResponseDTO toDTO(AuditLog log) {
        return AuditLogResponseDTO.builder()
                .id(log.getId())
                .entidad(log.getEntidad())
                .accion(log.getAccion().name())
                .registroId(log.getRegistroId())
                .valorAnterior(log.getValorAnterior())
                .valorNuevo(log.getValorNuevo())
                .fecha(log.getFecha())
                .usuarioId(log.getUsuario() != null ? log.getUsuario().getId() : null)
                .usuarioNombreCompleto(log.getUsuario() != null ? log.getUsuario().getNombreCompleto() : null)
                .build();
    }
}