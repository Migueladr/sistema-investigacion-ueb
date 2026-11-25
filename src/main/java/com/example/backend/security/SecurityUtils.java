package com.example.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/** Utilidades de sesión. Toda decisión de permisos se basa en las autoridades de Spring Security. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "sistema" : auth.getName();
    }

    public static Set<String> roles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public static boolean tiene(String rol) {
        return roles().contains("ROLE_" + rol);
    }

    public static boolean esAdmin() {
        return tiene("ADMIN");
    }

    public static boolean esSecretaria() {
        return tiene("SECRETARIA");
    }

    public static boolean esProfesor() {
        return tiene("PROFESOR");
    }

    public static boolean esEstudiante() {
        return tiene("ESTUDIANTE");
    }

    public static boolean puedeGestionar() {
        return esAdmin() || esSecretaria();
    }
}
