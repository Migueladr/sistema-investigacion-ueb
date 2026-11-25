package com.example.backend.pattern.factory;

import com.example.backend.dto.PublicacionForm;
import com.example.backend.entity.TipoPublicacion;
import com.example.backend.repository.PublicacionRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory Method: este handler encapsula la única publicación con procedimiento atómico propio.
 * Los puntos los asigna el trigger trg_revista_ai mediante sp_asignar_puntos.
 */
@Component
public class RevistaPublicacionHandler extends PublicacionHandlerBase {

    public RevistaPublicacionHandler(PublicacionRepository repository) {
        super(repository);
    }

    @Override
    public String tipo() {
        return TipoPublicacion.REVISTA.name();
    }

    @Override
    public void registrar(PublicacionForm formulario) {
        repository.registrarRevista(formulario);
    }

    @Override
    protected Map<String, String> etiquetas() {
        return campos("editorial", "Editorial", "categoria", "Categoría", "cuartil", "Cuartil",
                "volumen", "Volumen", "numero", "Número", "pag_inicio", "Página inicio",
                "pag_fin", "Página fin", "alcance", "Alcance");
    }
}
