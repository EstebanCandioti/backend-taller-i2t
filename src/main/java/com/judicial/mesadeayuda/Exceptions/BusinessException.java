package com.judicial.mesadeayuda.Exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 * Mapeada a HTTP 400 BAD REQUEST o 409 CONFLICT según el caso.
 *
 * Uso en Services:
 *   if (!ticket.puedeAsignarse()) {
 *       throw new BusinessException("El ticket ya tiene técnico asignado", HttpStatus.CONFLICT);
 *   }
 *
 *   if (software.getContrato() == null) {
 *       throw new BusinessException("El software debe estar vinculado a un contrato");
 *   }
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;

    /**
     * Constructor con mensaje. Default: 400 BAD REQUEST.
     */
    public BusinessException(String mensaje) {
        super(mensaje);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    /**
     * Constructor con mensaje y código HTTP específico.
     * Uso: new BusinessException("Email duplicado", HttpStatus.CONFLICT)
     */
    public BusinessException(String mensaje, HttpStatus httpStatus) {
        super(mensaje);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}