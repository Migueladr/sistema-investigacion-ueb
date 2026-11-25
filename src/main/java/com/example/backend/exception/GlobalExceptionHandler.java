package com.example.backend.exception;

import com.example.backend.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo centralizado: captura errores SQL (incluidos los SIGNAL de los procedimientos
 * almacenados y triggers) y los traduce a mensajes comprensibles para el usuario.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Object negocio(BusinessException ex, HttpServletRequest request, RedirectAttributes attrs) {
        return responder(request, attrs, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public Object datos(DataAccessException ex, HttpServletRequest request, RedirectAttributes attrs) {
        log.warn("Error SQL en {}: {}", request.getRequestURI(), ex.getMessage());
        return responder(request, attrs, mensajeSql(ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiResponse.error("Revise los campos indicados", errores));
    }

    /** Recursos estáticos ausentes (por ejemplo /favicon.ico): 404 limpio, sin llenar el log. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> recursoNoEncontrado(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public Object inesperado(Exception ex, HttpServletRequest request, RedirectAttributes attrs) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return responder(request, attrs, "Ocurrió un error inesperado. Intente nuevamente.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Object responder(HttpServletRequest request, RedirectAttributes attrs,
                             String mensaje, HttpStatus status) {
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(status).body(ApiResponse.error(mensaje, null));
        }
        attrs.addFlashAttribute("error", mensaje);
        return "redirect:" + destinoSeguro(request);
    }

    /** Evita redirecciones abiertas: solo acepta la ruta local del header Referer. */
    private String destinoSeguro(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.startsWith(request.getScheme() + "://" + request.getServerName())) {
            int indice = referer.indexOf(request.getContextPath() + "/", request.getScheme().length() + 3);
            if (indice >= 0) {
                return referer.substring(indice);
            }
        }
        return "/dashboard";
    }

    /** Extrae MESSAGE_TEXT de SIGNAL SQLSTATE sin filtrar detalles internos de la BD. */
    private String mensajeSql(DataAccessException ex) {
        String texto = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        if (texto == null) {
            return "No se pudo completar la operación en la base de datos.";
        }
        int marca = texto.indexOf("MESSAGE_TEXT=");
        if (marca >= 0) {
            String extraido = texto.substring(marca + "MESSAGE_TEXT=".length()).replace("'", "").trim();
            int fin = extraido.indexOf(';');
            return fin > 0 ? extraido.substring(0, fin) : extraido;
        }
        if (texto.toLowerCase().contains("duplicate")) {
            return "El registro ya existe. Revise el código o la clave indicada.";
        }
        if (texto.toLowerCase().contains("cannot add or update a child row")
                || texto.toLowerCase().contains("foreign key constraint fails")) {
            return "La operación vulnera una relación existente de la base de datos.";
        }
        return "No se pudo completar la operación. Verifique los datos y las validaciones de la base de datos.";
    }
}
