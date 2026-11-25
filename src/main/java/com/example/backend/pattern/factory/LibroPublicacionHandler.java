package com.example.backend.pattern.factory;

import com.example.backend.entity.TipoPublicacion;
import com.example.backend.repository.PublicacionRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Strategy LIBRO. */
@Component
public class LibroPublicacionHandler extends PublicacionHandlerBase {

    public LibroPublicacionHandler(PublicacionRepository repository) {
        super(repository);
    }

    @Override
    public String tipo() {
        return TipoPublicacion.LIBRO.name();
    }

    @Override
    protected Map<String, String> etiquetas() {
        return campos("editorial", "Editorial", "anio_publicacion", "Año de publicación");
    }
}
