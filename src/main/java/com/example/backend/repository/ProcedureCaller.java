package com.example.backend.repository;

import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Invocador de procedimientos almacenados. Los nombres son constantes internas, nunca entran
 * desde el navegador; los valores se enlazan con setObject (parametrización JDBC).
 */
@Repository
public class ProcedureCaller {

    private final JdbcTemplate jdbc;

    public ProcedureCaller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ejecuta un CALL con parámetros IN posicionales; el SP conserva sus transacciones. */
    public void ejecutar(String sql, Object... argumentos) {
        jdbc.execute(sql, (CallableStatementCallback<Void>) cs -> {
            for (int i = 0; i < argumentos.length; i++) {
                cs.setObject(i + 1, argumentos[i]);
            }
            cs.execute();
            return null;
        });
    }

    /** Ejecuta un SP sin parámetros que devuelve un único result set (reportes AV/MT). */
    public List<Map<String, Object>> consultar(String nombreProcedimiento) {
        return jdbc.execute("CALL " + nombreProcedimiento + "()", (CallableStatementCallback<List<Map<String, Object>>>) cs -> {
            if (!cs.execute()) {
                return List.of();
            }
            List<Map<String, Object>> filas = new ArrayList<>();
            try (ResultSet rs = cs.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        fila.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    filas.add(fila);
                }
            }
            return filas;
        });
    }
}
