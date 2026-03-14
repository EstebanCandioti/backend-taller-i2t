package com.judicial.mesadeayuda.DTO.Response;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SoftwareResponseDTO {

    private Integer id;
    private String nombre;
    private String proveedor;
    private Integer cantidadLicencias;
    private Integer licenciasEnUso;
    private Integer licenciasDisponibles;
    private LocalDate fechaVencimiento;
    private String observaciones;

    // Contrato vinculado
    private Integer contratoId;
    private String contratoNombre;

    // Juzgado asignado (puede ser null)
    private List<JuzgadoResponseDTO> juzgados;

    // Hardware donde está instalado (puede ser null)
    private List<HardwareResponseDTO> hardware;
}
