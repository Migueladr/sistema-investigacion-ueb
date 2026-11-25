package com.example.backend.pattern.observer;

import java.time.LocalDateTime;

/** Evento interno del dominio. Observer: los listeners reaccionan sin acoplar servicios. */
public record EventoDominio(TipoEvento tipo, String codigo, String descripcion, LocalDateTime fecha) {

    public enum TipoEvento {
        PROYECTO_CREADO, PROYECTO_CERRADO, PUBLICACION_REGISTRADA
    }

    public static EventoDominio de(TipoEvento tipo, String codigo, String descripcion) {
        return new EventoDominio(tipo, codigo, descripcion, LocalDateTime.now());
    }
}
