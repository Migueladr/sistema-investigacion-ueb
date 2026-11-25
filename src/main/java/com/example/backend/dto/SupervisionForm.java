package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupervisionForm(
        @NotNull Integer idDoctor,
        @NotNull Integer idNoDoctor,
        @NotBlank String fechaInicio,
        String fechaFin) {
}
