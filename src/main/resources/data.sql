-- Datos de muestra completos para PostgreSQL - Tienda de Importaciones Yo'Max
-- Generado para Railway con datos realistas para demostración
-- Modificado para evitar duplicados al ejecutar con spring.sql.init.mode=always

-- OPCIONES DE MENÚ
INSERT INTO opciones (nombre, ruta, icono, estado)
SELECT nombre, ruta, icono, estado
FROM (VALUES
  ('Gestión de Usuarios', '/usuarios/listar', 'bi-people', true),
  ('Gestión de Perfiles', '/perfiles/listar', 'bi-person-check', true),
  ('Gestión de Categorías', '/categorias/listar', 'bi-tags', true),
  ('Gestión de Productos', '/productos/listar', 'bi-cake2', true),
  ('Gestión de Clientes', '/clientes/listar', 'bi-person-vcard', true),
  ('Gestión de Empresa', '/empresa/listar', 'bi-shop-window', true),
  ('Listado de Ventas', '/ventas/listar', 'bi-receipt', true),
  ('Nueva Venta', '/ventas/nueva', 'bi-cart-plus', true),
  ('Ventas Web', '/ventas_web', 'bi-globe', true),
  ('Gestión Inventario', '/inventario/listar', 'bi-boxes', true),
  ('Ir al Catálogo', '/catalogo', 'bi-shop', true)
) AS v(nombre, ruta, icono, estado)
WHERE NOT EXISTS (
  SELECT 1 FROM opciones o WHERE o.ruta = v.ruta
);

-- CATEGORÍAS
INSERT INTO categorias (nombre, descripcion, estado)
SELECT nombre, descripcion, estado
FROM (VALUES
  ('Regalos', 'Detalles y regalos importados', 1),
  ('Accesorios', 'Artesanía y accesorios', 1),
  ('Decoración', 'Artículos de decoración para el hogar', 1),
  ('Juguetes', 'Juguetes importados', 1),
  ('Papelería', 'Artículos de papelería y complementos', 1),
  ('Electrónica', 'Dispositivos electrónicos importados', 1),
  ('Ropa', 'Prendas de vestir importadas', 1),
  ('Hogar', 'Artículos para el hogar importados', 1)
) AS v(nombre, descripcion, estado)
WHERE NOT EXISTS (
  SELECT 1 FROM categorias c WHERE c.nombre = v.nombre
);

-- PRODUCTOS
INSERT INTO productos (id_categoria, nombre, descripcion, precio, stock, stock_minimo, estado)
SELECT c.id, v.nombre, v.descripcion, v.precio, v.stock, v.stock_minimo, 1
FROM (VALUES
  -- Regalos
  ('Porta Velas de Madera', 'Porta velas tallado a mano', 25.50, 30, 2),
  ('Caja Regalo Corazón', 'Caja de regalo para detalles', 15.00, 50, 5),
  ('Set de Velas Aromáticas', 'Set de 6 velas con esencias naturales', 42.00, 18, 3),
  ('Cesta de Regalo Premium', 'Cesta con productos importados seleccionados', 120.00, 10, 2),
  ('Bolsa de Regalo', 'Bolsa de regalo premium con diseño', 18.00, 40, 6),
  ('Tarjeta de Felicitación', 'Set de tarjetas de felicitación', 12.00, 60, 10),
  
  -- Accesorios
  ('Pulsera Tejida', 'Pulsera de cuerda con dije', 9.99, 100, 10),
  ('Collar de Perlas', 'Collar de perlas cultivadas', 55.00, 15, 3),
  ('Aretes de Plata', 'Aretes de plata esterlina 925', 38.00, 25, 5),
  ('Reloj de Pulsar', 'Reloj analógico importado', 85.00, 12, 2),
  ('Gafas de Sol', 'Gafas de sol UV protection', 45.00, 30, 5),
  ('Cinturón de Cuero', 'Cinturón de cuero genuino', 35.00, 40, 6),
  
  -- Decoración
  ('Cuadro Decorativo 20x30', 'Impresión sobre madera', 45.00, 20, 1),
  ('Jarrón Cerámica', 'Jarrón de cerámica artesanal', 65.00, 15, 2),
  ('Lámpara de Mesa', 'Lámpara de mesa vintage', 95.00, 8, 1),
  ('Espejo Decorativo', 'Espejo con marco dorado', 110.00, 10, 2),
  ('Cojines Decorativos', 'Set de 2 cojines con diseños', 38.00, 25, 4),
  ('Macetero de Cerámica', 'Macetero artesanal mediano', 28.00, 35, 5),
  
  -- Juguetes
  ('Set de Bloques', 'Set de bloques educativos', 48.00, 22, 4),
  ('Muñeco de Peluche', 'Muñeco de peluche importado', 35.00, 28, 5),
  ('Rompecabezas 1000 piezas', 'Rompecabezas de paisajes', 28.00, 35, 7),
  ('Carro de Control Remoto', 'Carro deportivo RC', 65.00, 18, 3),
  ('Lego Set', 'Set de construcción Lego', 85.00, 12, 2),
  ('Pelota de Fútbol', 'Pelota de fútbol profesional', 32.00, 40, 6),
  
  -- Papelería
  ('Set de Llavero', 'Llavero metálico decorativo', 7.50, 200, 10),
  ('Bloc de Notas Premium', 'Bloc rayado tamaño A5', 12.00, 80, 5),
  ('Set de Plumones', 'Set de 12 plumones de colores', 18.00, 60, 8),
  ('Agenda 2024', 'Agenda de cuero premium', 42.00, 45, 6),
  ('Set de Lápices', 'Set de 24 lápices de colores', 15.00, 70, 10),
  ('Mochila Escolar', 'Mochila escolar resistente', 55.00, 25, 4),
  
  -- Electrónica
  ('Auriculares Bluetooth', 'Auriculares inalámbricos premium', 125.00, 18, 3),
  ('Cargador Universal', 'Cargador rápido universal', 35.00, 50, 8),
  ('Power Bank 20000mAh', 'Batería externa de alta capacidad', 55.00, 25, 5),
  ('Altavoz Bluetooth', 'Altavoz portátil Bluetooth', 75.00, 20, 3),
  ('Cámara Instantánea', 'Cámara instantánea con película', 180.00, 8, 1),
  
  -- Ropa
  ('Camiseta Premium', 'Camiseta de algodón premium', 28.00, 60, 10),
  ('Bufanda de Seda', 'Bufanda 100% seda importada', 45.00, 20, 4),
  ('Gorra Deportiva', 'Gorra deportiva importada', 22.00, 40, 6),
  ('Chaqueta Ligera', 'Chaqueta de mezclilla importada', 85.00, 15, 2),
  ('Zapatillas Casual', 'Zapatillas deportivas casuales', 65.00, 25, 4),
  
  -- Hogar
  ('Set de Utensilios', 'Set de utensilios de cocina', 48.00, 22, 4),
  ('Almohada Ortopédica', 'Almohada viscoelástica', 55.00, 18, 3),
  ('Manta de Piel Sintética', 'Malla decorativa de piel sintética', 75.00, 15, 2),
  ('Organizador de Cajones', 'Organizador modular', 28.00, 40, 6),
  ('Lámpara LED', 'Lámpara LED de escritorio', 42.00, 30, 5)
) AS v(nombre, descripcion, precio, stock, stock_minimo)
JOIN categorias c ON c.nombre = CASE
  WHEN v.nombre ILIKE '%Porta Velas%' OR v.nombre ILIKE '%Caja Regalo%' OR v.nombre ILIKE '%Velas%' OR v.nombre ILIKE '%Cesta%' OR v.nombre ILIKE '%Bolsa%' OR v.nombre ILIKE '%Tarjeta%' THEN 'Regalos'
  WHEN v.nombre ILIKE '%Pulsera%' OR v.nombre ILIKE '%Collar%' OR v.nombre ILIKE '%Aretes%' OR v.nombre ILIKE '%Reloj%' OR v.nombre ILIKE '%Gafas%' OR v.nombre ILIKE '%Cinturón%' THEN 'Accesorios'
  WHEN v.nombre ILIKE '%Cuadro%' OR v.nombre ILIKE '%Jarrón%' OR v.nombre ILIKE '%Lámpara%' OR v.nombre ILIKE '%Espejo%' OR v.nombre ILIKE '%Cojines%' OR v.nombre ILIKE '%Macetero%' THEN 'Decoración'
  WHEN v.nombre ILIKE '%Bloques%' OR v.nombre ILIKE '%Muñeco%' OR v.nombre ILIKE '%Rompecabezas%' OR v.nombre ILIKE '%Carro%' OR v.nombre ILIKE '%Lego%' OR v.nombre ILIKE '%Pelota%' THEN 'Juguetes'
  WHEN v.nombre ILIKE '%Llavero%' OR v.nombre ILIKE '%Bloc%' OR v.nombre ILIKE '%Plumones%' OR v.nombre ILIKE '%Agenda%' OR v.nombre ILIKE '%Lápices%' OR v.nombre ILIKE '%Mochila%' THEN 'Papelería'
  WHEN v.nombre ILIKE '%Auriculares%' OR v.nombre ILIKE '%Cargador%' OR v.nombre ILIKE '%Power Bank%' OR v.nombre ILIKE '%Altavoz%' OR v.nombre ILIKE '%Cámara%' THEN 'Electrónica'
  WHEN v.nombre ILIKE '%Camiseta%' OR v.nombre ILIKE '%Bufanda%' OR v.nombre ILIKE '%Gorra%' OR v.nombre ILIKE '%Chaqueta%' OR v.nombre ILIKE '%Zapatillas%' THEN 'Ropa'
  WHEN v.nombre ILIKE '%Utensilios%' OR v.nombre ILIKE '%Almohada%' OR v.nombre ILIKE '%Manta%' OR v.nombre ILIKE '%Organizador%' OR v.nombre ILIKE '%Lámpara LED%' THEN 'Hogar'
  ELSE 'Regalos' END
WHERE NOT EXISTS (
  SELECT 1 FROM productos p 
  WHERE p.nombre = v.nombre
);

-- IMÁGENES DE PRODUCTOS
INSERT INTO producto_imagenes (url, orden, producto_id)
SELECT '/static/uploads/' || replace(lower(p.nombre),' ','_') || '_1.jpg', 1, p.id FROM productos p
WHERE NOT EXISTS (
  SELECT 1 FROM producto_imagenes pi 
  WHERE pi.producto_id = p.id AND pi.orden = 1
);

-- CLIENTES
INSERT INTO clientes (tipo_documento, numero_documento, nombre, direccion, telefono, email, estado)
SELECT tipo_documento, numero_documento, nombre, direccion, telefono, email, estado
FROM (VALUES
  ('DNI', '12345678', 'María González', 'Av. Principal 123, Lima', '999123456', 'maria.gonzalez@email.com', 1),
  ('RUC', '20123456789', 'Empresa Import S.A.C.', 'Calle Comercio 45, Miraflores', '011223344', 'ventas@empresaimport.pe', 1),
  ('DNI', '87654321', 'Carlos Rodríguez', 'Jr. Secundaria 45, Trujillo', '988776655', 'carlos.rodriguez@email.com', 1),
  ('DNI', '45678901', 'Ana Martínez', 'Av. Brasil 789, Arequipa', '977665544', 'ana.martinez@email.com', 1),
  ('RUC', '20567890123', 'Comercial Global S.A.', 'Av. Industrial 234, Lima', '011334455', 'contacto@comercialglobal.pe', 1),
  ('DNI', '32165498', 'Pedro Sánchez', 'Calle Los Olivos 567, Chiclayo', '944332211', 'pedro.sanchez@email.com', 1)
) AS v(tipo_documento, numero_documento, nombre, direccion, telefono, email, estado)
WHERE NOT EXISTS (
  SELECT 1 FROM clientes c WHERE c.numero_documento = v.numero_documento
);

-- EMPRESA
INSERT INTO empresa (nombre, direccion, telefono, email, logo_url, nosotros, numero_yape, titular_yape)
SELECT nombre, direccion, telefono, email, logo_url, nosotros, numero_yape, titular_yape
FROM (VALUES
  ('Tienda de Importaciones Yo''Max',
  '963 Monseñor Francisco Gonzales, Ferreñafe, Pueblo Nuevo',
  '+51 918 823 760 ',
  'info@yomax.pe',
  '/static/uploads/logo.png',
  'Yo''Max es tu tienda de importaciones favorita, ofreciendo productos de alta calidad traídos de las mejores marcas internacionales. Nos especializamos en juguetes, accesorios, regalos, electrónica, ropa y artículos para el hogar que transforman cualquier espacio en algo especial.',
  '918 823 760 ',
  'YoMax Importaciones')
) AS v(nombre, direccion, telefono, email, logo_url, nosotros, numero_yape, titular_yape)
WHERE NOT EXISTS (
  SELECT 1 FROM empresa e WHERE e.nombre = v.nombre
);

-- VENTAS
INSERT INTO ventas (numero_venta, cliente_id, fecha_venta, metodo_pago, subtotal, descuento, total, nota, tipo_comprobante, estado, origen)
SELECT 'V-2024-001', c.id, NOW() - INTERVAL '7 days', 'Efectivo', 51.00, 0.00, 51.00, 'Venta de mostrador', 'BOLETA', 1, 'pos'
FROM clientes c WHERE c.numero_documento='12345678'
AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.numero_venta = 'V-2024-001');

INSERT INTO ventas (numero_venta, cliente_id, fecha_venta, metodo_pago, subtotal, descuento, total, nota, tipo_comprobante, estado, origen)
SELECT 'V-2024-002', c.id, NOW() - INTERVAL '5 days', 'Tarjeta', 120.00, 0.00, 120.00, 'Pedido especial', 'FACTURA', 1, 'pos'
FROM clientes c WHERE c.numero_documento='20123456789'
AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.numero_venta = 'V-2024-002');

INSERT INTO ventas (numero_venta, cliente_id, fecha_venta, metodo_pago, subtotal, descuento, total, nota, tipo_comprobante, estado, origen)
SELECT 'V-2024-003', c.id, NOW() - INTERVAL '3 days', 'Yape', 48.00, 0.00, 48.00, 'Venta rápida', 'BOLETA', 1, 'pos'
FROM clientes c WHERE c.numero_documento='87654321'
AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.numero_venta = 'V-2024-003');

INSERT INTO ventas (numero_venta, cliente_id, fecha_venta, metodo_pago, subtotal, descuento, total, nota, tipo_comprobante, estado, origen)
SELECT 'V-2024-004', c.id, NOW() - INTERVAL '1 day', 'Efectivo', 110.00, 5.00, 105.00, 'Cliente frecuente', 'BOLETA', 1, 'pos'
FROM clientes c WHERE c.numero_documento='45678901'
AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.numero_venta = 'V-2024-004');

-- DETALLES DE VENTAS
INSERT INTO detalles_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 2, p.precio, (p.precio * 2)
FROM ventas v JOIN productos p ON p.nombre='Porta Velas de Madera'
WHERE v.numero_venta='V-2024-001'
AND NOT EXISTS (
  SELECT 1 FROM detalles_venta dv 
  WHERE dv.venta_id = v.id AND dv.producto_id = p.id
);

INSERT INTO detalles_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, p.precio, p.precio
FROM ventas v JOIN productos p ON p.nombre='Cesta de Regalo Premium'
WHERE v.numero_venta='V-2024-002'
AND NOT EXISTS (
  SELECT 1 FROM detalles_venta dv 
  WHERE dv.venta_id = v.id AND dv.producto_id = p.id
);

INSERT INTO detalles_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, p.precio, p.precio
FROM ventas v JOIN productos p ON p.nombre='Set de Bloques'
WHERE v.numero_venta='V-2024-003'
AND NOT EXISTS (
  SELECT 1 FROM detalles_venta dv 
  WHERE dv.venta_id = v.id AND dv.producto_id = p.id
);

INSERT INTO detalles_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, p.precio, p.precio
FROM ventas v JOIN productos p ON p.nombre='Jarrón Cerámica'
WHERE v.numero_venta='V-2024-004'
AND NOT EXISTS (
  SELECT 1 FROM detalles_venta dv 
  WHERE dv.venta_id = v.id AND dv.producto_id = p.id
);

INSERT INTO detalles_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, p.precio, p.precio
FROM ventas v JOIN productos p ON p.nombre='Cuadro Decorativo 20x30'
WHERE v.numero_venta='V-2024-004'
AND NOT EXISTS (
  SELECT 1 FROM detalles_venta dv 
  WHERE dv.venta_id = v.id AND dv.producto_id = p.id
);

-- PRODUCTOS DESTACADOS PARA EMPRESA (relación many-to-many)
INSERT INTO empresa_productos_destacados (empresa_id, producto_id)
SELECT e.id, p.id
FROM empresa e
CROSS JOIN productos p
WHERE p.nombre IN ('Cesta de Regalo Premium', 'Set de Bloques', 'Auriculares Bluetooth', 'Jarrón Cerámica', 'Lego Set')
AND NOT EXISTS (
  SELECT 1 FROM empresa_productos_destacados epd 
  WHERE epd.empresa_id = e.id AND epd.producto_id = p.id
)
LIMIT 5;
