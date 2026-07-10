-- Migración aditiva para facturación electrónica y datos de empresa
-- Estas sentencias usan IF NOT EXISTS para no romper arranques repetidos.

ALTER TABLE ventas
  ADD COLUMN IF NOT EXISTS serie_correlativo VARCHAR(50),
  ADD COLUMN IF NOT EXISTS estado_sunat VARCHAR(50),
  ADD COLUMN IF NOT EXISTS cdr_sunat TEXT,
  ADD COLUMN IF NOT EXISTS pdf_url VARCHAR(255),
  ADD COLUMN IF NOT EXISTS xml_url VARCHAR(255),
  ADD COLUMN IF NOT EXISTS hash_cdr VARCHAR(255),
  ADD COLUMN IF NOT EXISTS nubefact_id VARCHAR(255);

ALTER TABLE ventas
  ALTER COLUMN tipo_comprobante SET DEFAULT 'nota_venta';

ALTER TABLE clientes
  ADD COLUMN IF NOT EXISTS ruc VARCHAR(11),
  ADD COLUMN IF NOT EXISTS razon_social VARCHAR(255),
  ADD COLUMN IF NOT EXISTS direccion_fiscal VARCHAR(255);

ALTER TABLE empresa
  ADD COLUMN IF NOT EXISTS ruc_empresa VARCHAR(11),
  ADD COLUMN IF NOT EXISTS razon_social_empresa VARCHAR(255),
  ADD COLUMN IF NOT EXISTS direccion_empresa VARCHAR(255),
  ADD COLUMN IF NOT EXISTS serie_boleta VARCHAR(20) DEFAULT 'B001',
  ADD COLUMN IF NOT EXISTS serie_factura VARCHAR(20) DEFAULT 'F001',
  ADD COLUMN IF NOT EXISTS correlativo_boleta INTEGER DEFAULT 1,
  ADD COLUMN IF NOT EXISTS correlativo_factura INTEGER DEFAULT 1,
  ADD COLUMN IF NOT EXISTS nubefact_ambiente VARCHAR(20) DEFAULT 'demo';

-- Migración aditiva para módulo de Monitor de Caja
-- Tabla sesiones_caja
CREATE TABLE IF NOT EXISTS sesiones_caja (
  id BIGSERIAL PRIMARY KEY,
  fecha_apertura TIMESTAMP NOT NULL,
  fecha_cierre TIMESTAMP,
  monto_inicial DECIMAL(10,2) NOT NULL,
  monto_cierre_declarado DECIMAL(10,2),
  monto_cierre_esperado DECIMAL(10,2),
  diferencia DECIMAL(10,2),
  motivo_diferencia VARCHAR(500),
  observaciones VARCHAR(1000),
  estado VARCHAR(20) NOT NULL,
  usuario_apertura_id BIGINT NOT NULL,
  usuario_cierre_id BIGINT,
  saldo_traspasado DECIMAL(10,2),
  CONSTRAINT fk_sesion_usuario_apertura FOREIGN KEY (usuario_apertura_id) REFERENCES usuarios(id),
  CONSTRAINT fk_sesion_usuario_cierre FOREIGN KEY (usuario_cierre_id) REFERENCES usuarios(id)
);

-- Tabla movimientos_caja
CREATE TABLE IF NOT EXISTS movimientos_caja (
  id BIGSERIAL PRIMARY KEY,
  sesion_id BIGINT NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  monto DECIMAL(10,2) NOT NULL,
  motivo VARCHAR(500) NOT NULL,
  categoria VARCHAR(50) NOT NULL DEFAULT 'Sin categoría',
  fecha TIMESTAMP NOT NULL,
  usuario_id BIGINT NOT NULL,
  CONSTRAINT fk_movimiento_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones_caja(id),
  CONSTRAINT fk_movimiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Migración aditiva para flujo de Notas de Crédito
CREATE TABLE IF NOT EXISTS notas_credito (
  id BIGSERIAL PRIMARY KEY,
  venta_id BIGINT NOT NULL,
  tipo_nota VARCHAR(10) NOT NULL, -- códigos SUNAT: 01, 06, 07, 09
  descripcion_tipo VARCHAR(255),
  motivo VARCHAR(1000) NOT NULL,
  serie VARCHAR(20),
  correlativo INTEGER,
  serie_correlativo VARCHAR(50),
  total_acreditado DECIMAL(10,2),
  estado_sunat VARCHAR(50) NOT NULL DEFAULT 'pendiente', -- pendiente, aceptado, rechazado
  nubefact_id VARCHAR(255),
  pdf_url VARCHAR(255),
  xml_url VARCHAR(255),
  hash_cdr VARCHAR(255),
  fecha_emision TIMESTAMP,
  emitida_por_usuario_id BIGINT,
  raw_response TEXT,
  CONSTRAINT fk_nc_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
  CONSTRAINT fk_nc_emitida_usuario FOREIGN KEY (emitida_por_usuario_id) REFERENCES usuarios(id)
);

-- Columna sesion_caja_id en tabla ventas
ALTER TABLE ventas
  ADD COLUMN IF NOT EXISTS sesion_caja_id BIGINT;

-- Columna para mostrar el estado de Nota de Crédito en el listado de ventas
ALTER TABLE ventas
  ADD COLUMN IF NOT EXISTS estado_nota_credito VARCHAR(20);

-- Correlativos para Notas de Crédito (boleta / factura)
ALTER TABLE empresa
  ADD COLUMN IF NOT EXISTS correlativo_nota_credito_boleta INTEGER DEFAULT 1,
  ADD COLUMN IF NOT EXISTS correlativo_nota_credito_factura INTEGER DEFAULT 1;

-- Migración aditiva para Cambios de Producto (cruce con Nota de Crédito)
CREATE TABLE IF NOT EXISTS cambios_producto (
  id BIGSERIAL PRIMARY KEY,
  venta_original_id BIGINT NOT NULL,
  detalle_venta_original_id BIGINT NOT NULL,
  producto_nuevo_id BIGINT NOT NULL,
  cantidad_devuelta INTEGER NOT NULL,
  cantidad_nuevo_producto INTEGER NOT NULL,
  nota_credito_id BIGINT NOT NULL,
  venta_excedente_id BIGINT,
  monto_nota_credito DECIMAL(10,2) NOT NULL,
  monto_producto_nuevo DECIMAL(10,2) NOT NULL,
  monto_excedente DECIMAL(10,2) NOT NULL,
  motivo VARCHAR(500),
  estado VARCHAR(20) NOT NULL,
  fecha_cambio TIMESTAMP NOT NULL,
  usuario_id BIGINT,

  CONSTRAINT fk_cambios_venta_original FOREIGN KEY (venta_original_id) REFERENCES ventas(id),
  CONSTRAINT fk_cambios_detalle_venta_original FOREIGN KEY (detalle_venta_original_id) REFERENCES detalles_venta(id),
  CONSTRAINT fk_cambios_producto_nuevo FOREIGN KEY (producto_nuevo_id) REFERENCES productos(id),
  CONSTRAINT fk_cambios_nota_credito FOREIGN KEY (nota_credito_id) REFERENCES notas_credito(id),
  CONSTRAINT fk_cambios_venta_excedente FOREIGN KEY (venta_excedente_id) REFERENCES ventas(id),
  CONSTRAINT fk_cambios_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Agregar columna email_cliente a pedidos_web para recibir el correo del cliente en checkout
ALTER TABLE pedidos_web
  ADD COLUMN IF NOT EXISTS email_cliente VARCHAR(255);

-- Índices para optimizar consultas del Monitor de Caja
CREATE INDEX IF NOT EXISTS idx_ventas_sesion_caja_id ON ventas(sesion_caja_id);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha_venta ON ventas(fecha_venta);
CREATE INDEX IF NOT EXISTS idx_ventas_metodo_pago ON ventas(metodo_pago);
CREATE INDEX IF NOT EXISTS idx_ventas_estado ON ventas(estado);

CREATE INDEX IF NOT EXISTS idx_movimientos_sesion_id ON movimientos_caja(sesion_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_caja(fecha);
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo ON movimientos_caja(tipo);

CREATE INDEX IF NOT EXISTS idx_sesiones_estado ON sesiones_caja(estado);
CREATE INDEX IF NOT EXISTS idx_sesiones_fecha_apertura ON sesiones_caja(fecha_apertura);
CREATE INDEX IF NOT EXISTS idx_sesiones_fecha_cierre ON sesiones_caja(fecha_cierre);

CREATE INDEX IF NOT EXISTS idx_notas_credito_venta_id ON notas_credito(venta_id);
CREATE INDEX IF NOT EXISTS idx_notas_credito_fecha_emision ON notas_credito(fecha_emision);
