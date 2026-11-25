-- =====================================================================
-- Migración no destructiva para investigacion_ueb
-- Aplicar DESPUÉS de importar investigacion_ueb.sql
-- =====================================================================
-- 1) Corrige dos defectos del volcado original:
--    - sp_tx_* inserta en auditoria_operaciones sin id_auditoria, pero la columna no era AUTO_INCREMENT.
--    - sp_tx_registrar_revista usa LAST_INSERT_ID(), pero publicacion.cod_publicacion no era AUTO_INCREMENT.
--    MODIFY solo cambia metadatos; no elimina ni reescribe registros.
-- 2) Crea las tablas de autenticación (perfiles del modelo original) con contraseñas BCrypt.
--    Las cuentas de desarrollo son: admin/admin123, secretaria/secretaria123,
--    carlos.mendoza/profesor123, pedro.gomez/estudiante123.
--    Cambiar estas contraseñas antes de exponer el sistema a producción.
-- =====================================================================

ALTER TABLE `auditoria_operaciones`
  MODIFY `id_auditoria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

ALTER TABLE `publicacion`
  MODIFY `cod_publicacion` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

CREATE TABLE IF NOT EXISTS `seg_rol` (
  `cod_rol` varchar(20) NOT NULL,
  `nombre` varchar(60) NOT NULL,
  `descripcion` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`cod_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `seg_usuario` (
  `username` varchar(80) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `id_profesor` int(11) DEFAULT NULL,
  `id_estudiante` int(11) DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  `fecha_creacion` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`username`),
  KEY `fk_seg_usuario_profesor` (`id_profesor`),
  KEY `fk_seg_usuario_estudiante` (`id_estudiante`),
  CONSTRAINT `fk_seg_usuario_profesor` FOREIGN KEY (`id_profesor`) REFERENCES `investigador` (`id_profesor`),
  CONSTRAINT `fk_seg_usuario_estudiante` FOREIGN KEY (`id_estudiante`) REFERENCES `estudiante` (`id_estudiante`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `seg_usuario_rol` (
  `username` varchar(80) NOT NULL,
  `cod_rol` varchar(20) NOT NULL,
  PRIMARY KEY (`username`, `cod_rol`),
  KEY `fk_seg_usuario_rol_rol` (`cod_rol`),
  CONSTRAINT `fk_seg_usuario_rol_usuario` FOREIGN KEY (`username`) REFERENCES `seg_usuario` (`username`) ON DELETE CASCADE,
  CONSTRAINT `fk_seg_usuario_rol_rol` FOREIGN KEY (`cod_rol`) REFERENCES `seg_rol` (`cod_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `seg_rol` (`cod_rol`, `nombre`, `descripcion`) VALUES
  ('ADMIN', 'Administrador', 'Acceso total a usuarios, seguridad y auditoría'),
  ('SECRETARIA', 'Secretaría', 'Gestión académica de proyectos y publicaciones'),
  ('PROFESOR', 'Profesor', 'Consulta de proyectos y publicaciones propias'),
  ('ESTUDIANTE', 'Estudiante', 'Consulta de proyectos en los que participa')
ON DUPLICATE KEY UPDATE `nombre` = VALUES(`nombre`), `descripcion` = VALUES(`descripcion`);

INSERT INTO `seg_usuario` (`username`, `password_hash`, `id_profesor`, `id_estudiante`, `activo`) VALUES
  ('admin',        '$2a$10$Y7TySrvgcb5vKSaStadCTuGus//PIdTN0XZc1Mr7KvsHuoUGWjd2O', NULL, NULL, 1),
  ('secretaria',   '$2a$10$i5E0Ucg6RaUrzOGIe0fvEuDudeBRgF0nfP2HoU07Kj.UmVVVcFWW2', NULL, NULL, 1),
  ('carlos.mendoza', '$2a$10$y0EUDaFbRpEJ3WvKIotEUugt0JR6mR4HyeOpNmes37OhMruGI5F1m', 1, NULL, 1),
  ('pedro.gomez',  '$2a$10$FjDnq9qXYM6R98vMxirHZeso.cqQ0SLhF5Qj9uUg8HqRgBjppHBNa', NULL, 1, 1)
ON DUPLICATE KEY UPDATE `password_hash` = VALUES(`password_hash`), `activo` = VALUES(`activo`);

INSERT INTO `seg_usuario_rol` (`username`, `cod_rol`) VALUES
  ('admin', 'ADMIN'),
  ('secretaria', 'SECRETARIA'),
  ('carlos.mendoza', 'PROFESOR'),
  ('pedro.gomez', 'ESTUDIANTE')
ON DUPLICATE KEY UPDATE `cod_rol` = VALUES(`cod_rol`);
