package com.example.backend.controller;

import com.example.backend.dto.FiltroProyecto;
import com.example.backend.dto.PublicacionForm;
import com.example.backend.service.DominioService;
import com.example.backend.service.InvestigadorService;
import com.example.backend.service.ProyectoService;
import com.example.backend.service.PublicacionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/publicaciones")
public class PublicacionController {

    private final PublicacionService service;
    private final ProyectoService proyectos;
    private final InvestigadorService investigadores;
    private final DominioService dominios;

    public PublicacionController(PublicacionService service, ProyectoService proyectos,
                                 InvestigadorService investigadores, DominioService dominios) {
        this.service = service;
        this.proyectos = proyectos;
        this.investigadores = investigadores;
        this.dominios = dominios;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String proyecto,
                        @RequestParam(required = false) Integer autor,
                        @RequestParam(required = false) String tipo,
                        @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("publicaciones", service.listar(q, proyecto, autor, tipo, page, 10));
        model.addAttribute("total", service.contar(q, proyecto, autor, tipo));
        model.addAttribute("pagina", page);
        model.addAttribute("q", q);
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("autor", autor);
        model.addAttribute("tipo", tipo);
        model.addAttribute("proyectos", proyectos.listar(new FiltroProyecto(null, null, null, null, null, null, 1, 100)));
        model.addAttribute("investigadores", investigadores.listar(null, null, 1, 100));
        return "publicaciones/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new PublicacionForm("REVISTA", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null));
        prepararCatalogos(model);
        return "publicaciones/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") PublicacionForm form, BindingResult errores,
                        Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            prepararCatalogos(model);
            return "publicaciones/form";
        }
        service.registrar(form);
        flash.addFlashAttribute("exito", "Publicación registrada. Los puntos fueron asignados por la base de datos.");
        return "redirect:/publicaciones";
    }

    @GetMapping("/{codigo}")
    public String detalle(@PathVariable int codigo, Model model) {
        model.addAttribute("datos", service.detalle(codigo));
        return "publicaciones/detalle";
    }

    @GetMapping("/{codigo}/editar")
    public String editar(@PathVariable int codigo, Model model) {
        var item = service.detalle(codigo).get("publicacion");
        com.example.backend.entity.PublicacionItem p =
                (com.example.backend.entity.PublicacionItem) item;
        model.addAttribute("codigo", codigo);
        model.addAttribute("titulo", p.titulo());
        model.addAttribute("nombre", p.nombre());
        return "publicaciones/editar";
    }

    @PostMapping("/{codigo}/editar")
    public String guardar(@PathVariable int codigo, @RequestParam String titulo,
                          @RequestParam(required = false) String nombre, RedirectAttributes flash) {
        service.editar(codigo, titulo, nombre);
        flash.addFlashAttribute("exito", "Datos básicos actualizados.");
        return "redirect:/publicaciones/" + codigo;
    }

    private void prepararCatalogos(Model model) {
        model.addAttribute("proyectos", proyectos.listar(new FiltroProyecto(null, null, null, null, null, null, 1, 100)));
        model.addAttribute("investigadores", investigadores.listar(null, null, 1, 100));
        model.addAttribute("lineas", dominios.lineas(null));
    }
}
