-- Datos de prueba para la aplicación 'acceso' (sin IDs fijos)
-- Inserciones diseñadas para evitar conflictos con sequences/identity.

-- CATEGORIAS
INSERT INTO categorias (nombre, descripcion, estado) VALUES
  ('Regalos', 'Detalles y regalos importados', 1),
  ('Accesorios', 'Artesanía y accesorios', 1),
  ('Decoración', 'Artículos de decoración para el hogar', 1),
  ('Juguetes', 'Juguetes importados', 1),
  ('Papelería', 'Artículos de papelería y complementos', 1);

-- PERFILES (para usuarios)
INSERT INTO perfiles (nombre, descripcion, estado) VALUES
  ('ADMIN', 'Administrador del sistema', 1),
  ('VENDEDOR', 'Usuario vendedor', 1),
  ('INVENTARIO', 'Gestión de inventario', 1);

-- NOTA: Las inserciones de usuarios se omiten porque la aplicación
-- tiene un inicializador (`DataInitializer`) que crea el usuario
-- administrador por defecto; insertar aquí puede causar conflictos
-- con los IDs o nombres de usuario ya reservados.

-- PRODUCTOS
INSERT INTO productos (id_categoria, nombre, descripcion, precio, stock, stock_minimo, estado)
SELECT c.id, v.nombre, v.descripcion, v.precio, v.stock, v.stock_minimo, 1
FROM (VALUES
  ('Porta Velas de Madera','Porta velas tallado a mano',25.50,30,2),
  ('Caja Regalo Corazón','Caja de regalo para detalles',15.00,50,5),
  ('Pulsera Tejida','Pulsera de cuerda con dije',9.99,100,10),
  ('Cuadro Decorativo 20x30','Impresión sobre madera',45.00,20,1),
  ('Set de Llavero','Llavero metálico decorativo',7.50,200,10),
  ('Bloc de Notas Premium','Bloc rayado tamaño A5',12.00,80,5)
) AS v(nombre, descripcion, precio, stock, stock_minimo)
JOIN categorias c ON c.nombre = CASE
  WHEN v.nombre ILIKE '%Porta Velas%' THEN 'Regalos'
  WHEN v.nombre ILIKE '%Caja Regalo%' THEN 'Regalos'
  WHEN v.nombre ILIKE '%Pulsera%' THEN 'Accesorios'
  WHEN v.nombre ILIKE '%Cuadro%' THEN 'Decoración'
  WHEN v.nombre ILIKE '%Llavero%' THEN 'Accesorios'
  ELSE 'Papelería' END;

-- IMAGENES DE PRODUCTOS (usa nombre del producto para relacionar)
INSERT INTO producto_imagenes (url, orden, producto_id)
SELECT '/static/uploads/' || replace(lower(p.nombre),' ','_') || '_1.jpg', 1, p.id FROM productos p
WHERE p.nombre IN ('Porta Velas de Madera','Caja Regalo Corazón','Pulsera Tejida','Cuadro Decorativo 20x30','Set de Llavero','Bloc de Notas Premium');

-- CLIENTES
INSERT INTO clientes (tipo_documento, numero_documento, nombre, direccion, telefono, email, estado) VALUES
  ('DNI', '12345678', 'Cliente Demo', 'Av. Principal 123', '999123456', 'cliente@demo.test', 1),
  ('RUC', '20123456789', 'Empresa Import S.A.', 'Calle Comercio 45', '011223344', 'ventas@import.test', 1),
  ('DNI', '87654321', 'Rosa Pérez', 'Jr. Secundaria 45', '988776655', 'rosa@demo.test', 1);

-- VENTAS y DETALLES (ejemplos)
INSERT INTO ventas (numero_venta, cliente_id, fecha_venta, metodo_pago, subtotal, descuento, total, nota, tipo_comprobante, estado, origen)
SELECT 'V-0001', c.id, now(), 'Efectivo', 50.49, 0.00, 50.49, 'Venta demo 1', 'BOLETA', 1, 'pos' FROM clientes c WHERE c.numero_documento='12345678';

INSERT INTO detalles_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, p.precio, p.precio FROM ventas v JOIN productos p ON p.nombre='Porta Velas de Madera' WHERE v.numero_venta='V-0001';

INSERT INTO ventas (numero_venta, cliente_id, fecha_venta, metodo_pago, subtotal, descuento, total, nota, tipo_comprobante, estado, origen)
SELECT 'V-0002', c.id, now(), 'Tarjeta', 27.49, 0.00, 27.49, 'Venta demo 2', 'BOLETA', 1, 'pos' FROM clientes c WHERE c.numero_documento='87654321';

INSERT INTO detalles_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, p.precio, p.precio FROM ventas v JOIN productos p ON p.nombre='Pulsera Tejida' WHERE v.numero_venta='V-0002';

-- EMPRESA (nombre solicitado Yo'Max -> escapar apóstrofe)
-- Usar los nombres de columnas reales creados por Hibernate (logo_url)
INSERT INTO empresa (nombre, direccion, telefono, email, logo_url, nosotros)
VALUES ('Yo''Max', 'Av. Comercio 100', '014445566', 'info@yomax.test', '/static/uploads/logo.png', 'Yo''Max - Importaciones y detalles');

-- Ajustes de secuencias: (opcional) se pueden ejecutar si se importan datos con ids fijos
-- SELECT setval(pg_get_serial_sequence('productos','id'), (SELECT COALESCE(MAX(id),0) FROM productos));
