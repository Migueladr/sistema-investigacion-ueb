package com.example.backend.dto;

public record FiltroProyecto(String q, String estado, Integer idDirector, String codDominio,
                             String fechaDesde, String fechaHasta, Integer page, Integer size) {

    public int pagina() {
        return page == null || page < 1 ? 1 : page;
    }

    public int tamanio() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
