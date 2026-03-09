package com.judicial.mesadeayuda.Entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;


@Entity
@Table(name = "hardware",
       indexes = {
           @Index(name = "idx_hardware_juzgado", columnList = "juzgado_id"),
           @Index(name = "idx_hardware_contrato", columnList = "contrato_id"),
           @Index(name = "idx_hardware_eliminado", columnList = "eliminado")
       })
@SQLRestriction("eliminado = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hardware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nro_inventario", nullable = false, unique = true, length = 50)
    private String nroInventario; // Ej: "HW-2025-0001"

    @Column(name = "clase", nullable = false, length = 100)
    private String clase; // Ej: "PC Desktop", "Impresora", "Scanner", "Monitor"

    @Column(name = "marca", nullable = false, length = 100)
    private String marca; // Ej: "HP", "Dell", "Lenovo"

    @Column(name = "modelo", nullable = false, length = 150)
    private String modelo; // Ej: "ProDesk 400 G7"

    @Column(name = "nro_serie", length = 100)
    private String nroSerie; // Número de serie del fabricante

    @Column(name = "ubicacion_fisica", nullable = false, length = 200)
    private String ubicacionFisica; // Ej: "Piso 2, Oficina 203"

    /**
     * Juzgado al que está asignado el equipo. OBLIGATORIO.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juzgado_id", nullable = false)
    private Juzgado juzgado;

    /**
     * Contrato que cubre este equipo. OPCIONAL (nullable).
     * Al eliminar un contrato (soft-delete), se setea en NULL via ON DELETE SET NULL en la BD.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(name = "fecha_alta", nullable = false)
    @Builder.Default
    private LocalDate fechaAlta = LocalDate.now();

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    /**
     * Contratos asociados a este hardware (many-to-many inverso).
     * mappedBy indica que Contrato es el dueño de la relación JoinTable.
     */
    @ManyToMany(mappedBy = "hardware", fetch = FetchType.LAZY)
    private List<Contrato> contratos;

    /**
     * Software instalado en este equipo.
     * Relación inversa - el dueño es Software.
     */
    @OneToMany(mappedBy = "hardware", fetch = FetchType.LAZY)
    private List<Software> softwareInstalado;

    /**
     * Tickets asociados a este equipo.
     * Relación inversa - el dueño es Ticket.
     */
    @OneToMany(mappedBy = "hardware", fetch = FetchType.LAZY)
    private List<Ticket> tickets;

    // ── Soft-delete ───────────────────────────────────────────
    @Column(name = "eliminado", nullable = false)
    @Builder.Default
    private boolean eliminado = false;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eliminado_por_id")
    private Usuario eliminadoPor;
}
