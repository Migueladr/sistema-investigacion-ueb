package com.example.backend.pattern.factory;

import com.example.backend.entity.TipoPublicacion;
import com.example.backend.repository.PublicacionRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Strategy CONGRESO: transacción local + triggers de validación y puntos. */
@Component
public class CongresoPublicacionHandler extends PublicacionHandlerBase {

    public CongresoPublicacionHandler(PublicacionRepository repository) {
        super(repository);
    }

    @Override
    public String tipo() {
        return TipoPublicacion.CONGRESO.name();
    }

    @Override
    protected Map<String, String> etiquetas() {
        return campos("tipo_congreso", "Tipo de congreso", "lugar", "Lugar", "pais", "País",
                "fecha_inicio", "Fecha inicio", "fecha_fin", "Fecha fin",
                "editorial_actas", "Editorial de actas");
    }
}
