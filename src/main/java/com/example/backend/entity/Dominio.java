package com.example.backend.entity;

import java.math.BigDecimal;

public record Dominio(String codigo, String nombre, Long proyectos, BigDecimal presupuestoVigente) {
}
