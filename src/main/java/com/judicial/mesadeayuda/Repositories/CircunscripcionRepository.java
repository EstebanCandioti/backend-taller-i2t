package com.judicial.mesadeayuda.Repositories;

import com.judicial.mesadeayuda.Entities.Circunscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad Circunscripcion.
 * Datos semi-estáticos de la estructura territorial.
 * Las consultas son simples ya que es el nivel más alto de la jerarquía.
 */
@Repository
public interface CircunscripcionRepository extends JpaRepository<Circunscripcion, Integer> {

    /**
     * Busca circunscripciones por distrito judicial.
     * Ej: findByDistritoJudicial("Santa Fe")
     */
    List<Circunscripcion> findByDistritoJudicial(String distritoJudicial);

    /**
     * Verifica si existe una circunscripción con ese nombre.
     * Usado para evitar duplicados al crear.
     */
    boolean existsByNombre(String nombre);

    /**
     * Busca una circunscripción eliminada por ID (bypasea @SQLRestriction).
     * Usado para restaurar registros soft-deleted.
     */
    @Query(value = "SELECT * FROM circunscripciones WHERE id = :id AND eliminado = 1", nativeQuery = true)
    Optional<Circunscripcion> findEliminadoById(Integer id);
}