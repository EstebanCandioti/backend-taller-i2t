package com.judicial.mesadeayuda.Mapper;

import java.time.LocalDate;

import com.judicial.mesadeayuda.DTO.Response.HardwareResponseDTO;
import com.judicial.mesadeayuda.Entities.Contrato;
import com.judicial.mesadeayuda.Entities.Hardware;

public class HardwareMapper {

    private HardwareMapper() {}

    public static HardwareResponseDTO toDTO(Hardware hardware) {
        Contrato contrato = hardware.getContrato();

        return HardwareResponseDTO.builder()
                .id(hardware.getId())
                .nroInventario(hardware.getNroInventario())
                .clase(hardware.getClase())
                .marca(hardware.getMarca())
                .modelo(hardware.getModelo())
                .nroSerie(hardware.getNroSerie())
                .ubicacionFisica(hardware.getUbicacionFisica())
                .fechaAlta(hardware.getFechaAlta())
                .observaciones(hardware.getObservaciones())
                // Juzgado
                .juzgadoId(hardware.getJuzgado().getId())
                .juzgadoNombre(hardware.getJuzgado().getNombre())
                // Contrato (puede ser null)
                .contratoId(contrato != null ? contrato.getId() : null)
                .contratoNombre(contrato != null ? contrato.getNombre() : null)
                .contratoFechaFin(contrato != null ? contrato.getFechaFin() : null)
                .contratoVencido(contrato != null && contrato.getFechaFin().isBefore(LocalDate.now()))
                .build();
    }
}