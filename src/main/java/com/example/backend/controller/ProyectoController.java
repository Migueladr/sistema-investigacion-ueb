package com.example.backend.controller;

import com.example.backend.dto.FiltroProyecto;
import com.example.backend.dto.ProyectoForm;
import com.example.backend.entity.Proyecto;
import com.example.backend.service.DominioService;
import com.example.backend.service.EstudianteService;
import com.example.backend.service.ProyectoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/proyectos")
public class ProyectoController {

    private final ProyectoService service;
    private final DominioService dominios;
    private final EstudianteService estudiantes;

    public ProyectoController(ProyectoService service, DominioService dominios, EstudianteService estudiantes) {
        this.service = service;
        this.dominios = dominios;
        this.estudiantes = estudiantes;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String estado,
                        @RequestParam(required = false) Integer idDirector,
                        @RequestParam(required = false) String codDominio,
                        @RequestParam(required = false) String fechaDesde,
                        @RequestParam(required = false) String fechaHasta,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        FiltroProyecto filtro = new FiltroProyecto(q, estado, idDirector, codDominio,
                fechaDesde, fechaHasta, page, 10);
        model.addAttribute("proyectos", service.listar(filtro));
        model.addAttribute("total", service.contar(filtro));
        model.addAttribute("pagina", filtro.pagina());
        model.addAttribute("filtro", filtro);
        model.addAttribute("directores", service.directores());
        model.addAttribute("dominios", dominios.dominios());
        return "proyectos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ProyectoForm(null, null, null, null, null, null, null, null, null, null, null));
        model.addAttribute("directores", service.directores());
        model.addAttribute("lineas", dominios.lineas(null));
        model.addAttribute("edicion", false);
        return "proyectos/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") ProyectoForm form, BindingResult errores,
                        Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            prepararFormulario(model, form, false);
            return "proyectos/form";
        }
        service.crear(form);
        flash.addFlashAttribute("exito", "Proyecto registrado con su equipo y línea de investigación.");
        return "redirect:/proyectos/" + form.codigo();
    }

    @GetMapping("/{codigo}/editar")
    public String editar(@PathVariable String codigo, Model model) {
        Proyecto p = service.buscar(codigo);
        ProyectoForm form = new ProyectoForm(p.codigo(), p.nombre(), p.acronimo(), p.presupuesto(),
                p.programa(), p.descripcion(), p.idDirector(), null, null,
                p.fechaInicio() == null ? null : p.fechaInicio().format(DateTimeFormatter.ISO_LOCAL_DATE),
                p.fechaFin() == null ? null : p.fechaFin().format(DateTimeFormatter.ISO_LOCAL_DATE));
        prepararFormulario(model, form, true);
        return "proyectos/form";
    }

    @PostMapping("/{codigo}/editar")
    public String guardar(@PathVariable String codigo, @Valid @ModelAttribute("form") ProyectoForm form,
                          BindingResult errores, Model model, RedirectAttributes flash) {
        ProyectoForm corregido = new ProyectoForm(codigo, form.nombre(), form.acronimo(), form.presupuesto(),
                form.programa(), form.descripcion(), form.idDirector(), form.idSubrogante(),
                form.codLinea(), form.fechaInicio(), form.fechaFin());
        if (errores.hasErrors()) {
            prepararFormulario(model, corregido, true);
            return "proyectos/form";
        }
        service.editar(corregido);
        flash.addFlashAttribute("exito", "Información del proyecto actualizada.");
        return "redirect:/proyectos/" + codigo;
    }

    @GetMapping("/{codigo}")
    public String detalle(@PathVariable String codigo, Model model) {
        model.addAttribute("detalle", service.detalle(codigo));
        model.addAttribute("investigadores", service.directores());
        model.addAttribute("estudiantes", estudiantes.listar(null, 1, 100));
        model.addAttribute("lineas", dominios.lineas(null));
        return "proyectos/detalle";
    }

    @GetMapping("/{codigo}/historial")
    public String historial(@PathVariable String codigo, Model model) {
        model.addAttribute("proyecto", service.buscar(codigo));
        model.addAttribute("historial", service.historial(codigo));
        return "proyectos/historial";
    }

    @PostMapping("/{codigo}/cerrar")
    public String cerrar(@PathVariable String codigo, RedirectAttributes flash) {
        service.cerrar(codigo);
        flash.addFlashAttribute("exito", "Proyecto cerrado mediante sp_tx_cierre_proyecto.");
        return "redirect:/proyectos/" + codigo;
    }

    @PostMapping("/{codigo}/director")
    public String cambiarDirector(@PathVariable String codigo, RedirectAttributes flash) {
        service.retirarDirector(codigo);
        flash.addFlashAttribute("exito", "El subrogante asumió la dirección del proyecto.");
        return "redirect:/proyectos/" + codigo;
    }

    @PostMapping("/{codigo}/lineas")
    public String linea(@PathVariable String codigo, @RequestParam String codLinea, RedirectAttributes flash) {
        service.asignarLinea(codigo, codLinea);
        flash.addFlashAttribute("exito", "Línea de investigación asociada.");
        return "redirect:/proyectos/" + codigo;
    }

    @PostMapping("/{codigo}/investigadores")
    public String investigador(@PathVariable String codigo, @RequestParam Integer id,
                               @RequestParam(defaultValue = "INVESTIGADOR") String rol,
                               RedirectAttributes flash) {
        service.asignarInvestigador(codigo, id, rol);
        flash.addFlashAttribute("exito", "Investigador asignado al proyecto.");
        return "redirect:/proyectos/" + codigo;
    }

    @PostMapping("/{codigo}/investigadores/{id}/fin")
    public String finInvestigador(@PathVariable String codigo, @PathVariable Integer id, RedirectAttributes flash) {
        service.finalizarInvestigador(codigo, id);
        flash.addFlashAttribute("exito", "Participación finalizada.");
        return "redirect:/proyectos/" + codigo;
    }

    @PostMapping("/{codigo}/estudiantes")
    public String estudiante(@PathVariable String codigo, @RequestParam Integer id,
                             @RequestParam(required = false) String fechaInicio, RedirectAttributes flash) {
        service.asignarEstudiante(codigo, id, fechaInicio);
        flash.addFlashAttribute("exito", "Estudiante asignado al proyecto.");
        return "redirect:/proyectos/" + codigo;
    }

    @PostMapping("/{codigo}/estudiantes/{id}/fin")
    public String finEstudiante(@PathVariable String codigo, @PathVariable Integer id, RedirectAttributes flash) {
        service.finalizarEstudiante(codigo, id);
        flash.addFlashAttribute("exito", "Participación del estudiante finalizada.");
        return "redirect:/proyectos/" + codigo;
    }

    private void prepararFormulario(Model model, ProyectoForm form, boolean edicion) {
        model.addAttribute("form", form);
        model.addAttribute("directores", service.directores());
        model.addAttribute("lineas", dominios.lineas(null));
        model.addAttribute("edicion", edicion);
    }
}
