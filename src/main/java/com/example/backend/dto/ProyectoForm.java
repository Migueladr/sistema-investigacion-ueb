package com.example.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Formulario de alta y edición. La creación se realiza con sp_tx_inicio_proyecto. */
public record ProyectoForm(
        @NotBlank(message = "El código es obligatorio") @Size(max = 20) String codigo,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @NotBlank(message = "El acrónimo es obligatorio") @Size(max = 20) String acronimo,
        @NotNull(message = "El presupuesto es obligatorio") @DecimalMin(value = "0", message = "El presupuesto no puede ser negativo") BigDecimal presupuesto,
        @Size(max = 100) String programa,
        @Size(max = 4000) String descripcion,
        @NotNull(message = "Seleccione el director") Integer idDirector,
        Integer idSubrogante,
        String codLinea,
        String fechaInicio,
        String fechaFin) {
}
