-- Script SQL para crear las tablas del sistema de pedidos web
-- Base de datos: PostgreSQL

-- Tabla: pedidos_web
CREATE TABLE IF NOT EXISTS pedidos_web (
    id BIGSERIAL PRIMARY KEY,
    numero_pedido VARCHAR(20) UNIQUE NOT NULL,
    cliente_id BIGINT NOT NULL,
    nombre_cliente VARCHAR(100) NOT NULL,
    dni_cliente VARCHAR(20) NOT NULL,
    telefono_cliente VARCHAR(20) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    descuento NUMERIC(10, 2),
    total NUMERIC(10, 2) NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    voucher_imagen VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo VARCHAR(500),
    fecha_pedido TIMESTAMP NOT NULL,
    fecha_verificacion TIMESTAMP,
    verificado_por BIGINT,
    venta_id BIGINT,
    creado_por_usuario_id BIGINT,
    fecha_creacion TIMESTAMP,
    modificado_por_usuario_id BIGINT,
    fecha_modificacion TIMESTAMP,
    ultima_accion VARCHAR(255),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_pedido_verificador FOREIGN KEY (verificado_por) REFERENCES usuarios(id),
    CONSTRAINT fk_pedido_venta FOREIGN KEY (venta_id) REFERENCES ventas(id)
);

-- Tabla: detalles_pedido_web
CREATE TABLE IF NOT EXISTS detalles_pedido_web (
    id BIGSERIAL PRIMARY KEY,
    pedido_web_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_unitario NUMERIC(10, 2) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (pedido_web_id) REFERENCES pedidos_web(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_pedidos_web_cliente ON pedidos_web(cliente_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_web_estado ON pedidos_web(estado);
CREATE INDEX IF NOT EXISTS idx_pedidos_web_fecha ON pedidos_web(fecha_pedido);
CREATE INDEX IF NOT EXISTS idx_detalles_pedido_web_pedido ON detalles_pedido_web(pedido_web_id);

-- Comentario sobre la tabla pedidos_web
COMMENT ON TABLE pedidos_web IS 'Tabla para almacenar pedidos web con verificación de pago por Yape';
COMMENT ON COLUMN pedidos_web.estado IS 'Estados: PENDIENTE, EN_REVISION, APROBADO, RECHAZADO, PROCESADO';
COMMENT ON COLUMN pedidos_web.voucher_imagen IS 'Ruta del archivo de imagen del voucher de pago Yape';

-- Comentario sobre la tabla detalles_pedido_web
COMMENT ON TABLE detalles_pedido_web IS 'Tabla para almacenar los detalles (items) de cada pedido web';
