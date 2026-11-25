package com.example.backend.entity;

import java.util.List;
import java.util.Map;

/** Detalle completo de un proyecto, ensamblado en el servicio a partir de varias consultas. */
public record ProyectoDetalle(Proyecto proyecto,
                              List<Map<String, Object>> lineas,
                              List<Map<String, Object>> integrantes,
                              List<Map<String, Object>> estudiantes,
                              List<Map<String, Object>> publicaciones,
                              List<Map<String, Object>> historial) {
}
