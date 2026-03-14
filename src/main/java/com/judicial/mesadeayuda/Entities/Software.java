package com.judicial.mesadeayuda.Entities;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "software",
       indexes = {
           @Index(name = "idx_software_contrato", columnList = "contrato_id"),
           @Index(name = "idx_software_eliminado", columnList = "eliminado")
       })
@SQLRestriction("eliminado = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Software {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre; // Ej: "Microsoft Office 365"

    @Column(name = "proveedor", nullable = false, length = 150)
    private String proveedor; // Ej: "CompuSoft SRL"

    @Column(name = "cantidad_licencias", nullable = false)
    @Builder.Default
    private Integer cantidadLicencias = 1;

    /**
     * Licencias actualmente en uso.
     * Validación: licenciasEnUso <= cantidadLicencias (ver Service).
     */
    @Column(name = "licencias_en_uso", nullable = false)
    @Builder.Default
    private Integer licenciasEnUso = 0;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    /**
     * Contrato vinculado. OBLIGATORIO (NOT NULL).
     * Validación en Service: el contrato debe existir y estar activo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    /**
     * Juzgado al que está asignada la licencia. OPCIONAL.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "software_juzgado",
        joinColumns = @JoinColumn(name = "software_id"),
        inverseJoinColumns = @JoinColumn(name = "juzgado_id")
    )
    @SQLJoinTableRestriction("eliminado = 0")
    private List<Juzgado> juzgados;

    /**
     * Equipo específico en el que está instalado el software. OPCIONAL.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "software_hardware",
        joinColumns = @JoinColumn(name = "software_id"),
        inverseJoinColumns = @JoinColumn(name = "hardware_id")
    )
    @SQLJoinTableRestriction("eliminado = 0")
    private List<Hardware> hardware;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    /**
     * Contratos asociados a este software (many-to-many inverso).
     */
    @ManyToMany(mappedBy = "softwareLicencias", fetch = FetchType.LAZY)
    private List<Contrato> contratos;

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
     * Calcula las licencias disponibles (no asignadas).
     */
    public int getLicenciasDisponibles() {
        return this.cantidadLicencias - this.licenciasEnUso;
    }

    /**
     * Indica si hay licencias disponibles para asignar.
     */
    public boolean tieneLicenciasDisponibles() {
        return getLicenciasDisponibles() > 0;
    }
}
