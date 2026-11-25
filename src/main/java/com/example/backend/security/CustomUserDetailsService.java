package com.example.backend.security;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Las credenciales viven en la migración seg_usuario (hash BCrypt), no en el SQL original.
 * Los perfiles PROFESOR/ESTUDIANTE se enlazan con cuentas_profesores y cuentas_estudiantes.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final NamedParameterJdbcTemplate jdbc;

    public CustomUserDetailsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List<UserDetails> encontrados = jdbc.query(
                "SELECT username, password_hash, activo FROM seg_usuario WHERE username = :u",
                new MapSqlParameterSource("u", username),
                (rs, i) -> User.withUsername(rs.getString("username"))
                        .password(rs.getString("password_hash"))
                        .disabled(!rs.getBoolean("activo"))
                        // hasRole("ADMIN") exige la autoridad "ROLE_ADMIN"; la migración guarda solo "ADMIN".
                        .authorities(rolesDe(username).stream()
                                .map(rol -> new SimpleGrantedAuthority(
                                        rol.startsWith("ROLE_") ? rol : "ROLE_" + rol))
                                .toList())
                        .build());
        if (encontrados.isEmpty()) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }
        return encontrados.get(0);
    }

    private List<String> rolesDe(String username) {
        return jdbc.queryForList(
                "SELECT r.cod_rol FROM seg_rol r " +
                        "JOIN seg_usuario_rol ur ON ur.cod_rol = r.cod_rol " +
                        "JOIN seg_usuario u ON u.username = ur.username " +
                        "WHERE u.username = :u",
                new MapSqlParameterSource("u", username), String.class);
    }
}
