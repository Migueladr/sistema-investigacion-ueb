package com.example.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Proyección de v_proyectos_resumen con el dominio resuelto. */
public record Proyecto(String codigo, String acronimo, String nombre, String director,
                       BigDecimal presupuesto, String estado, Integer investigadoresVigentes,
                       Long publicaciones, BigDecimal puntajeTotal, String dominio,
                       Integer idDirector, LocalDate fechaInicio, LocalDate fechaFin,
                       String programa, String descripcion) {
}
