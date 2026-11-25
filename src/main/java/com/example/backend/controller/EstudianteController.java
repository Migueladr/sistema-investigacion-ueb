package com.example.backend.controller;

import com.example.backend.dto.EstudianteForm;
import com.example.backend.service.EstudianteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estudiantes")
public class EstudianteController {

    private final EstudianteService service;

    public EstudianteController(EstudianteService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("estudiantes", service.listar(q, page, 10));
        model.addAttribute("total", service.contar(q));
        model.addAttribute("pagina", page);
        model.addAttribute("q", q);
        return "estudiantes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new EstudianteForm(null, null, null, null, null));
        model.addAttribute("edicion", false);
        return "estudiantes/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") EstudianteForm form, BindingResult errores,
                        Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            model.addAttribute("edicion", false);
            return "estudiantes/form";
        }
        service.guardar(form);
        flash.addFlashAttribute("exito", "Estudiante registrado.");
        return "redirect:/estudiantes";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, Model model) {
        model.addAttribute("datos", service.detalle(id));
        return "estudiantes/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        var e = service.buscar(id);
        model.addAttribute("form", new EstudianteForm(e.id(), e.cedula(), e.nombres(), e.apellidos(), e.carrera()));
        model.addAttribute("edicion", true);
        return "estudiantes/form";
    }

    @PostMapping("/{id}/editar")
    public String guardar(@PathVariable int id, @Valid @ModelAttribute("form") EstudianteForm form,
                          BindingResult errores, Model model, RedirectAttributes flash) {
        EstudianteForm corregido = new EstudianteForm(id, form.cedula(), form.nombres(),
                form.apellidos(), form.carrera());
        if (errores.hasErrors()) {
            model.addAttribute("form", corregido);
            model.addAttribute("edicion", true);
            return "estudiantes/form";
        }
        service.editar(corregido);
        flash.addFlashAttribute("exito", "Datos del estudiante actualizados.");
        return "redirect:/estudiantes/" + id;
    }
}
