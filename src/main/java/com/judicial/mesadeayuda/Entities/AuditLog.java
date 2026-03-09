package com.judicial.mesadeayuda.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad de auditoría / trazabilidad.
 *
 * IMPORTANTE: Esta tabla NO tiene soft-delete.
 * Es el registro INMUTABLE de todas las acciones del sistema.
 * Nunca debe modificarse ni eliminarse bajo ninguna circunstancia.
 *
 * Se genera automáticamente via AOP (Aspect-Oriented Programming)
 * ante cada operación CREATE, UPDATE, DELETE, RESTORE, ASSIGN, CLOSE.
 *
 * Los campos valor_anterior y valor_nuevo almacenan el estado del registro
 * en formato JSON antes y después de la operación.
 */
@Entity
@Table(name = "audit_log",
       indexes = {
           @Index(name = "idx_audit_entidad_registro", columnList = "entidad, registro_id"),
           @Index(name = "idx_audit_usuario", columnList = "usuario_id"),
           @Index(name = "idx_audit_fecha", columnList = "fecha")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /**
     * Acciones posibles registradas en el audit log.
     * Corresponden a las operaciones definidas en el Mapa de Endpoints.
     */
    public enum Accion {
        CREATE,      // Alta de cualquier entidad
        UPDATE,      // Modificación de datos
        DELETE,      // Eliminación lógica (soft-delete)
        RESTORE,     // Restauración de eliminación lógica
        ASSIGN,      // Asignación de técnico a ticket
        CLOSE,       // Cierre de ticket
        ACTIVATE,    // Activación de usuario
        DEACTIVATE   // Desactivación de usuario
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que realizó la acción.
     * NULL si la acción fue del sistema (ej: job automático de alertas).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /**
     * Nombre de la entidad afectada.
     * Ej: "Ticket", "Hardware", "Software", "Contrato", "Usuario", "Juzgado"
     */
    @Column(name = "entidad", nullable = false, length = 100)
    private String entidad;

    /**
     * Acción realizada sobre la entidad.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 50)
    private Accion accion;

    /**
     * ID del registro afectado en su tabla original.
     */
    @Column(name = "registro_id", nullable = false)
    private Integer registroId;

    /**
     * Estado anterior del registro en formato JSON.
     * NULL en operaciones CREATE.
     */
    @Column(name = "valor_anterior", columnDefinition = "JSON")
    private String valorAnterior;

    /**
     * Nuevo estado del registro en formato JSON.
     * NULL en operaciones DELETE.
     */
    @Column(name = "valor_nuevo", columnDefinition = "JSON")
    private String valorNuevo;

    @Column(name = "fecha", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}