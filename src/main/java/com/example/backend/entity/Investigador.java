package com.example.backend.entity;

public record Investigador(Integer id, String nombres, String apellidos, String despacho,
                           String telefono, String tipo, String nombreCompleto,
                           Long proyectosInvestigador, Long proyectosDirector) {

    public boolean esDoctor() {
        return "DOCTOR".equals(tipo);
    }
}
