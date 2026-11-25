package com.example.backend;

import com.example.backend.repository.ProcedureCaller;
import com.example.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integración real con MariaDB. Se ejecuta SOLO si se define DB_INTEGRATION_TESTS=true:
 *   DB_INTEGRATION_TESTS=true DB_USER=... DB_PASSWORD=... ./gradlew test
 * Nunca se afirma que la integración funcionó si no hubo conexión a la base de datos.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_INTEGRATION_TESTS", matches = "true")
class BaseDatosIntegracionTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProcedureCaller procedures;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Test
    void tablasDelModeloYDeSeguridadExisten() {
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE()
                   AND table_name IN ('proyecto_investigacion', 'investigador', 'estudiante',
                       'publicacion', 'auditoria_cambios', 'auditoria_operaciones',
                       'seg_usuario', 'seg_rol', 'seg_usuario_rol')
                """, Integer.class);
        assertEquals(9, cantidad, "Debe existir el modelo original y la migración de seguridad");
    }

    @Test
    void migracionCorrigeLosAutoIncrementNecesarios() {
        List<String> extras = jdbc.queryForList("""
                SELECT extra FROM information_schema.columns
                 WHERE table_schema = DATABASE()
                   AND ((table_name = 'publicacion' AND column_name = 'cod_publicacion')
                     OR (table_name = 'auditoria_operaciones' AND column_name = 'id_auditoria'))
                """, String.class);
        assertEquals(2, extras.size());
        assertTrue(extras.stream().allMatch("auto_increment"::equals));
    }

    @Test
    void usuarioAdministradorTieneAutoridadConPrefijoRole() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");
        assertTrue(admin.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
    }

    @Test
    void procedimientosDeReporteSePuedenInvocar() {
        List<String> nombres = jdbc.queryForList("""
                SELECT routine_name FROM information_schema.routines
                 WHERE routine_schema = DATABASE() AND routine_type = 'PROCEDURE'
                   AND (routine_name LIKE 'sp_av_%' OR routine_name LIKE 'sp_mt_%')
                """, String.class);
        assertTrue(nombres.size() >= 10, "Deben existir los diez procedimientos de reporte");
        assertNotNull(procedures.consultar("sp_av_05_presupuesto_por_dominio"));
    }
}
