package com.judicial.mesadeayuda.Repositories;

import com.judicial.mesadeayuda.Entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para la entidad AuditLog.
 * Solo tiene operaciones de lectura y escritura (nunca UPDATE ni DELETE).
 * El audit_log es un registro inmutable: solo se inserta, nunca se modifica.
 *
 * Usado en GET /api/audit con los filtros definidos en el Mapa de Endpoints.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Historial completo de una entidad específica.
     * Ej: todos los cambios del Ticket con id=5
     * Usado en GET /api/audit/{entidad}/{registroId}
     */
    List<AuditLog> findByEntidadAndRegistroIdOrderByFechaDesc(String entidad, Integer registroId);

    /**
     * Todas las acciones realizadas por un usuario.
     * Usado en GET /api/audit/usuario/{usuarioId}
     */
    List<AuditLog> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    /**
     * Filtro general del listado de auditoría.
     * Usado en GET /api/audit con query params.
     * Spring Data genera la query automáticamente por el nombre del método.
     */
    List<AuditLog> findByEntidadAndAccionAndFechaBetweenOrderByFechaDesc(
            String entidad,
            AuditLog.Accion accion,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    );

    /**
     * Registros de auditoría entre dos fechas.
     * Usado para el filtro por rango de fechas en el listado general.
     */
    List<AuditLog> findByFechaBetweenOrderByFechaDesc(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    );
}
