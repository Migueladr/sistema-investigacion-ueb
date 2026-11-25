package com.example.backend.entity;

public record Estudiante(Integer id, String cedula, String nombres, String apellidos, String carrera) {

    /** La cédula se oculta en listados; solo se muestra en el detalle de lugares autorizados. */
    public String cedulaEnmascarada() {
        if (cedula == null || cedula.length() < 4) {
            return "••••";
        }
        return "••••••" + cedula.substring(cedula.length() - 4);
    }
}
