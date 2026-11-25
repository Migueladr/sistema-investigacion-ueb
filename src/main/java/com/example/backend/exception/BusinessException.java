package com.example.backend.exception;

/** Error de negocio mostrado al usuario con un mensaje claro y seguro. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
