# Configuración de Variables de Entorno para Railway

Este documento explica cómo configurar las variables de entorno necesarias para desplegar la aplicación en Railway.

## Variables de Entorno Requeridas

Railway proporciona automáticamente la variable `DATABASE_URL` cuando creas una base de datos PostgreSQL. Debes configurar las siguientes variables en el dashboard de Railway:

### Base de Datos (Railway proporciona DATABASE_URL automáticamente)
- `DATABASE_URL`: Proporcionada automáticamente por Railway cuando agregas un servicio PostgreSQL
- `DATABASE_USERNAME`: Usuario de la base de datos (opcional, Railway lo maneja)
- `DATABASE_PASSWORD`: Contraseña de la base de datos (opcional, Railway lo maneja)

### Configuración de JPA
- `JPA_DDL_AUTO`: `update` (para producción) o `validate` (para evitar cambios en esquema)
- `JPA_SHOW_SQL`: `false` (para producción) o `true` (para desarrollo)

### Directorio de Carga de Archivos
- `FILE_UPLOAD_DIR`: `/app/uploads` (para producción) o `./src/main/resources/static/uploads/` (para desarrollo)

### Google reCAPTCHA v2 (Opcional)
- `RECAPTCHA_SITE_KEY`: Clave de sitio de reCAPTCHA
- `RECAPTCHA_SECRET_KEY`: Clave secreta de reCAPTCHA
- `RECAPTCHA_VERIFY_URL`: `https://www.google.com/recaptcha/api/siteverify`
- `RECAPTCHA_ENABLED`: `true` o `false`

### API Token para Servicio Externo
- `API_EXTERNAL_TOKEN`: Token de autenticación para el servicio externo de clientes

### Configuración de Email (Opcional)
- `MAIL_HOST`: Servidor SMTP (ej: smtp.gmail.com)
- `MAIL_PORT`: Puerto SMTP (ej: 587)
- `MAIL_USERNAME`: Usuario de email
- `MAIL_PASSWORD`: Contraseña o app password de email

## Pasos para Configurar en Railway

1. **Crear el proyecto en Railway**
   - Conecta tu repositorio de GitHub a Railway
   - Railway detectará automáticamente que es una aplicación Spring Boot

2. **Agregar Base de Datos PostgreSQL**
   - En el dashboard de Railway, agrega un servicio "PostgreSQL"
   - Railway proporcionará automáticamente la variable `DATABASE_URL`

3. **Configurar Variables de Entorno**
   - Ve a la pestaña "Variables" en tu servicio de aplicación
   - Agrega las variables de entorno necesarias:
     ```
     JPA_DDL_AUTO=update
     JPA_SHOW_SQL=false
     FILE_UPLOAD_DIR=/app/uploads
     RECAPTCHA_ENABLED=false
     API_EXTERNAL_TOKEN=tu_token_aqui
     ```

4. **Configurar Persistencia de Archivos**
   - Para que los archivos subidos persistan, necesitas configurar un volumen en Railway
   - Agrega un volumen en la configuración del servicio y monta en `/app/uploads`

5. **Desplegar**
   - Railway desplegará automáticamente cuando hagas push a GitHub
   - O puedes hacer un deploy manual desde el dashboard

## Notas Importantes

- Railway proporciona automáticamente `DATABASE_URL` en formato: `postgresql://user:password@host:port/database`
- Para producción, establece `JPA_DDL_AUTO=validate` para evitar cambios no deseados en el esquema
- Establece `JPA_SHOW_SQL=false` en producción para mejorar el rendimiento y seguridad
- Los archivos subidos se perderán si no configuras un volumen persistente en Railway
