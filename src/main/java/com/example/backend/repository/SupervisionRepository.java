package com.example.backend.repository;

import com.example.backend.dto.SupervisionForm;
import com.example.backend.entity.Supervision;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class SupervisionRepository {

    private static final RowMapper<Supervision> MAPEADOR = (rs, i) -> {
        LocalDate fin = rs.getObject("fecha_fin", LocalDate.class);
        return new Supervision(rs.getInt("id_doctor"), rs.getString("doctor"),
                rs.getInt("id_no_doctor"), rs.getString("no_doctor"),
                rs.getObject("fecha_inicio", LocalDate.class), fin,
                fin == null || fin.isAfter(LocalDate.now()));
    };

    private final NamedParameterJdbcTemplate jdbc;

    public SupervisionRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Supervision> listar(boolean soloVigentes, String q) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id_doctor, CONCAT(d.nombres,' ',d.apellidos) doctor,
                       s.id_no_doctor, CONCAT(n.nombres,' ',n.apellidos) no_doctor,
                       s.fecha_inicio, s.fecha_fin
                  FROM supervisa s
                  JOIN investigador d ON d.id_profesor = s.id_doctor
                  JOIN investigador n ON n.id_profesor = s.id_no_doctor
                 WHERE 1=1
                """);
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (soloVigentes) {
            sql.append(" AND (s.fecha_fin IS NULL OR s.fecha_fin > CURDATE()) ");
        }
        if (q != null && !q.isBlank()) {
            sql.append(" AND (d.nombres LIKE CONCAT('%',:q,'%') OR d.apellidos LIKE CONCAT('%',:q,'%')")
                    .append(" OR n.nombres LIKE CONCAT('%',:q,'%') OR n.apellidos LIKE CONCAT('%',:q,'%')) ");
            p.addValue("q", q.trim());
        }
        sql.append(" ORDER BY s.fecha_inicio DESC");
        return jdbc.query(sql.toString(), p, MAPEADOR);
    }

    /** El trigger trg_supervisa_bi invoca sp_validar_supervision; Java no reimplementa la regla. */
    public void registrar(SupervisionForm f) {
        jdbc.update("""
                INSERT INTO supervisa (id_doctor, id_no_doctor, fecha_inicio, fecha_fin)
                VALUES (:doctor, :noDoctor, :inicio, :fin)
                """, new MapSqlParameterSource()
                .addValue("doctor", f.idDoctor()).addValue("noDoctor", f.idNoDoctor())
                .addValue("inicio", f.fechaInicio()).addValue("fin", f.fechaFin()));
    }

    public int finalizar(int doctor, int noDoctor, String fechaInicio, String fechaFin) {
        return jdbc.update("""
                UPDATE supervisa SET fecha_fin = :fin
                 WHERE id_doctor = :doctor AND id_no_doctor = :noDoctor AND fecha_inicio = :inicio
                """, new MapSqlParameterSource()
                .addValue("fin", fechaFin).addValue("doctor", doctor)
                .addValue("noDoctor", noDoctor).addValue("inicio", fechaInicio));
    }

    public List<Map<String, Object>> doctores() {
        return jdbc.queryForList("""
                SELECT d.id_profesor, CONCAT(i.nombres,' ',i.apellidos) nombre
                  FROM doctor d JOIN investigador i ON i.id_profesor = d.id_profesor ORDER BY nombre
                """, Map.of());
    }

    public List<Map<String, Object>> noDoctores() {
        return jdbc.queryForList("""
                SELECT nd.id_profesor, CONCAT(i.nombres,' ',i.apellidos) nombre
                  FROM no_doctor nd JOIN investigador i ON i.id_profesor = nd.id_profesor ORDER BY nombre
                """, Map.of());
    }

    public List<Map<String, Object>> historial() {
        return jdbc.queryForList("""
                SELECT fecha_hora fecha, operacion tipo, clave_registro clave,
                       valores_nuevos detalle, usuario
                  FROM auditoria_cambios WHERE tabla = 'supervisa'
                 ORDER BY fecha_hora DESC LIMIT 200
                """, Map.of());
    }
}
