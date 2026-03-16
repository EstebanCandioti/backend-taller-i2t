package com.judicial.mesadeayuda.DTO.Response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WsNotificationDTO {
    private String tipo;
    private String entidad;
    private Integer registroId;
    private String mensaje;
    private String fecha;
}
