package com.judicial.mesadeayuda.Repositories;

import com.judicial.mesadeayuda.Entities.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad Contrato.
 * Incluye consultas para alertas de vencimiento y filtros del dashboard.
 * Estas consultas son usadas por el job programado diario (ContratoService).
 */
@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {

    /**
     * Contratos próximos a vencer.
     * El job diario llama a este método pasando LocalDate.now().
     * Un contrato está "próximo a vencer" cuando la fecha actual está
     * dentro del rango de alerta configurado en cada contrato.
     *
     * Ej: contrato con fecha_fin = 30/03 y dias_alerta = 30
     *     → aparece en la lista desde el 28/02
     */
    @Query("""
        SELECT c FROM Contrato c
        WHERE c.fechaFin >= :hoy
          AND DATEDIFF(c.fechaFin, :hoy) <= c.diasAlertaVencimiento
        ORDER BY c.fechaFin ASC
    """)
    List<Contrato> findProximosAVencer(LocalDate hoy);

    /**
     * Contratos ya vencidos (fecha_fin en el pasado).
     * Usado en GET /api/contratos/vencidos
     */
    List<Contrato> findByFechaFinBeforeOrderByFechaFinDesc(LocalDate fecha);

    /**
     * Contratos activos (fecha_fin >= hoy).
     * Usado en el filtro de estado del listado de contratos.
     */
    List<Contrato> findByFechaFinGreaterThanEqualOrderByFechaFinAsc(LocalDate fecha);

    /**
     * Filtra contratos por proveedor (búsqueda parcial, case-insensitive).
     */
    List<Contrato> findByProveedorContainingIgnoreCase(String proveedor);

    /**
     * Verifica si un contrato tiene software vinculado activo.
     * Usado antes de hacer soft-delete: no se puede eliminar un contrato
     * con software activo vinculado (rompe la regla de negocio).
     */
    @Query("SELECT COUNT(s) > 0 FROM Software s WHERE s.contrato.id = :contratoId")
    boolean tieneSoftwareActivo(Integer contratoId);

    @Query("""
        SELECT COUNT(c) FROM Contrato c
        WHERE c.fechaFin < :hoy
    """)
    long countVencidos(LocalDate hoy);

    @Query("""
        SELECT COUNT(c) FROM Contrato c
        WHERE c.fechaFin >= :hoy
          AND DATEDIFF(c.fechaFin, :hoy) <= c.diasAlertaVencimiento
    """)
    long countProximosAVencer(LocalDate hoy);

    @Query(value = "SELECT * FROM contratos WHERE id = :id AND eliminado = 1", nativeQuery = true)
    Optional<Contrato> findEliminadoById(Integer id);
}
