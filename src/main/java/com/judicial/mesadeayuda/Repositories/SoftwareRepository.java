package com.judicial.mesadeayuda.Repositories;

import com.judicial.mesadeayuda.Entities.Software;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad Software.
 * Soporta filtros y consultas de vencimiento de licencias.
 * Usado en GET /api/software y GET /api/software/vencimientos
 */
@Repository
public interface SoftwareRepository extends JpaRepository<Software, Integer> {

    /**
     * Filtro combinado para el listado de software.
     * Todos los parámetros son opcionales.
     * Usado en GET /api/software?contratoId=1&juzgadoId=3&proveedor=Microsoft
     */
    @Query("""
        SELECT DISTINCT s FROM Software s
        LEFT JOIN s.juzgados j
        WHERE (:contratoId IS NULL OR s.contrato.id = :contratoId)
          AND (:juzgadoId IS NULL OR j.id = :juzgadoId)
          AND (:proveedor IS NULL OR LOWER(s.proveedor) LIKE LOWER(CONCAT('%', :proveedor, '%')))
    """)
    Page<Software> findConFiltros(Integer contratoId, Integer juzgadoId, String proveedor, Pageable pageable);

    /**
     * Licencias próximas a vencer en los próximos X días.
     * Usado en GET /api/software/vencimientos?dias=30
     */
    @Query("""
        SELECT s FROM Software s
        WHERE s.fechaVencimiento IS NOT NULL
          AND s.fechaVencimiento >= :hoy
          AND s.fechaVencimiento <= :fechaLimite
        ORDER BY s.fechaVencimiento ASC
    """)
    List<Software> findProximasAVencer(LocalDate hoy, LocalDate fechaLimite);

    /**
     * Lista software asignado a un juzgado específico.
     * Usado en GET /api/juzgados/{id}/software
     */
    @Query("""
        SELECT DISTINCT s FROM Software s
        JOIN s.juzgados j
        WHERE j.id = :juzgadoId
        ORDER BY s.nombre ASC
    """)
    List<Software> findByJuzgadoId(Integer juzgadoId);

    /**
     * Lista software instalado en un equipo específico.
     * Usado en el detalle de hardware.
     */
    @Query("""
        SELECT DISTINCT s FROM Software s
        JOIN s.hardware h
        WHERE h.id = :hardwareId
    """)
    List<Software> findByHardwareId(Integer hardwareId);

    /**
     * Lista software vinculado a un contrato.
     * Usado en el detalle de contrato y para validar antes de eliminar contrato.
     */
    List<Software> findByContratoId(Integer contratoId);

    /**
     * Verifica si un contrato tiene software activo vinculado.
     * Regla de negocio: no se puede eliminar un contrato con software activo.
     */
    @Query("SELECT COUNT(s) > 0 FROM Software s WHERE s.contrato.id = :contratoId")
    boolean existsByContratoId(Integer contratoId);

    @Query("""
        SELECT COUNT(s) FROM Software s
        WHERE s.fechaVencimiento IS NOT NULL
          AND s.fechaVencimiento < :hoy
    """)
    long countLicenciasVencidas(LocalDate hoy);

    @Query("""
        SELECT COUNT(s) FROM Software s
        WHERE s.fechaVencimiento IS NOT NULL
          AND s.fechaVencimiento >= :hoy
          AND s.fechaVencimiento <= :fechaLimite
    """)
    long countLicenciasProximasAVencer(LocalDate hoy, LocalDate fechaLimite);

    @Query(value = "SELECT * FROM software WHERE id = :id AND eliminado = 1", nativeQuery = true)
    Optional<Software> findEliminadoById(Integer id);
}
