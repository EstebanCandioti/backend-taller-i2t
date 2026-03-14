package com.judicial.mesadeayuda.Mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import com.judicial.mesadeayuda.DTO.Response.SoftwareResponseDTO;
import com.judicial.mesadeayuda.Entities.Software;

public class SoftwareMapper {

    private SoftwareMapper() {}

    public static SoftwareResponseDTO toDTO(Software software) {
        return SoftwareResponseDTO.builder()
                .id(software.getId())
                .nombre(software.getNombre())
                .proveedor(software.getProveedor())
                .cantidadLicencias(software.getCantidadLicencias())
                .licenciasEnUso(software.getLicenciasEnUso())
                .licenciasDisponibles(software.getLicenciasDisponibles())
                .fechaVencimiento(software.getFechaVencimiento())
                .observaciones(software.getObservaciones())
                .contratoId(software.getContrato().getId())
                .contratoNombre(software.getContrato().getNombre())
                .juzgados(software.getJuzgados() != null
                        ? software.getJuzgados().stream()
                                .map(JuzgadoMapper::toDTO)
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .hardware(software.getHardware() != null
                        ? software.getHardware().stream()
                                .map(HardwareMapper::toDTO)
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}
