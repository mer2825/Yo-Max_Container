-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 21-09-2025
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- --------------------------------------------------------
-- Base de datos: `acceso`
-- --------------------------------------------------------

-- --------------------------------------------------------
-- Tabla: opciones
-- --------------------------------------------------------
CREATE TABLE `opciones` (
  `id` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `ruta` varchar(100) NOT NULL,
  `icono` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `opciones` (`id`, `nombre`, `ruta`, `icono`) VALUES
(1, 'Dashboard', '/', 'home'),
(2, 'Gestión de Usuarios', '/usuarios/listar', 'users'),
(3, 'Gestión de Perfiles', '/perfiles/listar', 'shield'),
(4, 'Gestión de Categorías', '/categorias/listar', 'layers');

-- --------------------------------------------------------
-- Tabla: perfiles
-- --------------------------------------------------------
CREATE TABLE `perfiles` (
  `id` bigint(20) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `estado` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `perfiles` (`id`, `nombre`, `descripcion`, `estado`) VALUES
(1, 'Administrador', 'Acceso total al sistema.', 1),
(2, 'Editor', 'Puede gestionar usuarios pero no perfiles.', 1),
(3, 'Supervisor', 'Solo puede visualizar información.', 0);

-- --------------------------------------------------------
-- Tabla: categorias
-- --------------------------------------------------------
CREATE TABLE `categorias` (
  `id` bigint(20) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `estado` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `categorias` (`id`, `nombre`, `descripcion`, `estado`) VALUES
(1, 'Hardware', 'Opciones relacionadas con hardware.', 1),
(2, 'Software', 'Opciones relacionadas con software.', 1),
(3, 'Servicios', 'Opciones de servicios adicionales.', 0);

-- --------------------------------------------------------
-- Tabla: perfil_opcion
-- --------------------------------------------------------
CREATE TABLE `perfil_opcion` (
  `id_perfil` bigint(20) NOT NULL,
  `id_opcion` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `perfil_opcion` (`id_perfil`, `id_opcion`) VALUES
(1, 1),
(1, 2),
(1, 3),
(2, 1),
(2, 2),
(3, 1);

-- --------------------------------------------------------
-- Tabla: categoria_opcion
-- --------------------------------------------------------
CREATE TABLE `categoria_opcion` (
  `id_categoria` bigint(20) NOT NULL,
  `id_opcion` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `categoria_opcion` (`id_categoria`, `id_opcion`) VALUES
(1, 1),
(1, 4),
(2, 2);

-- --------------------------------------------------------
-- Tabla: usuarios
-- --------------------------------------------------------
CREATE TABLE `usuarios` (
  `id` bigint(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `usuario` varchar(50) NOT NULL,
  `clave` varchar(255) DEFAULT NULL,
  `correo` varchar(255) DEFAULT NULL,
  `estado` int(11) NOT NULL DEFAULT 1,
  `id_perfil` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `usuarios` (`id`, `nombre`, `usuario`, `clave`, `correo`, `estado`, `id_perfil`) VALUES
(8, 'Daryl', 'admin', '$2a$10$OZuN1MJlw/01gIodlwqaQOKk.d5XhfbWAD8X2adyG9pkKtpDlVN1O', 'luis@ejemplo.com', 1, 1),
(10, 'María Supervisor', 'supervisor', '$2a$10$N9qo8uLOickgx2ZMRZoMye5aZl8ZzO8Fns2h0eCZgP2h7ZWCpU9/y', 'supervisor@ejemplo.com', 1, 3),
(11, 'Carlos Analista', 'analista', '$2a$10$N9qo8uLOickgx2ZMRZoMye5aZl8ZzO8Fns2h0eCZgP2h7ZWCpU9/y', 'analista@ejemplo.com', 0, 2),
(14, 'Luis Antonio', 'luis', '$2a$10$bDRnfg7TQgcBeV.e0cd.ZuNfDUGfPRPhp62tfLVtycqwV/unM0VWm', 'luis@ejemplo.com', 1, 1),
(15, 'Blanca Rosa', 'blanca', '$2a$10$UTJNtLoen3wHnh1WMF756uBNJo9Gm4Hlmm8XuiFTOrJy5wdnt1d3C', 'blanca@ejemplo.com', 0, 2);

-- --------------------------------------------------------
-- Índices y claves foráneas
-- --------------------------------------------------------

-- Opciones
ALTER TABLE `opciones`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_nombre_opcion` (`nombre`);

-- Perfiles
ALTER TABLE `perfiles`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_nombre_perfil` (`nombre`);

-- Categorías
ALTER TABLE `categorias`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_nombre_categoria` (`nombre`);

-- Perfil-Opción
ALTER TABLE `perfil_opcion`
  ADD PRIMARY KEY (`id_perfil`,`id_opcion`),
  ADD KEY `FK_perfil_opcion_opcion` (`id_opcion`);

-- Categoria-Opción
ALTER TABLE `categoria_opcion`
  ADD PRIMARY KEY (`id_categoria`,`id_opcion`),
  ADD KEY `FK_categoria_opcion_opcion` (`id_opcion`);

-- Usuarios
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_usuario` (`usuario`),
  ADD KEY `FK_usuarios_perfiles` (`id_perfil`);

-- AUTO_INCREMENT
ALTER TABLE `opciones`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

ALTER TABLE `perfiles`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

ALTER TABLE `categorias`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

ALTER TABLE `usuarios`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

-- Restricciones
ALTER TABLE `perfil_opcion`
  ADD CONSTRAINT `FK_perfil_opcion_opcion` FOREIGN KEY (`id_opcion`) REFERENCES `opciones` (`id`),
  ADD CONSTRAINT `FK_perfil_opcion_perfil` FOREIGN KEY (`id_perfil`) REFERENCES `perfiles` (`id`);

ALTER TABLE `categoria_opcion`
  ADD CONSTRAINT `FK_categoria_opcion_opcion` FOREIGN KEY (`id_opcion`) REFERENCES `opciones` (`id`),
  ADD CONSTRAINT `FK_categoria_opcion_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id`);

ALTER TABLE `usuarios`
  ADD CONSTRAINT `FK_usuarios_perfiles` FOREIGN KEY (`id_perfil`) REFERENCES `perfiles` (`id`);

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
