package com.judicial.mesadeayuda.Exceptions;

/**
 * Excepción lanzada cuando un recurso no se encuentra en la BD.
 * Mapeada a HTTP 404 NOT FOUND en el GlobalExceptionHandler.
 *
 * Uso en Services:
 *   Ticket ticket = ticketRepository.findById(id)
 *       .orElseThrow(() -> new NotFoundException("Ticket", id));
 */
public class NotFoundException extends RuntimeException {

    private final String entidad;
    private final Object identificador;

    /**
     * Constructor con entidad e identificador.
     * Genera mensaje: "Ticket con id 5 no encontrado"
     */
    public NotFoundException(String entidad, Object identificador) {
        super(entidad + " con id " + identificador + " no encontrado");
        this.entidad = entidad;
        this.identificador = identificador;
    }

    /**
     * Constructor con mensaje libre.
     * Uso: new NotFoundException("No existe un usuario con email xyz@mail.com")
     */
    public NotFoundException(String mensaje) {
        super(mensaje);
        this.entidad = null;
        this.identificador = null;
    }

    public String getEntidad() {
        return entidad;
    }

    public Object getIdentificador() {
        return identificador;
    }
}