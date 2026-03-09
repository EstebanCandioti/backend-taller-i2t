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
@Table(name = "circunscripciones")
@SQLRestriction("eliminado=false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Circunscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre; // Ej: "1ra Circunscripción"

    @Column(name = "distrito_judicial", nullable = false, length = 100)
    private String distritoJudicial; // Ej: "Santa Fe", "Rosario"

    /**
     * Juzgados que pertenecen a esta circunscripción.
     * mappedBy indica que Juzgado es el dueño de la relación.
     */
    @OneToMany(mappedBy = "circunscripcion", fetch = FetchType.LAZY)
    private List<Juzgado> juzgados;

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
