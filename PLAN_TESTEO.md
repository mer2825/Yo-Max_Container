# Plan de Testing

## 1. Objetivo

Diseñar un plan de validaciones y pruebas para reducir riesgos antes del despliegue en producción. El foco debe ser proteger los flujos críticos del negocio: ventas, inventario, usuarios, caja y conexión con SUNAT.

## 2. Áreas que necesitan más validaciones

### Ventas
- Validar que la venta no venga vacía ni sin detalles.
- Validar que cada producto exista y tenga precio válido.
- Validar cantidad mayor a cero.
- Validar stock suficiente antes de procesar la venta.
- Validar descuento no mayor al subtotal.
- Validar método de pago, tipo de comprobante y origen.
- Validar que el cliente sea obligatorio cuando la regla del negocio lo exija.

Puntos clave:
- [src/main/java/com/example/acceso/service/VentaServiceImpl.java](src/main/java/com/example/acceso/service/VentaServiceImpl.java)
- [src/test/java/com/example/acceso/service/VentaServiceIntegrationTest.java](src/test/java/com/example/acceso/service/VentaServiceIntegrationTest.java)

### Productos
- Nombre obligatorio y sin duplicados.
- Precio mayor a cero.
- Stock y stock mínimo no negativos.
- Categoría obligatoria.
- Validar estado del producto antes de editar, eliminar o activar/desactivar.

Punto clave:
- [src/main/java/com/example/acceso/service/ProductoService.java](src/main/java/com/example/acceso/service/ProductoService.java)

### Usuarios
- Usuario y correo obligatorios.
- Formato de correo válido.
- Contraseña con longitud y complejidad mínima.
- Evitar duplicados de usuario/correo.
- Proteger cambios en usuarios críticos como super administrador.

Punto clave:
- [src/main/java/com/example/acceso/service/UsuarioService.java](src/main/java/com/example/acceso/service/UsuarioService.java)

### Caja
- No permitir abrir más de una sesión a la vez.
- No cerrar una sesión que ya está cerrada.
- Validar montos positivos y no negativos.
- Validar categorías de movimiento y descripciones obligatorias.
- Validar retiros con fondos suficientes.

Punto clave:
- [src/main/java/com/example/acceso/service/CajaServiceImpl.java](src/main/java/com/example/acceso/service/CajaServiceImpl.java)

### Integraciones externas
- SUNAT: manejar respuestas vacías, tiempos de espera, errores de red, documentos rechazados y estados pendiente/aceptado.
- Proteger campos largos para evitar errores de persistencia.

Punto clave:
- [src/main/java/com/example/acceso/service/ApisunatService.java](src/main/java/com/example/acceso/service/ApisunatService.java)

### Controladores y validaciones web
- Aplicar validaciones con `@Valid` y `@NotBlank`, `@Email`, `@Positive` en los formularios.
- Manejar errores de forma consistente con el controlador global.

Puntos clave:
- [src/main/java/com/example/acceso/controller/GlobalExceptionHandler.java](src/main/java/com/example/acceso/controller/GlobalExceptionHandler.java)
- [src/main/java/com/example/acceso/controller/ProductoController.java](src/main/java/com/example/acceso/controller/ProductoController.java)
- [src/main/java/com/example/acceso/controller/UsuarioController.java](src/main/java/com/example/acceso/controller/UsuarioController.java)
- [src/main/java/com/example/acceso/controller/VentaController.java](src/main/java/com/example/acceso/controller/VentaController.java)

## 3. Tipos de pruebas a implementar

### A. Pruebas unitarias
Se usan para validar reglas simples y rápidas.

Ejemplos:
- precio inválido en producto
- stock negativo
- descuento mayor al subtotal
- correo inválido
- usuario duplicado
- sesión de caja ya abierta

### B. Pruebas de integración
Son las más importantes para este proyecto porque cubren la lógica real con la base de datos.

Se recomienda replicar el estilo de [src/test/java/com/example/acceso/service/VentaServiceIntegrationTest.java](src/test/java/com/example/acceso/service/VentaServiceIntegrationTest.java) para:
- crear venta con stock suficiente
- crear venta con stock insuficiente
- crear venta con descuento inválido
- crear producto con nombre duplicado
- crear usuario con correo duplicado
- abrir y cerrar caja

### C. Pruebas de controladores
Validan que el sistema responda correctamente a solicitudes inválidas o no autorizadas.

Ejemplos:
- crear producto con nombre vacío
- enviar venta sin detalles
- intentar acceder sin sesión
- recibir error 400/500 con mensajes claros

### D. Pruebas de regresión
Se deben mantener para asegurar que cambios futuros no rompan flujos críticos.

## 4. Casos de prueba prioritarios

### Prioridad alta
1. Crear venta con stock insuficiente.
2. Crear venta sin productos.
3. Crear producto con precio cero o negativo.
4. Crear usuario con correo duplicado.
5. Abrir caja cuando ya existe una abierta.
6. Cerrar caja con monto declarado menor al esperado.
7. Procesar comprobante SUNAT con error de red.

### Prioridad media
1. Descuento mayor al subtotal.
2. Producto con stock mínimo superior al stock actual.
3. Retiro de caja superior al efectivo disponible.
4. Validar formatos de datos en formularios.

### Prioridad baja
1. Reportes con fechas vacías.
2. Exportaciones de Excel/PDF con datos incompletos.
3. Comportamiento visual de formularios.

## 5. Plan de ejecución recomendado

### Fase 1: base de seguridad
- Implementar validaciones en servicios principales.
- Añadir pruebas para ventas, productos y usuarios.
- Ejecutar pruebas con `./mvnw test`.

### Fase 2: negocio y caja
- Añadir pruebas para caja, movimientos y cierres.
- Probar escenarios de error y límites.

### Fase 3: integración externa
- Mockear respuestas de SUNAT y validar estados.
- Probar idempotencia y errores de red.

### Fase 4: despliegue seguro
- Ejecutar pruebas completas en entorno de staging.
- Confirmar que no existan errores críticos.
- Hacer respaldo de base de datos antes del despliegue.
- Desplegar con monitoreo y logs activos.

## 6. Pasos prácticos para hacerlo bien

1. Empezar por los módulos críticos: ventas, productos, usuarios y caja.
2. Implementar validaciones en el servicio antes que en la interfaz.
3. Añadir tests de escenarios felices y escenarios fallidos.
4. Mantener los tests aislados pero realistas.
5. Ejecutar pruebas en cada cambio importante.
6. No desplegar si algún test crítico falla.
7. Revisar logs y métricas después del despliegue.

## 7. Recomendación final

Para este proyecto, el mejor enfoque es combinar:
- validaciones en servicios
- pruebas de integración como la que ya existe en ventas
- pruebas de controlador para errores de entrada
- una batería de pruebas de regresión para procesos críticos

Este enfoque hará el sistema más seguro, más fácil de mantener y mucho más confiable para producción.

