package com.judicial.mesadeayuda.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

/**
 * Configuración central de seguridad del sistema.
 *
 * REGLAS DE AUTORIZACIÓN POR ROL:
 * ┌──────────────────────┬───────────┬──────────┬──────────┐
 * │ Recurso              │ Admin     │ Operario │ Técnico  │
 * ├──────────────────────┼───────────┼──────────┼──────────┤
 * │ /api/auth/**         │ Público   │ Público  │ Público  │
 * │ /api/tickets (GET)   │ Todos     │ Todos    │ Solo sus │
 * │ /api/tickets (CUD)   │ ✔         │ ✔        │ ✘        │
 * │ /api/hardware/**     │ ✔         │ ✔        │ ✘        │
 * │ /api/software/**     │ ✔         │ ✔        │ ✘        │
 * │ /api/contratos/**    │ ✔         │ ✔        │ ✘        │
 * │ /api/juzgados/**     │ ✔         │ ✔        │ ✘        │
 * │ /api/circunscripc.** │ ✔         │ ✔        │ ✘        │
 * │ /api/usuarios (GET)  │ ✔         │ Lectura  │ ✘        │
 * │ /api/usuarios (CUD)  │ ✔         │ ✘        │ ✘        │
 * │ /api/audit/**        │ ✔         │ ✘        │ ✘        │
 * └──────────────────────┴───────────┴──────────┴──────────┘
 *
 * Nota: El filtrado de "solo sus tickets" para Técnico se maneja en el Service,
 * no en Security (ya que depende de la query a BD).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita @PreAuthorize en Controllers/Services
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CORS ──────────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── CSRF deshabilitado (API REST stateless con JWT) ───────
            .csrf(csrf -> csrf.disable())

            // ── Sesiones stateless (JWT, sin cookies de sesión) ───────
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Exception handling personalizado ──────────────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(authenticationEntryPoint)  // 401
                    .accessDeniedHandler(accessDeniedHandler)            // 403
            )

            // ── Reglas de autorización ────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // ── Públicos (sin token) ──────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()

                // ── Tickets ───────────────────────────────────────────
                // GET: todos los roles autenticados (filtro por rol en Service)
                .requestMatchers(HttpMethod.GET, "/api/tickets/**").authenticated()
                // CUD: solo Admin y Operario
                .requestMatchers(HttpMethod.POST, "/api/tickets/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.PUT, "/api/tickets/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.DELETE, "/api/tickets/**").hasAnyRole("Admin", "Operario")

                // ── Hardware ──────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/hardware/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.POST, "/api/hardware/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.PUT, "/api/hardware/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.DELETE, "/api/hardware/**").hasAnyRole("Admin", "Operario")

                // ── Software ──────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/software/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.POST, "/api/software/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.PUT, "/api/software/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.DELETE, "/api/software/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasAnyRole("Admin", "Operario")

                // ── Contratos ─────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/contratos/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.POST, "/api/contratos/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.PUT, "/api/contratos/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.DELETE, "/api/contratos/**").hasAnyRole("Admin", "Operario")

                // ── Juzgados ──────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/juzgados/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.POST, "/api/juzgados/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.PUT, "/api/juzgados/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.DELETE, "/api/juzgados/**").hasAnyRole("Admin", "Operario")

                // ── Circunscripciones ─────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/circunscripciones/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.POST, "/api/circunscripciones/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.PUT, "/api/circunscripciones/**").hasAnyRole("Admin", "Operario")
                .requestMatchers(HttpMethod.DELETE, "/api/circunscripciones/**").hasAnyRole("Admin", "Operario")

                // ── Usuarios ──────────────────────────────────────────
                // GET: Admin (completo) y Operario (lectura)
                .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyRole("Admin", "Operario")
                // CUD: solo Admin
                .requestMatchers(HttpMethod.POST, "/api/usuarios/**").hasRole("Admin")
                .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasRole("Admin")
                .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole("Admin")

                // ── Roles (lectura para selects) ──────────────────────
                .requestMatchers(HttpMethod.GET, "/api/roles/**").hasAnyRole("Admin", "Operario")

                // ── Auditoría: solo Admin ─────────────────────────────
                .requestMatchers("/api/audit/**").hasRole("Admin")

                // ── Todo lo demás requiere autenticación ──────────────
                .anyRequest().authenticated()
            )

            // ── Filtro JWT antes del filtro de autenticación ──────────
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager para inyectar en el AuthController (login).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * BCrypt como encoder de contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración CORS para permitir el frontend Angular.
     * En producción, restringir origins al dominio real.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
