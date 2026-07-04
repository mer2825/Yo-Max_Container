# CHECKLIST PARA DESPLIEGUE EN RAILWAY - ACTUALIZADO

## 🔴 CAMBIOS CRÍTICOS DESDE LA VERSIÓN ANTERIOR

Tu aplicación ha sido actualizada con nuevas funcionalidades y configuraciones. Aquí está TODO lo que necesitas considerar:

---

## 1️⃣ VARIABLES DE ENTORNO PARA RAILWAY

### A. Base de Datos (Railway proporciona automáticamente)
```env
DATABASE_URL=postgresql://user:password@host:port/database  # Railway lo proporciona
JPA_DDL_AUTO=validate  # ⚠️ CAMBIAR a 'validate' en producción (no 'update')
JPA_SHOW_SQL=false     # ⚠️ CAMBIAR a 'false' en producción para mejor rendimiento
SQL_INIT_MODE=never    # Nueva config recomendada para producción
```

**Nota importante:** En tu anterior versión tenías `update`, pero para Railway producción es más seguro usar `validate` para evitar cambios automáticos no deseados.

---

### B. Cloudinary (Almacenamiento de imágenes)
```env
CLOUDINARY_CLOUD_NAME=dpk8rajy5
CLOUDINARY_API_KEY=811574978487384
CLOUDINARY_API_SECRET=Ycyx71c6ztXQtHMcp7-VIFIoLgo
CLOUDINARY_FOLDER_ENV=produccion  # ⚠️ IMPORTANTE: cambiar a 'produccion' en Railway
```

**Cambio importante:** Tu código ahora usa `System.getenv()` directamente en `CloudinaryConfig.java`, no las propiedades de Spring. Asegúrate de que las variables estén exactamente nombradas así en Railway.

---

### C. Google reCAPTCHA v2
```env
RECAPTCHA_SITE_KEY=tu_clave_sitio
RECAPTCHA_SECRET_KEY=tu_clave_secreta
RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify
RECAPTCHA_ENABLED=true  # Habilitar en producción
```

**Acción:** Deberías generar nuevas claves de reCAPTCHA para tu dominio en producción si aún no las tienes.

---

### D. API Token Externo (Nuevo servicio)
```env
API_EXTERNAL_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Este token sigue siendo necesario. Verifica que siga siendo válido en tu proveedor externo.

---

### E. ⭐ NUEVA CONFIGURACIÓN: APISUNAT (Facturación Electrónica)
**ESTO ES NUEVO EN TU APLICACIÓN.** Ahora tienes integración con API SUNAT para emitir boletas y facturas electrónicas.

```env
# URLs y tokens para APISUNAT
APISUNAT_URL=https://back.apisunat.com
APISUNAT_TOKEN=DEV_naz8BIm1oDrQh0qqMlsNr6NiGdRblW9aI1KIMo01GSKSLgJrLYECYBAiN8nsjEA7
APISUNAT_PERSONA_ID=6a39bbb50154ec0029769a4a
APISUNAT_PERSONA_TOKEN=DEV_naz8BIm1oDrQh0qqMlsNr6NiGdRblW9aI1KIMo01GSKSLgJrLYECYBAiN8nsjEA7
APISUNAT_RUC=20556548745
APISUNAT_DOCUMENTS_URI=/personas/v1/sendBill
```

**⚠️ IMPORTANTE:**
- Estos son valores de DESARROLLO. Necesitas credenciales de PRODUCCIÓN de APISUNAT
- Contacta con APISUNAT para obtener tokens y IDs de producción
- Cambiar de URLs DEV a URLs de producción
- Sin estas variables, la funcionalidad de facturación electrónica fallará

---

### F. Configuración de Email (Opcional - Actualmente Comentada)
Si necesitas enviar emails desde tu aplicación:
```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu_email@gmail.com
MAIL_PASSWORD=tu_app_password  # Usar App Password, no contraseña normal
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_SSL_TRUST=*
```

Si NO la usas, puedes dejarla comentada en `application.properties`.

---

### G. Configuración de Puertos y Archivos
```env
PORT=8080  # Railway asigna automáticamente, pero esto es el default
FILE_UPLOAD_DIR=/app/uploads  # IMPORTANTE para persistencia
```

---

## 2️⃣ NUEVAS PROPIEDADES EN application.properties

Tu versión actual incluye nuevas configuraciones:

```properties
# NUEVA: Serialización de fechas como ISO-8601 (no timestamps)
spring.jackson.serialization.write-dates-as-timestamps=false

# NUEVA: Configuración de encoding UTF-8
spring.http.encoding.charset=UTF-8
spring.http.encoding.enabled=true
spring.http.encoding.force=true

# NUEVA: Content negotiation
spring.mvc.contentnegotiation.favor-parameter=false
```

**Estas son AUTOMÁTICAS.** No requieren variables de entorno, pero confirma que están en tu `application.properties`.

---

## 3️⃣ DEPENDENCIAS CRÍTICAS A VERIFICAR

Tu `pom.xml` incluye:

```xml
<!-- PostgreSQL Driver - REQUERIDO -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Cloudinary - REQUERIDO -->
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http44</artifactId>
    <version>1.32.2</version>
</dependency>

<!-- OpenPDF para PDFs - REQUERIDO para boletas/facturas -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.30</version>
</dependency>

<!-- Apache POI para Excel - REQUERIDO para reportes -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

**Java versión requerida:** 17 (especificado en pom.xml)

Railway debe usar Java 17 o superior. Verifica en la configuración del buildpack.

---

## 4️⃣ PERSISTENCIA DE ARCHIVOS EN RAILWAY

**⚠️ CRÍTICO:** Railway borra archivos en `/app` cuando hace redeploy. Necesitas:

### Opción 1: Usar Cloudinary (RECOMENDADO)
- Ya tienes integración con Cloudinary
- Los archivos se guardan en la nube (permanentes)
- No necesitas configurar volumen en Railway

### Opción 2: Usar Volumen de Railway
1. En el dashboard de Railway, ve a tu servicio de aplicación
2. Ve a la pestaña "Data"
3. Crea un volumen llamado `uploads` montado en `/app/uploads`
4. Configura `FILE_UPLOAD_DIR=/app/uploads` en variables de entorno

**Mi recomendación:** Usa Cloudinary (opción 1) porque:
- No necesitas configurar volúmenes
- Las imágenes se sirven rápidamente con CDN
- Escalable automáticamente
- Ya está integrado en tu código

---

## 5️⃣ BASE DE DATOS EN RAILWAY

### Pasos para configurar:

1. **En el dashboard de Railway:**
   - Haz clic en "New" → "Database" → "PostgreSQL"
   - Railway generará automáticamente `DATABASE_URL`

2. **La variable `DATABASE_URL` será generada automáticamente** en formato:
   ```
   postgresql://username:password@hostname:port/database_name
   ```

3. **Spring detecta automáticamente** esta variable y la usa

4. **Ejecutar scripts SQL iniciales:**
   - Si necesitas datos iniciales, coloca en `src/main/resources/data.sql`
   - Establece `SQL_INIT_MODE=always` para desarrollo o `never` para producción

---

## 6️⃣ CHECKLIST DE DESPLIEGUE PASO A PASO

### Antes de desplegar:

- [ ] Clonar/conectar repositorio a Railway
- [ ] Crear servicio PostgreSQL en Railway (generará DATABASE_URL)
- [ ] **CAMBIAR** `JPA_DDL_AUTO` de `update` a `validate` en variables
- [ ] **CAMBIAR** `JPA_SHOW_SQL` a `false` en producción
- [ ] **CAMBIAR** `CLOUDINARY_FOLDER_ENV` a `produccion`
- [ ] Obtener credenciales de APISUNAT para PRODUCCIÓN (no usar DEV)
- [ ] Verificar claves de reCAPTCHA para tu dominio
- [ ] Confirmar que la versión de Java es 17+
- [ ] Revisar que no haya secretos en el repositorio (.env no incluido en git)

### Durante despliegue:

- [ ] Configurar todas las variables de entorno en Railway
- [ ] Revisar logs para errores de conexión a BD
- [ ] Verificar que Cloudinary se conecta correctamente
- [ ] Probar funcionalidad de facturación (APISUNAT)

### Después de desplegar:

- [ ] Probar login y navegación
- [ ] Probar subida de imágenes (Cloudinary)
- [ ] Probar emisión de boletas/facturas (APISUNAT)
- [ ] Revisar logs en Railway para errores
- [ ] Monitorear base de datos

---

## 7️⃣ VARIABLES DE ENTORNO COMPLETAS PARA COPIAR A RAILWAY

```env
# === BASE DE DATOS (Railway la proporciona, pero confirma) ===
DATABASE_URL=postgresql://...  # Generado por Railway automáticamente
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false
SQL_INIT_MODE=never

# === CLOUDINARY ===
CLOUDINARY_CLOUD_NAME=dpk8rajy5
CLOUDINARY_API_KEY=811574978487384
CLOUDINARY_API_SECRET=Ycyx71c6ztXQtHMcp7-VIFIoLgo
CLOUDINARY_FOLDER_ENV=produccion

# === RECAPTCHA ===
RECAPTCHA_SITE_KEY=tu_clave_sitio
RECAPTCHA_SECRET_KEY=tu_clave_secreta
RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify
RECAPTCHA_ENABLED=true

# === API EXTERNA ===
API_EXTERNAL_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# === APISUNAT (NUEVA) - USA CREDENCIALES DE PRODUCCIÓN ===
APISUNAT_URL=https://back.apisunat.com
APISUNAT_TOKEN=PROD_xxx...
APISUNAT_PERSONA_ID=xxx
APISUNAT_PERSONA_TOKEN=PROD_xxx...
APISUNAT_RUC=20556548745
APISUNAT_DOCUMENTS_URI=/personas/v1/sendBill

# === PUERTO (Railway lo maneja automáticamente) ===
PORT=8080

# === ARCHIVOS (si no usas Cloudinary) ===
FILE_UPLOAD_DIR=/app/uploads
```

---

## 8️⃣ DIFERENCIAS KEY VS VERSIÓN ANTERIOR

| Elemento | Anterior | Actual | Acción en Railway |
|----------|----------|--------|-------------------|
| Base de Datos | PostgreSQL local | PostgreSQL Railway | Railway lo proporciona |
| Cloudinary | ✅ Sí | ✅ Sí (same) | Mantener credenciales |
| reCAPTCHA | ✅ Sí | ✅ Sí (same) | Verificar claves producción |
| APISUNAT | ❌ No | ✅ SÍ (NEW) | ⚠️ **Obtener credenciales PROD** |
| Serialización JSON | ❌ No | ✅ Sí (NEW) | Automático |
| Email | Comentado | Comentado (same) | No requiere cambios |
| JPA_DDL_AUTO | `update` | `update` (default) | ⚠️ **Cambiar a `validate`** |

---

## 9️⃣ POTENCIALES PROBLEMAS Y SOLUCIONES

### ❌ Error: "APISUNAT connection refused"
**Causa:** Credenciales de APISUNAT no configuradas o inválidas
**Solución:** Obtén tokens de PRODUCCIÓN, no uses valores DEV

### ❌ Error: "Cloudinary authentication failed"
**Causa:** Variables de Cloudinary no encontradas
**Solución:** Verifica que están exactamente nombradas en Railway como `CLOUDINARY_CLOUD_NAME`, etc.

### ❌ Error: "DATABASE_URL not found"
**Causa:** No creaste servicio PostgreSQL en Railway
**Solución:** Agrega base de datos desde dashboard de Railway

### ❌ Archivos se pierden al redeploy
**Causa:** No configuraste volumen o no usas Cloudinary
**Solución:** Usa Cloudinary o configura volumen `/app/uploads`

### ❌ Error: "Java version mismatch"
**Causa:** Railway usa Java < 17
**Solución:** En settings de Railway, especifica Java 17+

---

## 🔟 COMANDOS ÚTILES PARA TESTING LOCAL

```bash
# Ejecutar localmente con variables de entorno
export DATABASE_URL="postgresql://localhost:5432/acceso"
export CLOUDINARY_CLOUD_NAME="dpk8rajy5"
export CLOUDINARY_API_KEY="811574978487384"
export CLOUDINARY_API_SECRET="..."
export APISUNAT_URL="https://back.apisunat.com"
# ... otras variables

./mvnw spring-boot:run
```

---

## 📋 RESUMEN EJECUTIVO

**Tu aplicación tiene 3 cambios principales desde la versión anterior:**

1. **Nueva integración APISUNAT** → Necesitas credenciales de PRODUCCIÓN
2. **Mejor serialización JSON** → Automático, sin cambios necesarios
3. **Java 17 requerido** → Verifica que Railway use Java 17+

**Lo más importante para Railway:**
- ✅ DATABASE_URL (Railway la proporciona)
- ✅ Cloudinary (cambiar folder a 'produccion')
- ✅ APISUNAT (obtener credenciales PROD)
- ✅ Cambiar JPA_DDL_AUTO a 'validate'

**Tiempo estimado de configuración:** 15-30 minutos
