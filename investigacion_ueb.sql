-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 25-11-2026 a las 17:05:57
-- Versión del servidor: 10.4.32-MariaDB-log
-- Versión de PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `investigacion_ueb`
--

DELIMITER $$
--
-- Procedimientos
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_asignar_puntos` (IN `p_cod` INT, IN `p_clave` VARCHAR(30))   BEGIN
  UPDATE publicacion
     SET puntos_asignados = COALESCE((SELECT puntos FROM parametro_puntaje
                                       WHERE clave = p_clave), 0)
   WHERE cod_publicacion = p_cod;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_av_01_estado_equipos_vigentes` ()   BEGIN
  SELECT p.cod_proyecto,
         p.acronimo,
         CONCAT(d.nombres, ' ', d.apellidos)                    AS director,
         COALESCE(x.investigadores, 0)                          AS investigadores_vigentes,
         4 - COALESCE(x.investigadores, 0)                      AS cupos_libres,
         DATEDIFF(CURDATE(), p.fecha_inicio)                    AS dias_en_ejecucion,
         CASE
           WHEN COALESCE(x.subrogantes, 0) = 0 THEN 'SIN SUBROGANTE'
           WHEN COALESCE(x.investigadores, 0) >= 4 THEN 'EQUIPO COMPLETO'
           ELSE 'CON CUPOS LIBRES'
         END                                                    AS estado_equipo
  FROM proyecto_investigacion p
  JOIN investigador d ON d.id_profesor = p.id_director
  LEFT JOIN (SELECT pp.cod_proyecto,
                    COUNT(DISTINCT pp.id_profesor)              AS investigadores,
                    SUM(CASE WHEN pp.rol = 'SUBROGANTE' THEN 1 ELSE 0 END) AS subrogantes
             FROM participa_proyecto pp
             WHERE pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE()
             GROUP BY pp.cod_proyecto) x ON x.cod_proyecto = p.cod_proyecto
  WHERE p.fecha_fin IS NULL OR p.fecha_fin > CURDATE()
  ORDER BY p.cod_proyecto;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_av_02_mejor_publicacion_proyecto` ()   BEGIN
  SELECT p.cod_proyecto,
         p.nombre_proyecto,
         pu.titulo,
         pu.tipo,
         pu.puntos_asignados
  FROM publicacion pu
  JOIN proyecto_investigacion p ON p.cod_proyecto = pu.cod_proyecto
  WHERE pu.puntos_asignados = (SELECT MAX(p2.puntos_asignados)
                               FROM publicacion p2
                               WHERE p2.cod_proyecto = pu.cod_proyecto)
  ORDER BY p.cod_proyecto, pu.titulo;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_av_03_ranking_cumplimiento` ()   BEGIN
  SELECT RANK() OVER (ORDER BY COALESCE(SUM(pu.puntos_asignados), 0) DESC) AS posicion,
         CONCAT(i.nombres, ' ', i.apellidos)                               AS investigador,
         i.tipo,
         COUNT(DISTINCT e.cod_publicacion)                                 AS publicaciones,
         COALESCE(SUM(pu.puntos_asignados), 0)                             AS puntaje_total,
         CASE
           WHEN COALESCE(SUM(pu.puntos_asignados), 0) >= 2 THEN 'DESTACADO'
           WHEN COALESCE(SUM(pu.puntos_asignados), 0) >= 1 THEN 'CUMPLE'
           WHEN COALESCE(SUM(pu.puntos_asignados), 0) >  0 THEN 'EN PROCESO'
           ELSE 'SIN PRODUCCION'
         END                                                               AS nivel
  FROM investigador i
  LEFT JOIN escribe e      ON e.id_profesor = i.id_profesor
  LEFT JOIN publicacion pu ON pu.cod_publicacion = e.cod_publicacion
  GROUP BY i.id_profesor, i.nombres, i.apellidos, i.tipo
  ORDER BY puntaje_total DESC, investigador;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_av_04_supervision_no_doctores` ()   BEGIN
  SELECT CONCAT(i.nombres, ' ', i.apellidos) AS no_doctor,
         (SELECT CONCAT(d.nombres, ' ', d.apellidos)
            FROM supervisa s
            JOIN investigador d ON d.id_profesor = s.id_doctor
           WHERE s.id_no_doctor = i.id_profesor
             AND (s.fecha_fin IS NULL OR s.fecha_fin > CURDATE())
           LIMIT 1)                           AS supervisor_vigente,
         (SELECT COUNT(*) FROM supervisa s
           WHERE s.id_no_doctor = i.id_profesor) AS supervisiones_historicas,
         (SELECT COALESCE(SUM(TIMESTAMPDIFF(MONTH, s.fecha_inicio,
                                            COALESCE(s.fecha_fin, CURDATE()))), 0)
            FROM supervisa s
           WHERE s.id_no_doctor = i.id_profesor) AS meses_supervisados,
         CASE WHEN EXISTS (SELECT 1 FROM supervisa s
                            WHERE s.id_no_doctor = i.id_profesor
                              AND (s.fecha_fin IS NULL OR s.fecha_fin > CURDATE()))
              THEN 'CON SUPERVISOR' ELSE 'SIN SUPERVISOR' END AS situacion
  FROM investigador i
  WHERE i.tipo = 'NO_DOCTOR'
  ORDER BY i.id_profesor;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_av_05_presupuesto_por_dominio` ()   BEGIN
  SELECT dc.cod_dominio,
         dc.nom_dominio,
         COUNT(x.cod_proyecto)                         AS proyectos_vigentes,
         COALESCE(SUM(x.presupuesto_total), 0)         AS presupuesto_vigente,
         ROUND(COALESCE(SUM(x.presupuesto_total), 0) * 100 /
               (SELECT SUM(presupuesto_total)
                  FROM proyecto_investigacion
                 WHERE fecha_fin IS NULL OR fecha_fin > CURDATE()), 1) AS porcentaje_del_total
  FROM dominio_conocimiento dc
  LEFT JOIN (SELECT DISTINCT l.cod_dominio, p.cod_proyecto, p.presupuesto_total
               FROM linea_investigacion l
               JOIN proyecto_linea pl ON pl.cod_linea = l.cod_linea
               JOIN proyecto_investigacion p ON p.cod_proyecto = pl.cod_proyecto
              WHERE p.fecha_fin IS NULL OR p.fecha_fin > CURDATE()) x
         ON x.cod_dominio = dc.cod_dominio
  GROUP BY dc.cod_dominio, dc.nom_dominio
  ORDER BY presupuesto_vigente DESC;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_mt_01_produccion_completa` ()   BEGIN
  SELECT CONCAT(a.nombres, ' ', a.apellidos)  AS autor,
         pu.titulo,
         pu.tipo,
         p.acronimo                           AS proyecto,
         CONCAT(d.nombres, ' ', d.apellidos)  AS director,
         l.nombre_linea                       AS linea,
         dc.nom_dominio                       AS dominio
  FROM escribe e
  INNER JOIN investigador a              ON a.id_profesor     = e.id_profesor
  INNER JOIN publicacion pu              ON pu.cod_publicacion = e.cod_publicacion
  INNER JOIN proyecto_investigacion p    ON p.cod_proyecto    = pu.cod_proyecto
  INNER JOIN investigador d              ON d.id_profesor     = p.id_director
  INNER JOIN linea_investigacion l       ON l.cod_linea       = e.cod_linea
  INNER JOIN dominio_conocimiento dc     ON dc.cod_dominio    = l.cod_dominio
  ORDER BY pu.cod_publicacion, autor;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_mt_02_actividad_investigadores` ()   BEGIN
  SELECT i.id_profesor,
         CONCAT(i.nombres, ' ', i.apellidos)     AS investigador,
         i.tipo,
         COUNT(DISTINCT t.cod_linea)             AS lineas,
         COUNT(DISTINCT e.cod_publicacion)       AS publicaciones,
         COALESCE(GROUP_CONCAT(DISTINCT pu.tipo), 'NINGUNA') AS tipos_publicados
  FROM investigador i
  LEFT JOIN trabaja_linea t ON t.id_profesor = i.id_profesor
  LEFT JOIN escribe e       ON e.id_profesor = i.id_profesor
  LEFT JOIN publicacion pu  ON pu.cod_publicacion = e.cod_publicacion
  GROUP BY i.id_profesor, i.nombres, i.apellidos, i.tipo
  ORDER BY publicaciones, i.id_profesor;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_mt_03_participacion_vigente` ()   BEGIN
  SELECT CONCAT(i.nombres, ' ', i.apellidos) AS investigador,
         i.tipo,
         COALESCE(p.acronimo, '(sin proyecto vigente)') AS proyecto,
         pp.rol,
         pp.fecha_inicio
  FROM participa_proyecto pp
  RIGHT JOIN investigador i
         ON i.id_profesor = pp.id_profesor
        AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE())
  LEFT JOIN proyecto_investigacion p ON p.cod_proyecto = pp.cod_proyecto
  ORDER BY i.id_profesor, proyecto;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_mt_04_supervisores_y_proyectos` ()   BEGIN
  SELECT CONCAT(d.nombres, ' ', d.apellidos) AS supervisor_doctor,
         CONCAT(n.nombres, ' ', n.apellidos) AS supervisado,
         s.fecha_inicio,
         s.fecha_fin,
         p.acronimo                          AS proyecto_vigente,
         pp.rol
  FROM supervisa s
  INNER JOIN investigador d ON d.id_profesor = s.id_doctor
  INNER JOIN investigador n ON n.id_profesor = s.id_no_doctor
  LEFT JOIN participa_proyecto pp
         ON pp.id_profesor = n.id_profesor
        AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE())
  LEFT JOIN proyecto_investigacion p ON p.cod_proyecto = pp.cod_proyecto
  WHERE s.fecha_fin IS NULL OR s.fecha_fin > CURDATE()
  ORDER BY supervisor_doctor, supervisado, proyecto_vigente;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_mt_05_dominio_linea_proyecto` ()   BEGIN
  SELECT dc.nom_dominio,
         l.nombre_linea,
         p.acronimo                          AS proyecto,
         CONCAT(d.nombres, ' ', d.apellidos) AS director,
         COUNT(DISTINCT pu.cod_publicacion)  AS publicaciones
  FROM dominio_conocimiento dc
  INNER JOIN linea_investigacion l    ON l.cod_dominio  = dc.cod_dominio
  INNER JOIN proyecto_linea pl        ON pl.cod_linea   = l.cod_linea
  INNER JOIN proyecto_investigacion p ON p.cod_proyecto = pl.cod_proyecto
  INNER JOIN investigador d           ON d.id_profesor  = p.id_director
  LEFT JOIN publicacion pu            ON pu.cod_proyecto = p.cod_proyecto
  GROUP BY dc.nom_dominio, l.nombre_linea, p.acronimo, d.nombres, d.apellidos
  ORDER BY dc.nom_dominio, l.nombre_linea;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_retirar_director` (IN `p_cod` VARCHAR(20))   BEGIN
  DECLARE v_sub INT DEFAULT NULL;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  SELECT pp.id_profesor INTO v_sub FROM participa_proyecto pp
   WHERE pp.cod_proyecto = p_cod AND pp.rol = 'SUBROGANTE'
     AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE()) LIMIT 1;
  IF v_sub IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El proyecto no tiene director subrogante vigente';
  END IF;
  -- el subrogante deja de ser investigador y pasa a ser director
  UPDATE participa_proyecto SET fecha_fin = CURDATE()
   WHERE cod_proyecto = p_cod AND id_profesor = v_sub AND rol = 'SUBROGANTE'
     AND (fecha_fin IS NULL OR fecha_fin > CURDATE());
  UPDATE proyecto_investigacion SET id_director = v_sub WHERE cod_proyecto = p_cod;
  COMMIT;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_tx_cambio_investigador` (IN `p_cod_proyecto` VARCHAR(20), IN `p_id_sale` INT, IN `p_id_entra` INT, IN `p_rol` VARCHAR(20))   BEGIN
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION 
    BEGIN
        ROLLBACK;
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('CAMBIO_PERSONAL', 'sp_tx_cambio_investigador', 'ERROR: No se pudo realizar el cambio de personal.');
        
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: No se pudo realizar el cambio de personal.';
    END;

    
    START TRANSACTION;

        
        UPDATE participa_proyecto 
        SET fecha_fin = CURDATE()
        WHERE cod_proyecto = p_cod_proyecto 
          AND id_profesor = p_id_sale 
          AND fecha_fin IS NULL;

        
        INSERT INTO participa_proyecto (cod_proyecto, id_profesor, fecha_inicio, rol)
        VALUES (p_cod_proyecto, p_id_entra, CURDATE(), p_rol);

        
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('CAMBIO_PERSONAL', 'sp_tx_cambio_investigador', CONCAT('EXITO: Docente ', p_id_sale, ' reemplazado por ', p_id_entra));

    COMMIT;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_tx_cierre_proyecto` (IN `p_cod_proyecto` VARCHAR(20))   BEGIN
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION 
    BEGIN
        ROLLBACK;
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('CIERRE_PROYECTO', 'sp_tx_cierre_proyecto', 'ERROR: No se pudo cerrar el proyecto.');
        
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error al ejecutar el cierre del proyecto.';
    END;

    
    START TRANSACTION;

        
        UPDATE proyecto_investigacion 
        SET fecha_fin = CURDATE()
        WHERE cod_proyecto = p_cod_proyecto;

        
        UPDATE participa_proyecto 
        SET fecha_fin = CURDATE()
        WHERE cod_proyecto = p_cod_proyecto AND fecha_fin IS NULL;

        
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('CIERRE_PROYECTO', 'sp_tx_cierre_proyecto', CONCAT('EXITO: Proyecto ', p_cod_proyecto, ' cerrado correctamente.'));

    COMMIT;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_tx_inicio_proyecto` (IN `p_cod_proyecto` VARCHAR(20), IN `p_nombre` VARCHAR(150), IN `p_acronimo` VARCHAR(20), IN `p_presupuesto` DECIMAL(12,2), IN `p_programa` VARCHAR(100), IN `p_descripcion` TEXT, IN `p_id_director` INT, IN `p_id_subrogante` INT, IN `p_cod_linea` VARCHAR(10))   BEGIN
    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION 
    BEGIN
        ROLLBACK;
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('CREACION_PROYECTO', 'sp_tx_inicio_proyecto', 'ERROR: Fallo en la creacion atomica del proyecto.');
        
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Fallo la creacion atomica del proyecto y su equipo.';
    END;

    
    START TRANSACTION;

        
        INSERT INTO proyecto_investigacion 
            (cod_proyecto, nombre_proyecto, acronimo, presupuesto_total, programa_idi, fecha_inicio, descripcion, id_director)
        VALUES 
            (p_cod_proyecto, p_nombre, p_acronimo, p_presupuesto, p_programa, CURDATE(), p_descripcion, p_id_director);

        
        INSERT INTO participa_proyecto (cod_proyecto, id_profesor, fecha_inicio, rol)
        VALUES (p_cod_proyecto, p_id_subrogante, CURDATE(), 'SUBROGANTE');

        
        INSERT INTO proyecto_linea (cod_proyecto, cod_linea)
        VALUES (p_cod_proyecto, p_cod_linea);

        
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('CREACION_PROYECTO', 'sp_tx_inicio_proyecto', CONCAT('EXITO: Proyecto ', p_cod_proyecto, ' creado correctamente.'));

    COMMIT;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_tx_registrar_revista` (IN `p_cod_proyecto` VARCHAR(20), IN `p_titulo` VARCHAR(200), IN `p_nombre_revista` VARCHAR(150), IN `p_editorial` VARCHAR(100), IN `p_categoria` ENUM('ALTO_IMPACTO','REGIONAL'), IN `p_cuartil` VARCHAR(2), IN `p_id_autor` INT, IN `p_cod_linea` VARCHAR(10))   BEGIN
    DECLARE v_cod_pub INT;

    
    DECLARE EXIT HANDLER FOR SQLEXCEPTION 
    BEGIN
        ROLLBACK;
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('REGISTRO_PRODUCTO', 'sp_tx_registrar_revista', 'ERROR: Fallo en el registro de la publicacion.');
        
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Fallo el registro completo de la publicacion.';
    END;

    
    START TRANSACTION;

        
        INSERT INTO publicacion (cod_proyecto, titulo, nombre, tipo)
        VALUES (p_cod_proyecto, p_titulo, p_nombre_revista, 'REVISTA');

        SET v_cod_pub = LAST_INSERT_ID();

        
        INSERT INTO revista (cod_publicacion, editorial, categoria, cuartil)
        VALUES (v_cod_pub, p_editorial, p_categoria, p_cuartil);

        
        INSERT INTO escribe (id_profesor, cod_publicacion, cod_linea)
        VALUES (p_id_autor, v_cod_pub, p_cod_linea);

        
        INSERT INTO auditoria_operaciones (operacion, procedimiento_ejecutado, resultado)
        VALUES ('REGISTRO_PRODUCTO', 'sp_tx_registrar_revista', CONCAT('EXITO: Publicacion ID ', v_cod_pub, ' registrada.'));

    COMMIT;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_validar_director` (IN `p_cod` VARCHAR(20), IN `p_director` INT, IN `p_fin` DATE)   BEGIN
  DECLARE v_cont INT DEFAULT 0;
  IF p_fin IS NULL OR p_fin > CURDATE() THEN
    -- el director no puede ser ademas investigador del mismo proyecto
    SELECT COUNT(*) INTO v_cont FROM participa_proyecto pp
     WHERE pp.cod_proyecto = p_cod AND pp.id_profesor = p_director
       AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE());
    IF v_cont > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
        'El director no puede figurar ademas como investigador del mismo proyecto';
    END IF;
    -- maximo 2 proyectos vigentes por profesor (contando este)
    SELECT COUNT(*) INTO v_cont FROM (
      SELECT pp.cod_proyecto FROM participa_proyecto pp
       WHERE pp.id_profesor = p_director AND pp.cod_proyecto <> p_cod
         AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE())
      UNION
      SELECT pr.cod_proyecto FROM proyecto_investigacion pr
       WHERE pr.id_director = p_director AND pr.cod_proyecto <> p_cod
         AND (pr.fecha_fin IS NULL OR pr.fecha_fin > CURDATE())
    ) otros;
    IF v_cont >= 2 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
        'Un profesor solo puede trabajar en maximo 2 proyectos vigentes';
    END IF;
  END IF;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_validar_participacion` (IN `p_cod` VARCHAR(20), IN `p_prof` INT, IN `p_fin` DATE, IN `p_rol` VARCHAR(20), IN `p_old_prof` INT, IN `p_old_inicio` DATE)   BEGIN
  DECLARE v_dir  INT DEFAULT NULL;
  DECLARE v_cont INT DEFAULT 0;
  IF p_fin IS NULL OR p_fin > CURDATE() THEN
    -- el director no puede ser investigador de su propio proyecto
    SELECT id_director INTO v_dir FROM proyecto_investigacion WHERE cod_proyecto = p_cod;
    IF v_dir = p_prof THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
        'El director no puede figurar ademas como investigador del mismo proyecto';
    END IF;
    -- maximo 4 investigadores vigentes por proyecto
    SELECT COUNT(DISTINCT pp.id_profesor) INTO v_cont
      FROM participa_proyecto pp
     WHERE pp.cod_proyecto = p_cod AND pp.id_profesor <> p_prof
       AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE());
    IF v_cont >= 4 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
        'Un proyecto admite como maximo 4 investigadores vigentes';
    END IF;
    -- un solo director subrogante vigente por proyecto
    IF p_rol = 'SUBROGANTE' THEN
      SELECT COUNT(*) INTO v_cont FROM participa_proyecto pp
       WHERE pp.cod_proyecto = p_cod AND pp.rol = 'SUBROGANTE'
         AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE())
         AND NOT (pp.id_profesor <=> p_old_prof AND pp.fecha_inicio <=> p_old_inicio);
      IF v_cont > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
          'El proyecto ya tiene un director subrogante vigente';
      END IF;
    END IF;
    -- maximo 2 proyectos vigentes por profesor (contando este)
    SELECT COUNT(*) INTO v_cont FROM (
      SELECT pp.cod_proyecto FROM participa_proyecto pp
       WHERE pp.id_profesor = p_prof AND pp.cod_proyecto <> p_cod
         AND (pp.fecha_fin IS NULL OR pp.fecha_fin > CURDATE())
      UNION
      SELECT pr.cod_proyecto FROM proyecto_investigacion pr
       WHERE pr.id_director = p_prof AND pr.cod_proyecto <> p_cod
         AND (pr.fecha_fin IS NULL OR pr.fecha_fin > CURDATE())
    ) otros;
    IF v_cont >= 2 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
        'Un profesor solo puede trabajar en maximo 2 proyectos vigentes';
    END IF;
  END IF;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_validar_supervision` (IN `p_id_no_doctor` INT, IN `p_inicio` DATE, IN `p_fin` DATE, IN `p_old_doctor` INT, IN `p_old_inicio` DATE)   BEGIN
  DECLARE v_cont INT DEFAULT 0;
  SELECT COUNT(*) INTO v_cont
    FROM supervisa s
   WHERE s.id_no_doctor = p_id_no_doctor
     AND NOT (s.id_doctor <=> p_old_doctor AND s.fecha_inicio <=> p_old_inicio)
     AND s.fecha_inicio <= COALESCE(p_fin, DATE('9999-12-31'))
     AND COALESCE(s.fecha_fin, DATE('9999-12-31')) >= p_inicio;
  IF v_cont > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
      'Un profesor no doctor solo puede tener un supervisor doctor en un mismo periodo';
  END IF;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_validar_tipo_publicacion` (IN `p_cod` INT, IN `p_tipo` VARCHAR(20))   BEGIN
  DECLARE v_tipo VARCHAR(20) DEFAULT NULL;
  SELECT tipo INTO v_tipo FROM publicacion WHERE cod_publicacion = p_cod;
  IF v_tipo IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La publicacion indicada no existe';
  ELSEIF v_tipo <> p_tipo THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
      'El tipo de la publicacion no coincide con la tabla donde se registra';
  END IF;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `auditoria_cambios`
--

CREATE TABLE `auditoria_cambios` (
  `id_auditoria` bigint(20) NOT NULL,
  `tabla` varchar(40) NOT NULL,
  `operacion` enum('INSERT','UPDATE','DELETE') NOT NULL,
  `clave_registro` varchar(100) NOT NULL,
  `valores_anteriores` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`valores_anteriores`)),
  `valores_nuevos` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`valores_nuevos`)),
  `usuario` varchar(100) NOT NULL,
  `fecha_hora` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `auditoria_cambios`
--

INSERT INTO `auditoria_cambios` (`id_auditoria`, `tabla`, `operacion`, `clave_registro`, `valores_anteriores`, `valores_nuevos`, `usuario`, `fecha_hora`) VALUES
(1, 'proyecto_investigacion', 'UPDATE', 'PRY-2025-001', '{\"cod_proyecto\": \"PRY-2025-001\", \"nombre_proyecto\": \"Sistema de gestión de investigación\", \"acronimo\": \"SGI\", \"presupuesto_total\": 25000.00, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"id_director\": 1}', '{\"cod_proyecto\": \"PRY-2025-001\", \"nombre_proyecto\": \"Sistema de gestión de investigación\", \"acronimo\": \"SGI\", \"presupuesto_total\": 26000.00, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"id_director\": 1}', 'root@localhost', '2026-11-06 21:58:42'),
(2, 'publicacion', 'UPDATE', '1', '{\"cod_publicacion\": 1, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 1, \"titulo\": \"Modelo relacional para la gestión de investigación\", \"nombre\": \"Revista Latinoamericana de Informática\", \"tipo\": \"REVISTA\", \"puntos_asignados\": 1.00}', '{\"cod_publicacion\": 1, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 1, \"titulo\": \"Modelo relacional para la gestión de investigación\", \"nombre\": \"Revista Latinoamericana de Informática\", \"tipo\": \"REVISTA\", \"puntos_asignados\": 1.00}', 'secretaria2@localhost', '2026-11-19 07:24:19'),
(3, 'publicacion', 'UPDATE', '1', '{\"cod_publicacion\": 1, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 1, \"titulo\": \"Modelo relacional para la gestión de investigación\", \"nombre\": \"Revista Latinoamericana de Informática\", \"tipo\": \"REVISTA\", \"puntos_asignados\": 1.00}', '{\"cod_publicacion\": 1, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 1, \"titulo\": \"Modelo relacional para la gestión de investigación\", \"nombre\": \"Revista Latinoamericana de Informática\", \"tipo\": \"REVISTA\", \"puntos_asignados\": 1.00}', 'secretaria2@localhost', '2026-11-19 07:24:19'),
(4, 'publicacion', 'UPDATE', '2', '{\"cod_publicacion\": 2, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 2, \"titulo\": \"Arquitectura de microservicios universitarios\", \"nombre\": \"Congreso Internacional de Software\", \"tipo\": \"CONGRESO\", \"puntos_asignados\": 0.20}', '{\"cod_publicacion\": 2, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 2, \"titulo\": \"Arquitectura de microservicios universitarios\", \"nombre\": \"Congreso Internacional de Software\", \"tipo\": \"CONGRESO\", \"puntos_asignados\": 0.00}', 'root@localhost', '2026-11-19 07:29:30'),
(5, 'publicacion', 'UPDATE', '2', '{\"cod_publicacion\": 2, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 2, \"titulo\": \"Arquitectura de microservicios universitarios\", \"nombre\": \"Congreso Internacional de Software\", \"tipo\": \"CONGRESO\", \"puntos_asignados\": 0.20}', '{\"cod_publicacion\": 2, \"cod_proyecto\": \"PRY-2025-001\", \"nro_secuencia\": 2, \"titulo\": \"Arquitectura de microservicios universitarios\", \"nombre\": \"Congreso Internacional de Software\", \"tipo\": \"CONGRESO\", \"puntos_asignados\": 0.00}', 'root@localhost', '2026-11-19 07:29:30'),
(6, 'participa_proyecto', 'UPDATE', 'PRY-2025-001|7|2025-02-01', '{\"cod_proyecto\": \"PRY-2025-001\", \"id_profesor\": 7, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"rol\": \"INVESTIGADOR\"}', '{\"cod_proyecto\": \"PRY-2025-001\", \"id_profesor\": 7, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": \"2026-11-19\", \"rol\": \"INVESTIGADOR\"}', 'root@localhost', '2026-11-19 07:33:06'),
(7, 'participa_proyecto', 'UPDATE', 'PRY-2025-001|7|2025-02-01', '{\"cod_proyecto\": \"PRY-2025-001\", \"id_profesor\": 7, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"rol\": \"INVESTIGADOR\"}', '{\"cod_proyecto\": \"PRY-2025-001\", \"id_profesor\": 7, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": \"2026-11-19\", \"rol\": \"INVESTIGADOR\"}', 'root@localhost', '2026-11-19 07:33:06'),
(8, 'participa_proyecto', 'INSERT', 'PRY-2025-001|6|2026-11-19', NULL, '{\"cod_proyecto\": \"PRY-2025-001\", \"id_profesor\": 6, \"fecha_inicio\": \"2026-11-19\", \"fecha_fin\": null, \"rol\": \"INVESTIGADOR\"}', 'root@localhost', '2026-11-19 07:33:06'),
(9, 'participa_proyecto', 'INSERT', 'PRY-2025-001|6|2026-11-19', NULL, '{\"cod_proyecto\": \"PRY-2025-001\", \"id_profesor\": 6, \"fecha_inicio\": \"2026-11-19\", \"fecha_fin\": null, \"rol\": \"INVESTIGADOR\"}', 'root@localhost', '2026-11-19 07:33:06'),
(10, 'proyecto_investigacion', 'UPDATE', 'PRY-2025-001', '{\"cod_proyecto\": \"PRY-2025-001\", \"nombre_proyecto\": \"Sistema de gestión de investigación\", \"acronimo\": \"SGI\", \"presupuesto_total\": 26000.00, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"id_director\": 1}', '{\"cod_proyecto\": \"PRY-2025-001\", \"nombre_proyecto\": \"Sistema de gestión de investigación - PRUEBA\", \"acronimo\": \"SGI\", \"presupuesto_total\": 26000.00, \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"id_director\": 1}', 'root@localhost', '2026-11-23 21:49:54'),
(11, 'proyecto_investigacion', 'UPDATE', 'PRY-2025-001', '{\"cod_proyecto\": \"PRY-2025-001\", \"nombre_proyecto\": \"Sistema de gestión de investigación\", \"acronimo\": \"SGI\", \"presupuesto_total\": 26000.00, \"programa_idi\": \"Programa de Investigación Institucional\", \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"descripcion\": \"Sistema para gestionar proyectos y publicaciones\", \"id_director\": 1}', '{\"cod_proyecto\": \"PRY-2025-001\", \"nombre_proyecto\": \"Sistema de gestión de investigación - PRUEBA\", \"acronimo\": \"SGI\", \"presupuesto_total\": 26000.00, \"programa_idi\": \"Programa de Investigación Institucional\", \"fecha_inicio\": \"2025-02-01\", \"fecha_fin\": null, \"descripcion\": \"Sistema para gestionar proyectos y publicaciones\", \"id_director\": 1}', 'root@localhost', '2026-11-23 21:49:54');

--
-- Disparadores `auditoria_cambios`
--
DELIMITER $$
CREATE TRIGGER `trg_auditoria_cambios_bd` BEFORE DELETE ON `auditoria_cambios` FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoria no se puede borrar'
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_auditoria_cambios_bu` BEFORE UPDATE ON `auditoria_cambios` FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La auditoria no se puede modificar'
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `auditoria_operaciones`
--

CREATE TABLE `auditoria_operaciones` (
  `id_auditoria` int(11) NOT NULL,
  `fecha_registro` datetime DEFAULT current_timestamp(),
  `operacion` varchar(100) NOT NULL,
  `procedimiento_ejecutado` varchar(100) NOT NULL,
  `resultado` varchar(255) NOT NULL,
  `usuario` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `auditoria_operaciones`
--

INSERT INTO `auditoria_operaciones` (`id_auditoria`, `fecha_registro`, `operacion`, `procedimiento_ejecutado`, `resultado`, `usuario`) VALUES
(4, '2026-11-19 07:32:02', 'CAMBIO_PERSONAL', 'sp_tx_cambio_investigador', 'ERROR: No se pudo realizar el cambio de personal.', NULL),
(5, '2026-11-19 07:32:03', 'CAMBIO_PERSONAL', 'sp_tx_cambio_investigador', 'ERROR: No se pudo realizar el cambio de personal.', NULL),
(6, '2026-11-19 07:32:03', 'CAMBIO_PERSONAL', 'sp_tx_cambio_investigador', 'ERROR: No se pudo realizar el cambio de personal.', NULL),
(7, '2026-11-19 07:33:06', 'CAMBIO_PERSONAL', 'sp_tx_cambio_investigador', 'EXITO: Docente 7 reemplazado por 6', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `congreso`
--

CREATE TABLE `congreso` (
  `cod_publicacion` int(11) NOT NULL,
  `tipo_congreso` varchar(30) DEFAULT NULL,
  `lugar` varchar(100) DEFAULT NULL,
  `pais` varchar(60) DEFAULT NULL,
  `fecha_inicio` date DEFAULT NULL,
  `fecha_fin` date DEFAULT NULL,
  `editorial_actas` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `congreso`
--

INSERT INTO `congreso` (`cod_publicacion`, `tipo_congreso`, `lugar`, `pais`, `fecha_inicio`, `fecha_fin`, `editorial_actas`) VALUES
(2, 'INTERNACIONAL', 'Quito', 'Ecuador', '2025-09-10', '2025-09-12', 'Actas del Congreso de Software');

--
-- Disparadores `congreso`
--
DELIMITER $$
CREATE TRIGGER `trg_congreso_ai` AFTER INSERT ON `congreso` FOR EACH ROW BEGIN
  CALL sp_asignar_puntos(NEW.cod_publicacion, 'CONGRESO');
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_congreso_bi` BEFORE INSERT ON `congreso` FOR EACH ROW BEGIN
  CALL sp_validar_tipo_publicacion(NEW.cod_publicacion, 'CONGRESO');
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cuentas_estudiantes`
--

CREATE TABLE `cuentas_estudiantes` (
  `usuario` varchar(80) NOT NULL,
  `id_estudiante` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `cuentas_estudiantes`
--

INSERT INTO `cuentas_estudiantes` (`usuario`, `id_estudiante`) VALUES
('pedro.gomez', 1),
('lucia.perez', 2),
('andres.lopez', 3);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cuentas_profesores`
--

CREATE TABLE `cuentas_profesores` (
  `usuario` varchar(80) NOT NULL,
  `id_profesor` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `cuentas_profesores`
--

INSERT INTO `cuentas_profesores` (`usuario`, `id_profesor`) VALUES
('carlos.mendoza', 1),
('maria.vargas', 2),
('luis.paredes', 3),
('ana.torres', 4),
('jorge.cevallos', 5),
('sofia.naranjo', 6),
('diego.montero', 7);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `doctor`
--

CREATE TABLE `doctor` (
  `id_profesor` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `doctor`
--

INSERT INTO `doctor` (`id_profesor`) VALUES
(1),
(2),
(6);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `dominio_conocimiento`
--

CREATE TABLE `dominio_conocimiento` (
  `cod_dominio` varchar(10) NOT NULL,
  `nom_dominio` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `dominio_conocimiento`
--

INSERT INTO `dominio_conocimiento` (`cod_dominio`, `nom_dominio`) VALUES
('DOM01', 'Prueba2'),
('DOM02', 'Prueba2'),
('DOM03', 'Prueba2');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `escribe`
--

CREATE TABLE `escribe` (
  `id_profesor` int(11) NOT NULL,
  `cod_publicacion` int(11) NOT NULL,
  `cod_linea` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `escribe`
--

INSERT INTO `escribe` (`id_profesor`, `cod_publicacion`, `cod_linea`) VALUES
(3, 1, 'LIN01'),
(3, 2, 'LIN01'),
(3, 5, 'LIN01'),
(4, 3, 'LIN03'),
(5, 3, 'LIN03'),
(6, 4, 'LIN04'),
(7, 1, 'LIN01'),
(7, 5, 'LIN01');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `estudiante`
--

CREATE TABLE `estudiante` (
  `id_estudiante` int(11) NOT NULL,
  `cedula` varchar(10) NOT NULL,
  `nombres` varchar(60) NOT NULL,
  `apellidos` varchar(60) NOT NULL,
  `carrera` varchar(60) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `estudiante`
--

INSERT INTO `estudiante` (`id_estudiante`, `cedula`, `nombres`, `apellidos`, `carrera`) VALUES
(1, '0201234567', 'Pedro', 'Gómez', 'Sistemas'),
(2, '0207654321', 'Lucía', 'Pérez', 'Software'),
(3, '0209876543', 'Andrés', 'López', 'Sistemas');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `investigador`
--

CREATE TABLE `investigador` (
  `id_profesor` int(11) NOT NULL,
  `nombres` varchar(60) NOT NULL,
  `apellidos` varchar(60) NOT NULL,
  `despacho` varchar(20) DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `tipo` enum('DOCTOR','NO_DOCTOR') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `investigador`
--

INSERT INTO `investigador` (`id_profesor`, `nombres`, `apellidos`, `despacho`, `telefono`, `tipo`) VALUES
(1, 'Carlos', 'Mendoza', 'D-101', '0991000001', 'DOCTOR'),
(2, 'María', 'Vargas', 'D-102', '0991000002', 'DOCTOR'),
(3, 'Luis', 'Paredes', 'D-201', '0991000003', 'NO_DOCTOR'),
(4, 'Ana', 'Torres', 'D-202', '0991000004', 'NO_DOCTOR'),
(5, 'Jorge', 'Cevallos', 'D-203', '0991000005', 'NO_DOCTOR'),
(6, 'Sofía', 'Naranjo', 'D-103', '0991000006', 'DOCTOR'),
(7, 'Diego', 'Montero', 'D-204', '0991000007', 'NO_DOCTOR');

--
-- Disparadores `investigador`
--
DELIMITER $$
CREATE TRIGGER `trg_investigador_ai` AFTER INSERT ON `investigador` FOR EACH ROW BEGIN
  IF NEW.tipo = 'DOCTOR' THEN
    INSERT INTO doctor (id_profesor) VALUES (NEW.id_profesor);
  ELSE
    INSERT INTO no_doctor (id_profesor) VALUES (NEW.id_profesor);
  END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_investigador_bu` BEFORE UPDATE ON `investigador` FOR EACH ROW BEGIN
  IF NEW.tipo <> OLD.tipo THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
      'No se puede cambiar el tipo (DOCTOR / NO_DOCTOR) de un investigador';
  END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `libro`
--

CREATE TABLE `libro` (
  `cod_publicacion` int(11) NOT NULL,
  `editorial` varchar(100) DEFAULT NULL,
  `anio_publicacion` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `libro`
--

INSERT INTO `libro` (`cod_publicacion`, `editorial`, `anio_publicacion`) VALUES
(4, 'Editorial UEB', 2024);

--
-- Disparadores `libro`
--
DELIMITER $$
CREATE TRIGGER `trg_libro_ai` AFTER INSERT ON `libro` FOR EACH ROW BEGIN
  CALL sp_asignar_puntos(NEW.cod_publicacion, 'LIBRO');
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_libro_bi` BEFORE INSERT ON `libro` FOR EACH ROW BEGIN
  CALL sp_validar_tipo_publicacion(NEW.cod_publicacion, 'LIBRO');
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `linea_investigacion`
--

CREATE TABLE `linea_investigacion` (
  `cod_linea` varchar(10) NOT NULL,
  `nombre_linea` varchar(120) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `cod_dominio` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `linea_investigacion`
--

INSERT INTO `linea_investigacion` (`cod_linea`, `nombre_linea`, `descripcion`, `cod_dominio`) VALUES
('LIN01', 'Bases de datos y sistemas de información', 'Modelado, gestión y consulta de datos', 'DOM01'),
('LIN02', 'Inteligencia artificial aplicada', 'Aprendizaje automático aplicado', 'DOM01'),
('LIN03', 'Producción agroecológica', 'Prácticas agroecológicas andinas', 'DOM02'),
('LIN04', 'Tecnologías educativas', 'Recursos digitales para la educación', 'DOM03');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `no_doctor`
--

CREATE TABLE `no_doctor` (
  `id_profesor` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `no_doctor`
--

INSERT INTO `no_doctor` (`id_profesor`) VALUES
(3),
(4),
(5),
(7);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `parametro_puntaje`
--

CREATE TABLE `parametro_puntaje` (
  `clave` varchar(30) NOT NULL,
  `descripcion` varchar(100) NOT NULL,
  `puntos` decimal(4,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `parametro_puntaje`
--

INSERT INTO `parametro_puntaje` (`clave`, `descripcion`, `puntos`) VALUES
('CONGRESO', 'Publicación en congreso', 0.20),
('LIBRO', 'Libro', 1.00),
('PATENTE', 'Patente', 1.00),
('REVISTA_Q1', 'Revista de alto impacto, cuartil Q1', 1.00),
('REVISTA_Q2', 'Revista de alto impacto, cuartil Q2', 0.80),
('REVISTA_Q3', 'Revista de alto impacto, cuartil Q3', 0.60),
('REVISTA_Q4', 'Revista de alto impacto, cuartil Q4', 0.40),
('REVISTA_Q5', 'Revista de alto impacto, cuartil Q5', 0.20),
('REVISTA_REGIONAL', 'Revista regional', 0.20);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `participante_estudiante`
--

CREATE TABLE `participante_estudiante` (
  `cod_proyecto` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `id_estudiante` int(11) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `participante_estudiante`
--

INSERT INTO `participante_estudiante` (`cod_proyecto`, `id_estudiante`, `fecha_inicio`, `fecha_fin`) VALUES
('PRY-2025-001', 1, '2026-11-23', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `participa_proyecto`
--

CREATE TABLE `participa_proyecto` (
  `cod_proyecto` varchar(20) NOT NULL,
  `id_profesor` int(11) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date DEFAULT NULL,
  `rol` enum('INVESTIGADOR','SUBROGANTE') NOT NULL DEFAULT 'INVESTIGADOR'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `participa_proyecto`
--

INSERT INTO `participa_proyecto` (`cod_proyecto`, `id_profesor`, `fecha_inicio`, `fecha_fin`, `rol`) VALUES
('PRY-2025-001', 3, '2025-02-01', NULL, 'INVESTIGADOR'),
('PRY-2025-001', 4, '2025-02-01', NULL, 'SUBROGANTE'),
('PRY-2025-001', 6, '2026-11-19', NULL, 'INVESTIGADOR'),
('PRY-2025-001', 7, '2025-02-01', '2026-11-19', 'INVESTIGADOR'),
('PRY-2025-002', 3, '2025-06-01', NULL, 'INVESTIGADOR'),
('PRY-2025-002', 5, '2025-03-01', NULL, 'INVESTIGADOR'),
('PRY-2025-002', 6, '2025-03-01', NULL, 'SUBROGANTE'),
('PRY-2026-004', 4, '2026-01-15', NULL, 'INVESTIGADOR'),
('PRY-2026-004', 7, '2026-01-15', NULL, 'SUBROGANTE');

--
-- Disparadores `participa_proyecto`
--
DELIMITER $$
CREATE TRIGGER `trg_aud_participa_ad` AFTER DELETE ON `participa_proyecto` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('participa_proyecto', 'DELETE', CONCAT_WS('|', OLD.cod_proyecto, OLD.id_profesor, OLD.fecha_inicio),
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto,
                 'id_profesor', OLD.id_profesor,
                 'fecha_inicio', OLD.fecha_inicio,
                 'fecha_fin', OLD.fecha_fin,
                 'rol', OLD.rol),
          NULL,
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_participa_ai` AFTER INSERT ON `participa_proyecto` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('participa_proyecto', 'INSERT', CONCAT_WS('|', NEW.cod_proyecto, NEW.id_profesor, NEW.fecha_inicio), NULL,
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto,
                 'id_profesor', NEW.id_profesor,
                 'fecha_inicio', NEW.fecha_inicio,
                 'fecha_fin', NEW.fecha_fin,
                 'rol', NEW.rol),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_participa_au` AFTER UPDATE ON `participa_proyecto` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('participa_proyecto', 'UPDATE', CONCAT_WS('|', OLD.cod_proyecto, OLD.id_profesor, OLD.fecha_inicio),
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto,
                 'id_profesor', OLD.id_profesor,
                 'fecha_inicio', OLD.fecha_inicio,
                 'fecha_fin', OLD.fecha_fin,
                 'rol', OLD.rol),
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto,
                 'id_profesor', NEW.id_profesor,
                 'fecha_inicio', NEW.fecha_inicio,
                 'fecha_fin', NEW.fecha_fin,
                 'rol', NEW.rol),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_participa_proyecto_d` AFTER DELETE ON `participa_proyecto` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('participa_proyecto', 'DELETE', CONCAT(OLD.cod_proyecto,'|',OLD.id_profesor,'|',OLD.fecha_inicio),
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto, 'id_profesor', OLD.id_profesor, 'fecha_inicio', OLD.fecha_inicio, 'fecha_fin', OLD.fecha_fin, 'rol', OLD.rol),
          NULL,
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_participa_proyecto_i` AFTER INSERT ON `participa_proyecto` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('participa_proyecto', 'INSERT', CONCAT(NEW.cod_proyecto,'|',NEW.id_profesor,'|',NEW.fecha_inicio),
          NULL,
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto, 'id_profesor', NEW.id_profesor, 'fecha_inicio', NEW.fecha_inicio, 'fecha_fin', NEW.fecha_fin, 'rol', NEW.rol),
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_participa_proyecto_u` AFTER UPDATE ON `participa_proyecto` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('participa_proyecto', 'UPDATE', CONCAT(NEW.cod_proyecto,'|',NEW.id_profesor,'|',NEW.fecha_inicio),
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto, 'id_profesor', OLD.id_profesor, 'fecha_inicio', OLD.fecha_inicio, 'fecha_fin', OLD.fecha_fin, 'rol', OLD.rol),
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto, 'id_profesor', NEW.id_profesor, 'fecha_inicio', NEW.fecha_inicio, 'fecha_fin', NEW.fecha_fin, 'rol', NEW.rol),
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_participa_bi` BEFORE INSERT ON `participa_proyecto` FOR EACH ROW BEGIN
  CALL sp_validar_participacion(NEW.cod_proyecto, NEW.id_profesor, NEW.fecha_fin,
                                NEW.rol, NULL, NULL);
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_participa_bu` BEFORE UPDATE ON `participa_proyecto` FOR EACH ROW BEGIN
  CALL sp_validar_participacion(NEW.cod_proyecto, NEW.id_profesor, NEW.fecha_fin,
                                NEW.rol, OLD.id_profesor, OLD.fecha_inicio);
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `patente`
--

CREATE TABLE `patente` (
  `cod_publicacion` int(11) NOT NULL,
  `num_patente` varchar(30) DEFAULT NULL,
  `fecha_concesion` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `patente`
--

INSERT INTO `patente` (`cod_publicacion`, `num_patente`, `fecha_concesion`) VALUES
(5, 'IEPI-2026-0142', '2026-03-20');

--
-- Disparadores `patente`
--
DELIMITER $$
CREATE TRIGGER `trg_patente_ai` AFTER INSERT ON `patente` FOR EACH ROW BEGIN
  CALL sp_asignar_puntos(NEW.cod_publicacion, 'PATENTE');
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_patente_bi` BEFORE INSERT ON `patente` FOR EACH ROW BEGIN
  CALL sp_validar_tipo_publicacion(NEW.cod_publicacion, 'PATENTE');
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `proyecto_investigacion`
--

CREATE TABLE `proyecto_investigacion` (
  `cod_proyecto` varchar(20) NOT NULL,
  `nombre_proyecto` varchar(150) NOT NULL,
  `acronimo` varchar(20) NOT NULL,
  `presupuesto_total` decimal(12,2) NOT NULL DEFAULT 0.00,
  `programa_idi` varchar(100) DEFAULT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `id_director` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `proyecto_investigacion`
--

INSERT INTO `proyecto_investigacion` (`cod_proyecto`, `nombre_proyecto`, `acronimo`, `presupuesto_total`, `programa_idi`, `fecha_inicio`, `fecha_fin`, `descripcion`, `id_director`) VALUES
('PRY-2023-003', 'Recursos digitales para el aula rural', 'REDAR', 8000.00, 'Programa de Innovación Educativa', '2023-03-01', '2025-06-30', 'Recursos digitales para docentes de zonas rurales', 6),
('PRY-2025-001', 'Sistema de gestión de investigación - PRUEBA', 'SGI', 26000.00, 'Programa de Investigación Institucional', '2025-02-01', NULL, 'Sistema para gestionar proyectos y publicaciones', 1),
('PRY-2025-002', 'Prácticas agroecológicas andinas', 'PAA', 18000.00, 'Programa de Vinculación con la Sociedad', '2025-03-01', NULL, 'Prácticas agroecológicas en la provincia Bolívar', 2),
('PRY-2026-004', 'Modelos de datos para análisis', 'MDA', 15000.00, 'Programa de Investigación Institucional', '2026-01-15', NULL, 'Modelos de datos y analítica para la gestión universitaria', 1);

--
-- Disparadores `proyecto_investigacion`
--
DELIMITER $$
CREATE TRIGGER `trg_aud_proyecto_ad` AFTER DELETE ON `proyecto_investigacion` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('proyecto_investigacion', 'DELETE', OLD.cod_proyecto,
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto,
                 'nombre_proyecto', OLD.nombre_proyecto,
                 'acronimo', OLD.acronimo,
                 'presupuesto_total', OLD.presupuesto_total,
                 'fecha_inicio', OLD.fecha_inicio,
                 'fecha_fin', OLD.fecha_fin,
                 'id_director', OLD.id_director),
          NULL,
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_proyecto_ai` AFTER INSERT ON `proyecto_investigacion` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('proyecto_investigacion', 'INSERT', NEW.cod_proyecto, NULL,
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto,
                 'nombre_proyecto', NEW.nombre_proyecto,
                 'acronimo', NEW.acronimo,
                 'presupuesto_total', NEW.presupuesto_total,
                 'fecha_inicio', NEW.fecha_inicio,
                 'fecha_fin', NEW.fecha_fin,
                 'id_director', NEW.id_director),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_proyecto_au` AFTER UPDATE ON `proyecto_investigacion` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('proyecto_investigacion', 'UPDATE', OLD.cod_proyecto,
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto,
                 'nombre_proyecto', OLD.nombre_proyecto,
                 'acronimo', OLD.acronimo,
                 'presupuesto_total', OLD.presupuesto_total,
                 'fecha_inicio', OLD.fecha_inicio,
                 'fecha_fin', OLD.fecha_fin,
                 'id_director', OLD.id_director),
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto,
                 'nombre_proyecto', NEW.nombre_proyecto,
                 'acronimo', NEW.acronimo,
                 'presupuesto_total', NEW.presupuesto_total,
                 'fecha_inicio', NEW.fecha_inicio,
                 'fecha_fin', NEW.fecha_fin,
                 'id_director', NEW.id_director),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_proyecto_investigacion_d` AFTER DELETE ON `proyecto_investigacion` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('proyecto_investigacion', 'DELETE', OLD.cod_proyecto,
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto, 'nombre_proyecto', OLD.nombre_proyecto, 'acronimo', OLD.acronimo, 'presupuesto_total', OLD.presupuesto_total, 'programa_idi', OLD.programa_idi, 'fecha_inicio', OLD.fecha_inicio, 'fecha_fin', OLD.fecha_fin, 'descripcion', OLD.descripcion, 'id_director', OLD.id_director),
          NULL,
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_proyecto_investigacion_i` AFTER INSERT ON `proyecto_investigacion` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('proyecto_investigacion', 'INSERT', NEW.cod_proyecto,
          NULL,
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto, 'nombre_proyecto', NEW.nombre_proyecto, 'acronimo', NEW.acronimo, 'presupuesto_total', NEW.presupuesto_total, 'programa_idi', NEW.programa_idi, 'fecha_inicio', NEW.fecha_inicio, 'fecha_fin', NEW.fecha_fin, 'descripcion', NEW.descripcion, 'id_director', NEW.id_director),
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_proyecto_investigacion_u` AFTER UPDATE ON `proyecto_investigacion` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('proyecto_investigacion', 'UPDATE', NEW.cod_proyecto,
          JSON_OBJECT('cod_proyecto', OLD.cod_proyecto, 'nombre_proyecto', OLD.nombre_proyecto, 'acronimo', OLD.acronimo, 'presupuesto_total', OLD.presupuesto_total, 'programa_idi', OLD.programa_idi, 'fecha_inicio', OLD.fecha_inicio, 'fecha_fin', OLD.fecha_fin, 'descripcion', OLD.descripcion, 'id_director', OLD.id_director),
          JSON_OBJECT('cod_proyecto', NEW.cod_proyecto, 'nombre_proyecto', NEW.nombre_proyecto, 'acronimo', NEW.acronimo, 'presupuesto_total', NEW.presupuesto_total, 'programa_idi', NEW.programa_idi, 'fecha_inicio', NEW.fecha_inicio, 'fecha_fin', NEW.fecha_fin, 'descripcion', NEW.descripcion, 'id_director', NEW.id_director),
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_proyecto_bi` BEFORE INSERT ON `proyecto_investigacion` FOR EACH ROW BEGIN
  CALL sp_validar_director(NEW.cod_proyecto, NEW.id_director, NEW.fecha_fin);
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_proyecto_bu` BEFORE UPDATE ON `proyecto_investigacion` FOR EACH ROW BEGIN
  CALL sp_validar_director(NEW.cod_proyecto, NEW.id_director, NEW.fecha_fin);
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `proyecto_linea`
--

CREATE TABLE `proyecto_linea` (
  `cod_proyecto` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `cod_linea` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `proyecto_linea`
--

INSERT INTO `proyecto_linea` (`cod_proyecto`, `cod_linea`) VALUES
('PRY-2023-003', 'LIN04'),
('PRY-2025-001', 'LIN01'),
('PRY-2025-001', 'LIN02'),
('PRY-2025-002', 'LIN03'),
('PRY-2026-004', 'LIN02');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `prueba_recuperacion`
--

CREATE TABLE `prueba_recuperacion` (
  `id` int(11) NOT NULL,
  `descripcion` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `prueba_recuperacion`
--

INSERT INTO `prueba_recuperacion` (`id`, `descripcion`) VALUES
(1, 'Registro para probar recuperacion');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `prueba_transacciones`
--

CREATE TABLE `prueba_transacciones` (
  `id` int(11) NOT NULL,
  `descripcion` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `prueba_transacciones`
--

INSERT INTO `prueba_transacciones` (`id`, `descripcion`) VALUES
(2, 'Registro confirmado'),
(6, 'Prueba de secretaria'),
(7, 'Administrador con permisos');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `publicacion`
--

CREATE TABLE `publicacion` (
  `cod_publicacion` int(11) NOT NULL,
  `cod_proyecto` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `nro_secuencia` int(11) NOT NULL DEFAULT 0,
  `titulo` varchar(200) NOT NULL,
  `nombre` varchar(150) DEFAULT NULL,
  `tipo` enum('CONGRESO','REVISTA','LIBRO','PATENTE') NOT NULL,
  `puntos_asignados` decimal(4,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `publicacion`
--

INSERT INTO `publicacion` (`cod_publicacion`, `cod_proyecto`, `nro_secuencia`, `titulo`, `nombre`, `tipo`, `puntos_asignados`) VALUES
(1, 'PRY-2025-001', 1, 'Modelo relacional para la gestión de investigación', 'Revista Latinoamericana de Informática', 'REVISTA', 1.00),
(2, 'PRY-2025-001', 2, 'Arquitectura de microservicios universitarios', 'Congreso Internacional de Software', 'CONGRESO', 0.00),
(3, 'PRY-2025-002', 1, 'Prácticas agroecológicas en la provincia Bolívar', 'Revista Agroandina', 'REVISTA', 0.20),
(4, 'PRY-2023-003', 1, 'Manual de recursos digitales para el aula', 'Editorial UEB', 'LIBRO', 1.00),
(5, 'PRY-2025-001', 3, 'Método de indexación para consultas académicas', 'IEPI', 'PATENTE', 1.00);

--
-- Disparadores `publicacion`
--
DELIMITER $$
CREATE TRIGGER `trg_aud_publicacion_ad` AFTER DELETE ON `publicacion` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('publicacion', 'DELETE', OLD.cod_publicacion,
          JSON_OBJECT('cod_publicacion', OLD.cod_publicacion,
                 'cod_proyecto', OLD.cod_proyecto,
                 'nro_secuencia', OLD.nro_secuencia,
                 'titulo', OLD.titulo,
                 'nombre', OLD.nombre,
                 'tipo', OLD.tipo,
                 'puntos_asignados', OLD.puntos_asignados),
          NULL,
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_publicacion_ai` AFTER INSERT ON `publicacion` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('publicacion', 'INSERT', NEW.cod_publicacion, NULL,
          JSON_OBJECT('cod_publicacion', NEW.cod_publicacion,
                 'cod_proyecto', NEW.cod_proyecto,
                 'nro_secuencia', NEW.nro_secuencia,
                 'titulo', NEW.titulo,
                 'nombre', NEW.nombre,
                 'tipo', NEW.tipo,
                 'puntos_asignados', NEW.puntos_asignados),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_publicacion_au` AFTER UPDATE ON `publicacion` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('publicacion', 'UPDATE', OLD.cod_publicacion,
          JSON_OBJECT('cod_publicacion', OLD.cod_publicacion,
                 'cod_proyecto', OLD.cod_proyecto,
                 'nro_secuencia', OLD.nro_secuencia,
                 'titulo', OLD.titulo,
                 'nombre', OLD.nombre,
                 'tipo', OLD.tipo,
                 'puntos_asignados', OLD.puntos_asignados),
          JSON_OBJECT('cod_publicacion', NEW.cod_publicacion,
                 'cod_proyecto', NEW.cod_proyecto,
                 'nro_secuencia', NEW.nro_secuencia,
                 'titulo', NEW.titulo,
                 'nombre', NEW.nombre,
                 'tipo', NEW.tipo,
                 'puntos_asignados', NEW.puntos_asignados),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_publicacion_d` AFTER DELETE ON `publicacion` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('publicacion', 'DELETE', OLD.cod_publicacion,
          JSON_OBJECT('cod_publicacion', OLD.cod_publicacion, 'cod_proyecto', OLD.cod_proyecto, 'nro_secuencia', OLD.nro_secuencia, 'titulo', OLD.titulo, 'nombre', OLD.nombre, 'tipo', OLD.tipo, 'puntos_asignados', OLD.puntos_asignados),
          NULL,
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_publicacion_i` AFTER INSERT ON `publicacion` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('publicacion', 'INSERT', NEW.cod_publicacion,
          NULL,
          JSON_OBJECT('cod_publicacion', NEW.cod_publicacion, 'cod_proyecto', NEW.cod_proyecto, 'nro_secuencia', NEW.nro_secuencia, 'titulo', NEW.titulo, 'nombre', NEW.nombre, 'tipo', NEW.tipo, 'puntos_asignados', NEW.puntos_asignados),
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_publicacion_u` AFTER UPDATE ON `publicacion` FOR EACH ROW INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('publicacion', 'UPDATE', NEW.cod_publicacion,
          JSON_OBJECT('cod_publicacion', OLD.cod_publicacion, 'cod_proyecto', OLD.cod_proyecto, 'nro_secuencia', OLD.nro_secuencia, 'titulo', OLD.titulo, 'nombre', OLD.nombre, 'tipo', OLD.tipo, 'puntos_asignados', OLD.puntos_asignados),
          JSON_OBJECT('cod_publicacion', NEW.cod_publicacion, 'cod_proyecto', NEW.cod_proyecto, 'nro_secuencia', NEW.nro_secuencia, 'titulo', NEW.titulo, 'nombre', NEW.nombre, 'tipo', NEW.tipo, 'puntos_asignados', NEW.puntos_asignados),
          USER())
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_publicacion_bi` BEFORE INSERT ON `publicacion` FOR EACH ROW BEGIN
  SET NEW.nro_secuencia = (SELECT COALESCE(MAX(p.nro_secuencia), 0) + 1
                             FROM publicacion p
                            WHERE p.cod_proyecto = NEW.cod_proyecto);
  SET NEW.puntos_asignados = 0;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_publicacion_bu` BEFORE UPDATE ON `publicacion` FOR EACH ROW BEGIN
  IF NEW.tipo <> OLD.tipo THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede cambiar el tipo de una publicacion';
  END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `revista`
--

CREATE TABLE `revista` (
  `cod_publicacion` int(11) NOT NULL,
  `editorial` varchar(100) DEFAULT NULL,
  `volumen` varchar(10) DEFAULT NULL,
  `numero` varchar(10) DEFAULT NULL,
  `pag_inicio` int(11) DEFAULT NULL,
  `pag_fin` int(11) DEFAULT NULL,
  `categoria` enum('ALTO_IMPACTO','REGIONAL') NOT NULL,
  `cuartil` varchar(2) DEFAULT NULL,
  `alcance` varchar(40) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `revista`
--

INSERT INTO `revista` (`cod_publicacion`, `editorial`, `volumen`, `numero`, `pag_inicio`, `pag_fin`, `categoria`, `cuartil`, `alcance`) VALUES
(1, 'Editorial Académica Latina', '12', '2', 15, 30, 'ALTO_IMPACTO', 'Q1', 'Internacional'),
(3, 'Editorial Agroandina', '5', '1', 40, 52, 'REGIONAL', NULL, 'Regional');

--
-- Disparadores `revista`
--
DELIMITER $$
CREATE TRIGGER `trg_revista_ai` AFTER INSERT ON `revista` FOR EACH ROW BEGIN
  CALL sp_asignar_puntos(NEW.cod_publicacion,
       IF(NEW.categoria = 'REGIONAL', 'REVISTA_REGIONAL', CONCAT('REVISTA_', NEW.cuartil)));
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_revista_au` AFTER UPDATE ON `revista` FOR EACH ROW BEGIN
  CALL sp_asignar_puntos(NEW.cod_publicacion,
       IF(NEW.categoria = 'REGIONAL', 'REVISTA_REGIONAL', CONCAT('REVISTA_', NEW.cuartil)));
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_revista_bi` BEFORE INSERT ON `revista` FOR EACH ROW BEGIN
  CALL sp_validar_tipo_publicacion(NEW.cod_publicacion, 'REVISTA');
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `supervisa`
--

CREATE TABLE `supervisa` (
  `id_doctor` int(11) NOT NULL,
  `id_no_doctor` int(11) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `supervisa`
--

INSERT INTO `supervisa` (`id_doctor`, `id_no_doctor`, `fecha_inicio`, `fecha_fin`) VALUES
(1, 3, '2025-01-15', NULL),
(2, 4, '2024-02-01', '2025-02-28'),
(2, 5, '2025-05-01', NULL),
(6, 4, '2025-03-01', NULL);

--
-- Disparadores `supervisa`
--
DELIMITER $$
CREATE TRIGGER `trg_aud_supervisa_ad` AFTER DELETE ON `supervisa` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('supervisa', 'DELETE', CONCAT_WS('|', OLD.id_doctor, OLD.id_no_doctor, OLD.fecha_inicio),
          JSON_OBJECT('id_doctor', OLD.id_doctor,
                 'id_no_doctor', OLD.id_no_doctor,
                 'fecha_inicio', OLD.fecha_inicio,
                 'fecha_fin', OLD.fecha_fin),
          NULL,
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_supervisa_ai` AFTER INSERT ON `supervisa` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('supervisa', 'INSERT', CONCAT_WS('|', NEW.id_doctor, NEW.id_no_doctor, NEW.fecha_inicio), NULL,
          JSON_OBJECT('id_doctor', NEW.id_doctor,
                 'id_no_doctor', NEW.id_no_doctor,
                 'fecha_inicio', NEW.fecha_inicio,
                 'fecha_fin', NEW.fecha_fin),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_aud_supervisa_au` AFTER UPDATE ON `supervisa` FOR EACH ROW BEGIN
  INSERT INTO auditoria_cambios (tabla, operacion, clave_registro, valores_anteriores, valores_nuevos, usuario)
  VALUES ('supervisa', 'UPDATE', CONCAT_WS('|', OLD.id_doctor, OLD.id_no_doctor, OLD.fecha_inicio),
          JSON_OBJECT('id_doctor', OLD.id_doctor,
                 'id_no_doctor', OLD.id_no_doctor,
                 'fecha_inicio', OLD.fecha_inicio,
                 'fecha_fin', OLD.fecha_fin),
          JSON_OBJECT('id_doctor', NEW.id_doctor,
                 'id_no_doctor', NEW.id_no_doctor,
                 'fecha_inicio', NEW.fecha_inicio,
                 'fecha_fin', NEW.fecha_fin),
          USER());
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_supervisa_bi` BEFORE INSERT ON `supervisa` FOR EACH ROW BEGIN
  CALL sp_validar_supervision(NEW.id_no_doctor, NEW.fecha_inicio, NEW.fecha_fin, NULL, NULL);
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_supervisa_bu` BEFORE UPDATE ON `supervisa` FOR EACH ROW BEGIN
  CALL sp_validar_supervision(NEW.id_no_doctor, NEW.fecha_inicio, NEW.fecha_fin,
                              OLD.id_doctor, OLD.fecha_inicio);
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `trabaja_linea`
--

CREATE TABLE `trabaja_linea` (
  `id_profesor` int(11) NOT NULL,
  `cod_linea` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `trabaja_linea`
--

INSERT INTO `trabaja_linea` (`id_profesor`, `cod_linea`) VALUES
(1, 'LIN01'),
(1, 'LIN02'),
(2, 'LIN03'),
(3, 'LIN01'),
(4, 'LIN03'),
(5, 'LIN03'),
(6, 'LIN04'),
(7, 'LIN01');

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_carga_investigadores`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_carga_investigadores` (
`id_profesor` int(11)
,`investigador` varchar(121)
,`tipo` enum('DOCTOR','NO_DOCTOR')
,`proyectos_como_investigador` bigint(21)
,`proyectos_como_director` bigint(21)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_investigadores_sin_linea`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_investigadores_sin_linea` (
`id_profesor` int(11)
,`investigador` varchar(121)
,`tipo` enum('DOCTOR','NO_DOCTOR')
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_proyectos_estudiante`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_proyectos_estudiante` (
`cod_proyecto` varchar(20)
,`nombre_proyecto` varchar(150)
,`acronimo` varchar(20)
,`presupuesto_total` decimal(12,2)
,`programa_idi` varchar(100)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_proyectos_profesor`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_proyectos_profesor` (
`cod_proyecto` varchar(20)
,`nombre_proyecto` varchar(150)
,`acronimo` varchar(20)
,`presupuesto_total` decimal(12,2)
,`programa_idi` varchar(100)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_proyectos_resumen`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_proyectos_resumen` (
`cod_proyecto` varchar(20)
,`acronimo` varchar(20)
,`nombre_proyecto` varchar(150)
,`director` varchar(121)
,`presupuesto_total` decimal(12,2)
,`estado` varchar(10)
,`investigadores_vigentes` bigint(21)
,`publicaciones` bigint(21)
,`puntaje_total` decimal(26,2)
);

-- --------------------------------------------------------

--
-- Estructura para la vista `v_carga_investigadores`
--
DROP TABLE IF EXISTS `v_carga_investigadores`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_carga_investigadores`  AS SELECT `i`.`id_profesor` AS `id_profesor`, concat(`i`.`nombres`,' ',`i`.`apellidos`) AS `investigador`, `i`.`tipo` AS `tipo`, (select count(distinct `pp`.`cod_proyecto`) from `participa_proyecto` `pp` where `pp`.`id_profesor` = `i`.`id_profesor` and (`pp`.`fecha_fin` is null or `pp`.`fecha_fin` > curdate())) AS `proyectos_como_investigador`, (select count(0) from `proyecto_investigacion` `p` where `p`.`id_director` = `i`.`id_profesor` and (`p`.`fecha_fin` is null or `p`.`fecha_fin` > curdate())) AS `proyectos_como_director` FROM `investigador` AS `i` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_investigadores_sin_linea`
--
DROP TABLE IF EXISTS `v_investigadores_sin_linea`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_investigadores_sin_linea`  AS SELECT `i`.`id_profesor` AS `id_profesor`, concat(`i`.`nombres`,' ',`i`.`apellidos`) AS `investigador`, `i`.`tipo` AS `tipo` FROM (`investigador` `i` left join `trabaja_linea` `t` on(`t`.`id_profesor` = `i`.`id_profesor`)) WHERE `t`.`id_profesor` is null ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_proyectos_estudiante`
--
DROP TABLE IF EXISTS `v_proyectos_estudiante`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_proyectos_estudiante`  AS SELECT DISTINCT `p`.`cod_proyecto` AS `cod_proyecto`, `p`.`nombre_proyecto` AS `nombre_proyecto`, `p`.`acronimo` AS `acronimo`, `p`.`presupuesto_total` AS `presupuesto_total`, `p`.`programa_idi` AS `programa_idi` FROM ((`proyecto_investigacion` `p` join `participante_estudiante` `pe` on(`p`.`cod_proyecto` collate utf8mb4_general_ci = `pe`.`cod_proyecto` collate utf8mb4_general_ci)) join `cuentas_estudiantes` `ce` on(`ce`.`id_estudiante` = `pe`.`id_estudiante`)) WHERE `ce`.`usuario` = substring_index(user(),'@',1) ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_proyectos_profesor`
--
DROP TABLE IF EXISTS `v_proyectos_profesor`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_proyectos_profesor`  AS SELECT DISTINCT `p`.`cod_proyecto` AS `cod_proyecto`, `p`.`nombre_proyecto` AS `nombre_proyecto`, `p`.`acronimo` AS `acronimo`, `p`.`presupuesto_total` AS `presupuesto_total`, `p`.`programa_idi` AS `programa_idi` FROM ((`proyecto_investigacion` `p` join `participa_proyecto` `pp` on(`p`.`cod_proyecto` collate utf8mb4_general_ci = `pp`.`cod_proyecto` collate utf8mb4_general_ci)) join `cuentas_profesores` `cp` on(`cp`.`id_profesor` = `pp`.`id_profesor`)) WHERE `cp`.`usuario` = substring_index(user(),'@',1) ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_proyectos_resumen`
--
DROP TABLE IF EXISTS `v_proyectos_resumen`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY INVOKER VIEW `v_proyectos_resumen`  AS SELECT `p`.`cod_proyecto` AS `cod_proyecto`, `p`.`acronimo` AS `acronimo`, `p`.`nombre_proyecto` AS `nombre_proyecto`, concat(`d`.`nombres`,' ',`d`.`apellidos`) AS `director`, `p`.`presupuesto_total` AS `presupuesto_total`, CASE WHEN `p`.`fecha_fin` is null OR `p`.`fecha_fin` > curdate() THEN 'VIGENTE' ELSE 'FINALIZADO' END AS `estado`, (select count(distinct `pp`.`id_profesor`) from `participa_proyecto` `pp` where `pp`.`cod_proyecto` = `p`.`cod_proyecto` and (`pp`.`fecha_fin` is null or `pp`.`fecha_fin` > curdate())) AS `investigadores_vigentes`, (select count(0) from `publicacion` `pu` where `pu`.`cod_proyecto` = `p`.`cod_proyecto`) AS `publicaciones`, (select coalesce(sum(`pu`.`puntos_asignados`),0) from `publicacion` `pu` where `pu`.`cod_proyecto` = `p`.`cod_proyecto`) AS `puntaje_total` FROM (`proyecto_investigacion` `p` join `investigador` `d` on(`d`.`id_profesor` = `p`.`id_director`)) ;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `auditoria_cambios`
--
ALTER TABLE `auditoria_cambios`
  ADD PRIMARY KEY (`id_auditoria`);

--
-- Indices de la tabla `auditoria_operaciones`
--
ALTER TABLE `auditoria_operaciones`
  ADD PRIMARY KEY (`id_auditoria`);

--
-- Indices de la tabla `congreso`
--
ALTER TABLE `congreso`
  ADD PRIMARY KEY (`cod_publicacion`);

--
-- Indices de la tabla `cuentas_estudiantes`
--
ALTER TABLE `cuentas_estudiantes`
  ADD PRIMARY KEY (`usuario`),
  ADD UNIQUE KEY `id_estudiante` (`id_estudiante`);

--
-- Indices de la tabla `cuentas_profesores`
--
ALTER TABLE `cuentas_profesores`
  ADD PRIMARY KEY (`usuario`),
  ADD UNIQUE KEY `id_profesor` (`id_profesor`);

--
-- Indices de la tabla `doctor`
--
ALTER TABLE `doctor`
  ADD PRIMARY KEY (`id_profesor`);

--
-- Indices de la tabla `dominio_conocimiento`
--
ALTER TABLE `dominio_conocimiento`
  ADD PRIMARY KEY (`cod_dominio`);

--
-- Indices de la tabla `escribe`
--
ALTER TABLE `escribe`
  ADD PRIMARY KEY (`id_profesor`,`cod_publicacion`,`cod_linea`),
  ADD KEY `fk_escribe_publicacion` (`cod_publicacion`),
  ADD KEY `fk_escribe_linea` (`cod_linea`);

--
-- Indices de la tabla `estudiante`
--
ALTER TABLE `estudiante`
  ADD PRIMARY KEY (`id_estudiante`);

--
-- Indices de la tabla `investigador`
--
ALTER TABLE `investigador`
  ADD PRIMARY KEY (`id_profesor`);

--
-- Indices de la tabla `libro`
--
ALTER TABLE `libro`
  ADD PRIMARY KEY (`cod_publicacion`);

--
-- Indices de la tabla `linea_investigacion`
--
ALTER TABLE `linea_investigacion`
  ADD PRIMARY KEY (`cod_linea`),
  ADD KEY `fk_linea_dominio` (`cod_dominio`);

--
-- Indices de la tabla `no_doctor`
--
ALTER TABLE `no_doctor`
  ADD PRIMARY KEY (`id_profesor`);

--
-- Indices de la tabla `parametro_puntaje`
--
ALTER TABLE `parametro_puntaje`
  ADD PRIMARY KEY (`clave`);

--
-- Indices de la tabla `participante_estudiante`
--
ALTER TABLE `participante_estudiante`
  ADD PRIMARY KEY (`cod_proyecto`,`id_estudiante`),
  ADD KEY `fk_participante_estudiante` (`id_estudiante`);

--
-- Indices de la tabla `participa_proyecto`
--
ALTER TABLE `participa_proyecto`
  ADD PRIMARY KEY (`cod_proyecto`,`id_profesor`),
  ADD KEY `fk_participa_investigador` (`id_profesor`);

--
-- Indices de la tabla `patente`
--
ALTER TABLE `patente`
  ADD PRIMARY KEY (`cod_publicacion`);

--
-- Indices de la tabla `proyecto_investigacion`
--
ALTER TABLE `proyecto_investigacion`
  ADD PRIMARY KEY (`cod_proyecto`);

--
-- Indices de la tabla `proyecto_linea`
--
ALTER TABLE `proyecto_linea`
  ADD PRIMARY KEY (`cod_proyecto`,`cod_linea`),
  ADD KEY `fk_proyecto_linea_linea` (`cod_linea`);

--
-- Indices de la tabla `prueba_recuperacion`
--
ALTER TABLE `prueba_recuperacion`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `prueba_transacciones`
--
ALTER TABLE `prueba_transacciones`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `publicacion`
--
ALTER TABLE `publicacion`
  ADD PRIMARY KEY (`cod_publicacion`),
  ADD KEY `fk_publicacion_proyecto` (`cod_proyecto`);

--
-- Indices de la tabla `revista`
--
ALTER TABLE `revista`
  ADD PRIMARY KEY (`cod_publicacion`);

--
-- Indices de la tabla `supervisa`
--
ALTER TABLE `supervisa`
  ADD PRIMARY KEY (`id_doctor`,`id_no_doctor`,`fecha_inicio`),
  ADD KEY `fk_supervisa_no_doctor` (`id_no_doctor`);

--
-- Indices de la tabla `trabaja_linea`
--
ALTER TABLE `trabaja_linea`
  ADD PRIMARY KEY (`id_profesor`,`cod_linea`),
  ADD KEY `fk_trabaja_linea` (`cod_linea`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `auditoria_cambios`
--
ALTER TABLE `auditoria_cambios`
  MODIFY `id_auditoria` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `congreso`
--
ALTER TABLE `congreso`
  ADD CONSTRAINT `fk_congreso_publicacion` FOREIGN KEY (`cod_publicacion`) REFERENCES `publicacion` (`cod_publicacion`);

--
-- Filtros para la tabla `cuentas_estudiantes`
--
ALTER TABLE `cuentas_estudiantes`
  ADD CONSTRAINT `fk_cuentas_estudiantes_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiante` (`id_estudiante`);

--
-- Filtros para la tabla `cuentas_profesores`
--
ALTER TABLE `cuentas_profesores`
  ADD CONSTRAINT `fk_cuentas_profesores_investigador` FOREIGN KEY (`id_profesor`) REFERENCES `investigador` (`id_profesor`);

--
-- Filtros para la tabla `doctor`
--
ALTER TABLE `doctor`
  ADD CONSTRAINT `fk_doctor_investigador` FOREIGN KEY (`id_profesor`) REFERENCES `investigador` (`id_profesor`);

--
-- Filtros para la tabla `escribe`
--
ALTER TABLE `escribe`
  ADD CONSTRAINT `fk_escribe_investigador` FOREIGN KEY (`id_profesor`) REFERENCES `investigador` (`id_profesor`),
  ADD CONSTRAINT `fk_escribe_linea` FOREIGN KEY (`cod_linea`) REFERENCES `linea_investigacion` (`cod_linea`),
  ADD CONSTRAINT `fk_escribe_publicacion` FOREIGN KEY (`cod_publicacion`) REFERENCES `publicacion` (`cod_publicacion`);

--
-- Filtros para la tabla `libro`
--
ALTER TABLE `libro`
  ADD CONSTRAINT `fk_libro_publicacion` FOREIGN KEY (`cod_publicacion`) REFERENCES `publicacion` (`cod_publicacion`);

--
-- Filtros para la tabla `linea_investigacion`
--
ALTER TABLE `linea_investigacion`
  ADD CONSTRAINT `fk_linea_dominio` FOREIGN KEY (`cod_dominio`) REFERENCES `dominio_conocimiento` (`cod_dominio`);

--
-- Filtros para la tabla `no_doctor`
--
ALTER TABLE `no_doctor`
  ADD CONSTRAINT `fk_no_doctor_investigador` FOREIGN KEY (`id_profesor`) REFERENCES `investigador` (`id_profesor`);

--
-- Filtros para la tabla `participante_estudiante`
--
ALTER TABLE `participante_estudiante`
  ADD CONSTRAINT `fk_participante_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiante` (`id_estudiante`),
  ADD CONSTRAINT `fk_participante_proyecto` FOREIGN KEY (`cod_proyecto`) REFERENCES `proyecto_investigacion` (`cod_proyecto`);

--
-- Filtros para la tabla `participa_proyecto`
--
ALTER TABLE `participa_proyecto`
  ADD CONSTRAINT `fk_participa_investigador` FOREIGN KEY (`id_profesor`) REFERENCES `investigador` (`id_profesor`),
  ADD CONSTRAINT `fk_participa_proyecto` FOREIGN KEY (`cod_proyecto`) REFERENCES `proyecto_investigacion` (`cod_proyecto`);

--
-- Filtros para la tabla `patente`
--
ALTER TABLE `patente`
  ADD CONSTRAINT `fk_patente_publicacion` FOREIGN KEY (`cod_publicacion`) REFERENCES `publicacion` (`cod_publicacion`);

--
-- Filtros para la tabla `proyecto_linea`
--
ALTER TABLE `proyecto_linea`
  ADD CONSTRAINT `fk_proyecto_linea_linea` FOREIGN KEY (`cod_linea`) REFERENCES `linea_investigacion` (`cod_linea`),
  ADD CONSTRAINT `fk_proyecto_linea_proyecto` FOREIGN KEY (`cod_proyecto`) REFERENCES `proyecto_investigacion` (`cod_proyecto`);

--
-- Filtros para la tabla `publicacion`
--
ALTER TABLE `publicacion`
  ADD CONSTRAINT `fk_publicacion_proyecto` FOREIGN KEY (`cod_proyecto`) REFERENCES `proyecto_investigacion` (`cod_proyecto`);

--
-- Filtros para la tabla `supervisa`
--
ALTER TABLE `supervisa`
  ADD CONSTRAINT `fk_supervisa_doctor` FOREIGN KEY (`id_doctor`) REFERENCES `doctor` (`id_profesor`),
  ADD CONSTRAINT `fk_supervisa_no_doctor` FOREIGN KEY (`id_no_doctor`) REFERENCES `no_doctor` (`id_profesor`);

--
-- Filtros para la tabla `trabaja_linea`
--
ALTER TABLE `trabaja_linea`
  ADD CONSTRAINT `fk_trabaja_investigador` FOREIGN KEY (`id_profesor`) REFERENCES `investigador` (`id_profesor`),
  ADD CONSTRAINT `fk_trabaja_linea` FOREIGN KEY (`cod_linea`) REFERENCES `linea_investigacion` (`cod_linea`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
