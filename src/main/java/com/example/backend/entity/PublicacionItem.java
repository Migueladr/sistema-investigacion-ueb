package com.example.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PublicacionItem(Integer codigo, String codProyecto, String proyecto, String titulo,
                              String nombre, String tipo, BigDecimal puntos, String autor,
                              LocalDate fechaRegistro, String detalleResumen) {
}
