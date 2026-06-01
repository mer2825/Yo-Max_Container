# 🚀 Guía de Instalación - Sistema de Gestión de Usuarios

## 📋 Requisitos Previos

### 1. Java 21
- **Descargar:** https://adoptium.net/
- **Verificar:** `java -version` (debe mostrar Java 21)

### 2. PostgreSQL
- **Descargar:** https://www.postgresql.org/download/
- **Instalar:** Seguir el instalador por defecto
- **Puerto:** 5432 (configurado en application.properties)

### 3. Maven (Opcional - ya incluido en el proyecto)
- El proyecto incluye Maven Wrapper, no necesitas instalar Maven por separado

## 🔧 Configuración de Base de Datos

### 1. Crear Base de Datos
```sql
-- Abre pgAdmin o tu cliente PostgreSQL
CREATE DATABASE acceso;
```

### 2. Configurar Usuario
El proyecto está configurado con:
- **Usuario:** `postgres`
- **Contraseña:** `mercedes28@`
- **Base de datos:** `acceso`
- **Puerto:** `5432`

Si tu configuración es diferente, edita `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/acceso
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
```

### 3. Ejecutar Scripts SQL
El proyecto incluye archivos SQL:
- `acceso.sql` - Script principal de base de datos
- `NAcc.sql` - Script adicional

**Para ejecutar:**
1. Abre pgAdmin
2. Selecciona la base de datos `acceso`
3. Haz clic derecho → Query Tool
4. Abre y ejecuta el archivo `acceso.sql`
5. Repite con `NAcc.sql` si es necesario

## 🚀 Ejecutar la Aplicación

### Opción 1: Usar Maven Wrapper (Recomendado)
```bash
# Navegar al directorio del proyecto
cd D:\Proyecto_Yomax\acceso

# Ejecutar la aplicación
.\mvnw.cmd spring-boot:run
```

### Opción 2: Usar Maven instalado
```bash
# Si tienes Maven instalado
mvn spring-boot:run
```

## 🌐 Acceder a la Aplicación

Una vez iniciada, la aplicación estará disponible en:
- **URL:** `http://localhost:8080`

## 🔑 Credenciales de Acceso

Las credenciales dependen de los datos en tu base de datos. Revisa la tabla `usuarios`:
```sql
SELECT * FROM usuarios;
```

Si no hay usuarios, puedes crear uno:
```sql
INSERT INTO usuarios (nombre, usuario, clave, correo, activo) 
VALUES ('Administrador', 'admin', 'tu_contraseña', 'admin@ejemplo.com', true);
```

## 📁 Estructura del Proyecto

```
acceso/
├── pom.xml                          # Configuración Maven
├── mvnw.cmd                         # Maven Wrapper (Windows)
├── mvnw                             # Maven Wrapper (Linux/Mac)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/acceso/  # Código fuente
│   │   └── resources/
│   │       ├── application.properties # Configuración
│   │       ├── static/              # Archivos estáticos
│   │       └── templates/           # Plantillas Thymeleaf
│   └── test/                        # Pruebas
├── acceso.sql                       # Script SQL principal
└── NAcc.sql                         # Script SQL adicional
```

## 🛠️ Configuración Importante

### application.properties
- **DDL Auto:** `create` (recrea tablas al iniciar)
- **Show SQL:** `true` (muestra queries en consola)
- **Upload Dir:** `./src/main/resources/static/uploads/`

## 🚨 Solución de Problemas

### Error: "Connection refused"
- Verifica que PostgreSQL esté corriendo
- Revisa el puerto (5432)
- Verifica usuario y contraseña

### Error: "Database does not exist"
- Crea la base de datos `acceso` en PostgreSQL
- Ejecuta los scripts SQL

### Error: "Java version not supported"
- Instala Java 21
- Verifica con `java -version`

### Puerto 8080 ocupado
- Cambia el puerto en application.properties:
```properties
server.port=8081
```

## 📞 Soporte

Si tienes problemas:
1. Verifica que Java 21 esté instalado
2. Verifica que PostgreSQL esté corriendo
3. Revisa los logs de la aplicación
4. Verifica que la base de datos `acceso` exista

---
**¡Listo!** Con estos pasos deberías poder ejecutar el proyecto en tu computadora.
