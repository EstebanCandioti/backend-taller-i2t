package com.judicial.mesadeayuda.DTO.Response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TicketResponseDTO {

    private Integer id;
    private String titulo;
    private String descripcion;
    private String prioridad;
    private String estado;
    private String tipoRequerimiento;

    // Juzgado (datos embebidos)
    private Integer juzgadoId;
    private String juzgadoNombre;

    // Técnico asignado (puede ser null)
    private Integer tecnicoId;
    private String tecnicoNombreCompleto;

    // Hardware relacionado (puede ser null)
    private Integer hardwareId;
    private String hardwareNroInventario;
    private String hardwareDescripcion; // "HP ProDesk 400 G7"

    // Referente del juzgado
    private String referenteNombre;
    private String referenteTelefono;

    // Creador
    private Integer creadoPorId;
    private String creadoPorNombreCompleto;

    // Resolución (solo presente cuando está CERRADO)
    private String resolucion;

    // Fechas
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaCierre;
}
