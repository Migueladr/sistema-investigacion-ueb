package com.example.backend.repository;

import com.example.backend.entity.Dominio;
import com.example.backend.entity.Linea;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DominioRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DominioRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Dominio> DOMINIO = (rs, i) -> new Dominio(
            rs.getString("cod_dominio"), rs.getString("nom_dominio"), rs.getLong("proyectos"),
            rs.getBigDecimal("presupuesto"));

    private static final RowMapper<Linea> LINEA = (rs, i) -> new Linea(
            rs.getString("cod_linea"), rs.getString("nombre_linea"), rs.getString("descripcion"),
            rs.getString("cod_dominio"), rs.getString("nom_dominio"));

    public List<Dominio> dominios() {
        return jdbc.query("""
                SELECT dc.cod_dominio, dc.nom_dominio,
                       COUNT(DISTINCT pl.cod_proyecto) proyectos,
                       COALESCE(SUM(CASE WHEN p.fecha_fin IS NULL OR p.fecha_fin > CURDATE()
                                         THEN p.presupuesto_total ELSE 0 END),0) presupuesto
                  FROM dominio_conocimiento dc
                  LEFT JOIN linea_investigacion l ON l.cod_dominio = dc.cod_dominio
                  LEFT JOIN proyecto_linea pl ON pl.cod_linea = l.cod_linea
                  LEFT JOIN proyecto_investigacion p ON p.cod_proyecto = pl.cod_proyecto
                 GROUP BY dc.cod_dominio, dc.nom_dominio ORDER BY dc.cod_dominio
                """, DOMINIO);
    }

    public List<Linea> lineas(String codDominio) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.cod_linea, l.nombre_linea, l.descripcion, l.cod_dominio, dc.nom_dominio
                  FROM linea_investigacion l
                  JOIN dominio_conocimiento dc ON dc.cod_dominio = l.cod_dominio
                 WHERE 1=1
                """);
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (codDominio != null && !codDominio.isBlank()) {
            sql.append(" AND l.cod_dominio = :dominio ");
            p.addValue("dominio", codDominio);
        }
        sql.append(" ORDER BY l.cod_linea");
        return jdbc.query(sql.toString(), p, LINEA);
    }

    public List<Map<String, Object>> proyectosPorDominio(String codDominio) {
        return jdbc.queryForList("""
                SELECT p.cod_proyecto, p.acronimo, p.nombre_proyecto, p.presupuesto_total,
                       CASE WHEN p.fecha_fin IS NULL OR p.fecha_fin > CURDATE() THEN 'VIGENTE' ELSE 'FINALIZADO' END estado
                  FROM dominio_conocimiento dc
                  JOIN linea_investigacion l ON l.cod_dominio = dc.cod_dominio
                  JOIN proyecto_linea pl ON pl.cod_linea = l.cod_linea
                  JOIN proyecto_investigacion p ON p.cod_proyecto = pl.cod_proyecto
                 WHERE dc.cod_dominio = :dominio ORDER BY p.cod_proyecto
                """, new MapSqlParameterSource("dominio", codDominio));
    }

    public List<Map<String, Object>> investigadoresPorLinea(String codLinea) {
        return jdbc.queryForList("""
                SELECT i.id_profesor, CONCAT(i.nombres,' ',i.apellidos) investigador, i.tipo
                  FROM trabaja_linea t JOIN investigador i ON i.id_profesor = t.id_profesor
                 WHERE t.cod_linea = :linea ORDER BY investigador
                """, new MapSqlParameterSource("linea", codLinea));
    }

    public List<Map<String, Object>> parametrosPuntaje() {
        return jdbc.queryForList("SELECT clave, descripcion, puntos FROM parametro_puntaje ORDER BY clave", Map.of());
    }
}
