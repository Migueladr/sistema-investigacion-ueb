package com.example.backend;

import com.example.backend.pattern.observer.NotificacionService;
import com.example.backend.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * El contexto se inicia sin abrir conexiones (Hikari las crea bajo demanda).
 * Comprueba el cableado de la aplicación y el Singleton gestionado por Spring.
 */
@SpringBootTest
class BackendApplicationTests {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private ReporteService reportes;

    @Test
    void contextLoads() {
        assertSame(contexto.getBean(NotificacionService.class),
                contexto.getBean(NotificacionService.class),
                "Spring debe mantener una única instancia del servicio Observer");
        assertEquals(10, reportes.catalogo().size(),
                "Deben registrarse los diez procedimientos de reporte del SQL original");
    }
}
