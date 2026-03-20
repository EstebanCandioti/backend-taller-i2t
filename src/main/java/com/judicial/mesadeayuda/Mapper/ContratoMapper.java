package com.judicial.mesadeayuda.Mapper;

import java.util.Collections;

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
                // Hardware y Software se completan en ContratoService via FK directa
                .hardware(Collections.emptyList())
                .software(Collections.emptyList())
                .build();
    }
}