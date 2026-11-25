package com.example.backend.controller;

import com.example.backend.service.DominioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/lineas")
public class LineaController {

    private final DominioService service;

    public LineaController(DominioService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String dominio, Model model) {
        model.addAttribute("lineas", service.lineas(dominio));
        model.addAttribute("dominios", service.dominios());
        model.addAttribute("seleccionado", dominio);
        return "lineas/lista";
    }

    @GetMapping("/{codigo}")
    public String detalle(@PathVariable String codigo, Model model) {
        model.addAttribute("detalle", service.detalleLinea(codigo));
        model.addAttribute("lineas", service.lineas(null));
        model.addAttribute("codigo", codigo);
        return "lineas/detalle";
    }
}
