package com.judicial.mesadeayuda.Job;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.judicial.mesadeayuda.Entities.Contrato;
import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Repositories.ContratoRepository;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import com.judicial.mesadeayuda.Service.EmailService;
import com.judicial.mesadeayuda.Service.NotificationWebSocketService;

/**
 * Job programado que se ejecuta diariamente a las 08:00hs.
 *
 * FLUJO:
 *   1. Contratos próximos a vencer:
 *      - Consulta contratos con fecha_fin dentro de su rango de alerta individual.
 *      - Envía email de alerta a Admin y Operarios activos.
 *   2. Contratos ya vencidos con hardware activo:
 *      - Detecta contratos cuya fecha_fin ya pasó y que tienen hardware vinculado.
 *      - Envía email de alerta urgente indicando hardware sin cobertura.
 *
 * Configuración:
 *   - Horario: 08:00hs todos los días (cron: "0 0 8 * * *")
 *   - La zona horaria se toma del servidor.
 *   - El pool de scheduling está configurado en AsyncConfig con @EnableScheduling.
 *   - Cada contrato define sus propios días de alerta (campo dias_alerta_vencimiento).
 *
 * Tolerancia a fallos:
 *   - Si falla el envío de email a un destinatario, continúa con los demás.
 *   - Todos los errores se loguean sin interrumpir el job.
 */
@Component
public class ContratoVencimientoJob {

    private static final Logger log = LoggerFactory.getLogger(ContratoVencimientoJob.class);

    private final ContratoRepository contratoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final NotificationWebSocketService notificationWsService;

    public ContratoVencimientoJob(ContratoRepository contratoRepository,
                                  UsuarioRepository usuarioRepository,
                                  EmailService emailService,
                                  NotificationWebSocketService notificationWsService) {
        this.contratoRepository = contratoRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.notificationWsService = notificationWsService;
    }

    /**
     * Ejecuta la verificación diaria de contratos.
     * Cron: "0 0 8 * * *" = todos los días a las 08:00:00
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void verificarContratos() {
        log.info("═══ Iniciando job de vencimiento de contratos ═══");

        try {
            LocalDate hoy = LocalDate.now();
            List<Usuario> destinatarios = obtenerDestinatarios();

            if (destinatarios.isEmpty()) {
                log.warn("No se encontraron usuarios Admin/Operario activos para enviar alertas");
                return;
            }

            // 1. Contratos próximos a vencer
            verificarProximosAVencer(hoy, destinatarios);

            // 2. Contratos ya vencidos con hardware activo vinculado
            verificarVencidosConHardware(hoy, destinatarios);

            log.info("═══ Job de vencimiento de contratos finalizado ═══");

        } catch (Exception e) {
            log.error("Error crítico en el job de vencimiento de contratos: ", e);
        }
    }

    /**
     * Busca contratos próximos a vencer y envía alerta.
     */
    private void verificarProximosAVencer(LocalDate hoy, List<Usuario> destinatarios) {
        List<Contrato> contratosProximos = contratoRepository.findProximosAVencer(hoy);

        if (contratosProximos.isEmpty()) {
            log.info("No hay contratos próximos a vencer.");
            return;
        }

        log.info("Se encontraron {} contrato(s) próximo(s) a vencer", contratosProximos.size());
        enviarAlertas(contratosProximos, destinatarios, "próximos a vencer");

        for (Contrato contrato : contratosProximos) {
            long diasRestantes = contrato.getFechaFin().toEpochDay() - hoy.toEpochDay();
            notificationWsService.notificarPorRol(
                    List.of("Admin", "Operario"),
                    "CONTRATO_POR_VENCER",
                    "Contrato",
                    contrato.getId(),
                    "El contrato '" + contrato.getNombre() + "' vence en " + diasRestantes + " dias"
            );
        }
    }

    /**
     * Busca contratos ya vencidos que tengan hardware activo vinculado.
     * Estos representan equipos sin cobertura contractual.
     */
    private void verificarVencidosConHardware(LocalDate hoy, List<Usuario> destinatarios) {
        List<Contrato> contratosVencidos = contratoRepository
                .findByFechaFinBeforeOrderByFechaFinDesc(hoy);

        // Filtrar solo los que tienen hardware activo vinculado
        List<Contrato> vencidosConHardware = contratosVencidos.stream()
                .filter(c -> c.getHardware() != null && !c.getHardware().isEmpty())
                .toList();

        if (vencidosConHardware.isEmpty()) {
            log.info("No hay contratos vencidos con hardware activo vinculado.");
            return;
        }

        log.warn("Se encontraron {} contrato(s) vencido(s) con hardware activo", vencidosConHardware.size());
        enviarAlertasVencidos(vencidosConHardware, destinatarios);
    }

    /**
     * Envía email de alerta de contratos próximos a vencer.
     * Si falla el envío a un destinatario, notifica via WebSocket.
     */
    private void enviarAlertas(List<Contrato> contratos, List<Usuario> destinatarios, String tipo) {
        int enviados = 0;
        int fallidos = 0;

        for (Usuario usuario : destinatarios) {
            try {
                emailService.enviarAlertaContratos(contratos, usuario.getEmail());
                enviados++;
            } catch (Exception e) {
                fallidos++;
                log.error("Error enviando alerta ({}) a {}: {}", tipo, usuario.getEmail(), e.getMessage());
                notificarFalloEmail(usuario.getEmail(),
                        "Fallo al enviar email de alerta de contratos " + tipo + " a " + usuario.getEmail());
            }
        }

        log.info("Alertas de contratos {}: {} enviados, {} fallidos", tipo, enviados, fallidos);
    }

    /**
     * Envía email de alerta urgente por contratos vencidos con hardware sin cobertura.
     * Si falla el envío a un destinatario, notifica via WebSocket.
     */
    private void enviarAlertasVencidos(List<Contrato> contratos, List<Usuario> destinatarios) {
        int enviados = 0;
        int fallidos = 0;

        for (Usuario usuario : destinatarios) {
            try {
                emailService.enviarAlertaContratosVencidos(contratos, usuario.getEmail());
                enviados++;
            } catch (Exception e) {
                fallidos++;
                log.error("Error enviando alerta de vencidos a {}: {}", usuario.getEmail(), e.getMessage());
                notificarFalloEmail(usuario.getEmail(),
                        "Fallo al enviar email de alerta de contratos vencidos a " + usuario.getEmail());
            }
        }

        log.info("Alertas de contratos vencidos con hardware: {} enviados, {} fallidos", enviados, fallidos);
    }

    /**
     * Notifica a todos los Admin via WebSocket que falló un envío de email.
     */
    private void notificarFalloEmail(String emailFallido, String mensaje) {
        try {
            notificationWsService.notificarPorRol(
                    List.of("Admin"),
                    "EMAIL_FALLIDO",
                    "Sistema",
                    null,
                    mensaje
            );
        } catch (Exception e) {
            log.error("Error al notificar fallo de email via WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Obtiene todos los usuarios con rol Admin u Operario que estén activos.
     * Estos son los destinatarios de las alertas de vencimiento.
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


