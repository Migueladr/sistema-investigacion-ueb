package com.example.backend.controller.api;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.EstudianteForm;
import com.example.backend.dto.FiltroProyecto;
import com.example.backend.dto.InvestigadorForm;
import com.example.backend.dto.ProyectoForm;
import com.example.backend.dto.PublicacionForm;
import com.example.backend.dto.ReporteRequest;
import com.example.backend.dto.SupervisionForm;
import com.example.backend.dto.UsuarioForm;
import com.example.backend.service.AuditoriaService;
import com.example.backend.service.DashboardService;
import com.example.backend.service.DominioService;
import com.example.backend.service.EstudianteService;
import com.example.backend.service.InvestigadorService;
import com.example.backend.service.ProyectoService;
import com.example.backend.service.PublicacionService;
import com.example.backend.service.ReporteService;
import com.example.backend.service.SupervisionService;
import com.example.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * API JSON para consumo externo. Todas las respuestas usan ApiResponse y los mismos
 * servicios que la interfaz web; no existe lógica de negocio duplicada en este controlador.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final DashboardService dashboard;
    private final ProyectoService proyectos;
    private final InvestigadorService investigadores;
    private final EstudianteService estudiantes;
    private final PublicacionService publicaciones;
    private final DominioService dominios;
    private final SupervisionService supervisiones;
    private final ReporteService reportes;
    private final AuditoriaService auditoria;
    private final UsuarioService usuarios;

    public ApiController(DashboardService dashboard, ProyectoService proyectos,
                         InvestigadorService investigadores, EstudianteService estudiantes,
                         PublicacionService publicaciones, DominioService dominios,
                         SupervisionService supervisiones, ReporteService reportes,
                         AuditoriaService auditoria, UsuarioService usuarios) {
        this.dashboard = dashboard;
        this.proyectos = proyectos;
        this.investigadores = investigadores;
        this.estudiantes = estudiantes;
        this.publicaciones = publicaciones;
        this.dominios = dominios;
        this.supervisiones = supervisiones;
        this.reportes = reportes;
        this.auditoria = auditoria;
        this.usuarios = usuarios;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Object> dashboard() {
        return ApiResponse.ok(dashboard.obtener());
    }

    @GetMapping("/proyectos")
    public ApiResponse<Object> proyectos(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String estado,
                                         @RequestParam(required = false) Integer idDirector,
                                         @RequestParam(required = false) String codDominio,
                                         @RequestParam(required = false) String fechaDesde,
                                         @RequestParam(required = false) String fechaHasta,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        FiltroProyecto filtro = new FiltroProyecto(q, estado, idDirector, codDominio,
                fechaDesde, fechaHasta, page, size);
        return ApiResponse.ok(proyectos.listar(filtro), proyectos.contar(filtro));
    }

    @GetMapping("/proyectos/{codigo}")
    public ApiResponse<Object> proyecto(@PathVariable String codigo) {
        return ApiResponse.ok(proyectos.detalle(codigo));
    }

    @PostMapping("/proyectos")
    public ApiResponse<Object> crearProyecto(@Valid @RequestBody ProyectoForm form) {
        proyectos.crear(form);
        return ApiResponse.ok(Map.of("codigo", form.codigo()));
    }

    @PostMapping("/proyectos/{codigo}/cerrar")
    public ApiResponse<Object> cerrarProyecto(@PathVariable String codigo) {
        proyectos.cerrar(codigo);
        return ApiResponse.ok(Map.of("codigo", codigo, "estado", "CERRADO"));
    }

    /** Expone sp_tx_cambio_investigador, antes disponible solo en la capa de servicio. */
    @PostMapping("/proyectos/{codigo}/investigadores/cambio")
    public ApiResponse<Object> cambiarInvestigador(@PathVariable String codigo,
                                                   @RequestParam int sale,
                                                   @RequestParam int entra,
                                                   @RequestParam(defaultValue = "INVESTIGADOR") String rol) {
        proyectos.cambiarInvestigador(codigo, sale, entra, rol);
        return ApiResponse.ok(Map.of("codigo", codigo, "sale", sale, "entra", entra, "rol", rol));
    }

    @GetMapping("/investigadores")
    public ApiResponse<Object> investigadores(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String tipo,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(investigadores.listar(q, tipo, page, size), investigadores.contar(q, tipo));
    }

    @GetMapping("/investigadores/{id}")
    public ApiResponse<Object> investigador(@PathVariable int id) {
        return ApiResponse.ok(investigadores.detalle(id));
    }

    @PostMapping("/investigadores")
    public ApiResponse<Object> crearInvestigador(@Valid @RequestBody InvestigadorForm form) {
        investigadores.guardar(form);
        return ApiResponse.ok(Map.of("id", form.idProfesor()));
    }

    @GetMapping("/estudiantes")
    public ApiResponse<Object> estudiantes(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(estudiantes.listar(q, page, size), estudiantes.contar(q));
    }

    @GetMapping("/estudiantes/{id}")
    public ApiResponse<Object> estudiante(@PathVariable int id) {
        return ApiResponse.ok(estudiantes.detalle(id));
    }

    @PostMapping("/estudiantes")
    public ApiResponse<Object> crearEstudiante(@Valid @RequestBody EstudianteForm form) {
        estudiantes.guardar(form);
        return ApiResponse.ok(Map.of("id", form.idEstudiante()));
    }

    @GetMapping("/publicaciones")
    public ApiResponse<Object> publicaciones(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String proyecto,
                                             @RequestParam(required = false) Integer autor,
                                             @RequestParam(required = false) String tipo,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(publicaciones.listar(q, proyecto, autor, tipo, page, size),
                publicaciones.contar(q, proyecto, autor, tipo));
    }

    @GetMapping("/publicaciones/{codigo}")
    public ApiResponse<Object> publicacion(@PathVariable int codigo) {
        return ApiResponse.ok(publicaciones.detalle(codigo));
    }

    @PostMapping("/publicaciones")
    public ApiResponse<Object> crearPublicacion(@Valid @RequestBody PublicacionForm form) {
        publicaciones.registrar(form);
        return ApiResponse.ok(Map.of("tipo", form.tipo(), "titulo", form.titulo()));
    }

    @GetMapping("/dominios")
    public ApiResponse<Object> dominios(@RequestParam(required = false) String codigo) {
        return ApiResponse.ok(dominios.vistaDominios(codigo));
    }

    @GetMapping("/lineas")
    public ApiResponse<Object> lineas(@RequestParam(required = false) String dominio) {
        return ApiResponse.ok(dominios.lineas(dominio));
    }

    @GetMapping("/supervisiones")
    public ApiResponse<Object> supervisiones(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "true") boolean vigentes) {
        return ApiResponse.ok(supervisiones.listar(vigentes, q));
    }

    @PostMapping("/supervisiones")
    public ApiResponse<Object> crearSupervision(@Valid @RequestBody SupervisionForm form) {
        supervisiones.registrar(form);
        return ApiResponse.ok(form);
    }

    @GetMapping("/reportes")
    public ApiResponse<Object> reportes() {
        return ApiResponse.ok(reportes.catalogo());
    }

    /** El mismo procedimiento alimenta la tabla HTML y la descarga CSV. */
    @GetMapping(value = "/reportes/{codigo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Object> reporte(@PathVariable String codigo,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reportes.generar(codigo, new ReporteRequest(q, page, size, false)));
    }

    @GetMapping(value = "/reportes/{codigo}/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> reporteCsv(@PathVariable String codigo,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "100") int size) {
        String csv = reportes.generar(codigo, new ReporteRequest(q, page, size, true)).csv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-" + codigo + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/auditoria/cambios")
    public ApiResponse<Object> cambios(@RequestParam(required = false) String operacion,
                                       @RequestParam(required = false) String tabla,
                                       @RequestParam(required = false) String usuario,
                                       @RequestParam(required = false) String desde,
                                       @RequestParam(required = false) String hasta) {
        return ApiResponse.ok(auditoria.cambios(operacion, tabla, usuario, desde, hasta));
    }

    @GetMapping("/auditoria/operaciones")
    public ApiResponse<Object> operaciones(@RequestParam(required = false) String operacion,
                                           @RequestParam(required = false) String procedimiento,
                                           @RequestParam(required = false) String usuario,
                                           @RequestParam(required = false) String desde,
                                           @RequestParam(required = false) String hasta) {
        return ApiResponse.ok(auditoria.operaciones(operacion, procedimiento, usuario, desde, hasta));
    }

    @GetMapping("/usuarios")
    public ApiResponse<Object> usuarios() {
        return ApiResponse.ok(usuarios.listar());
    }

    @PostMapping("/usuarios")
    public ApiResponse<Object> crearUsuario(@Valid @RequestBody UsuarioForm form) {
        usuarios.guardar(form);
        return ApiResponse.ok(Map.of("username", form.username()));
    }

    @PostMapping("/usuarios/cambiar-clave")
    public ApiResponse<Object> cambiarClave(@RequestParam String actual,
                                            @RequestParam String nueva,
                                            @RequestParam String confirmacion) {
        if (!nueva.equals(confirmacion)) {
            throw new com.example.backend.exception.BusinessException("Las contraseñas nuevas no coinciden");
        }
        usuarios.cambiarContrasena(com.example.backend.security.SecurityUtils.usuarioActual(), actual, nueva);
        return ApiResponse.ok(Map.of("resultado", "Contraseña actualizada"));
    }
}
