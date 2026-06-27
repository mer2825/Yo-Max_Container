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

-- Columna sesion_caja_id en tabla ventas
-- Nota: La restricción de clave foránea se debe crear manualmente después de que la tabla ventas tenga datos
-- o ejecutar: ALTER TABLE ventas ADD CONSTRAINT fk_venta_sesion_caja FOREIGN KEY (sesion_caja_id) REFERENCES sesiones_caja(id);
ALTER TABLE ventas
  ADD COLUMN IF NOT EXISTS sesion_caja_id BIGINT;
