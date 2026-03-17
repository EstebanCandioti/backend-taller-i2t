package com.judicial.mesadeayuda.Mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import com.judicial.mesadeayuda.DTO.Response.ContratoResponseDTO;
import com.judicial.mesadeayuda.Entities.Contrato;

public class ContratoMapper {

    private ContratoMapper() {}

    public static ContratoResponseDTO toDTO(Contrato contrato) {
        return ContratoResponseDTO.builder()
                .id(contrato.getId())
                .nombre(contrato.getNombre())
                .proveedor(contrato.getProveedor())
                .fechaInicio(contrato.getFechaInicio())
                .fechaFin(contrato.getFechaFin())
                .cobertura(contrato.getCobertura())
                .monto(contrato.getMonto())
                .diasAlertaVencimiento(contrato.getDiasAlertaVencimiento())
                .observaciones(contrato.getObservaciones())
                // Renovacion
                .renovadoAId(contrato.getRenovadoAId())
                // Estado calculado
                .vencido(contrato.estaVencido())
                .proximoAVencer(contrato.estaProximoAVencer())
                // Hardware asociado
                .hardware(contrato.getHardware() != null
                        ? contrato.getHardware().stream()
                            .map(hw -> ContratoResponseDTO.HardwareSimpleDTO.builder()
                                    .id(hw.getId())
                                    .nroInventario(hw.getNroInventario())
                                    .clase(hw.getClase())
                                    .marca(hw.getMarca())
                                    .modelo(hw.getModelo())
                                    .build())
                            .collect(Collectors.toList())
                        : Collections.emptyList())
                // Software asociado
                .software(contrato.getSoftwareLicencias() != null
                        ? contrato.getSoftwareLicencias().stream()
                            .map(sw -> ContratoResponseDTO.SoftwareSimpleDTO.builder()
                                    .id(sw.getId())
                                    .nombre(sw.getNombre())
                                    .proveedor(sw.getProveedor())
                                    .cantidadLicencias(sw.getCantidadLicencias())
                                    .build())
                            .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}