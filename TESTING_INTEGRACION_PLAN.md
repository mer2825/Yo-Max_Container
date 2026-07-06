# Plan de integración de testing y validaciones para producción

## Objetivo

Integrar pruebas y validaciones de forma ordenada para reducir riesgos antes del despliegue en producción y garantizar que los procesos críticos del sistema funcionen correctamente.

## Fase 1: Priorizar módulos críticos

Empezar por los módulos que más impactan el negocio:

1. Ventas
2. Productos
3. Usuarios
4. Caja
5. Integración con SUNAT

## Fase 2: Fortalecer validaciones en servicios

Las validaciones más importantes deben implementarse en los servicios, porque ahí está la lógica de negocio real.

### Ventas
- Validar que la venta no esté vacía.
- Validar que exista al menos un detalle.
- Validar cantidad mayor a cero.
- Validar que el producto exista.
- Validar stock suficiente antes de procesar.
- Validar descuento no mayor al subtotal.
- Validar comprobante y método de pago.

### Productos
- Nombre obligatorio.
- Precio mayor a cero.
- Stock y stock mínimo no negativos.
- Categoría obligatoria.
- Evitar nombres duplicados.

### Usuarios
- Nombre obligatorio.
- Usuario obligatorio.
- Correo obligatorio y con formato válido.
- Contraseña mínima y válida.
- Evitar duplicados de usuario y correo.

### Caja
- No permitir abrir más de una sesión a la vez.
- No permitir cerrar una sesión ya cerrada.
- Validar montos positivos.
- Validar categoría y motivo de movimiento.
- Validar retiros con fondos suficientes.

### SUNAT
- Manejar errores de red.
- Manejar respuestas vacías o incompletas.
- Manejar estados rechazado, pendiente y aceptado.
- Proteger campos largos para evitar errores de persistencia.

## Fase 3: Añadir pruebas por nivel

### 3.1 Pruebas de integración

Son las más importantes para este proyecto porque prueban flujos reales con la base de datos.

Recomendación:
- Mantener el estilo usado en los tests actuales de servicios.
- Crear pruebas para escenarios felices y escenarios fallidos.

### 3.2 Pruebas de controladores

Agregar tests para validar respuestas HTTP ante entradas inválidas.

Ejemplos:
- crear producto con nombre vacío
- enviar venta sin detalles
- intentar acceder a recursos sin autorización
- error 400 o 500 con mensaje claro

### 3.3 Pruebas de regresión

Crear una suite mínima que se ejecute antes de cada despliegue.

Casos críticos a conservar:
- crear venta correcta
- venta con stock insuficiente
- crear producto válido
- producto inválido
- crear usuario válido
- usuario duplicado
- abrir/cerrar caja
- flujo de SUNAT

## Fase 4: Orden de implementación recomendado

1. Ventas
2. Productos
3. Usuarios
4. Caja
5. Controladores
6. Integración SUNAT
7. Pruebas de regresión

## Fase 5: Ejecución práctica

Después de cada bloque de cambios:

1. Ejecutar las pruebas:
   - ./mvnw test
2. Revisar fallos.
3. Corregir la causa real.
4. Repetir hasta que la suite esté estable.

## Fase 6: Criterios para considerar el sistema listo

El sistema puede considerarse más seguro para producción cuando:

- no hay pruebas críticas fallando
- los flujos principales pasan correctamente
- los errores de negocio se manejan con mensajes claros
- las operaciones sensibles no aceptan datos inválidos
- la integración con terceros se maneja sin romper el flujo principal

## Recomendación final

El mejor camino es combinar:
- validaciones fuertes en servicios
- pruebas de integración realistas
- pruebas de controladores
- pruebas de regresión antes de despliegue

Esto hará el sistema más estable, más mantenible y más seguro para producción.
