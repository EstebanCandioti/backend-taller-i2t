package com.judicial.mesadeayuda.DTO.Request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContratoRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "El proveedor es obligatorio")
    @Size(max = 150, message = "El proveedor no puede superar los 150 caracteres")
    private String proveedor;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @Size(max = 255, message = "La cobertura no puede superar los 255 caracteres")
    private String cobertura;

    private BigDecimal monto;

    /**
     * Días antes del vencimiento para emitir alerta. Default: 30.
     */
    private Integer diasAlertaVencimiento;

    private String observaciones;

    /**
     * IDs de hardware a asociar al contrato (many-to-many).
     */
    private List<Integer> hardwareIds;

    /**
     * IDs de software a asociar al contrato (many-to-many).
     */
    private List<Integer> softwareIds;
}