package com.example.backend;

import com.example.backend.dto.ReporteDefinicion;
import com.example.backend.dto.ReporteRequest;
import com.example.backend.dto.ReporteResultado;
import com.example.backend.pattern.factory.CongresoPublicacionHandler;
import com.example.backend.pattern.factory.LibroPublicacionHandler;
import com.example.backend.pattern.factory.PatentePublicacionHandler;
import com.example.backend.pattern.factory.PublicacionHandlerFactory;
import com.example.backend.pattern.factory.RevistaPublicacionHandler;
import com.example.backend.pattern.observer.EventoDominio;
import com.example.backend.pattern.observer.NotificacionService;
import com.example.backend.pattern.strategy.ReporteStrategyImpl;
import com.example.backend.pattern.template.ReporteProcesadorEstandar;
import com.example.backend.repository.PublicacionRepository;
import com.example.backend.repository.ReporteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de los patrones GoF implementados, sin base de datos. */
class PatronesTest {

    @Test
    void factoryResuelveElHandlerAdecuadoSegunElTipo() {
        PublicacionRepository repositorio = mock(PublicacionRepository.class);
        var factory = new PublicacionHandlerFactory(List.of(
                new RevistaPublicacionHandler(repositorio),
                new CongresoPublicacionHandler(repositorio),
                new LibroPublicacionHandler(repositorio),
                new PatentePublicacionHandler(repositorio)));

        assertInstanceOf(RevistaPublicacionHandler.class, factory.obtener("REVISTA"));
        assertInstanceOf(CongresoPublicacionHandler.class, factory.obtener(" congreso "));
        assertInstanceOf(LibroPublicacionHandler.class, factory.obtener("libro"));
        assertInstanceOf(PatentePublicacionHandler.class, factory.obtener("PATENTE"));
        assertThrows(IllegalArgumentException.class, () -> factory.obtener("INEXISTENTE"));
    }

    @Test
    void strategyEjecutaUnicamenteElProcedimientoDeSuDefinicion() {
        ReporteRepository repositorio = mock(ReporteRepository.class);
        when(repositorio.ejecutar("sp_av_01_estado_equipos_vigentes"))
                .thenReturn(List.of(Map.of("cod_proyecto", "P1")));
        var definicion = new ReporteDefinicion("av01", "Equipos", "Descripción",
                "sp_av_01_estado_equipos_vigentes", List.of("cod_proyecto"));
        var estrategia = new ReporteStrategyImpl(repositorio, definicion);

        assertEquals(definicion, estrategia.definicion());
        assertEquals(1, estrategia.ejecutar().size());
        verify(repositorio).ejecutar("sp_av_01_estado_equipos_vigentes");
    }

    @Test
    void templateMethodFiltraPaginaYGeneraCsvConLosMismosDatos() {
        ReporteRepository repositorio = mock(ReporteRepository.class);
        when(repositorio.ejecutar("sp_prueba")).thenReturn(List.of(
                Map.of("codigo", "A", "valor", "uno"),
                Map.of("codigo", "B", "valor", "dos")));
        var definicion = new ReporteDefinicion("av01", "Título", "Descripción",
                "sp_prueba", List.of("codigo", "valor"));

        ReporteResultado resultado = new ReporteProcesadorEstandar().procesar(
                new ReporteStrategyImpl(repositorio, definicion),
                new ReporteRequest("uno", 1, 10, true));

        assertEquals(1, resultado.total());
        assertEquals("A", resultado.filas().get(0).get("codigo"));
        assertTrue(resultado.csv().startsWith("\"codigo\",\"valor\""));
        assertTrue(resultado.csv().contains("\"A\",\"uno\""));
    }

    @Test
    void observerConservaSoloLasUltimasTreintaNotificaciones() {
        var servicio = new NotificacionService();
        for (int i = 0; i < 35; i++) {
            servicio.registrar(EventoDominio.de(EventoDominio.TipoEvento.PROYECTO_CREADO,
                    "P" + i, "Proyecto " + i));
        }

        assertEquals(30, servicio.ultimas(50).size());
        assertEquals(5, servicio.ultimas(5).size());
        assertTrue(servicio.ultimas(5).stream()
                .allMatch(e -> Integer.parseInt(e.codigo().substring(1)) >= 30));
    }
}
