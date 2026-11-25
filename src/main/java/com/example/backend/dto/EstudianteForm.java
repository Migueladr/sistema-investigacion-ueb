package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EstudianteForm(
        @NotNull(message = "El código de estudiante es obligatorio") Integer idEstudiante,
        @NotBlank(message = "La cédula es obligatoria") @Pattern(regexp = "[0-9]{10}", message = "La cédula debe tener 10 dígitos") String cedula,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 60) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 60) String apellidos,
        @NotBlank(message = "La carrera es obligatoria") @Size(max = 60) String carrera) {
}
