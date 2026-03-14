package com.judicial.mesadeayuda.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.judicial.mesadeayuda.Entities.Circunscripcion;
import com.judicial.mesadeayuda.Entities.Juzgado;

/**
 * Repository para la entidad Juzgado.
 * Soporta filtros por circunscripción, ciudad y fuero
 * según lo definido en el Mapa de Endpoints (GET /api/juzgados).
 */
@Repository
public interface JuzgadoRepository extends JpaRepository<Juzgado, Integer> {

    /**
     * Lista juzgados por circunscripción.
     * Usado en la vista de organización territorial.
     */
    List<Juzgado> findByCircunscripcion(Circunscripcion circunscripcion);

    /**
     * Lista juzgados por ciudad.
     * Ej: findByCiudad("Santa Fe")
     */
    List<Juzgado> findByCiudad(String ciudad);

    /**
     * Lista juzgados por fuero.
     * Ej: findByFuero("Civil")
     */
    List<Juzgado> findByFuero(String fuero);

    /**
     * Filtro combinado: circunscripción + ciudad + fuero.
     * Los parámetros son opcionales: si son null, no se aplica ese filtro.
     * Usado en GET /api/juzgados?circunscripcion=1&ciudad=Santa+Fe&fuero=Civil
     */
    @Query("""
        SELECT j FROM Juzgado j
        WHERE (:circunscripcionId IS NULL OR j.circunscripcion.id = :circunscripcionId)
          AND (:ciudad IS NULL OR LOWER(j.ciudad) = LOWER(:ciudad))
          AND (:fuero IS NULL OR LOWER(j.fuero) = LOWER(:fuero))
    """)
    List<Juzgado> findConFiltros(Integer circunscripcionId, String ciudad, String fuero);

    /**
     * Verifica si el juzgado tiene hardware activo asociado.
     * Usado antes de hacer soft-delete: no se puede eliminar un juzgado
     * con hardware, software o tickets activos.
     */
    @Query("SELECT COUNT(h) > 0 FROM Hardware h WHERE h.juzgado.id = :juzgadoId")
    boolean tieneHardwareActivo(Integer juzgadoId);

    /**
     * Verifica si el juzgado tiene software activo asociado.
     */
    @Query("""
        SELECT COUNT(DISTINCT s) > 0 FROM Software s
        JOIN s.juzgados j
        WHERE j.id = :juzgadoId
    """)
    boolean tieneSoftwareActivo(Integer juzgadoId);

    /**
     * Verifica si el juzgado tiene tickets activos (no cerrados).
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM Ticket t
        WHERE t.juzgado.id = :juzgadoId
          AND t.estado <> 'CERRADO'
    """)
    boolean tieneTicketsActivos(Integer juzgadoId);
}
