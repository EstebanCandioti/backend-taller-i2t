package com.judicial.mesadeayuda.Controller;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.judicial.mesadeayuda.DTO.Request.LoginRequestDTO;
import com.judicial.mesadeayuda.DTO.Response.ApiResponse;
import com.judicial.mesadeayuda.DTO.Response.LoginResponseDTO;
import com.judicial.mesadeayuda.Security.CustomUserDetails;
import com.judicial.mesadeayuda.Security.JwtTokenProvider;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Controller de autenticación.
 * Endpoint público (sin token) para login.
 *
 * POST /api/auth/login
 *   Body: { "email": "...", "password": "..." }
 *   Response: { token, tipo, usuarioId, nombreCompleto, email, rol }
 *
 * El logout se maneja del lado del cliente descartando el token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /** Rate limiting: máx 5 intentos por IP en 15 minutos */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(15))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            HttpServletRequest request) {

        // Rate limiting por IP
        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> createBucket());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Demasiados intentos de login. Espere 15 minutos."));
        }

        // 1. Autenticar
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // 2. Generar token
        String token = jwtTokenProvider.generarToken(authentication);

        // 3. Extraer datos del usuario autenticado
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String rol = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        // 4. Construir respuesta
        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .usuarioId(userDetails.getId())
                .nombreCompleto(userDetails.getNombreCompleto())
                .email(userDetails.getUsername())
                .rol(rol.replace("ROLE_", ""))
                .build();

        return ResponseEntity.ok(ApiResponse.success("Login exitoso", loginResponse));
    }
}