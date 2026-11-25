package com.example.backend.repository;

import com.example.backend.dto.UsuarioForm;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class UsuarioRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UsuarioRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listar() {
        return jdbc.queryForList("""
                SELECT u.username, u.activo, u.id_profesor, u.id_estudiante, u.fecha_creacion,
                       COALESCE(GROUP_CONCAT(r.nombre ORDER BY r.nombre SEPARATOR ', '),'') roles,
                       COALESCE(CONCAT(i.nombres,' ',i.apellidos), CONCAT(e.nombres,' ',e.apellidos), '') nombre
                  FROM seg_usuario u
                  LEFT JOIN seg_usuario_rol ur ON ur.username = u.username
                  LEFT JOIN seg_rol r ON r.cod_rol = ur.cod_rol
                  LEFT JOIN investigador i ON i.id_profesor = u.id_profesor
                  LEFT JOIN estudiante e ON e.id_estudiante = u.id_estudiante
                 GROUP BY u.username, u.activo, u.id_profesor, u.id_estudiante, u.fecha_creacion,
                          i.nombres, i.apellidos, e.nombres, e.apellidos
                 ORDER BY u.username
                """, Map.of());
    }

    public Map<String, Object> buscar(String username) {
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT * FROM seg_usuario WHERE username = :u",
                new MapSqlParameterSource("u", username));
        return filas.isEmpty() ? null : filas.get(0);
    }

    public boolean existe(String username) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM seg_usuario WHERE username = :u",
                new MapSqlParameterSource("u", username), Long.class);
        return n != null && n > 0;
    }

    public void guardar(UsuarioForm f, String hash) {
        jdbc.update("""
                INSERT INTO seg_usuario (username, password_hash, id_profesor, id_estudiante, activo)
                VALUES (:u, :hash, :prof, :est, :activo)
                """, new MapSqlParameterSource()
                .addValue("u", f.username()).addValue("hash", hash)
                .addValue("prof", f.idProfesor()).addValue("est", f.idEstudiante())
                .addValue("activo", Boolean.TRUE.equals(f.activo())));
        reemplazarRoles(f);
        sincronizarPerfiles(f.username(), f.idProfesor(), f.idEstudiante());
    }

    public void actualizar(UsuarioForm f, String hash) {
        jdbc.update("""
                UPDATE seg_usuario SET password_hash = COALESCE(:hash, password_hash),
                       id_profesor = :prof, id_estudiante = :est, activo = :activo
                 WHERE username = :u
                """, new MapSqlParameterSource()
                .addValue("hash", hash).addValue("prof", f.idProfesor()).addValue("est", f.idEstudiante())
                .addValue("activo", Boolean.TRUE.equals(f.activo())).addValue("u", f.username()));
        reemplazarRoles(f);
        sincronizarPerfiles(f.username(), f.idProfesor(), f.idEstudiante());
    }

    /** Mantiene las tablas originales de enlace usadas por las vistas de profesor/estudiante. */
    private void sincronizarPerfiles(String username, Integer idProfesor, Integer idEstudiante) {
        MapSqlParameterSource p = new MapSqlParameterSource("u", username);
        jdbc.update("DELETE FROM cuentas_profesores WHERE usuario = :u", p);
        jdbc.update("DELETE FROM cuentas_estudiantes WHERE usuario = :u", p);
        if (idProfesor != null) {
            jdbc.update("INSERT INTO cuentas_profesores (usuario, id_profesor) VALUES (:u, :id)",
                    p.addValue("id", idProfesor));
        }
        if (idEstudiante != null) {
            jdbc.update("INSERT INTO cuentas_estudiantes (usuario, id_estudiante) VALUES (:u, :id)",
                    p.addValue("id", idEstudiante));
        }
    }

    private void reemplazarRoles(UsuarioForm f) {
        jdbc.update("DELETE FROM seg_usuario_rol WHERE username = :u",
                new MapSqlParameterSource("u", f.username()));
        if (f.roles() != null) {
            for (String rol : f.roles()) {
                jdbc.update("INSERT INTO seg_usuario_rol (username, cod_rol) VALUES (:u, :r)",
                        new MapSqlParameterSource("u", f.username()).addValue("r", rol));
            }
        }
    }

    public List<Map<String, Object>> rolesDisponibles() {
        return jdbc.queryForList("SELECT cod_rol, nombre, descripcion FROM seg_rol ORDER BY cod_rol", Map.of());
    }

    public List<String> rolesDe(String username) {
        return jdbc.queryForList("""
                SELECT cod_rol FROM seg_usuario_rol WHERE username = :u
                """, new MapSqlParameterSource("u", username), String.class);
    }
}
