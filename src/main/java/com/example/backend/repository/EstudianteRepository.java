package com.example.backend.repository;

import com.example.backend.dto.EstudianteForm;
import com.example.backend.entity.Estudiante;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class EstudianteRepository {

    private static final RowMapper<Estudiante> MAPEADOR = (rs, i) -> new Estudiante(
            rs.getInt("id_estudiante"), rs.getString("cedula"), rs.getString("nombres"),
            rs.getString("apellidos"), rs.getString("carrera"));

    private final NamedParameterJdbcTemplate jdbc;

    public EstudianteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Estudiante> listar(String q, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM estudiante WHERE 1=1 ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (cedula LIKE CONCAT('%',:q,'%') OR nombres LIKE CONCAT('%',:q,'%')")
                    .append(" OR apellidos LIKE CONCAT('%',:q,'%') OR carrera LIKE CONCAT('%',:q,'%')) ");
            p.addValue("q", q.trim());
        }
        sql.append(" ORDER BY apellidos, nombres LIMIT :limite OFFSET :offset");
        return jdbc.query(sql.toString(), p.addValue("limite", size).addValue("offset", (page - 1) * size), MAPEADOR);
    }

    public long contar(String q) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM estudiante WHERE 1=1 ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (cedula LIKE CONCAT('%',:q,'%') OR nombres LIKE CONCAT('%',:q,'%')")
                    .append(" OR apellidos LIKE CONCAT('%',:q,'%')) ");
            p.addValue("q", q.trim());
        }
        Long n = jdbc.queryForObject(sql.toString(), p, Long.class);
        return n == null ? 0 : n;
    }

    public Estudiante buscar(int id) {
        List<Estudiante> found = jdbc.query("SELECT * FROM estudiante WHERE id_estudiante = :id",
                new MapSqlParameterSource("id", id), MAPEADOR);
        return found.isEmpty() ? null : found.get(0);
    }

    public void guardar(EstudianteForm f) {
        jdbc.update("""
                INSERT INTO estudiante (id_estudiante, cedula, nombres, apellidos, carrera)
                VALUES (:id, :cedula, :nombres, :apellidos, :carrera)
                """, parametros(f));
    }

    public int actualizar(EstudianteForm f) {
        return jdbc.update("""
                UPDATE estudiante SET cedula = :cedula, nombres = :nombres,
                       apellidos = :apellidos, carrera = :carrera
                 WHERE id_estudiante = :id
                """, parametros(f));
    }

    private MapSqlParameterSource parametros(EstudianteForm f) {
        return new MapSqlParameterSource().addValue("id", f.idEstudiante())
                .addValue("cedula", f.cedula()).addValue("nombres", f.nombres())
                .addValue("apellidos", f.apellidos()).addValue("carrera", f.carrera());
    }

    public List<Map<String, Object>> proyectos(int id) {
        return jdbc.queryForList("""
                SELECT p.cod_proyecto, p.nombre_proyecto, p.acronimo, pe.fecha_inicio, pe.fecha_fin,
                       CASE WHEN pe.fecha_fin IS NULL OR pe.fecha_fin > CURDATE() THEN 'VIGENTE' ELSE 'FINALIZADO' END estado
                  FROM participante_estudiante pe
                  JOIN proyecto_investigacion p ON p.cod_proyecto = pe.cod_proyecto
                 WHERE pe.id_estudiante = :id ORDER BY pe.fecha_inicio DESC
                """, new MapSqlParameterSource("id", id));
    }

    /** Un estudiante autenticado solo consulta sus propias participaciones. */
    public List<Map<String, Object>> proyectosDelAutenticado() {
        String usuario = SecurityContextHolder.getContext().getAuthentication() == null
                ? "" : SecurityContextHolder.getContext().getAuthentication().getName();
        return jdbc.queryForList("""
                SELECT p.cod_proyecto, p.nombre_proyecto, p.acronimo, pe.fecha_inicio, pe.fecha_fin
                  FROM participante_estudiante pe
                  JOIN proyecto_investigacion p ON p.cod_proyecto = pe.cod_proyecto
                  JOIN cuentas_estudiantes ce ON ce.id_estudiante = pe.id_estudiante
                 WHERE ce.usuario = :usuario ORDER BY pe.fecha_inicio DESC
                """, new MapSqlParameterSource("usuario", usuario));
    }

    public int asignar(String codigo, Integer id, String fechaInicio) {
        return jdbc.update("""
                INSERT INTO participante_estudiante (cod_proyecto, id_estudiante, fecha_inicio)
                VALUES (:c, :id, COALESCE(:f, CURDATE()))
                """, new MapSqlParameterSource("c", codigo).addValue("id", id).addValue("f", fechaInicio));
    }

    public int finalizar(String codigo, Integer id) {
        return jdbc.update("""
                UPDATE participante_estudiante SET fecha_fin = CURDATE()
                 WHERE cod_proyecto = :c AND id_estudiante = :id AND fecha_fin IS NULL
                """, new MapSqlParameterSource("c", codigo).addValue("id", id));
    }
}
