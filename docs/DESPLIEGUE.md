# Despliegue de Metamorfosis (RQ4)

| Parte | Dónde | URL de ejemplo |
| --- | --- | --- |
| Base de datos | Supabase (ya existe) | — |
| Backend Spring Boot | Render, plan Free, con Docker (`backend/Dockerfile`) | `https://metamorfosis-api.onrender.com` |
| Frontend React | Vercel, plan Hobby (`frontend/vercel.json`) | `https://metamorfosis.vercel.app` |

El orden importa: primero el backend (el frontend necesita su URL) y luego el frontend
(el backend necesita la URL de Vercel para CORS y para los enlaces de los correos).

## 1. Supabase: copiar la conexión

1. Supabase → el proyecto → botón **Connect** → **Session pooler** (puerto **5432**).
   No usar la conexión directa (`db.xxx.supabase.co`): es solo IPv6 y Render no la alcanza.
   No usar el puerto 6543 (transaction mode): Hibernate usa sentencias preparadas.
2. De esa cadena salen tres valores:
   - `DB_URL` = `jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres`
     (la misma cadena, con `jdbc:` adelante y sin usuario ni contraseña)
   - `DB_USERNAME` = `postgres.PROJECT-REF`
   - `DB_PASSWORD` = la contraseña de la base

Flyway aplica las migraciones al arrancar; si la base ya tiene las migraciones V1 a V16, no hace nada.

## 2. Render: backend

1. render.com → entrar con GitHub → **New +** → **Web Service** → repositorio `FelipeBeltranING/metamorfosis`.
2. Configuración:
   - **Name:** `metamorfosis-api`
   - **Branch:** `main`
   - **Root Directory:** `backend`
   - **Language / Runtime:** Docker (detecta `backend/Dockerfile`)
   - **Instance Type:** Free
3. **Environment Variables:**

| Variable | Valor |
| --- | --- |
| `DB_URL` | la del paso 1 |
| `DB_USERNAME` | la del paso 1 |
| `DB_PASSWORD` | la del paso 1 |
| `APP_JWT_SECRET` | un texto aleatorio de 32 caracteres o más (no el de local) |
| `APP_CORS_ALLOWED_ORIGINS` | la URL de Vercel, sin `/` al final (por ahora `http://localhost:5173`; se cambia en el paso 4) |
| `APP_FRONTEND_URL` | la URL de Vercel (igual: se completa en el paso 4) |
| `APP_CORREO_PROVEEDOR` | `brevo` (si se deja `consola`, los correos de registro solo quedan en el log de Render) |
| `BREVO_API_KEY` | la clave de Brevo |
| `APP_CORREO_REMITENTE` | el correo verificado en Brevo |

4. **Create Web Service**. El primer build tarda varios minutos. Cuando el log diga
   `Started BackendApplication`, copiar la URL del servicio (`https://metamorfosis-api.onrender.com`).

Prueba rápida: `POST https://metamorfosis-api.onrender.com/api/auth/login` desde Postman
debe responder 401 con credenciales falsas (eso confirma que el backend y la base responden).

## 3. Vercel: frontend

1. vercel.com → entrar con GitHub → **Add New…** → **Project** → importar `FelipeBeltranING/metamorfosis`.
2. **Root Directory:** `frontend` (Vercel detecta Vite: build `npm run build`, salida `dist`).
3. **Environment Variables:** `VITE_API_URL` = la URL de Render, sin `/` al final.
4. **Deploy**. Copiar la URL que entrega Vercel.

`frontend/vercel.json` hace que rutas como `/perfil` o `/metas` funcionen al recargar la página.

## 4. Cerrar el círculo

1. En Render, cambiar `APP_CORS_ALLOWED_ORIGINS` y `APP_FRONTEND_URL` por la URL de Vercel.
   Render redespliega solo al guardar.
2. Abrir la URL de Vercel, registrarse e iniciar sesión.

Cada push a `main` vuelve a desplegar el frontend en Vercel y el backend en Render.

## 5. Mediciones de la RQ4

El plan Free de Render apaga el backend tras 15 minutos sin uso y tarda cerca de un minuto en despertar.
Antes de medir o de una sesión con usuarios, abrir la app y esperar a que cargue.

1. **PageSpeed Insights** (pagespeed.web.dev): pegar la URL de Vercel; anotar Accesibilidad en móvil y en escritorio.
   Para las pantallas con sesión: Chrome → DevTools → Lighthouse sobre la URL desplegada, con la sesión iniciada.
2. **WAVE** (wave.webaim.org): cada pantalla pública por URL; las de sesión con la extensión de WAVE para Chrome.
3. **SUS**: enviar la URL de Vercel a los usuarios reales, pedir que hagan las tareas del guion y que llenen el cuestionario.

Evidencia: capturas con la URL visible y el puntaje (debe ser superior a 70).
