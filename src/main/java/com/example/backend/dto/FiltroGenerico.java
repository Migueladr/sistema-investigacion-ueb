package com.example.backend.dto;

/** Filtro reutilizable para búsquedas AJAX y reportes. */
public record FiltroGenerico(String q, Integer page, Integer size) {

    public int pagina() {
        return page == null || page < 1 ? 1 : page;
    }

    public int tamanio() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
