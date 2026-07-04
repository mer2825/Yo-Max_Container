# GUÍA RÁPIDA: DESPLIEGUE EN RAILWAY EN 5 PASOS

## Paso 1: Conectar repositorio en Railway
1. Ve a https://railway.app
2. Crea cuenta o inicia sesión
3. Clic en "New Project" → "Deploy from GitHub"
4. Selecciona tu repositorio `Yo-Max_Container`
5. Autoriza acceso
6. Railway detectará automáticamente que es Spring Boot

---

## Paso 2: Crear Base de Datos PostgreSQL
1. En tu proyecto de Railway, clic en "Add" → "Database" → "PostgreSQL"
2. Railway creará automáticamente la variable de entorno `DATABASE_URL`
3. Espera a que se despliegue (2-3 minutos)

**Verificar:**
- Ve a la pestaña "Variables" del servicio PostgreSQL
- Debes ver `DATABASE_URL` con el formato: `postgresql://...`

---

## Paso 3: Configurar Variables de Entorno - PARTE 1 (Sin APISUNAT aún)

En tu servicio de aplicación, ve a "Variables" y agrega:

```
# Base de Datos
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false
SQL_INIT_MODE=never

# Cloudinary
CLOUDINARY_CLOUD_NAME=dpk8rajy5
CLOUDINARY_API_KEY=811574978487384
CLOUDINARY_API_SECRET=Ycyx71c6ztXQtHMcp7-VIFIoLgo
CLOUDINARY_FOLDER_ENV=produccion

# reCAPTCHA
RECAPTCHA_SITE_KEY=tu_clave_sitio_aqui
RECAPTCHA_SECRET_KEY=tu_clave_secreta_aqui
RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify
RECAPTCHA_ENABLED=true

# API Externa
API_EXTERNAL_TOKEN=tu_token_aqui

# Puerto
PORT=8080
```

**¿Dónde están mis claves de reCAPTCHA?**
- Si no las tienes, ve a https://www.google.com/recaptcha/admin
- Crea nuevo sitio reCAPTCHA v2
- Usa tus claves nuevas

---

## Paso 4: Configurar Variables de APISUNAT (IMPORTANTE)

⚠️ **ANTES DE HACER ESTO:**
- Contacta con APISUNAT: https://www.apisunat.com
- Solicita credenciales de PRODUCCIÓN (no uses valores DEV)
- Ellos te darán: token, persona-id, persona-token

Una vez tengas credenciales reales de APISUNAT, agrega en Railway:

```
APISUNAT_URL=https://back.apisunat.com
APISUNAT_TOKEN=tu_token_produccion_real
APISUNAT_PERSONA_ID=tu_persona_id_real
APISUNAT_PERSONA_TOKEN=tu_token_persona_produccion_real
APISUNAT_RUC=20556548745
APISUNAT_DOCUMENTS_URI=/personas/v1/sendBill
```

**Si no tienes aún credenciales APISUNAT:**
- Puedes dejar este paso para después
- La aplicación funcionará sin APISUNAT, solo fallarán emisiones de facturas

---

## Paso 5: Desplegar

1. Railway detecta cambios en Git automáticamente
2. O ve a "Settings" del servicio → Clic en "Deploy"
3. Espera logs en "Deployments" tab
4. Cuando termina, ves URL: `https://tu-app.up.railway.app`

---

## Verificar que el despliegue funcionó

```bash
# 1. Acceder a la app
https://tu-app.up.railway.app

# 2. Revisar logs (en Railway tab "Logs")
# Busca: "Started Application in X.XXX seconds"

# 3. Test rápido
- Intenta login
- Carga una imagen (Cloudinary)
```

---

## Si hay errores, revisar en este orden:

### ❌ "Aplicación no inicia"
**Ver logs en Railway** → Tab "Logs"
```
Si ves: "WARN org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext"
→ Revisa DATABASE_URL está configurada correctamente
```

### ❌ "Imagen no sube a Cloudinary"
```
Si ves: "Cloudinary authentication error"
→ Verifica CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
→ Asegúrate que estén exactamente como aparecen aquí (sin espacios)
```

### ❌ "APISUNAT no funciona"
```
Si ves: "APISUNAT connection refused" o "401 Unauthorized"
→ Verifica tokens de APISUNAT son de PRODUCCIÓN (no DEV)
→ Contacta APISUNAT si son valores reales pero aún fallan
```

### ❌ "Database connection error"
```
Si ves: "org.postgresql.util.PSQLException"
→ Verifica DATABASE_URL está presente en Variables
→ Railway debería haberla creado automáticamente al agregar PostgreSQL
```

---

## CHECKLIST FINAL

Antes de considerar despliegue exitoso:

- [ ] Aplicación está corriendo (logs dicen "Started Application in...")
- [ ] Puedes acceder a https://tu-app.up.railway.app
- [ ] Login funciona
- [ ] Subida de imágenes funciona (Cloudinary)
- [ ] Base de datos tiene datos iniciales (si usas data.sql)
- [ ] Verifica logs cada día primer día en caso de errores tardíos

---

## LINKS ÚTILES

- Railway Dashboard: https://railway.app/dashboard
- Documentación Railway: https://docs.railway.app
- APISUNAT: https://www.apisunat.com
- Cloudinary Dashboard: https://cloudinary.com/console
- Google reCAPTCHA: https://www.google.com/recaptcha/admin

---

## PRÓXIMOS PASOS

1. **Primeros 24 horas:** Monitorea logs diarios, revisa si hay errores
2. **Después de 24h:** Si todo OK, considera estable
3. **Semana 1:** Haz pruebas de todas las funcionalidades
4. **Semana 2:** Considera agregar volumen si necesitas persistencia de archivos

---

## SOPORTE RÁPIDO

Si algo no funciona:

1. **Abre Railway "Logs"** y copia el error
2. **Busca el error en Google + "railway java"**
3. **Contacta Railway support** si es problema de infraestructura
4. **Contacta proveedores** si es error de credenciales (Cloudinary, APISUNAT, etc)
