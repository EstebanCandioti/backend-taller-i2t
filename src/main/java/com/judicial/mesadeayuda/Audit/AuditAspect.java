package com.judicial.mesadeayuda.Audit;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.judicial.mesadeayuda.Entities.AuditLog;
import com.judicial.mesadeayuda.Service.AuditLogService;

/**
 * Aspect de auditoría que intercepta métodos anotados con @Auditable.
 *
 * FLUJO:
 *   1. @Before: Para UPDATE/DELETE, captura el estado ANTERIOR del registro
 *      (via el ID que viene como primer parámetro del método).
 *   2. @AfterReturning: Después de la ejecución exitosa, registra en audit_log
 *      con el estado nuevo (el objeto retornado por el método).
 *
 * CONVENCIONES que los Services deben respetar:
 *   - Métodos crear(): retornan el DTO del objeto creado.
 *   - Métodos editar(Integer id, ...): primer arg es el ID.
 *   - Métodos eliminar(Integer id): primer arg es el ID.
 *   - Métodos asignar(Integer id, ...): primer arg es el ID del ticket.
 *   - Métodos cerrar(Integer id, ...): primer arg es el ID del ticket.
 *   - Métodos activar(Integer id) / desactivar(Integer id): primer arg es el ID.
 *
 * NOTA: Si el método lanza excepción, NO se registra auditoría (es correcto:
 * solo auditamos operaciones exitosas).
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * ThreadLocal para guardar el estado anterior entre @Before y @AfterReturning
     * del mismo hilo de ejecución.
     */
    private final ThreadLocal<String> valorAnteriorHolder = new ThreadLocal<>();

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // ── @Before: Capturar estado anterior para UPDATE/DELETE ──

    /**
     * Para acciones que modifican o eliminan, necesitamos el estado ANTES del cambio.
     * Se ejecuta antes del método del Service.
     */
    @Before("@annotation(auditable)")
    public void capturarEstadoAnterior(JoinPoint joinPoint, Auditable auditable) {
        try {
            AuditLog.Accion accion = auditable.accion();

            // Solo capturar estado anterior para acciones que modifican registros existentes
            if (necesitaEstadoAnterior(accion)) {
                Object[] args = joinPoint.getArgs();
                if (args.length > 0 && args[0] instanceof Integer id) {
                    // Obtener el estado actual del registro antes de modificarlo
                    String entidad = auditable.entidad();
                    Object estadoAnterior = obtenerEstadoActual(joinPoint, id, entidad);
                    if (estadoAnterior != null) {
                        valorAnteriorHolder.set(objectMapper.writeValueAsString(estadoAnterior));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error capturando estado anterior para auditoría: {}", e.getMessage());
            // No interrumpir la operación principal
        }
    }

    // ── @AfterReturning: Registrar en audit_log ───────────────

    /**
     * Después de que el método del Service se ejecuta exitosamente,
     * registra la acción en audit_log.
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "resultado")
    public void registrarAuditoria(JoinPoint joinPoint, Auditable auditable, Object resultado) {
        try {
            String entidad = auditable.entidad();
            AuditLog.Accion accion = auditable.accion();

            Integer registroId = extraerRegistroId(joinPoint, resultado, accion);

            if (registroId == null) {
                log.warn("No se pudo extraer el ID del registro para auditar: {}.{}",
                        entidad, accion);
                return;
            }

            // Estado nuevo (el resultado del método, serializado a JSON)
            String valorNuevo = null;
            if (accion != AuditLog.Accion.DELETE) {
                valorNuevo = objectMapper.writeValueAsString(resultado);
            }

            // Estado anterior (capturado en @Before)
            String valorAnterior = valorAnteriorHolder.get();
            valorAnteriorHolder.remove(); // Limpiar ThreadLocal

            // Para CREATE no hay valor anterior
            if (accion == AuditLog.Accion.CREATE) {
                valorAnterior = null;
            }

            auditLogService.registrar(entidad, accion, registroId, valorAnterior, valorNuevo);

            log.debug("Auditoría registrada: {} {} #{}", accion, entidad, registroId);

        } catch (Exception e) {
            log.error("Error registrando auditoría: {}", e.getMessage());
            // NUNCA interrumpir la operación principal por un error de auditoría
        }
    }

    // ── HELPERS PRIVADOS ──────────────────────────────────────

    /**
     * Determina si la acción necesita capturar el estado anterior.
     */
    private boolean necesitaEstadoAnterior(AuditLog.Accion accion) {
        return accion == AuditLog.Accion.UPDATE
                || accion == AuditLog.Accion.DELETE
                || accion == AuditLog.Accion.ASSIGN
                || accion == AuditLog.Accion.CLOSE
                || accion == AuditLog.Accion.ACTIVATE
                || accion == AuditLog.Accion.DEACTIVATE;
    }

    /**
     * Extrae el ID del registro afectado.
     * - Para CREATE: del objeto retornado (resultado.getId())
     * - Para UPDATE/DELETE/ASSIGN/CLOSE: del primer argumento del método
     */
    private Integer extraerRegistroId(JoinPoint joinPoint, Object resultado, AuditLog.Accion accion) {
        if (accion == AuditLog.Accion.CREATE) {
            // El resultado es el DTO creado, extraer su ID via reflection
            return extraerIdDeObjeto(resultado);
        } else {
            // El primer parámetro es el ID
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof Integer id) {
                return id;
            }
        }
        return null;
    }

    /**
     * Extrae el campo "id" de un objeto DTO via reflection.
     */
    private Integer extraerIdDeObjeto(Object obj) {
        if (obj == null) return null;
        try {
            Method getId = obj.getClass().getMethod("getId");
            Object id = getId.invoke(obj);
            if (id instanceof Integer) return (Integer) id;
            if (id instanceof Long) return ((Long) id).intValue();
        } catch (Exception e) {
            log.warn("No se pudo extraer ID del objeto {}: {}", obj.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene el estado actual de un registro antes de ser modificado.
     * Llama al método "obtenerPorId" del mismo Service interceptado.
     */
    private Object obtenerEstadoActual(JoinPoint joinPoint, Integer id, String entidad) {
        try {
            Object target = joinPoint.getTarget();
            Method obtenerPorId = target.getClass().getMethod("obtenerPorId", Integer.class);
            return obtenerPorId.invoke(target, id);
        } catch (Exception e) {
            log.warn("No se pudo obtener estado anterior de {} #{}: {}", entidad, id, e.getMessage());
            return null;
        }
    }
}