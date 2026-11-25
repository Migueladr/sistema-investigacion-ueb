package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formulario único; cada Strategy usa solamente los campos de su tipo. */
public record PublicacionForm(
        @NotBlank(message = "Seleccione el tipo") @Pattern(regexp = "REVISTA|CONGRESO|LIBRO|PATENTE") String tipo,
        @NotBlank(message = "El proyecto es obligatorio") String codProyecto,
        @NotBlank(message = "El título es obligatorio") @Size(max = 200) String titulo,
        @Size(max = 150) String nombre,
        @NotNull(message = "Seleccione el autor") Integer idAutor,
        @NotBlank(message = "La línea de investigación es obligatoria") String codLinea,

        @Size(max = 100) String editorial,
        @Pattern(regexp = "ALTO_IMPACTO|REGIONAL|") String categoria,
        @Pattern(regexp = "Q[1-5]|") String cuartil,
        @Size(max = 10) String volumen,
        @Size(max = 10) String numero,
        String pagInicio,
        String pagFin,
        @Size(max = 40) String alcance,

        @Size(max = 30) String tipoCongreso,
        @Size(max = 100) String lugar,
        @Size(max = 60) String pais,
        String fechaInicio,
        String fechaFin,
        @Size(max = 100) String editorialActas,

        String anioPublicacion,

        @Size(max = 30) String numPatente,
        String fechaConcesion) {
}
