package com.example.backend.controller;

import com.example.backend.dto.ReporteRequest;
import com.example.backend.service.ReporteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping
    public String catalogo(Model model) {
        model.addAttribute("reportes", service.catalogo());
        return "reportes/lista";
    }

    @GetMapping("/{codigo}")
    public String ver(@PathVariable String codigo,
                      @RequestParam(required = false) String q,
                      @RequestParam(defaultValue = "1") int page, Model model) {
        ReporteRequest request = new ReporteRequest(q, page, 10, false);
        model.addAttribute("resultado", service.generar(codigo, request));
        model.addAttribute("reportes", service.catalogo());
        model.addAttribute("definicion", service.definicion(codigo));
        model.addAttribute("q", q);
        return "reportes/ver";
    }
}
