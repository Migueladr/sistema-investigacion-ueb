package com.example.backend.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** Consultas de solo lectura sobre las tablas de auditoría; la interfaz nunca permite modificarlas. */
@Repository
public class AuditoriaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AuditoriaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> cambios(String tipo, String tabla, String usuario, String desde, String hasta, int limite) {
        StringBuilder sql = new StringBuilder("""
                SELECT id_auditoria, fecha_hora, tabla, operacion, clave_registro,
                       valores_anteriores, valores_nuevos, usuario
                  FROM auditoria_cambios WHERE 1=1
                """);
        sql.append(filtros(tipo, tabla, usuario, desde, hasta, "fecha_hora", "tabla"));
        sql.append(" ORDER BY fecha_hora DESC LIMIT :limite");
        MapSqlParameterSource p = parametros(tipo, tabla, usuario, desde, hasta).addValue("limite", limite);
        return jdbc.queryForList(sql.toString(), p);
    }

    public List<Map<String, Object>> operaciones(String tipo, String procedimiento, String usuario,
                                                  String desde, String hasta, int limite) {
        StringBuilder sql = new StringBuilder("""
                SELECT id_auditoria, fecha_registro, operacion, procedimiento_ejecutado, resultado, usuario
                  FROM auditoria_operaciones WHERE 1=1
                """);
        sql.append(filtros(tipo, procedimiento, usuario, desde, hasta, "fecha_registro", "procedimiento_ejecutado"));
        sql.append(" ORDER BY fecha_registro DESC LIMIT :limite");
        MapSqlParameterSource p = parametros(tipo, procedimiento, usuario, desde, hasta).addValue("limite", limite);
        return jdbc.queryForList(sql.toString(), p);
    }

    private String filtros(String tipo, String tabla, String usuario, String desde, String hasta,
                           String columnaFecha, String columnaObjeto) {
        StringBuilder sql = new StringBuilder();
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND operacion = :tipo ");
        }
        if (tabla != null && !tabla.isBlank()) {
            sql.append(" AND ").append(columnaObjeto).append(" LIKE :tabla ");
        }
        if (usuario != null && !usuario.isBlank()) {
            sql.append(" AND usuario LIKE :usuario ");
        }
        if (desde != null && !desde.isBlank()) {
            sql.append(" AND ").append(columnaFecha).append(" >= :desde ");
        }
        if (hasta != null && !hasta.isBlank()) {
            sql.append(" AND ").append(columnaFecha).append(" <= :hasta ");
        }
        return sql.toString();
    }

    private MapSqlParameterSource parametros(String tipo, String tabla, String usuario, String desde, String hasta) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (tipo != null && !tipo.isBlank()) {
            p.addValue("tipo", tipo);
        }
        if (tabla != null && !tabla.isBlank()) {
            p.addValue("tabla", "%" + tabla + "%");
        }
        if (usuario != null && !usuario.isBlank()) {
            p.addValue("usuario", "%" + usuario + "%");
        }
        if (desde != null && !desde.isBlank()) {
            p.addValue("desde", desde);
        }
        if (hasta != null && !hasta.isBlank()) {
            p.addValue("hasta", hasta + " 23:59:59");
        }
        return p;
    }
}
