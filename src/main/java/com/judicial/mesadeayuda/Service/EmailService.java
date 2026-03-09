package com.judicial.mesadeayuda.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.judicial.mesadeayuda.Entities.Contrato;
import com.judicial.mesadeayuda.Entities.Software;
import com.judicial.mesadeayuda.Entities.Ticket;

/**
 * Service de envío de emails.
 *
 * Casos de uso:
 *   1. Notificación al técnico cuando se le asigna un ticket (@Async).
 *   2. Alerta de contratos próximos a vencer (llamado desde job programado).
 *
 * Los emails son asíncronos para no bloquear las operaciones principales.
 * Si falla el envío, se loguea el error pero NO se revierte la operación.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Email asíncrono al técnico cuando se le asigna un ticket.
     * No bloquea la respuesta HTTP al frontend.
     */
    @Async
    public void enviarNotificacionAsignacion(Ticket ticket) {
        try {
            String tecnicoEmail = ticket.getTecnico().getEmail();
            String tecnicoNombre = ticket.getTecnico().getNombreCompleto();

            String asunto = "Nuevo ticket asignado: #" + ticket.getId() + " - " + ticket.getTitulo();

            String cuerpo = String.format("""
                    Hola %s,

                    Se te ha asignado un nuevo ticket de soporte:

                    ─────────────────────────────────
                    Ticket #%d
                    Título: %s
                    Prioridad: %s
                    Juzgado: %s
                    Tipo: %s
                    ─────────────────────────────────

                    Descripción:
                    %s

                    Referente: %s - Tel: %s

                    Por favor, comunicarse con el referente del juzgado para coordinar la atención.

                    — Sistema Mesa de Ayuda - Poder Judicial Provincial
                    """,
                    tecnicoNombre,
                    ticket.getId(),
                    ticket.getTitulo(),
                    ticket.getPrioridad().name(),
                    ticket.getJuzgado().getNombre(),
                    ticket.getTipoRequerimiento() != null ? ticket.getTipoRequerimiento() : "No especificado",
                    ticket.getDescripcion(),
                    ticket.getReferenteNombre() != null ? ticket.getReferenteNombre() : "No indicado",
                    ticket.getReferenteTelefono() != null ? ticket.getReferenteTelefono() : "No indicado"
            );

            enviar(tecnicoEmail, asunto, cuerpo);
            log.info("Email de asignación enviado a {} para ticket #{}", tecnicoEmail, ticket.getId());

        } catch (Exception e) {
            log.error("Error al enviar email de asignación para ticket #{}: {}",
                    ticket.getId(), e.getMessage());
        }
    }

    /**
     * Email de alerta para contratos próximos a vencer.
     * Llamado desde el job programado diario.
     *
     * @param contratos lista de contratos próximos a vencer
     * @param destinatario email del admin/operario que debe recibir la alerta
     */
    public void enviarAlertaContratos(List<Contrato> contratos, String destinatario) {
        try {
            if (contratos.isEmpty()) return;

            StringBuilder cuerpo = new StringBuilder();
            cuerpo.append("Los siguientes contratos están próximos a vencer:\n\n");

            for (Contrato c : contratos) {
                long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), c.getFechaFin());
                cuerpo.append(String.format(
                        "• %s (Proveedor: %s) - Vence: %s (%d días restantes)\n",
                        c.getNombre(),
                        c.getProveedor(),
                        c.getFechaFin().format(DATE_FORMATTER),
                        diasRestantes
                ));
            }

            cuerpo.append("\nIngrese al sistema para revisar los detalles de cada contrato.\n\n");
            cuerpo.append("— Sistema Mesa de Ayuda - Poder Judicial Provincial");

            String asunto = "⚠ Alerta: " + contratos.size() + " contrato(s) próximo(s) a vencer";

            enviar(destinatario, asunto, cuerpo.toString());
            log.info("Email de alerta de contratos enviado a {} ({} contratos)", destinatario, contratos.size());

        } catch (Exception e) {
            log.error("Error al enviar alerta de contratos a {}: {}", destinatario, e.getMessage());
        }
    }

    /**
 * Email de alerta urgente por contratos vencidos con hardware sin cobertura.
 * Llamado desde ContratoVencimientoJob.
 */
public void enviarAlertaContratosVencidos(List<Contrato> contratos, String destinatario) {
    try {
        if (contratos.isEmpty()) return;

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("⚠ ALERTA URGENTE: Los siguientes contratos están VENCIDOS y tienen hardware activo sin cobertura:\n\n");

        for (Contrato c : contratos) {
            long diasVencido = ChronoUnit.DAYS.between(c.getFechaFin(), LocalDate.now());
            cuerpo.append(String.format(
                    "• %s (Proveedor: %s) - Venció: %s (hace %d días) - %d equipo(s) sin cobertura\n",
                    c.getNombre(),
                    c.getProveedor(),
                    c.getFechaFin().format(DATE_FORMATTER),
                    diasVencido,
                    c.getHardware() != null ? c.getHardware().size() : 0
            ));
        }

        cuerpo.append("\nEs necesario renovar estos contratos o reasignar el hardware a contratos vigentes.\n\n");
        cuerpo.append("— Sistema Mesa de Ayuda - Poder Judicial Provincial");

        String asunto = "🚨 URGENTE: " + contratos.size() + " contrato(s) vencido(s) con hardware activo";

        enviar(destinatario, asunto, cuerpo.toString());
        log.info("Email de alerta de contratos vencidos enviado a {} ({} contratos)", destinatario, contratos.size());

    } catch (Exception e) {
        log.error("Error al enviar alerta de contratos vencidos a {}: {}", destinatario, e.getMessage());
    }
}

/**
 * Email de alerta por licencias de software próximas a vencer.
 * Llamado desde SoftwareVencimientoJob.
 */
public void enviarAlertaSoftwareProximoAVencer(List<Software> licencias, String destinatario) {
    try {
        if (licencias.isEmpty()) return;

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Las siguientes licencias de software están próximas a vencer:\n\n");

        for (Software s : licencias) {
            long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), s.getFechaVencimiento());
            cuerpo.append(String.format(
                    "• %s (Proveedor: %s) - Vence: %s (%d días restantes) - %d licencia(s)\n",
                    s.getNombre(),
                    s.getProveedor(),
                    s.getFechaVencimiento().format(DATE_FORMATTER),
                    diasRestantes,
                    s.getCantidadLicencias()
            ));
        }

        cuerpo.append("\nIngrese al sistema para revisar los detalles y gestionar las renovaciones.\n\n");
        cuerpo.append("— Sistema Mesa de Ayuda - Poder Judicial Provincial");

        String asunto = "⚠ Alerta: " + licencias.size() + " licencia(s) de software próxima(s) a vencer";

        enviar(destinatario, asunto, cuerpo.toString());
        log.info("Email de alerta de software enviado a {} ({} licencias)", destinatario, licencias.size());

    } catch (Exception e) {
        log.error("Error al enviar alerta de software a {}: {}", destinatario, e.getMessage());
    }
}

/**
 * Email de alerta urgente por licencias de software ya vencidas.
 * Llamado desde SoftwareVencimientoJob.
 */
public void enviarAlertaSoftwareVencido(List<Software> licencias, String destinatario) {
    try {
        if (licencias.isEmpty()) return;

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("⚠ ALERTA URGENTE: Las siguientes licencias de software están VENCIDAS:\n\n");

        for (Software s : licencias) {
            long diasVencido = ChronoUnit.DAYS.between(s.getFechaVencimiento(), LocalDate.now());
            cuerpo.append(String.format(
                    "• %s (Proveedor: %s) - Venció: %s (hace %d días) - %d licencia(s)\n",
                    s.getNombre(),
                    s.getProveedor(),
                    s.getFechaVencimiento().format(DATE_FORMATTER),
                    diasVencido,
                    s.getCantidadLicencias()
            ));
        }

        cuerpo.append("\nEs necesario renovar estas licencias para mantener el cumplimiento.\n\n");
        cuerpo.append("— Sistema Mesa de Ayuda - Poder Judicial Provincial");

        String asunto = "🚨 URGENTE: " + licencias.size() + " licencia(s) de software VENCIDA(s)";

        enviar(destinatario, asunto, cuerpo.toString());
        log.info("Email de alerta de software vencido enviado a {} ({} licencias)", destinatario, licencias.size());

    } catch (Exception e) {
        log.error("Error al enviar alerta de software vencido a {}: {}", destinatario, e.getMessage());
    }
}

    // ── HELPER ────────────────────────────────────────────────

    private void enviar(String para, String asunto, String cuerpo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(para);
        message.setSubject(asunto);
        message.setText(cuerpo);
        mailSender.send(message);
    }
}