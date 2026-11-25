package com.example.backend.pattern.template;

import com.example.backend.dto.ReporteDefinicion;
import com.example.backend.dto.ReporteRequest;
import com.example.backend.dto.ReporteResultado;
import com.example.backend.pattern.strategy.ReporteStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Template Method: el orden validar -> consultar -> filtrar -> paginar -> exportar es fijo.
 * Las subclases pueden cambiar los pasos protegidos sin alterar el algoritmo.
 */
public abstract class ReporteProcesador {

    public final ReporteResultado procesar(ReporteStrategy strategy, ReporteRequest request) {
        ReporteDefinicion definicion = strategy.definicion();
        validar(definicion);
        List<Map<String, Object>> filas = strategy.ejecutar();
        List<Map<String, Object>> filtradas = filtrar(filas, request.q());
        int total = filtradas.size();
        List<Map<String, Object>> pagina = paginar(filtradas, request.pagina(), request.tamanio());
        String csv = request.csv() ? exportarCsv(definicion.columnas(), pagina) : null;
        return new ReporteResultado(definicion.codigo(), definicion.titulo(),
                definicion.columnas(), pagina, total, request.pagina(), request.tamanio(), csv);
    }

    protected void validar(ReporteDefinicion definicion) {
        if (definicion.procedimiento() == null || definicion.procedimiento().isBlank()) {
            throw new IllegalArgumentException("El reporte no tiene un procedimiento asociado");
        }
    }

    protected List<Map<String, Object>> filtrar(List<Map<String, Object>> filas, String q) {
        if (q == null || q.isBlank()) {
            return filas;
        }
        String patron = q.trim().toLowerCase();
        return filas.stream().filter(fila -> fila.values().stream()
                        .anyMatch(v -> v != null && v.toString().toLowerCase().contains(patron)))
                .collect(Collectors.toList());
    }

    protected List<Map<String, Object>> paginar(List<Map<String, Object>> filas, int pagina, int tamanio) {
        int desde = Math.min((pagina - 1) * tamanio, filas.size());
        int hasta = Math.min(desde + tamanio, filas.size());
        return new ArrayList<>(filas.subList(desde, hasta));
    }

    protected String exportarCsv(List<String> columnas, List<Map<String, Object>> filas) {
        StringBuilder csv = new StringBuilder();
        csv.append(columnas.stream().map(this::escapar).collect(Collectors.joining(","))).append('\n');
        for (Map<String, Object> fila : filas) {
            csv.append(columnas.stream()
                    .map(c -> escapar(fila.get(c) == null ? "" : fila.get(c).toString()))
                    .collect(Collectors.joining(","))).append('\n');
        }
        return csv.toString();
    }

    private String escapar(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
