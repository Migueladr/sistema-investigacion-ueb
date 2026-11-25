package com.example.backend.pattern.factory;

import com.example.backend.entity.TipoPublicacion;
import com.example.backend.repository.PublicacionRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Strategy PATENTE. */
@Component
public class PatentePublicacionHandler extends PublicacionHandlerBase {

    public PatentePublicacionHandler(PublicacionRepository repository) {
        super(repository);
    }

    @Override
    public String tipo() {
        return TipoPublicacion.PATENTE.name();
    }

    @Override
    protected Map<String, String> etiquetas() {
        return campos("num_patente", "Número de patente", "fecha_concesion", "Fecha de concesión");
    }
}
