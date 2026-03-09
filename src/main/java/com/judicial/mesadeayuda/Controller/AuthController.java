package com.judicial.mesadeayuda.Controller;

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

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Autentica al usuario y devuelve un token JWT.
     *
     * Flujo:
     *   1. Recibe email + password
     *   2. AuthenticationManager valida contra BD (via CustomUserDetailsService + BCrypt)
     *   3. Si es válido, genera token JWT
     *   4. Devuelve token + datos básicos del usuario
     *
     * Errores manejados por GlobalExceptionHandler:
     *   - 401: BadCredentialsException (credenciales inválidas)
     *   - 400: MethodArgumentNotValidException (validación del DTO)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO loginRequest) {

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