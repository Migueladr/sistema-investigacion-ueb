package com.example.backend.repository;

import com.example.backend.dto.DashboardData;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Todas las cifras del panel se obtienen de la base de datos; no hay valores simulados. */
@Repository
public class DashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ProcedureCaller procedures;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc, ProcedureCaller procedures) {
        this.jdbc = jdbc;
        this.procedures = procedures;
    }

    public DashboardData cargar() {
        Map<String, Object> general = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN fecha_fin IS NULL OR fecha_fin > CURDATE() THEN 1 ELSE 0 END),0) AS vigentes,
                       COALESCE(SUM(CASE WHEN fecha_fin IS NOT NULL AND fecha_fin <= CURDATE() THEN 1 ELSE 0 END),0) AS finalizados,
                       COALESCE(SUM(CASE WHEN fecha_fin IS NULL OR fecha_fin > CURDATE() THEN presupuesto_total ELSE 0 END),0) AS presupuesto
                  FROM proyecto_investigacion
                """, Map.of());
        long investigadores = jdbc.queryForObject("SELECT COUNT(*) FROM investigador", Map.of(), Long.class);
        long estudiantes = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT id_estudiante) FROM participante_estudiante
                 WHERE fecha_fin IS NULL OR fecha_fin > CURDATE()
                """, Map.of(), Long.class);
        long publicaciones = jdbc.queryForObject("SELECT COUNT(*) FROM publicacion", Map.of(), Long.class);

        List<Map<String, Object>> porEstado = jdbc.queryForList("""
                SELECT CASE WHEN fecha_fin IS NULL OR fecha_fin > CURDATE() THEN 'VIGENTE' ELSE 'FINALIZADO' END AS estado,
                       COUNT(*) AS total
                  FROM proyecto_investigacion GROUP BY estado ORDER BY estado
                """, Map.of());
        List<Map<String, Object>> porTipo = jdbc.queryForList("""
                SELECT tipo, COUNT(*) AS total FROM publicacion GROUP BY tipo ORDER BY tipo
                """, Map.of());
        // Procedimiento existente: presupuesto por dominio de conocimiento.
        List<Map<String, Object>> porDominio = procedures.consultar("sp_av_05_presupuesto_por_dominio");

        return new DashboardData(
                ((Number) general.get("total")).longValue(),
                ((Number) general.get("vigentes")).longValue(),
                ((Number) general.get("finalizados")).longValue(),
                investigadores, estudiantes, publicaciones,
                (BigDecimal) general.get("presupuesto"),
                porEstado, porTipo, porDominio);
    }
}
