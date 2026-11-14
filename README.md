# Metamorfosis

Aplicación web y móvil de seguimiento de metas. Cada meta tiene un **camino**: **hitos** que se logran
una vez (dicen cuánto falta) y **hábitos** que se repiten (dicen qué tan constante vas). Al llegar, la
meta se marca como cumplida, se ganan insignias y se puede compartir el logro.

## Contexto académico
Proyecto final del curso Ingeniería de Software II — Universidad de los Llanos.

## Equipo y roles
| Integrante | Rol(es) |
|---|---|
| Samuel | Gerente de Proyecto / UI-UX Designer / Facilitador de Equipo |
| Felipe | Líder Técnico / Seguridad Informática / Infraestructura |
| Daniel | QA / Seguridad Informática / Infraestructura |

Todos los integrantes actúan como **desarrolladores** y cada uno documenta su propio trabajo como parte
del flujo de desarrollo (no hay un rol de documentador dedicado).

> Cristian Villalobos (UI-UX, datos y cumplimiento normativo) participó en el Sprint 0 y dejó el
> proyecto durante la entrega final. Sus historias se reasignaron: la HU15 y la HU19 a Daniel y la
> HU26 a Felipe. El cambio queda en la RACI y en los riesgos de la entrega final.

## Requisitos de la entrega final
| Requisito | Cómo se cumple |
|---|---|
| Web y móvil | React con diseño responsive (probado en 375 px y 1280 px) |
| Dos niveles | Cliente-servidor: el servidor ejecuta la API (Spring Boot) y la base de datos (PostgreSQL); cualquier otro equipo o celular entra por el navegador. Ver [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md) |
| ORM | JPA / Hibernate con migraciones Flyway (`backend/src/main/resources/db/migration`) |
| Accesibilidad y usabilidad > 70 (online) | PageSpeed Insights y WAVE sobre la URL pública; cuestionario SUS |
| Complemento de accesibilidad | Widget Sienna (botón flotante) y `eslint-plugin-jsx-a11y` en el linter |
| Manual técnico y de usuario | Anexos de la entrega final |
| Minificar y ofuscar el cliente | `npm run build`: Vite minifica y `javascript-obfuscator` ofusca; sin source maps |
| Web services | API REST con JSON y autenticación JWT |
| Reportes y gráficas | Reporte general con Chart.js (HU7) y estadísticas de cada hábito (HU29) |
| Evidencia de pruebas | JUnit 5 + Mockito; cobertura con JaCoCo (`mvnw verify`) |
| Logs | Auditoría en la tabla `log_actividad` y log de aplicación en `backend/logs/metamorfosis.log` |
| Internacionalización (opcional) | Español e inglés, ver [Internacionalización](#internacionalización) |

## Internacionalización
La app está en **español** (por defecto) e **inglés**. El idioma se elige con el selector del encabezado
(en el celular, dentro del menú de la cuenta), se recuerda en el navegador y, la primera vez, se toma
del idioma del navegador.

- **Frontend** (i18next + react-i18next): los textos están en `frontend/src/i18n/es/*.json` y
  `frontend/src/i18n/en/*.json`, un archivo por parte de la app. Para agregar un texto, crea la misma
  clave en los dos idiomas y úsala con `const { t } = useTranslation()` y `t('clave')`.
- **Backend**: el frontend envía la cabecera `Accept-Language`. Los servicios escriben sus mensajes en
  español y `TraductorMensajes` los traduce al responder, con `backend/src/main/resources/i18n/mensajes_en.txt`
  (`mensaje en español => mensaje en inglés`; `{0}`, `{1}` para las partes que cambian). Los correos de
  registro e invitación también salen en el idioma de quien los origina.

## Instalación y ejecución local
Requisitos: Java 21, Node.js 22 o superior, Git y una base PostgreSQL (el equipo usa Supabase).

```bash
git clone https://github.com/FelipeBeltranING/metamorfosis.git
cd metamorfosis/backend
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# completar la conexión a la base, app.jwt.secret (32 caracteres o más) y, si se quiere, Brevo
./mvnw spring-boot:run          # API en http://localhost:8080; Flyway crea las tablas al arrancar
```

```bash
cd metamorfosis/frontend
cp .env.example .env.local      # VITE_API_URL=http://localhost:8080
npm install
npm run dev                     # app en http://localhost:5173
```

| Comando | Para qué |
|---|---|
| `./mvnw test` | Pruebas del backend |
| `./mvnw verify` | Pruebas e informe de cobertura en `backend/target/site/jacoco/index.html` |
| `npm run lint` | Linter con reglas de accesibilidad |
| `npm run build` | Versión de producción minificada y ofuscada en `frontend/dist` |
| `npm run preview` | Sirve el build de producción para probarlo |

El despliegue en Render (backend) y Vercel (frontend) está en [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).

## Estado
Entrega final (24 de octubre – 18 de noviembre de 2026). El plan de cierre, con lo que falta, está en
[docs/PLAN_ENTREGA_FINAL.md](docs/PLAN_ENTREGA_FINAL.md).

## Créditos
Los avatares (HU20) usan las ilustraciones «Big Smile» de Ashley Seo, con licencia
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), a través de [DiceBear](https://www.dicebear.com) (MIT).

## Licencia
Este proyecto está bajo la licencia MIT. Ver [LICENSE](LICENSE).
