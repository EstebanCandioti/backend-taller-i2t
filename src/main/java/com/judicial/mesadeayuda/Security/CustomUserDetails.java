package com.judicial.mesadeayuda.Security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.judicial.mesadeayuda.Entities.Usuario;

import lombok.Getter;

/**
 * Implementación de UserDetails que envuelve nuestra entidad Usuario.
 * Spring Security usa esta clase para autenticación y autorización.
 *
 * El authority se define como "ROLE_" + nombre del rol.
 * Ej: ROLE_Admin, ROLE_Operario, ROLE_Técnico
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Integer id;
    private final String email;
    private final String password;
    private final String nombreCompleto;
    private final boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.activo = usuario.isActivo();
        this.authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())
        );
    }

    @Override
    public String getUsername() {
        return this.email; // Usamos email como username
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.activo; // Usuario inactivo = cuenta bloqueada
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.activo;
    }
}