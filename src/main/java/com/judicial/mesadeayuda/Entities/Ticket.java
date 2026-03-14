package com.judicial.mesadeayuda.Entities;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Entidad principal del sistema: representa un ticket de soporte técnico.
 *
 * FLUJO DE ESTADOS:
 *   SOLICITADO → ASIGNADO → EN_CURSO → CERRADO
 *
 * REGLAS DE NEGOCIO:
 *   - La asignación de técnico es MANUAL (Operario/Admin).
 *   - Al asignar técnico: estado cambia a ASIGNADO + email automático.
 *   - El Técnico NO puede editar el ticket, solo tiene lectura.
 *   - El cierre lo hace Operario/Admin con campo "resolución" obligatorio.
 *   - El soft-delete solo está permitido en estado SOLICITADO (sin técnico).
 */
@Entity
@Table(name = "tickets",
       indexes = {
           @Index(name = "idx_tickets_estado", columnList = "estado"),
           @Index(name = "idx_tickets_prioridad", columnList = "prioridad"),
           @Index(name = "idx_tickets_juzgado", columnList = "juzgado_id"),
           @Index(name = "idx_tickets_tecnico", columnList = "tecnico_id"),
           @Index(name = "idx_tickets_creado_por", columnList = "creado_por_id"),
           @Index(name = "idx_tickets_eliminado", columnList = "eliminado")
       })
@SQLRestriction("eliminado = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    // ── Enums internos ────────────────────────────────────────

    public enum Prioridad {
        BAJA, MEDIA, ALTA, CRITICA
    }

    public enum Estado {
        SOLICITADO, ASIGNADO, EN_CURSO, CERRADO
    }

    // ── Campos ────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo; // Ej: "Impresora no imprime"

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 10)
    @Builder.Default
    private Prioridad prioridad = Prioridad.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private Estado estado = Estado.SOLICITADO;

    @Column(name = "tipo_requerimiento", length = 100)
    private String tipoRequerimiento; // Ej: "Hardware", "Software", "Red", "Otro"

    /**
     * Juzgado que reporta el problema. OBLIGATORIO.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juzgado_id", nullable = false)
    private Juzgado juzgado;

    /**
     * Técnico asignado manualmente por Operario/Admin.
     * NULL cuando el ticket está en estado SOLICITADO.
     * Al asignarse: estado → ASIGNADO + email automático al técnico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    /**
     * Equipo relacionado al problema. OPCIONAL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hardware_id")
    private Hardware hardware;

    @Column(name = "referente_nombre", length = 150)
    private String referenteNombre; // Nombre del referente en el juzgado

    @Column(name = "referente_telefono", length = 30)
    private String referenteTelefono;

    /**
     * Usuario (Operario/Admin) que creó el ticket. OBLIGATORIO.
     * Se obtiene del contexto de seguridad JWT en el Service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id", nullable = false)
    private Usuario creadoPor;

    /**
     * Descripción de la resolución. OBLIGATORIO al cerrar el ticket.
     */
    @Column(name = "resolucion", columnDefinition = "TEXT")
    private String resolucion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion; // Se setea al asignar técnico

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre; // Se setea al cerrar el ticket

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
     * Indica si el ticket puede ser asignado (solo en estado SOLICITADO).
     */
    public boolean puedeAsignarse() {
        return this.estado == Estado.SOLICITADO;
    }

    /**
     * Indica si el ticket puede ser cerrado (debe tener técnico asignado).
     */
    public boolean puedeCerrarse() {
        return this.estado == Estado.ASIGNADO || this.estado == Estado.EN_CURSO;
    }

    /**
     * Indica si el ticket puede ser eliminado lógicamente (solo SOLICITADO sin técnico).
     */
    public boolean puedeEliminarse() {
        return this.estado == Estado.SOLICITADO && this.tecnico == null;
    }
}
