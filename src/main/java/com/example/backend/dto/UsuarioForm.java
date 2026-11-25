package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UsuarioForm(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Use letras, números, punto, guion o guion bajo") String username,
        @Size(max = 100) String password,
        Integer idProfesor,
        Integer idEstudiante,
        List<String> roles,
        Boolean activo) {
}
