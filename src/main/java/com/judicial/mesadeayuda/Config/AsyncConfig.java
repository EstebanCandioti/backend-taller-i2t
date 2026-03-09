package com.judicial.mesadeayuda.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita:
 *   - @Async: para envío de emails sin bloquear (EmailService)
 *   - @Scheduled: para el job diario de alertas de contratos
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // La configuración por defecto de Spring es suficiente.
    // Si se necesita más control, se puede definir un ThreadPoolTaskExecutor aquí.
}