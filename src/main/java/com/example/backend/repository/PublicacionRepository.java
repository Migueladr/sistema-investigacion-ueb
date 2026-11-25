package com.example.backend.repository;

import com.example.backend.dto.PublicacionForm;
import com.example.backend.entity.PublicacionItem;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** Acceso a publicacion, escribe y a las tablas específicas por tipo. */
@Repository
public class PublicacionRepository {

    private static final String SELECT = """
            SELECT pu.cod_publicacion, pu.cod_proyecto, p.acronimo, pu.titulo, pu.nombre, pu.tipo,
                   pu.puntos_asignados,
                   (SELECT GROUP_CONCAT(CONCAT(i.nombres,' ',i.apellidos) SEPARATOR ', ')
                      FROM escribe e JOIN investigador i ON i.id_profesor = e.id_profesor
                     WHERE e.cod_publicacion = pu.cod_publicacion) AS autores
              FROM publicacion pu
              JOIN proyecto_investigacion p ON p.cod_proyecto = pu.cod_proyecto
             """;

    private static final RowMapper<PublicacionItem> MAPEADOR = (rs, i) -> new PublicacionItem(
            rs.getInt("cod_publicacion"), rs.getString("cod_proyecto"), rs.getString("acronimo"),
            rs.getString("titulo"), rs.getString("nombre"), rs.getString("tipo"),
            rs.getBigDecimal("puntos_asignados"), rs.getString("autores"), null, null);

    private final NamedParameterJdbcTemplate jdbc;
    private final ProcedureCaller procedures;

    public PublicacionRepository(NamedParameterJdbcTemplate jdbc, ProcedureCaller procedures) {
        this.jdbc = jdbc;
        this.procedures = procedures;
    }

    public List<PublicacionItem> listar(String q, String codProyecto, Integer idAutor, String tipo,
                                        int page, int size) {
        StringBuilder sql = new StringBuilder(SELECT);
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) {
            sql.append(" WHERE pu.titulo LIKE CONCAT('%',:q,'%') ");
            p.addValue("q", q.trim());
        } else {
            sql.append(" WHERE 1=1 ");
        }
        if (codProyecto != null && !codProyecto.isBlank()) {
            sql.append(" AND pu.cod_proyecto = :proyecto ");
            p.addValue("proyecto", codProyecto);
        }
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND pu.tipo = :tipo ");
            p.addValue("tipo", tipo);
        }
        if (idAutor != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM escribe e WHERE e.cod_publicacion = pu.cod_publicacion")
                    .append(" AND e.id_profesor = :autor) ");
            p.addValue("autor", idAutor);
        }
        sql.append(" ORDER BY pu.cod_publicacion DESC LIMIT :limite OFFSET :offset");
        return jdbc.query(sql.toString(), p.addValue("limite", size).addValue("offset", (page - 1) * size), MAPEADOR);
    }

    public long contar(String q, String codProyecto, Integer idAutor, String tipo) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM publicacion pu WHERE 1=1 ");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (q != null && !q.isBlank()) {
            sql.append(" AND pu.titulo LIKE CONCAT('%',:q,'%') ");
            p.addValue("q", q.trim());
        }
        if (codProyecto != null && !codProyecto.isBlank()) {
            sql.append(" AND pu.cod_proyecto = :proyecto ");
            p.addValue("proyecto", codProyecto);
        }
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND pu.tipo = :tipo ");
            p.addValue("tipo", tipo);
        }
        if (idAutor != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM escribe e WHERE e.cod_publicacion = pu.cod_publicacion")
                    .append(" AND e.id_profesor = :autor) ");
            p.addValue("autor", idAutor);
        }
        Long n = jdbc.queryForObject(sql.toString(), p, Long.class);
        return n == null ? 0 : n;
    }

    public PublicacionItem buscar(int codigo) {
        List<PublicacionItem> found = jdbc.query(SELECT + " WHERE pu.cod_publicacion = :codigo",
                new MapSqlParameterSource("codigo", codigo), MAPEADOR);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Procedimiento atómico existente: revista + autores + puntos por trigger. */
    public void registrarRevista(PublicacionForm f) {
        procedures.ejecutar("CALL sp_tx_registrar_revista(?,?,?,?,?,?,?,?)",
                f.codProyecto(), f.titulo(), f.nombre(), f.editorial(), f.categoria(),
                f.cuartil(), f.idAutor(), f.codLinea());
    }

    /** Primera parte de los tipos sin procedimiento propio; se invoca dentro de @Transactional. */
    public int insertarBase(PublicacionForm f) {
        KeyHolder clave = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO publicacion (cod_proyecto, titulo, nombre, tipo)
                VALUES (:proyecto, :titulo, :nombre, :tipo)
                """, new MapSqlParameterSource()
                .addValue("proyecto", f.codProyecto()).addValue("titulo", f.titulo())
                .addValue("nombre", f.nombre()).addValue("tipo", f.tipo()), clave, new String[]{"cod_publicacion"});
        Number id = clave.getKey();
        if (id == null) {
            throw new IllegalStateException("La base de datos no devolvió el código de publicación");
        }
        return id.intValue();
    }

    /** Las validaciones de tipo y la asignación de puntos las ejecutan los triggers de la BD. */
    public void insertarDetalle(String tipo, int codigo, PublicacionForm f) {
        MapSqlParameterSource p = new MapSqlParameterSource("codigo", codigo);
        switch (tipo) {
            case "CONGRESO" -> jdbc.update("""
                    INSERT INTO congreso (cod_publicacion, tipo_congreso, lugar, pais, fecha_inicio, fecha_fin, editorial_actas)
                    VALUES (:codigo, :tipoCongreso, :lugar, :pais, :fechaInicio, :fechaFin, :editorialActas)
                    """, p.addValue("tipoCongreso", f.tipoCongreso()).addValue("lugar", f.lugar())
                    .addValue("pais", f.pais()).addValue("fechaInicio", f.fechaInicio())
                    .addValue("fechaFin", f.fechaFin()).addValue("editorialActas", f.editorialActas()));
            case "LIBRO" -> jdbc.update("""
                    INSERT INTO libro (cod_publicacion, editorial, anio_publicacion)
                    VALUES (:codigo, :editorial, :anio)
                    """, p.addValue("editorial", f.editorial()).addValue("anio", entero(f.anioPublicacion())));
            case "PATENTE" -> jdbc.update("""
                    INSERT INTO patente (cod_publicacion, num_patente, fecha_concesion)
                    VALUES (:codigo, :numero, :fecha)
                    """, p.addValue("numero", f.numPatente()).addValue("fecha", f.fechaConcesion()));
            case "REVISTA" -> jdbc.update("""
                    INSERT INTO revista (cod_publicacion, editorial, categoria, cuartil, volumen, numero, pag_inicio, pag_fin, alcance)
                    VALUES (:codigo, :editorial, :categoria, :cuartil, :volumen, :numero, :pagInicio, :pagFin, :alcance)
                    """, p.addValue("editorial", f.editorial()).addValue("categoria", f.categoria())
                    .addValue("cuartil", f.cuartil()).addValue("volumen", f.volumen())
                    .addValue("numero", f.numero()).addValue("pagInicio", entero(f.pagInicio()))
                    .addValue("pagFin", entero(f.pagFin())).addValue("alcance", f.alcance()));
            default -> throw new IllegalArgumentException("Tipo de publicación no soportado: " + tipo);
        }
    }

    public void insertarAutor(int codigo, PublicacionForm f) {
        jdbc.update("INSERT INTO escribe (id_profesor, cod_publicacion, cod_linea) VALUES (:id, :codigo, :linea)",
                new MapSqlParameterSource("id", f.idAutor()).addValue("codigo", codigo).addValue("linea", f.codLinea()));
    }

    /** Devuelve únicamente los campos de la tabla específica del tipo. */
    public Map<String, Object> detalleEspecifico(int codigo, String tipo) {
        String sql = switch (tipo) {
            case "REVISTA" -> "SELECT editorial, volumen, numero, pag_inicio, pag_fin, categoria, cuartil, alcance FROM revista WHERE cod_publicacion = :c";
            case "CONGRESO" -> "SELECT tipo_congreso, lugar, pais, fecha_inicio, fecha_fin, editorial_actas FROM congreso WHERE cod_publicacion = :c";
            case "LIBRO" -> "SELECT editorial, anio_publicacion FROM libro WHERE cod_publicacion = :c";
            case "PATENTE" -> "SELECT num_patente, fecha_concesion FROM patente WHERE cod_publicacion = :c";
            default -> throw new IllegalArgumentException("Tipo de publicación no soportado: " + tipo);
        };
        List<Map<String, Object>> rows = jdbc.queryForList(sql, new MapSqlParameterSource("c", codigo));
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    public List<Map<String, Object>> autores(int codigo) {
        return jdbc.queryForList("""
                SELECT i.id_profesor, CONCAT(i.nombres,' ',i.apellidos) autor, l.nombre_linea
                  FROM escribe e JOIN investigador i ON i.id_profesor = e.id_profesor
                  JOIN linea_investigacion l ON l.cod_linea = e.cod_linea
                 WHERE e.cod_publicacion = :c
                """, new MapSqlParameterSource("c", codigo));
    }

    public int actualizarDatosBasicos(int codigo, String titulo, String nombre) {
        return jdbc.update("UPDATE publicacion SET titulo = :titulo, nombre = :nombre WHERE cod_publicacion = :c",
                new MapSqlParameterSource("c", codigo).addValue("titulo", titulo).addValue("nombre", nombre));
    }

    private Integer entero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return Integer.valueOf(valor);
    }
}
