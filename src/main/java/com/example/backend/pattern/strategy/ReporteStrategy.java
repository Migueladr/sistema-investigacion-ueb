package com.example.backend.pattern.strategy;

import com.example.backend.dto.ReporteDefinicion;

import java.util.List;
import java.util.Map;

/** Strategy: cada reporte expone su definición y ejecuta su procedimiento almacenado. */
public interface ReporteStrategy {

    ReporteDefinicion definicion();

    List<Map<String, Object>> ejecutar();
}
