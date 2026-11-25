package com.example.backend.pattern.strategy;

import com.example.backend.dto.ReporteDefinicion;
import com.example.backend.repository.ReporteRepository;

import java.util.List;
import java.util.Map;

public class ReporteStrategyImpl implements ReporteStrategy {

    private final ReporteRepository repository;
    private final ReporteDefinicion definicion;

    public ReporteStrategyImpl(ReporteRepository repository, ReporteDefinicion definicion) {
        this.repository = repository;
        this.definicion = definicion;
    }

    @Override
    public ReporteDefinicion definicion() {
        return definicion;
    }

    @Override
    public List<Map<String, Object>> ejecutar() {
        return repository.ejecutar(definicion.procedimiento());
    }
}
