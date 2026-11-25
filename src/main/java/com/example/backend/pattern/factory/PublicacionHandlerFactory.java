package com.example.backend.pattern.factory;

import com.example.backend.entity.TipoPublicacion;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Factory Method: Spring inyecta los handlers y este método elige el especializado por tipo. */
@Component
public class PublicacionHandlerFactory {

    private final Map<String, PublicacionHandler> handlers = new HashMap<>();

    public PublicacionHandlerFactory(List<PublicacionHandler> beans) {
        beans.forEach(h -> handlers.put(h.tipo(), h));
    }

    public PublicacionHandler obtener(String tipo) {
        PublicacionHandler handler = handlers.get(TipoPublicacion.desde(tipo).name());
        if (handler == null) {
            throw new IllegalArgumentException("No existe un registrador para el tipo " + tipo);
        }
        return handler;
    }
}
