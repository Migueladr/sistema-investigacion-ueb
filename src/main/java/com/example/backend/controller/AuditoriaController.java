package com.example.backend.controller;

import com.example.backend.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String operacion,
                        @RequestParam(required = false) String tabla,
                        @RequestParam(required = false) String usuario,
                        @RequestParam(required = false) String desde,
                        @RequestParam(required = false) String hasta, Model model) {
        model.addAttribute("cambios", service.cambios(operacion, tabla, usuario, desde, hasta));
        model.addAttribute("operaciones", service.operaciones(operacion, tabla, usuario, desde, hasta));
        model.addAttribute("filtros", new AuditoriaFiltros(operacion, tabla, usuario, desde, hasta));
        return "auditoria/lista";
    }

    /** Objeto inmutable para conservar los filtros en el formulario. */
    public record AuditoriaFiltros(String operacion, String tabla, String usuario, String desde, String hasta) {
    }
}
