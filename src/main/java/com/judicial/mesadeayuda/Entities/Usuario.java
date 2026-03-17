package com.judicial.mesadeayuda.Entities;

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

@Entity
@Table(name = "usuarios")
@SQLRestriction("eliminado = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password; // Hash BCrypt - nunca exponer en respuestas

    @Column(name = "telefono", length = 30)
    private String telefono;

    /**
     * Rol asignado al usuario. Define los permisos en el sistema.
     * LAZY para no cargar siempre que se recupere el usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    /**
     * Indica si el usuario puede acceder al sistema.
     * false = acceso bloqueado (no confundir con eliminado lógico).
     */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaAlta = LocalDateTime.now();

    // ── Soft-delete ───────────────────────────────────────────
    @Column(name = "eliminado", nullable = false)
    @Builder.Default
    private boolean eliminado = false;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    /**
     * Usuario Admin que realizó la eliminación lógica.
     * Self-referencing: un Usuario puede eliminar a otro Usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eliminado_por_id")
    private Usuario eliminadoPor;

    // ── Método utilitario ─────────────────────────────────────

    /**
     * Devuelve el nombre completo del usuario.
     */
    public String getNombreCompleto() {
        return this.nombre + " " + this.apellido;
    }
}
