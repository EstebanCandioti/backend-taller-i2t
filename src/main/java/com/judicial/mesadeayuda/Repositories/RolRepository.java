package com.judicial.mesadeayuda.Repositories;

import com.judicial.mesadeayuda.Entities.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository para la entidad Rol.
 * Los roles son datos de configuración estática (Admin, Operario, Técnico).
 * No requiere consultas complejas.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    /**
     * Busca un rol por nombre exacto.
     * Usado en el Service al crear usuarios para asignar el rol correcto.
     * Ej: rolRepository.findByNombre("Admin")
     */
    Optional<Rol> findByNombre(String nombre);
}