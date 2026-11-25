package com.example.backend.dto;

import java.util.List;
import java.util.Map;

/**	resultado de un reporte: columnas, fila paginada y CSV opcional generado con los mismos datos. */
public record ReporteResultado(String codigo, String titulo, List<String> columnas,
                               List<Map<String, Object>> filas, long total, int pagina, int tamanio,
                               String csv) {
}
