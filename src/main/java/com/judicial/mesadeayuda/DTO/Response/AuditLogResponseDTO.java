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
public class AuditLogResponseDTO {

    private Long id;
    private String entidad;
    private String accion;
    private Integer registroId;
    private String valorAnterior;  // JSON string
    private String valorNuevo;     // JSON string
    private LocalDateTime fecha;

    // Usuario que realizó la acción (puede ser null en acciones del sistema)
    private Integer usuarioId;
    private String usuarioNombreCompleto;
}