# Módulo Compacto - Reporte de Terminalización de Caja

## Descripción

El módulo **Compacto** es un nuevo reporte gerencial que muestra el flujo de dinero que entró a caja por día, desglosado por método de pago (Efectivo, Yape y Otros). Este módulo está diseñado para la terminalización de caja y proporciona una visión clara y concisa de los ingresos diarios.

## Características

- **Visualización por día**: Muestra el flujo de dinero día por día en un período seleccionado
- **Desglose por método de pago**:
  - Efectivo (color verde)
  - Yape (color azul)
  - Otros métodos de pago (color amarillo)
- **KPIs principales**:
  - Total General
  - Total Efectivo
  - Total Yape
  - Total Otros
- **Exportación**: Disponible en PDF y Excel
- **Filtrado por período**: Selector de fechas personalizable

## Estructura del Módulo

### Backend (Java/Spring Boot)

#### 1. DTOs
- **`ReporteCompactoDTO.java`**: Contiene la estructura de datos del reporte
  - `desde`, `hasta`: Rango de fechas
  - `totalGeneral`, `totalEfectivo`, `totalYape`, `totalOtros`: Totales por método de pago
  - `detallePorDia`: Lista de mapas con el desglose diario

#### 2. Servicios
- **`ReporteCompactoService.java`**: Interfaz del servicio
- **`ReporteCompactoServiceImpl.java`**: Implementación que:
  - Obtiene ventas del período (solo activas, estado != 2)
  - Calcula totales por método de pago
  - Genera detalle día por día
  - Identifica métodos de pago (efectivo, yape, otros)

#### 3. Controlador
- **`CompactoController.java`**: Maneja las rutas del módulo
  - `GET /compacto` - Página principal del reporte
  - `GET /compacto/reporte` - Reporte con rango de fechas
  - `GET /compacto/pdf` - Exportación a PDF
  - `GET /compacto/excel` - Exportación a Excel

#### 4. Exportación
- **`ReporteExportService.java`**: Métodos agregados
  - `exportarCompactoPdf()`: Genera PDF con KPIs y tabla de detalle
  - `exportarCompactoExcel()`: Genera Excel con hoja de resumen y detalle por día

### Frontend (Thymeleaf/Bootstrap)

#### Template
- **`reportes/compacto.html`**: Vista principal del reporte
  - Encabezado con título y botones de exportación
  - Selector de período con fechas
  - KPIs en cards con iconos y colores distintivos
  - Tabla de detalle por día con formato de moneda

#### Navegación
- **`topbar.html`**: Actualizado con dropdown en "Reportes"
  - Opción 1: "Reportes Gerenciales" (ruta `/reportes`)
  - Opción 2: "Compacto (Terminalización)" (ruta `/compacto`)

## Integración en el Proyecto

### 1. Arquitectura
El módulo sigue la arquitectura MVC del proyecto:
- **Modelo**: Usa el modelo `Venta` existente
- **Vista**: Template Thymeleaf independiente
- **Controlador**: Controlador dedicado con inyección de dependencias

### 2. Dependencias
El módulo aprovecha las dependencias existentes:
- Spring Boot Web
- Thymeleaf
- Apache POI (Excel)
- iText (PDF)
- Bootstrap 5

### 3. Repositorios Utilizados
- **`VentaRepository`**: Para obtener las ventas del período
  - `findByFechaVentaBetween()`: Consulta principal

## Métodos de Pago Soportados

### Efectivo
El sistema reconoce como efectivo los siguientes términos (case-insensitive):
- "efectivo"
- "cash"
- "ef"
- "contado"

### Yape
El sistema reconoce como Yape los siguientes términos (case-insensitive):
- "yape"
- "yapeplin"

### Otros
Cualquier método de pago que no sea efectivo ni Yape se clasifica como "Otros".

## Uso del Módulo

### Acceso
1. Iniciar sesión como administrador
2. En el menú superior, hacer clic en "Reportes"
3. Seleccionar "Compacto (Terminalización)" del dropdown

### Filtrado
- **Ver mes actual**: Botón rápido para ver el mes en curso
- **Período personalizado**: Seleccionar fecha desde y hasta, luego clic en "Filtrar"

### Exportación
- **PDF**: Genera documento con KPIs y tabla de detalle
- **Excel**: Genera archivo con hoja de resumen y hoja de detalle por día

## Buenas Prácticas Implementadas

1. **Separación de responsabilidades**: DTOs, Servicios, Controladores separados
2. **Inyección de dependencias**: Uso de constructor para inyección
3. **Interfaces**: Servicio con interfaz para desacoplamiento
4. **Naming conventions**: Nombres descriptivos en español siguiendo el estándar del proyecto
5. **Manejo de fechas**: Validación y normalización de rangos de fechas
6. **Filtrado de datos**: Solo se consideran ventas activas (estado != 2)
7. **Formato consistente**: Uso de `BigDecimal` para valores monetarios
8. **Estilos reutilizables**: Clases CSS consistentes con el resto del proyecto
9. **Responsive design**: Adaptable a dispositivos móviles
10. **Accesibilidad**: Uso de iconos Bootstrap Icons

## Notas Técnicas

- El reporte muestra días con ventas en el rango seleccionado
- Los totales se calculan en tiempo real desde la base de datos
- El formato de moneda es S/ (Soles peruanos)
- Las fechas se muestran en formato dd/MM/yyyy
- Los colores en la interfaz ayudan a distinguir rápidamente los métodos de pago

## Posibles Extensiones Futuras

- Agregar más métodos de pago específicos (Tarjeta, Transferencia, etc.)
- Incluir gráficos de tendencia
- Comparativa entre períodos
- Exportación a CSV
- Filtro por método de pago específico
- Incluir datos de sesiones de caja para conciliación

## Mantenimiento

### Agregar nuevo método de pago
Para agregar un nuevo método de pago reconocido:

1. Modificar el método correspondiente en `ReporteCompactoServiceImpl.java`:
   - `esEfectivo()`: Agregar nuevo término
   - `esYape()`: Agregar nuevo término
   - O crear un nuevo método para el nuevo tipo

2. Actualizar el DTO si se necesita un campo específico

3. Modificar el template para mostrar el nuevo método de pago

### Modificar rango de fechas por defecto
En `CompactoController.java`, modificar:
```java
LocalDate primerDia = hoy.withDayOfMonth(1); // Cambiar a .minusDays(30) para últimos 30 días
```

## Autor
Módulo desarrollado siguiendo las buenas prácticas y estándares del proyecto Yo-Max Container.

## Fecha de Implementación
Julio 2026