package com.example.backend.dto;

/** Envoltura homogénea para todos los endpoints REST. */
public record ApiResponse<T>(boolean ok, String mensaje, T datos, Long total) {

    public static <T> ApiResponse<T> ok(T datos) {
        return new ApiResponse<>(true, null, datos, null);
    }

    public static <T> ApiResponse<T> ok(T datos, long total) {
        return new ApiResponse<>(true, null, datos, total);
    }

    public static <T> ApiResponse<T> error(String mensaje, T datos) {
        return new ApiResponse<>(false, mensaje, datos, null);
    }
}
