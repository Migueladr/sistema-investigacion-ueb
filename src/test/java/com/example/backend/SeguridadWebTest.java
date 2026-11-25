package com.example.backend;

import com.example.backend.config.SecurityConfig;
import com.example.backend.controller.HomeController;
import com.example.backend.controller.ProyectoController;
import com.example.backend.dto.DashboardData;
import com.example.backend.pattern.observer.NotificacionService;
import com.example.backend.security.CustomUserDetailsService;
import com.example.backend.service.DashboardService;
import com.example.backend.service.DominioService;
import com.example.backend.service.EstudianteService;
import com.example.backend.service.ProyectoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas de MVC y seguridad sin base de datos: los servicios se sustituyen por mocks.
 * Verifica autenticación, autorización por rol y protección CSRF.
 */
@WebMvcTest(controllers = {HomeController.class, ProyectoController.class})
@Import(SecurityConfig.class)
class SeguridadWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;
    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private NotificacionService notificaciones;
    @MockitoBean
    private EstudianteService estudiantes;
    @MockitoBean
    private ProyectoService proyectoService;
    @MockitoBean
    private DominioService dominioService;

    @BeforeEach
    void preparar() {
        when(dashboardService.obtener()).thenReturn(new DashboardData(
                2, 1, 1, 3, 2, 4, BigDecimal.TEN,
                List.of(Map.of("estado", "VIGENTE", "total", 1)),
                List.of(Map.of("tipo", "REVISTA", "total", 4)),
                List.of(Map.of("nom_dominio", "Salud", "porcentaje_del_total", 100))));
        when(notificaciones.ultimas(5)).thenReturn(List.of());
    }

    @Test
    void paginaLoginEsPublicaYSeRenderiza() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void visitanteAnonimoEsRedirigidoAlLogin() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void usuarioAutenticadoPuedeVerElPanel() throws Exception {
        mvc.perform(get("/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void estudianteNoPuedeRegistrarProyectos() throws Exception {
        mvc.perform(post("/proyectos").with(csrf()).with(user("pedro").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void peticionPostSinTokenCsrfEsRechazada() throws Exception {
        mvc.perform(post("/proyectos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
