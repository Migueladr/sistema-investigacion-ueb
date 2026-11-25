package com.example.backend.dto;

import java.util.List;

/** Catálogo de los diez reportes; cada definición apunta a un procedimiento existente. */
public record ReporteDefinicion(String codigo, String titulo, String descripcion,
                                String procedimiento, List<String> columnas) {
}
