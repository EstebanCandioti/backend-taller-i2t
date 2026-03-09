package com.judicial.mesadeayuda.DTO.Response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class HardwareResponseDTO {

    private Integer id;
    private String nroInventario;
    private String clase;
    private String marca;
    private String modelo;
    private String nroSerie;
    private String ubicacionFisica;
    private LocalDate fechaAlta;
    private String observaciones;

    // Juzgado (datos embebidos)
    private Integer juzgadoId;
    private String juzgadoNombre;

    // Contrato asociado (puede ser null)
    private Integer contratoId;
    private String contratoNombre;
    private LocalDate contratoFechaFin;
    private boolean contratoVencido;
}