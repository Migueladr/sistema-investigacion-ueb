package com.example.backend.repository;

import com.example.backend.dto.InvestigadorForm;
import com.example.backend.entity.Investigador;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class InvestigadorRepository {

    private static final RowMapper<Investigador> MAPEADOR = (rs, i) -> new Investigador(
            rs.getInt("id_profesor"), rs.getString("nombres"), rs.getString("apellidos"),
            rs.getString("despacho"), rs.getString("telefono"), rs.getString("tipo"),
            rs.getString("nombres") + " " + rs.getString("apellidos"),
            rs.getLong("proyectos_como_investigador"), rs.getLong("proyectos_como_director"));

    private final NamedParameterJdbcTemplate jdbc;

    public InvestigadorRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Investigador> listar(String q, String tipo, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT i.id_profesor, i.nombres, i.apellidos, i.despacho, i.telefono, i.tipo,
                       COALESCE(v.proyectos_como_investigador,0) proyectos_como_investigador,
                       COALESCE(v.proyectos_como_director,0) proyectos_como_director
                  FROM investigador i
                  LEFT JOIN v_carga_investigadores v ON v.id_profesor = i.id_profesor
                 WHERE 1=1
                """);
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (i.nombres LIKE CONCAT('%',:q,'%') OR i.apellidos LIKE CONCAT('%',:q,'%')) ");
            p.addValue("q", q.trim());
        }
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND i.tipo = :tipo ");
            p.addValue("tipo", tipo);
        }
        sql.append(" ORDER BY i.apellidos, i.nombres LIMIT :limite OFFSET :offset");
        return jdbc.query(sql.toString(), p.addValue("limite", size).addValue("offset", (page - 1) * size), MAPEADOR);
    }

    public long contar(String q, String tipo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM investigador i WHERE 1=1 ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (i.nombres LIKE CONCAT('%',:q,'%') OR i.apellidos LIKE CONCAT('%',:q,'%')) ");
            p.addValue("q", q.trim());
        }
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND i.tipo = :tipo ");
            p.addValue("tipo", tipo);
        }
        Long n = jdbc.queryForObject(sql.toString(), p, Long.class);
        return n == null ? 0 : n;
    }

    public Investigador buscar(int id) {
        List<Investigador> found = jdbc.query("""
                SELECT i.id_profesor, i.nombres, i.apellidos, i.despacho, i.telefono, i.tipo,
                       COALESCE(v.proyectos_como_investigador,0) proyectos_como_investigador,
                       COALESCE(v.proyectos_como_director,0) proyectos_como_director
                  FROM investigador i
                  LEFT JOIN v_carga_investigadores v ON v.id_profesor = i.id_profesor
                 WHERE i.id_profesor = :id
                """, new MapSqlParameterSource("id", id), MAPEADOR);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * El trigger trg_investigador_ai crea automáticamente el registro en doctor o no_doctor;
     * el trigger trg_investigador_bu impide cambiar el tipo. Java no duplica esas reglas.
     */
    public void guardar(InvestigadorForm f) {
        jdbc.update("""
                INSERT INTO investigador (id_profesor, nombres, apellidos, despacho, telefono, tipo)
                VALUES (:id, :nombres, :apellidos, :despacho, :telefono, :tipo)
                """, parametros(f));
    }

    public int actualizar(InvestigadorForm f) {
        return jdbc.update("""
                UPDATE investigador
                   SET nombres = :nombres, apellidos = :apellidos, despacho = :despacho,
                       telefono = :telefono, tipo = :tipo
                 WHERE id_profesor = :id
                """, parametros(f));
    }

    private MapSqlParameterSource parametros(InvestigadorForm f) {
        return new MapSqlParameterSource()
                .addValue("id", f.idProfesor()).addValue("nombres", f.nombres())
                .addValue("apellidos", f.apellidos()).addValue("despacho", f.despacho())
                .addValue("telefono", f.telefono()).addValue("tipo", f.tipo());
    }

    public List<Map<String, Object>> proyectosVigentes(int id) {
        return jdbc.queryForList("""
                SELECT p.cod_proyecto, p.nombre_proyecto, p.acronimo, p.presupuesto_total,
                       CASE WHEN p.id_director = :id THEN 'DIRECTOR' ELSE pp.rol END AS rol,
                       pp.fecha_inicio, pp.fecha_fin
                  FROM proyecto_investigacion p
                  LEFT JOIN participa_proyecto pp ON pp.cod_proyecto = p.cod_proyecto AND pp.id_profesor = :id
                 WHERE (p.id_director = :id OR pp.id_profesor IS NOT NULL)
                   AND (p.fecha_fin IS NULL OR p.fecha_fin > CURDATE())
                """, new MapSqlParameterSource("id", id));
    }

    public List<Map<String, Object>> publicaciones(int id) {
        return jdbc.queryForList("""
                SELECT pu.cod_publicacion, pu.titulo, pu.tipo, pu.puntos_asignados,
                       l.nombre_linea, p.acronimo AS proyecto
                  FROM escribe e
                  JOIN publicacion pu ON pu.cod_publicacion = e.cod_publicacion
                  JOIN proyecto_investigacion p ON p.cod_proyecto = pu.cod_proyecto
                  JOIN linea_investigacion l ON l.cod_linea = e.cod_linea
                 WHERE e.id_profesor = :id ORDER BY pu.cod_publicacion DESC
                """, new MapSqlParameterSource("id", id));
    }

    public List<Map<String, Object>> lineas(int id) {
        return jdbc.queryForList("""
                SELECT l.cod_linea, l.nombre_linea, dc.nom_dominio
                  FROM trabaja_linea t
                  JOIN linea_investigacion l ON l.cod_linea = t.cod_linea
                  JOIN dominio_conocimiento dc ON dc.cod_dominio = l.cod_dominio
                 WHERE t.id_profesor = :id ORDER BY l.cod_linea
                """, new MapSqlParameterSource("id", id));
    }

    public List<Map<String, Object>> supervisores(int id) {
        return jdbc.queryForList("""
                SELECT d.id_profesor, CONCAT(d.nombres,' ',d.apellidos) nombre,
                       s.fecha_inicio, s.fecha_fin
                  FROM supervisa s JOIN investigador d ON d.id_profesor = s.id_doctor
                 WHERE s.id_no_doctor = :id ORDER BY s.fecha_inicio DESC
                """, new MapSqlParameterSource("id", id));
    }

    public List<Map<String, Object>> supervisados(int id) {
        return jdbc.queryForList("""
                SELECT n.id_profesor, CONCAT(n.nombres,' ',n.apellidos) nombre,
                       s.fecha_inicio, s.fecha_fin
                  FROM supervisa s JOIN investigador n ON n.id_profesor = s.id_no_doctor
                 WHERE s.id_doctor = :id ORDER BY s.fecha_inicio DESC
                """, new MapSqlParameterSource("id", id));
    }

    public List<Map<String, Object>> historialParticipacion(int id) {
        return jdbc.queryForList("""
                SELECT pp.cod_proyecto, p.acronimo, pp.rol, pp.fecha_inicio, pp.fecha_fin,
                       CASE WHEN pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE() THEN 'VIGENTE' ELSE 'FINALIZADO' END estado
                  FROM participa_proyecto pp
                  JOIN proyecto_investigacion p ON p.cod_proyecto = pp.cod_proyecto
                 WHERE pp.id_profesor = :id ORDER BY pp.fecha_inicio DESC
                """, new MapSqlParameterSource("id", id));
    }

    public List<Map<String, Object>> sinLinea() {
        return jdbc.queryForList("SELECT id_profesor, investigador, tipo FROM v_investigadores_sin_linea", Map.of());
    }
}
