package com.judicial.mesadeayuda.Entities;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="juzgados")
@SQLRestriction("eliminado=0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Juzgado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre; // Ej: "Juzgado Civil N°3"

    @Column(name = "fuero", nullable = false, length = 100)
    private String fuero; // Ej: "Civil", "Penal", "Familia", "Laboral"

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad; // Ej: "Santa Fe", "Rosario"

    @Column(name = "edificio", length = 150)
    private String edificio; // Ej: "Tribunales Centro - Piso 2"

    /**
     * Circunscripción a la que pertenece el juzgado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circunscripcion_id", nullable = false)
    private Circunscripcion circunscripcion;

    /**
     * Hardware asignado a este juzgado.
     * Relación inversa - el dueño es Hardware.
     */
    @OneToMany(mappedBy = "juzgado", fetch = FetchType.LAZY)
    private List<Hardware> hardware;

    /**
     * Software asignado a este juzgado.
     * Relación inversa - el dueño es Software.
     */
    @OneToMany(mappedBy = "juzgado", fetch = FetchType.LAZY)
    private List<Software> software;

    /**
     * Tickets asociados a este juzgado.
     * Relación inversa - el dueño es Ticket.
     */
    @OneToMany(mappedBy = "juzgado", fetch = FetchType.LAZY)
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
