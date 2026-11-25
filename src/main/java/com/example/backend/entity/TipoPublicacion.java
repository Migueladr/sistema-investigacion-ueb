package com.example.backend.entity;

/** Tipos definidos en el ENUM de la columna publicacion.tipo. */
public enum TipoPublicacion {
    REVISTA, CONGRESO, LIBRO, PATENTE;

    public static TipoPublicacion desde(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Tipo de publicación no indicado");
        }
        return valueOf(texto.trim().toUpperCase());
    }
}
