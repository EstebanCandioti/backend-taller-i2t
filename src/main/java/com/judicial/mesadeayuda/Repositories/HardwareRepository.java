package com.judicial.mesadeayuda.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.judicial.mesadeayuda.Entities.Hardware;

/**
 * Repository para la entidad Hardware.
 * Soporta los filtros definidos en el Mapa de Endpoints:
 * GET
 * /api/hardware?juzgadoId=3&clase=PC+Desktop&modelo=ProDesk&ubicacion=Piso+2
 */
@Repository
public interface HardwareRepository extends JpaRepository<Hardware, Integer> {

    /**
     * Busca un equipo por número de inventario.
     * El nro_inventario es único en la BD.
     */
    Optional<Hardware> findByNroInventario(String nroInventario);

    /**
     * Verifica si ya existe un equipo con ese número de inventario.
     * Usado al crear hardware para evitar duplicados.
     */
    boolean existsByNroInventario(String nroInventario);

    /**
     * Filtro combinado para el listado de hardware.
     * Todos los parámetros son opcionales (IS NULL = no aplica ese filtro).
     * Usado en GET /api/hardware con query params.
     */
    @Query("""
                SELECT h FROM Hardware h
                WHERE (:juzgadoId IS NULL OR h.juzgado.id = :juzgadoId)
                  AND (:clase IS NULL OR LOWER(h.clase) LIKE LOWER(CONCAT('%', :clase, '%')))
                  AND (:modelo IS NULL OR LOWER(h.modelo) LIKE LOWER(CONCAT('%', :modelo, '%')))
                  AND (:ubicacion IS NULL OR LOWER(h.ubicacionFisica) LIKE LOWER(CONCAT('%', :ubicacion, '%')))
                  AND (:q IS NULL
                       OR LOWER(h.nroInventario) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(h.marca) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(h.modelo) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(h.clase) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(h.nroSerie) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(h.ubicacionFisica) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Hardware> findConFiltros(Integer juzgadoId, String clase, String modelo, String ubicacion, String q, Pageable pageable);

    /**
     * Lista todo el hardware asignado a un juzgado.
     * Usado en GET /api/juzgados/{id}/hardware
     */
    List<Hardware> findByJuzgadoId(Integer juzgadoId);

    /**
     * Lista hardware vinculado a un contrato específico.
     * Usado en el detalle de contrato.
     */
    List<Hardware> findByContratoId(Integer contratoId);

    /**
     * Verifica si un equipo tiene tickets activos asociados.
     * Usado antes de hacer soft-delete: no se puede eliminar hardware
     * con tickets activos vinculados.
     */
    @Query("""
                SELECT COUNT(t) > 0 FROM Ticket t
                WHERE t.hardware.id = :hardwareId
                  AND t.estado <> 'CERRADO'
            """)
    boolean tieneTicketsActivos(Integer hardwareId);

    /**
     * Genera el próximo número de inventario correlativo para el año actual.
     * Formato: HW-YYYY-XXXX (ej: HW-2025-0042)
     * Usado en InventarioService cuando no se provee nroInventario.
     */
    @Query("""
                SELECT COUNT(h) FROM Hardware h
                WHERE h.nroInventario LIKE CONCAT('HW-', :anio, '-%')
            """)
    long countByAnio(int anio);

    /**
     * Busca un hardware eliminado por ID (bypasea @SQLRestriction).
     * Usado en el restore de soft-delete.
     */
    @Query(value = "SELECT * FROM hardware WHERE id = :id AND eliminado = 1", nativeQuery = true)
    Optional<Hardware> findEliminadoById(Integer id);
}