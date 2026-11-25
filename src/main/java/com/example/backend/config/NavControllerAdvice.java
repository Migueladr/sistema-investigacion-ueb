package com.example.backend.config;

import com.example.backend.security.SecurityUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/** Menú lateral construido según el rol; evita depender de etiquetas de seguridad en Thymeleaf. */
@ControllerAdvice
public class NavControllerAdvice {

    public record NavItem(String url, String etiqueta, String icono) {
    }

    @ModelAttribute("menu")
    public List<NavItem> menu() {
        List<NavItem> items = new ArrayList<>();
        items.add(new NavItem("/dashboard", "Dashboard", "bi-grid-1x2"));
        items.add(new NavItem("/proyectos", "Proyectos", "bi-diagram-3"));
        items.add(new NavItem("/investigadores", "Investigadores", "bi-people"));
        items.add(new NavItem("/estudiantes", "Estudiantes", "bi-mortarboard"));
        items.add(new NavItem("/publicaciones", "Publicaciones", "bi-journal-text"));
        items.add(new NavItem("/dominios", "Dominios", "bi-globe2"));
        items.add(new NavItem("/lineas", "Líneas de investigación", "bi-branch-Plus"));
        if (SecurityUtils.puedeGestionar()) {
            items.add(new NavItem("/supervisiones", "Supervisiones", "bi-person-check"));
        }
        items.add(new NavItem("/reportes", "Reportes", "bi-bar-chart-line"));
        if (SecurityUtils.puedeGestionar()) {
            items.add(new NavItem("/auditoria", "Auditoría", "bi-shield-check"));
        }
        if (SecurityUtils.esAdmin()) {
            items.add(new NavItem("/usuarios", "Usuarios y seguridad", "bi-person-gear"));
        }
        return items;
    }

    @ModelAttribute("usuarioActual")
    public String usuarioActual() {
        return SecurityUtils.usuarioActual();
    }

    @ModelAttribute("rolesActuales")
    public String rolesActuales() {
        return String.join(", ", SecurityUtils.roles()).replace("ROLE_", "");
    }
}
