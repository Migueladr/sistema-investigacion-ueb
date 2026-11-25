package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InvestigadorForm(
        @NotNull(message = "El código de profesor es obligatorio") Integer idProfesor,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 60) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 60) String apellidos,
        @Size(max = 20) String despacho,
        @Pattern(regexp = "[0-9]{7,15}|", message = "El teléfono debe contener entre 7 y 15 dígitos") String telefono,
        @NotBlank(message = "El tipo es obligatorio") @Pattern(regexp = "DOCTOR|NO_DOCTOR") String tipo) {
}
