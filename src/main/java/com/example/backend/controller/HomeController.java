package com.example.backend.controller;

import com.example.backend.pattern.observer.NotificacionService;
import com.example.backend.service.DashboardService;
import com.example.backend.service.EstudianteService;
import com.example.backend.security.SecurityUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class HomeController {

    private final DashboardService dashboardService;
    private final NotificacionService notificaciones;
    private final EstudianteService estudiantes;

    public HomeController(DashboardService dashboardService, NotificacionService notificaciones,
                          EstudianteService estudiantes) {
        this.dashboardService = dashboardService;
        this.notificaciones = notificaciones;
        this.estudiantes = estudiantes;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("datos", dashboardService.obtener());
        model.addAttribute("avisos", notificaciones.ultimas(5));
        model.addAttribute("misProyectos", SecurityUtils.esEstudiante() ? estudiantes.misProyectos() : Map.of());
        return "dashboard";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UserDetails usuario, Model model) {
        if (usuario != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/acceso-denegado")
    public String denegado() {
        return "acceso-denegado";
    }
}
