package com.example.backend.config;

import com.example.backend.dto.ReporteDefinicion;
import com.example.backend.pattern.strategy.ReporteStrategy;
import com.example.backend.pattern.strategy.ReporteStrategyImpl;
import com.example.backend.repository.ReporteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registro de estrategias reales: una por cada procedimiento de reporte del SQL original.
 * Las columnas son las devueltas por el SELECT de cada procedimiento.
 */
@Configuration
public class ReporteStrategyConfig {

    private final ReporteRepository repository;

    public ReporteStrategyConfig(ReporteRepository repository) {
        this.repository = repository;
    }

    private ReporteStrategy estrategia(String codigo, String titulo, String descripcion,
                                       String procedimiento, String... columnas) {
        return new ReporteStrategyImpl(repository,
                new ReporteDefinicion(codigo, titulo, descripcion, procedimiento, List.of(columnas)));
    }

    @Bean
    public ReporteStrategy av01() {
        return estrategia("av01", "Estado de equipos de investigación vigentes",
                "Cupos, días en ejecución y situación del subrogante por proyecto vigente.",
                "sp_av_01_estado_equipos_vigentes",
                "cod_proyecto", "acronimo", "director", "investigadores_vigentes",
                "cupos_libres", "dias_en_ejecucion", "estado_equipo");
    }

    @Bean
    public ReporteStrategy av02() {
        return estrategia("av02", "Mejor publicación por proyecto",
                "Publicación(es) con el mayor puntaje en cada proyecto.",
                "sp_av_02_mejor_publicacion_proyecto",
                "cod_proyecto", "nombre_proyecto", "titulo", "tipo", "puntos_asignados");
    }

    @Bean
    public ReporteStrategy av03() {
        return estrategia("av03", "Ranking de cumplimiento de investigadores",
                "Puntaje acumulado, publicaciones y nivel de cumplimiento por investigador.",
                "sp_av_03_ranking_cumplimiento",
                "posicion", "investigador", "tipo", "publicaciones", "puntaje_total", "nivel");
    }

    @Bean
    public ReporteStrategy av04() {
        return estrategia("av04", "Supervisión de investigadores no doctores",
                "Supervisor vigente, historial y meses supervisados por no doctor.",
                "sp_av_04_supervision_no_doctores",
                "no_doctor", "supervisor_vigente", "supervisiones_historicas",
                "meses_supervisados", "situacion");
    }

    @Bean
    public ReporteStrategy av05() {
        return estrategia("av05", "Presupuesto por dominio de conocimiento",
                "Distribución del presupuesto vigente entre los dominios.",
                "sp_av_05_presupuesto_por_dominio",
                "cod_dominio", "nom_dominio", "proyectos_vigentes",
                "presupuesto_vigente", "porcentaje_del_total");
    }

    @Bean
    public ReporteStrategy mt01() {
        return estrategia("mt01", "Producción científica completa",
                "Autor,publicación,proyecto,director,línea y dominio de cada producción.",
                "sp_mt_01_produccion_completa",
                "autor", "titulo", "tipo", "proyecto", "director", "linea", "dominio");
    }

    @Bean
    public ReporteStrategy mt02() {
        return estrategia("mt02", "Actividad de investigadores",
                "Líneas, publicaciones y tipos producidos por investigador.",
                "sp_mt_02_actividad_investigadores",
                "id_profesor", "investigador", "tipo", "lineas", "publicaciones", "tipos_publicados");
    }

    @Bean
    public ReporteStrategy mt03() {
        return estrategia("mt03", "Participación vigente en proyectos",
                "Rol y fecha de inicio de cada investigador, incluso sin proyecto vigente.",
                "sp_mt_03_participacion_vigente",
                "investigador", "tipo", "proyecto", "rol", "fecha_inicio");
    }

    @Bean
    public ReporteStrategy mt04() {
        return estrategia("mt04", "Supervisores y proyectos",
                "Supervisiones vigentes y proyecto en el que participa el supervisado.",
                "sp_mt_04_supervisores_y_proyectos",
                "supervisor_doctor", "supervisado", "fecha_inicio", "fecha_fin",
                "proyecto_vigente", "rol");
    }

    @Bean
    public ReporteStrategy mt05() {
        return estrategia("mt05", "Dominio, línea y proyecto",
                "Relación jerárquica dominio-línea-proyecto con número de publicaciones.",
                "sp_mt_05_dominio_linea_proyecto",
                "nom_dominio", "nombre_linea", "proyecto", "director", "publicaciones");
    }
}
