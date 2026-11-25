package com.example.backend.repository;

import com.example.backend.dto.FiltroProyecto;
import com.example.backend.dto.ProyectoForm;
import com.example.backend.entity.Proyecto;
import com.example.backend.security.SecurityUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Acceso a proyecto_investigacion y a sus relaciones; usa v_proyectos_resumen para consultas. */
@Repository
public class ProyectoRepository {

    private static final String SELECT = """
            SELECT r.cod_proyecto, r.acronimo, r.nombre_proyecto, r.director, r.presupuesto_total,
                   r.estado, r.investigadores_vigentes, r.publicaciones, r.puntaje_total,
                   (SELECT GROUP_CONCAT(dc.nom_dominio SEPARATOR ', ')
                      FROM proyecto_linea pl
                      JOIN linea_investigacion l ON l.cod_linea = pl.cod_linea
                      JOIN dominio_conocimiento dc ON dc.cod_dominio = l.cod_dominio
                     WHERE pl.cod_proyecto = r.cod_proyecto) AS dominio,
                   p.id_director, p.fecha_inicio, p.fecha_fin, p.programa_idi, p.descripcion
              FROM v_proyectos_resumen r
              JOIN proyecto_investigacion p ON p.cod_proyecto = r.cod_proyecto
             """;

    private static final RowMapper<Proyecto> MAPeador = (rs, i) -> new Proyecto(
            rs.getString("cod_proyecto"), rs.getString("acronimo"), rs.getString("nombre_proyecto"),
            rs.getString("director"), rs.getBigDecimal("presupuesto_total"), rs.getString("estado"),
            (Integer) rs.getObject("investigadores_vigentes"), rs.getLong("publicaciones"),
            rs.getBigDecimal("puntaje_total"), rs.getString("dominio"),
            (Integer) rs.getObject("id_director"), rs.getObject("fecha_inicio", LocalDate.class),
            rs.getObject("fecha_fin", LocalDate.class), rs.getString("programa_idi"),
            rs.getString("descripcion"));

    private final NamedParameterJdbcTemplate jdbc;
    private final ProcedureCaller procedures;

    public ProyectoRepository(NamedParameterJdbcTemplate jdbc, ProcedureCaller procedures) {
        this.jdbc = jdbc;
        this.procedures = procedures;
    }

    public List<Proyecto> listar(FiltroProyecto f) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = construirFiltro(f, p);
        return jdbc.query(SELECT + where + " ORDER BY r.cod_proyecto DESC LIMIT :limite OFFSET :offset",
                p.addValue("limite", f.tamanio())
                        .addValue("offset", (f.pagina() - 1) * f.tamanio()), MAPeador);
    }

    public long contar(FiltroProyecto f) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String where = construirFiltro(f, p);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM v_proyectos_resumen r " +
                "JOIN proyecto_investigacion p ON p.cod_proyecto = r.cod_proyecto " + where, p, Long.class);
        return total == null ? 0 : total;
    }

    /** Los perfiles PROFESOR y ESTUDIANTE solo ven los proyectos vinculados a su cuenta. */
    private String construirFiltro(FiltroProyecto f, MapSqlParameterSource p) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1 ");
        if (f.q() != null && !f.q().isBlank()) {
            sql.append(" AND (r.cod_proyecto LIKE CONCAT('%',:q,'%')")
                    .append(" OR r.nombre_proyecto LIKE CONCAT('%',:q,'%')")
                    .append(" OR r.acronimo LIKE CONCAT('%',:q,'%')) ");
            p.addValue("q", f.q().trim());
        }
        if (f.estado() != null && !f.estado().isBlank()) {
            sql.append(" AND r.estado = :estado ");
            p.addValue("estado", f.estado());
        }
        if (f.idDirector() != null) {
            sql.append(" AND p.id_director = :idDirector ");
            p.addValue("idDirector", f.idDirector());
        }
        if (f.codDominio() != null && !f.codDominio().isBlank()) {
            sql.append(" AND r.cod_proyecto IN (SELECT cod_proyecto FROM proyecto_linea pl " +
                    "JOIN linea_investigacion l ON l.cod_linea = pl.cod_linea WHERE l.cod_dominio = :dominio) ");
            p.addValue("dominio", f.codDominio());
        }
        if (f.fechaDesde() != null && !f.fechaDesde().isBlank()) {
            sql.append(" AND p.fecha_inicio >= :desde ");
            p.addValue("desde", f.fechaDesde());
        }
        if (f.fechaHasta() != null && !f.fechaHasta().isBlank()) {
            sql.append(" AND p.fecha_inicio <= :hasta ");
            p.addValue("hasta", f.fechaHasta());
        }
        return filtroPorPerfil(sql, p);
    }

    private String filtroPorPerfil(StringBuilder sql, MapSqlParameterSource p) {
        String usuario = SecurityUtils.usuarioActual();
        if (SecurityUtils.esProfesor()) {
            sql.append(" AND (p.id_director IN (SELECT id_profesor FROM cuentas_profesores WHERE usuario = :usuario) ")
                    .append(" OR r.cod_proyecto IN (SELECT cod_proyecto FROM participa_proyecto ")
                    .append(" WHERE id_profesor IN (SELECT id_profesor FROM cuentas_profesores WHERE usuario = :usuario))) ");
            p.addValue("usuario", usuario);
        } else if (SecurityUtils.esEstudiante()) {
            sql.append(" AND r.cod_proyecto IN (SELECT cod_proyecto FROM participante_estudiante ")
                    .append(" WHERE id_estudiante IN (SELECT id_estudiante FROM cuentas_estudiantes WHERE usuario = :usuario)) ");
            p.addValue("usuario", usuario);
        }
        return sql.toString();
    }

    public Proyecto buscar(String codigo) {
        List<Proyecto> found = jdbc.query(SELECT + " WHERE r.cod_proyecto = :codigo",
                new MapSqlParameterSource("codigo", codigo), MAPeador);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Operación atómica existente en la base de datos. */
    public void crear(ProyectoForm f) {
        procedures.ejecutar("CALL sp_tx_inicio_proyecto(?,?,?,?,?,?,?,?,?)",
                f.codigo(), f.nombre(), f.acronimo(), f.presupuesto(), f.programa(), f.descripcion(),
                f.idDirector(), f.idSubrogante(), f.codLinea());
    }

    public int actualizar(ProyectoForm f) {
        return jdbc.update("""
                UPDATE proyecto_investigacion
                   SET nombre_proyecto = :nombre, acronimo = :acronimo, presupuesto_total = :presupuesto,
                       programa_idi = :programa, descripcion = :descripcion, id_director = :director
                 WHERE cod_proyecto = :codigo
                """, new MapSqlParameterSource()
                .addValue("nombre", f.nombre()).addValue("acronimo", f.acronimo())
                .addValue("presupuesto", f.presupuesto()).addValue("programa", f.programa())
                .addValue("descripcion", f.descripcion()).addValue("director", f.idDirector())
                .addValue("codigo", f.codigo()));
    }

    public void cerrar(String codigo) {
        procedures.ejecutar("CALL sp_tx_cierre_proyecto(?)", codigo);
    }

    public void retirarDirector(String codigo) {
        procedures.ejecutar("CALL sp_retirar_director(?)", codigo);
    }

    public void cambioInvestigador(String codigo, int sale, int entra, String rol) {
        procedures.ejecutar("CALL sp_tx_cambio_investigador(?,?,?,?)", codigo, sale, entra, rol);
    }

    public List<Map<String, Object>> lineas(String codigo) {
        return jdbc.queryForList("""
                SELECT l.cod_linea, l.nombre_linea, dc.nom_dominio
                  FROM proyecto_linea pl
                  JOIN linea_investigacion l ON l.cod_linea = pl.cod_linea
                  JOIN dominio_conocimiento dc ON dc.cod_dominio = l.cod_dominio
                 WHERE pl.cod_proyecto = :codigo ORDER BY l.cod_linea
                """, new MapSqlParameterSource("codigo", codigo));
    }

    public List<Map<String, Object>> integrantes(String codigo) {
        return jdbc.queryForList("""
                SELECT i.id_profesor, CONCAT(i.nombres,' ',i.apellidos) investigador, i.tipo,
                       pp.rol, pp.fecha_inicio, pp.fecha_fin,
                       CASE WHEN pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE() THEN 'VIGENTE' ELSE 'FINALIZADO' END estado
                  FROM participa_proyecto pp
                  JOIN investigador i ON i.id_profesor = pp.id_profesor
                 WHERE pp.cod_proyecto = :codigo ORDER BY pp.fecha_inicio DESC
                """, new MapSqlParameterSource("codigo", codigo));
    }

    public List<Map<String, Object>> estudiantes(String codigo) {
        return jdbc.queryForList("""
                SELECT e.id_estudiante, CONCAT(e.nombres,' ',e.apellidos) estudiante, e.carrera,
                       pe.fecha_inicio, pe.fecha_fin,
                       CASE WHEN pe.fecha_fin IS NULL OR pe.fecha_fin > CURDATE() THEN 'VIGENTE' ELSE 'FINALIZADO' END estado
                  FROM participante_estudiante pe
                  JOIN estudiante e ON e.id_estudiante = pe.id_estudiante
                 WHERE pe.cod_proyecto = :codigo ORDER BY pe.fecha_inicio DESC
                """, new MapSqlParameterSource("codigo", codigo));
    }

    public List<Map<String, Object>> publicaciones(String codigo) {
        return jdbc.queryForList("""
                SELECT cod_publicacion, titulo, tipo, nombre, puntos_asignados
                  FROM publicacion WHERE cod_proyecto = :codigo ORDER BY cod_publicacion
                """, new MapSqlParameterSource("codigo", codigo));
    }

    public List<Map<String, Object>> historial(String codigo) {
        return jdbc.queryForList("""
                (SELECT fecha_hora AS fecha, operacion AS tipo, tabla AS destino,
                        COALESCE(valores_nuevos, valores_anteriores) AS detalle, usuario
                   FROM auditoria_cambios
                  WHERE clave_registro = :codigo OR clave_registro LIKE CONCAT(:codigo, '|%'))
                UNION ALL
                (SELECT fecha_registro AS fecha, operacion AS tipo, procedimiento_ejecutado AS destino,
                        resultado AS detalle, COALESCE(usuario, 'bd') AS usuario
                   FROM auditoria_operaciones WHERE resultado LIKE CONCAT('%', :codigo, '%'))
                ORDER BY fecha DESC LIMIT 50
                """, new MapSqlParameterSource("codigo", codigo));
    }

    public int agregarLinea(String codigo, String codLinea) {
        return jdbc.update("INSERT INTO proyecto_linea (cod_proyecto, cod_linea) VALUES (:c, :l)",
                new MapSqlParameterSource("c", codigo).addValue("l", codLinea));
    }

    public int agregarInvestigador(String codigo, Integer id, String rol) {
        return jdbc.update("""
                INSERT INTO participa_proyecto (cod_proyecto, id_profesor, fecha_inicio, rol)
                VALUES (:c, :id, CURDATE(), :rol)
                """, new MapSqlParameterSource("c", codigo).addValue("id", id).addValue("rol", rol));
    }

    public int finalizarInvestigador(String codigo, Integer id) {
        return jdbc.update("""
                UPDATE participa_proyecto SET fecha_fin = CURDATE()
                 WHERE cod_proyecto = :c AND id_profesor = :id AND fecha_fin IS NULL
                """, new MapSqlParameterSource("c", codigo).addValue("id", id));
    }

    public int agregarEstudiante(String codigo, Integer id, String fechaInicio) {
        return jdbc.update("""
                INSERT INTO participante_estudiante (cod_proyecto, id_estudiante, fecha_inicio)
                VALUES (:c, :id, COALESCE(:fecha, CURDATE()))
                """, new MapSqlParameterSource("c", codigo).addValue("id", id).addValue("fecha", fechaInicio));
    }

    public int finalizarEstudiante(String codigo, Integer id) {
        return jdbc.update("""
                UPDATE participante_estudiante SET fecha_fin = CURDATE()
                 WHERE cod_proyecto = :c AND id_estudiante = :id AND fecha_fin IS NULL
                """, new MapSqlParameterSource("c", codigo).addValue("id", id));
    }

    public List<Map<String, Object>> directores() {
        return jdbc.queryForList("""
                SELECT i.id_profesor, CONCAT(i.nombres,' ',i.apellidos) nombre
                  FROM investigador i ORDER BY nombre
                """, Map.of());
    }

    /** Verificación de propiedad para perfiles PROFESOR y ESTUDIANTE. */
    public boolean puedeVer(String codigo, String usuario) {
        Long n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM proyecto_investigacion p
                 WHERE p.cod_proyecto = :c
                   AND (p.id_director IN (SELECT id_profesor FROM cuentas_profesores WHERE usuario = :u)
                     OR EXISTS (SELECT 1 FROM participa_proyecto pp
                                 WHERE pp.cod_proyecto = p.cod_proyecto
                                   AND pp.id_profesor IN (SELECT id_profesor FROM cuentas_profesores WHERE usuario = :u))
                     OR EXISTS (SELECT 1 FROM participante_estudiante pe
                                 WHERE pe.cod_proyecto = p.cod_proyecto
                                   AND pe.id_estudiante IN (SELECT id_estudiante FROM cuentas_estudiantes WHERE usuario = :u)))
                """, new MapSqlParameterSource("c", codigo).addValue("u", usuario), Long.class);
        return n != null && n > 0;
    }

    public BigDecimal presupuestoDe(String codigo) {
        BigDecimal v = jdbc.queryForObject("SELECT presupuesto_total FROM proyecto_investigacion WHERE cod_proyecto = :c",
                new MapSqlParameterSource("c", codigo), BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
