package com.example.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Métricas del panel, todas calculadas con consultas sobre la base de datos. */
public record DashboardData(long totalProyectos, long vigentes, long finalizados,
                            long investigadores, long estudiantes, long publicaciones,
                            BigDecimal presupuestoVigente,
                            List<Map<String, Object>> porEstado,
                            List<Map<String, Object>> publicacionesPorTipo,
                            List<Map<String, Object>> presupuestoPorDominio) {
}
