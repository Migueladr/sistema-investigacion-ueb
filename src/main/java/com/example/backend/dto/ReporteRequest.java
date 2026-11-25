package com.example.backend.dto;

public record ReporteRequest(String q, Integer page, Integer size, boolean csv) {

    public int pagina() {
        return page == null || page < 1 ? 1 : page;
    }

    public int tamanio() {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }
}
