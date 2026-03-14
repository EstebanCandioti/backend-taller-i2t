package com.judicial.mesadeayuda.Mapper;


import com.judicial.mesadeayuda.DTO.Response.TicketResponseDTO;
import com.judicial.mesadeayuda.Entities.Ticket;

/**
 * Mapper manual para Ticket.
 * Convierte Entity → ResponseDTO (nunca al revés: el Request se mapea en el Service).
 */
public class TicketMapper {

    private TicketMapper() {} // Utility class

    public static TicketResponseDTO toDTO(Ticket ticket) {
        return TicketResponseDTO.builder()
                .id(ticket.getId())
                .titulo(ticket.getTitulo())
                .descripcion(ticket.getDescripcion())
                .prioridad(ticket.getPrioridad().name())
                .estado(ticket.getEstado().name())
                .tipoRequerimiento(ticket.getTipoRequerimiento())
                // Juzgado
                .juzgadoId(ticket.getJuzgado().getId())
                .juzgadoNombre(ticket.getJuzgado().getNombre())
                // Técnico (puede ser null)
                .tecnicoId(ticket.getTecnico() != null ? ticket.getTecnico().getId() : null)
                .tecnicoNombreCompleto(ticket.getTecnico() != null ? ticket.getTecnico().getNombreCompleto() : null)
                // Hardware (puede ser null)
                .hardwareId(ticket.getHardware() != null ? ticket.getHardware().getId() : null)
                .hardwareNroInventario(ticket.getHardware() != null ? ticket.getHardware().getNroInventario() : null)
                .hardwareDescripcion(ticket.getHardware() != null
                        ? ticket.getHardware().getMarca() + " " + ticket.getHardware().getModelo() : null)
                // Referente
                .referenteNombre(ticket.getReferenteNombre())
                .referenteTelefono(ticket.getReferenteTelefono())
                // Creador
                .creadoPorId(ticket.getCreadoPor().getId())
                .creadoPorNombreCompleto(ticket.getCreadoPor().getNombreCompleto())
                // Resolución y fechas
                .resolucion(ticket.getResolucion())
                .fechaCreacion(ticket.getFechaCreacion())
                .fechaAsignacion(ticket.getFechaAsignacion())
                .fechaActualizacion(ticket.getFechaActualizacion())
                .fechaCierre(ticket.getFechaCierre())
                .build();
    }
}
