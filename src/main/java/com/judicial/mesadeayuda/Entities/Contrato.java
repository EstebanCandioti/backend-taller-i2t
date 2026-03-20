package com.judicial.mesadeayuda.Entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Index;


@Entity
@Table(name = "contratos",
       indexes = {
           @Index(name = "idx_contratos_fecha_fin", columnList = "fecha_fin"),
           @Index(name = "idx_contratos_eliminado", columnList = "eliminado")
       })
@SQLRestriction("eliminado = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre; // Ej: "Mantenimiento IT 2024"

    @Column(name = "proveedor", nullable = false, length = 150)
    private String proveedor; // Ej: "TechService SA"

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "cobertura", length = 255)
    private String cobertura; // Ej: "Hardware + Soporte", "Software Office"

    @Column(name = "monto", precision = 15, scale = 2)
    private BigDecimal monto;

    /**
     * Días antes del vencimiento para emitir alerta.
     * Default: 30 días. Configurable por contrato.
     */
    @Column(name = "dias_alerta_vencimiento", nullable = false)
    @Builder.Default
    private Integer diasAlertaVencimiento = 30;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    /**
     * ID del contrato nuevo generado por renovación.
     * Si no es null, este contrato ya fue renovado y no puede renovarse otra vez.
     */
    @Column(name = "renovado_a_id")
    private Integer renovadoAId;

    // ── Soft-delete ───────────────────────────────────────────
    @Column(name = "eliminado", nullable = false)
    @Builder.Default
    private boolean eliminado = false;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eliminado_por_id")
    private Usuario eliminadoPor;

    // ── Métodos utilitarios ───────────────────────────────────

    /**
     * Indica si el contrato ya venció (fecha_fin en el pasado).
     */
    public boolean estaVencido() {
        return LocalDate.now().isAfter(this.fechaFin);
    }

    /**
     * Indica si el contrato está próximo a vencer según diasAlertaVencimiento.
     */
    public boolean estaProximoAVencer() {
        LocalDate fechaAlerta = this.fechaFin.minusDays(this.diasAlertaVencimiento);
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaAlerta) && !hoy.isAfter(this.fechaFin);
    }

}
