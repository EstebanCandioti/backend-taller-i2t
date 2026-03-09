package com.judicial.mesadeayuda.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.judicial.mesadeayuda.Entities.Rol;
import com.judicial.mesadeayuda.Entities.Usuario;

/**
 * Repository para la entidad Usuario.
 * Incluye consultas para autenticación JWT, gestión de roles y
 * listado de técnicos activos para el selector de asignación de tickets.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Busca un usuario por email.
     * Usado por Spring Security (UserDetailsService) para autenticación JWT.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica si ya existe un usuario con ese email.
     * Usado en el Service al crear o editar usuarios para evitar duplicados.
     */
    boolean existsByEmail(String email);

    /**
     * Lista todos los usuarios de un rol específico.
     * Ej: listar todos los técnicos, todos los operarios.
     */
    List<Usuario> findByRol(Rol rol);

    /**
     * Lista técnicos activos para el selector de asignación de tickets.
     * Solo devuelve técnicos con activo=true (pueden recibir tickets).
     * El @Where de la Entity ya filtra eliminado=0 automáticamente.
     */
    @Query("SELECT u FROM Usuario u WHERE u.rol.nombre = 'Técnico' AND u.activo = true")
    List<Usuario> findTecnicosActivos();

    /**
     * Lista usuarios filtrando por rol y estado activo.
     * Usado en el endpoint GET /api/usuarios con filtros.
     */
    List<Usuario> findByRolAndActivo(Rol rol, boolean activo);

    /**
     * Búsqueda de usuarios por nombre o apellido o email.
     * Usado para el filtro de búsqueda libre en la vista de usuarios.
     */
    @Query("""
                SELECT u FROM Usuario u
                WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    List<Usuario> buscarPorTexto(String q);

    @Query(value = "SELECT * FROM usuarios WHERE id = :id AND eliminado = 1", nativeQuery = true)
    Optional<Usuario> findEliminadoById(Integer id);

    @Modifying
    @Query(value = "UPDATE usuarios SET eliminado = 0, activo = 1, fecha_eliminacion = NULL, eliminado_por_id = NULL WHERE id = :id AND eliminado = 1", nativeQuery = true)
    int restore(Integer id);
}
