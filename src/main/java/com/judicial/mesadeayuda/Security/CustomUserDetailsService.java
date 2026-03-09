package com.judicial.mesadeayuda.Security;

import com.judicial.mesadeayuda.Entities.Usuario;
import com.judicial.mesadeayuda.Repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de UserDetailsService que carga usuarios desde la BD.
 * Spring Security invoca este servicio automáticamente durante la autenticación.
 *
 * Busca por email (username en nuestro sistema) y construye un CustomUserDetails.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carga usuario por email. Usado por:
     *   1. AuthenticationManager durante el login
     *   2. JwtAuthenticationFilter para reconstruir el contexto en cada request
     *
     * @param email el email del usuario
     * @throws UsernameNotFoundException si no existe o está eliminado (soft-delete)
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No se encontró usuario con email: " + email));

        return new CustomUserDetails(usuario);
    }
}