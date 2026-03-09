package com.judicial.mesadeayuda.Mapper;

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
                // Contrato
                .contratoId(software.getContrato().getId())
                .contratoNombre(software.getContrato().getNombre())
                // Juzgado (puede ser null)
                .juzgadoId(software.getJuzgado() != null ? software.getJuzgado().getId() : null)
                .juzgadoNombre(software.getJuzgado() != null ? software.getJuzgado().getNombre() : null)
                // Hardware (puede ser null)
                .hardwareId(software.getHardware() != null ? software.getHardware().getId() : null)
                .hardwareNroInventario(software.getHardware() != null ? software.getHardware().getNroInventario() : null)
                .build();
    }
}
