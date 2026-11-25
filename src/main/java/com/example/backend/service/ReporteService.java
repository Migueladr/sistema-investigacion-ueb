package com.example.backend.service;

import com.example.backend.dto.ReporteDefinicion;
import com.example.backend.dto.ReporteRequest;
import com.example.backend.dto.ReporteResultado;
import com.example.backend.exception.BusinessException;
import com.example.backend.pattern.strategy.ReporteStrategy;
import com.example.backend.pattern.template.ReporteProcesador;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteService {

    private final List<ReporteStrategy> estrategias;
    private final ReporteProcesador procesador;

    public ReporteService(List<ReporteStrategy> estrategias, ReporteProcesador procesador) {
        this.estrategias = estrategias;
        this.procesador = procesador;
    }

    public List<ReporteDefinicion> catalogo() {
        return estrategias.stream().map(ReporteStrategy::definicion).toList();
    }

    public ReporteDefinicion definicion(String codigo) {
        return estrategias.stream().map(ReporteStrategy::definicion)
                .filter(d -> d.codigo().equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No existe el reporte indicado"));
    }

    public ReporteResultado generar(String codigo, ReporteRequest request) {
        ReporteStrategy estrategia = estrategias.stream()
                .filter(e -> e.definicion().codigo().equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No existe el reporte indicado"));
        return procesador.procesar(estrategia, request);
    }
}
