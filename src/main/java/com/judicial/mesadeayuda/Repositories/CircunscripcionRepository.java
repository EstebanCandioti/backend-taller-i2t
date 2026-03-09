package com.judicial.mesadeayuda.Repositories;

import com.judicial.mesadeayuda.Entities.Circunscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    List<CircunscripcionRepository> findByDistritoJudicial(String distritoJudicial);

    /**
     * Verifica si existe una circunscripción con ese nombre.
     * Usado para evitar duplicados al crear.
     */
    boolean existsByNombre(String nombre);
}