package com.judicial.mesadeayuda.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.judicial.mesadeayuda.Entities.Ticket;

/**
 * Repository para la entidad Ticket.
 * Es el repository más complejo del sistema por la cantidad de filtros
 * y las restricciones de acceso por rol.
 *
 * IMPORTANTE: los técnicos solo ven sus propios tickets.
 * Esto se implementa a nivel de Service usando el usuarioId del contexto JWT.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /**
     * Filtro completo para Admin/Operario.
     * Todos los parámetros son opcionales (IS NULL = no aplica el filtro).
     * Usado en GET /api/tickets con todos los query params disponibles.
     */
    @Query("""
                SELECT t FROM Ticket t
                WHERE (:estado IS NULL OR t.estado = :estado)
                  AND (:prioridad IS NULL OR t.prioridad = :prioridad)
                  AND (:juzgadoId IS NULL OR t.juzgado.id = :juzgadoId)
                  AND (:tecnicoId IS NULL OR t.tecnico.id = :tecnicoId)
                  AND (:q IS NULL OR LOWER(t.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                                  OR LOWER(t.descripcion) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Ticket> findConFiltros(
            Ticket.Estado estado,
            Ticket.Prioridad prioridad,
            Integer juzgadoId,
            Integer tecnicoId,
            String q,
            Pageable pageable);

    /**
     * Tickets asignados a un técnico específico (vista del Técnico).
     * Solo devuelve tickets no cerrados para la vista principal.
     * Usado cuando el usuario autenticado tiene rol Técnico.
     */
    @Query("""
                SELECT t FROM Ticket t
                WHERE t.tecnico.id = :tecnicoId
                  AND t.estado <> 'CERRADO'
                ORDER BY
                    CASE t.prioridad
                        WHEN 'CRITICA' THEN 1
                        WHEN 'ALTA' THEN 2
                        WHEN 'MEDIA' THEN 3
                        WHEN 'BAJA' THEN 4
                    END ASC
            """)
    List<Ticket> findTicketsActivosByTecnico(Integer tecnicoId);

    /**
     * Todos los tickets de un técnico (incluyendo cerrados).
     * Usado para el historial completo del técnico.
     */
    List<Ticket> findByTecnicoId(Integer tecnicoId);

    /**
     * Tickets de un técnico con paginación.
     */
    Page<Ticket> findByTecnicoId(Integer tecnicoId, Pageable pageable);

    /**
     * Tickets de un juzgado específico.
     * Usado en GET /api/juzgados/{id}/tickets
     */
    List<Ticket> findByJuzgadoId(Integer juzgadoId);

    /**
     * Tickets asociados a un equipo de hardware.
     * Usado en GET /api/hardware/{id}/tickets
     */
    List<Ticket> findByHardwareId(Integer hardwareId);

    /**
     * Cuenta tickets activos asignados a un técnico.
     * Usado en el selector de técnicos al asignar tickets
     * para mostrar la carga actual de cada técnico.
     */
    @Query("""
                SELECT COUNT(t) FROM Ticket t
                WHERE t.tecnico.id = :tecnicoId
                  AND t.estado <> 'CERRADO'
            """)
    long countTicketsActivosByTecnico(Integer tecnicoId);

    /**
     * Verifica si un técnico tiene tickets activos.
     * Usado antes de desactivar o eliminar lógicamente un usuario técnico.
     */
    @Query("""
                SELECT COUNT(t) > 0 FROM Ticket t
                WHERE t.tecnico.id = :tecnicoId
                  AND t.estado <> 'CERRADO'
            """)
    boolean tieneTicketsActivosByTecnico(Integer tecnicoId);

    /**
     * Métricas para el dashboard: tickets agrupados por estado.
     * Devuelve [estado, cantidad].
     */
    @Query("SELECT t.estado, COUNT(t) FROM Ticket t GROUP BY t.estado")
    List<Object[]> countByEstado();

    /**
     * Métricas para el dashboard: tickets agrupados por prioridad.
     * Devuelve [prioridad, cantidad].
     */
    @Query("SELECT t.prioridad, COUNT(t) FROM Ticket t GROUP BY t.prioridad")
    List<Object[]> countByPrioridad();

    @Query("""
                SELECT COUNT(t) FROM Ticket t
                WHERE t.tecnico IS NULL
                  AND t.estado <> 'CERRADO'
            """)
    long countSinAsignar();

    @Query("""
                SELECT COUNT(t) FROM Ticket t
                WHERE t.estado <> 'CERRADO'
            """)
    long countActivos();

    /**
     * Busca un ticket eliminado por ID (bypasea @SQLRestriction).
     * Usado en el restore de soft-delete.
     */
    @Query(value = "SELECT * FROM tickets WHERE id = :id AND eliminado = 1", nativeQuery = true)
    Optional<Ticket> findEliminadoById(Integer id);
}
