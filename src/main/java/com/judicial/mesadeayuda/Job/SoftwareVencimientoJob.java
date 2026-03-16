package com.judicial.mesadeayuda.Job;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.judicial.mesadeayuda.Entities.Software;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Repositories.SoftwareRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Service.EmailService;
import com.judicial.mesadeayuda.Service.NotificationWebSocketService;

/**
 * Job programado que se ejecuta diariamente a las 08:15hs.
 *
 * FLUJO:
 *   1. Licencias próximas a vencer (dentro de los próximos 30 días):
 *      - Envía email de alerta a Admin y Operarios activos.
 *   2. Licencias ya vencidas:
 *      - Detecta software cuya fecha_vencimiento ya pasó.
 *      - Envía email de alerta urgente.
 *
 * Configuración:
 *   - Horario: 08:15hs todos los días (cron: "0 15 8 * * *")
 *   - Se ejecuta 15 minutos después del ContratoVencimientoJob para no solaparse.
 *   - El pool de scheduling está configurado en AsyncConfig con @EnableScheduling.
 *
 * Tolerancia a fallos:
 *   - Si falla el envío de email a un destinatario, continúa con los demás.
 *   - Todos los errores se loguean sin interrumpir el job.
 */
@Component
public class SoftwareVencimientoJob {

    private static final Logger log = LoggerFactory.getLogger(SoftwareVencimientoJob.class);
    private static final int DIAS_ALERTA_DEFAULT = 30;

    private final SoftwareRepository softwareRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final NotificationWebSocketService notificationWsService;

    public SoftwareVencimientoJob(SoftwareRepository softwareRepository,
                                  UsuarioRepository usuarioRepository,
                                  EmailService emailService,
                                  NotificationWebSocketService notificationWsService) {
        this.softwareRepository = softwareRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.notificationWsService = notificationWsService;
    }

    /**
     * Ejecuta la verificación diaria de licencias de software.
     * Cron: "0 15 8 * * *" = todos los días a las 08:15:00
     */
    @Scheduled(cron = "0 15 8 * * *")
    public void verificarLicenciasSoftware() {
        log.info("═══ Iniciando job de vencimiento de licencias de software ═══");

        try {
            LocalDate hoy = LocalDate.now();
            List<Usuario> destinatarios = obtenerDestinatarios();

            if (destinatarios.isEmpty()) {
                log.warn("No se encontraron usuarios Admin/Operario activos para enviar alertas");
                return;
            }

            // 1. Licencias próximas a vencer (dentro de los próximos 30 días)
            verificarProximasAVencer(hoy, destinatarios);

            // 2. Licencias ya vencidas
            verificarVencidas(hoy, destinatarios);

            log.info("═══ Job de vencimiento de licencias de software finalizado ═══");

        } catch (Exception e) {
            log.error("Error crítico en el job de vencimiento de licencias de software: ", e);
        }
    }

    /**
     * Busca licencias de software próximas a vencer y envía alerta.
     */
    private void verificarProximasAVencer(LocalDate hoy, List<Usuario> destinatarios) {
        LocalDate fechaLimite = hoy.plusDays(DIAS_ALERTA_DEFAULT);
        List<Software> proximasAVencer = softwareRepository.findProximasAVencer(hoy, fechaLimite);

        if (proximasAVencer.isEmpty()) {
            log.info("No hay licencias de software próximas a vencer en los próximos {} días.", DIAS_ALERTA_DEFAULT);
            return;
        }

        log.info("Se encontraron {} licencia(s) de software próxima(s) a vencer", proximasAVencer.size());

        int enviados = 0;
        int fallidos = 0;

        for (Usuario usuario : destinatarios) {
            try {
                emailService.enviarAlertaSoftwareProximoAVencer(proximasAVencer, usuario.getEmail());
                enviados++;
            } catch (Exception e) {
                fallidos++;
                log.error("Error enviando alerta de software a {}: {}", usuario.getEmail(), e.getMessage());
            }
        }

        log.info("Alertas de licencias próximas a vencer: {} enviados, {} fallidos", enviados, fallidos);

        for (Software software : proximasAVencer) {
            long diasRestantes = software.getFechaVencimiento().toEpochDay() - hoy.toEpochDay();
            notificationWsService.notificarPorRol(
                    List.of("Admin", "Operario"),
                    "LICENCIA_POR_VENCER",
                    "Software",
                    software.getId(),
                    "La licencia '" + software.getNombre() + "' vence en " + diasRestantes + " dias"
            );
        }
    }

    /**
     * Busca licencias de software ya vencidas y envía alerta urgente.
     */
    private void verificarVencidas(LocalDate hoy, List<Usuario> destinatarios) {
        List<Software> vencidas = softwareRepository.findAll().stream()
                .filter(s -> s.getFechaVencimiento() != null && s.getFechaVencimiento().isBefore(hoy))
                .toList();

        if (vencidas.isEmpty()) {
            log.info("No hay licencias de software vencidas.");
            return;
        }

        log.warn("Se encontraron {} licencia(s) de software vencida(s)", vencidas.size());

        int enviados = 0;
        int fallidos = 0;

        for (Usuario usuario : destinatarios) {
            try {
                emailService.enviarAlertaSoftwareVencido(vencidas, usuario.getEmail());
                enviados++;
            } catch (Exception e) {
                fallidos++;
                log.error("Error enviando alerta de software vencido a {}: {}", usuario.getEmail(), e.getMessage());
            }
        }

        log.info("Alertas de licencias vencidas: {} enviados, {} fallidos", enviados, fallidos);
    }

    /**
     * Obtiene todos los usuarios con rol Admin u Operario que estén activos.
     */
    private List<Usuario> obtenerDestinatarios() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.isActivo())
                .filter(u -> {
                    String rolNombre = u.getRol().getNombre();
                    return "Admin".equals(rolNombre) || "Operario".equals(rolNombre);
                })
                .toList();
    }
}


