package com.judicial.mesadeayuda.Audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.judicial.mesadeayuda.Entities.AuditLog;

/**
 * Anotación para marcar métodos de Service que deben ser auditados.
 *
 * Uso en Services:
 *   @Auditable(entidad = "Ticket", accion = AuditLog.Accion.CREATE)
 *   public TicketResponseDTO crear(TicketRequestDTO dto) { ... }
 *
 * El AuditAspect intercepta los métodos anotados y registra
 * automáticamente la acción en audit_log.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Nombre de la entidad afectada.
     * Ej: "Ticket", "Hardware", "Software", "Contrato", "Usuario", "Juzgado"
     */
    String entidad();

    /**
     * Tipo de acción realizada.
     */
    AuditLog.Accion accion();
}