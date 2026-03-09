package com.judicial.mesadeayuda.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Componente encargado de generar y validar tokens JWT.
 *
 * El token incluye:
 *   - subject: email del usuario
 *   - claim "userId": ID del usuario
 *   - claim "rol": nombre del rol (Admin, Operario, Técnico)
 *   - expiración configurable desde application.properties
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long jwtExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
    }

    /**
     * Genera un token JWT a partir de la autenticación exitosa.
     */
    public String generarToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtExpiration);

        String rol = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        return Jwts.builder()
                .subject(userDetails.getUsername()) // email
                .claim("userId", userDetails.getId())
                .claim("rol", rol.replace("ROLE_", ""))
                .claim("nombreCompleto", userDetails.getNombreCompleto())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(key)
                .compact();
    }

    /**
     * Extrae el email (subject) del token.
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extrae el ID del usuario del token.
     */
    public Integer getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Integer.class);
    }

    /**
     * Extrae el rol del usuario del token.
     */
    public String getRolFromToken(String token) {
        return parseClaims(token).get("rol", String.class);
    }

    /**
     * Valida la firma y expiración del token.
     */
    public boolean validarToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expirado: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Token JWT malformado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Token JWT no soportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Token JWT vacío o inválido: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("Firma JWT inválida: {}", ex.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}