package com.example.backend.repository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 *ejecuta únicamente los diez procedimientos de reporte definidos en el SQL original.
 * El mapper convierte cada nombre de columna en un encabezado legible para la tabla.
 */
@Repository
public class ReporteRepository {

    private final ProcedureCaller procedures;

    public ReporteRepository(ProcedureCaller procedures) {
        this.procedures = procedures;
    }

    public List<Map<String, Object>> ejecutar(String procedimiento) {
        return procedures.consultar(procedimiento);
    }

    public static String etiqueta(String columna) {
        return switch (columna) {
            case "cod_proyecto" -> "Código";
            case "acronimo" -> "Acrónimo";
            case "nombre_proyecto" -> "Proyecto";
            case "director" -> "Director";
            case "investigadores_vigentes" -> "Investigadores";
            case "cupos_libres" -> "Cupos libres";
            case "dias_en_ejecucion" -> "Días en ejecución";
            case "estado_equipo" -> "Estado del equipo";
            case "titulo" -> "Título";
            case "tipo" -> "Tipo";
            case "puntos_asignados" -> "Puntos";
            case "posicion" -> "Posición";
            case "investigador" -> "Investigador";
            case "publicaciones" -> "Publicaciones";
            case "puntaje_total" -> "Puntaje total";
            case "nivel" -> "Nivel";
            case "no_doctor" -> "No doctor";
            case "supervisor_vigente" -> "Supervisor vigente";
            case "supervisiones_historicas" -> "Supervisiones históricas";
            case "meses_supervisados" -> "Meses supervisados";
            case "situacion" -> "Situación";
            case "cod_dominio" -> "Código dominio";
            case "nom_dominio" -> "Dominio";
            case "proyectos_vigentes" -> "Proyectos vigentes";
            case "presupuesto_vigente" -> "Presupuesto vigente";
            case "porcentaje_del_total" -> "Porcentaje del total";
            case "autor" -> "Autor";
            case "proyecto" -> "Proyecto";
            case "linea" -> "Línea";
            case "dominio" -> "Dominio";
            case "id_profesor" -> "Código profesor";
            case "lineas" -> "Líneas";
            case "tipos_publicados" -> "Tipos publicados";
            case "rol" -> "Rol";
            case "fecha_inicio" -> "Fecha inicio";
            case "supervisor_doctor" -> "Supervisor doctor";
            case "supervisado" -> "Supervisado";
            case "fecha_fin" -> "Fecha fin";
            case "proyecto_vigente" -> "Proyecto vigente";
            case "nombre_linea" -> "Línea de investigación";
            default -> columna.replace('_', ' ');
        };
    }
}
