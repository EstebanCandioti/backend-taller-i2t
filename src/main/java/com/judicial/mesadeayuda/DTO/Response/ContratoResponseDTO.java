package com.judicial.mesadeayuda.DTO.Response;

import java.math.BigDecimal;
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
public class ContratoResponseDTO {

    private Integer id;
    private String nombre;
    private String proveedor;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String cobertura;
    private BigDecimal monto;
    private Integer diasAlertaVencimiento;
    private String observaciones;

    // Renovacion
    private Integer renovadoAId;

    // Estado calculado
    private boolean vencido;
    private boolean proximoAVencer;

    // Hardware asociado (lista simplificada)
    private List<HardwareSimpleDTO> hardware;

    // Software asociado (lista simplificada)
    private List<SoftwareSimpleDTO> software;

    /**
     * DTO simplificado de Hardware para embeber en Contrato.
     * Evita carga recursiva completa.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    public static class HardwareSimpleDTO {
        private Integer id;
        private String nroInventario;
        private String clase;
        private String marca;
        private String modelo;
    }

    /**
     * DTO simplificado de Software para embeber en Contrato.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @Builder
    public static class SoftwareSimpleDTO {
        private Integer id;
        private String nombre;
        private String proveedor;
        private Integer cantidadLicencias;
    }
}