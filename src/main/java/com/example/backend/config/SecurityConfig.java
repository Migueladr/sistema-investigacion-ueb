package com.example.backend.config;

import com.example.backend.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Control de acceso por rol. CSRF permanece habilitado (configuración por defecto);
 * las peticiones AJAX envían el token desde la metaetiqueta del layout.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // CustomUserDetailsService ya es un @Service: no se registra dos veces,
    // de lo contrario Spring no sabe cuál usar y el login falla.

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadena(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/acceso-denegado", "/css/**", "/js/**",
                                "/img/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/usuarios/cambiar-clave", "/usuarios/cambiar-clave").authenticated()
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/proyectos/**", "/investigadores/**",
                                "/estudiantes/**", "/publicaciones/**", "/dominios/**",
                                "/lineas/**", "/supervisiones/**", "/api/proyectos/**",
                                "/api/publicaciones/**", "/api/supervisiones/**")
                        .hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers("/auditoria/**", "/api/auditoria/**")
                        .hasAnyRole("ADMIN", "SECRETARIA")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error"))
                .logout(salida -> salida
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"))
                .sessionManagement(sesion -> sesion
                        .sessionFixation(s -> s.migrateSession()));
        return http.build();
    }
}
