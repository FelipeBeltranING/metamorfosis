# Metamorfosis

Aplicación web y móvil de seguimiento de metas. Los usuarios registran objetivos y su progreso.

## Contexto académico
Proyecto final del curso Ingeniería de Software 2 — Universidad de los Llanos.

## Equipo y roles
| Integrante | Rol(es) |
|---|---|
| Samuel | Gerente de Proyecto / UI-UX Designer / Facilitador de Equipo |
| Felipe | Líder Técnico / Seguridad Informática / Infraestructura |
| Cristian | UI-UX Designer / Analista de Datos / Cumplimiento Normativo |
| Daniel | QA / Seguridad Informática / Infraestructura |

Todos los integrantes actúan como **desarrolladores** y cada uno documenta su propio trabajo como parte del flujo de desarrollo (no hay un rol de documentador dedicado).

## Requisitos del proyecto
- Aplicación web y móvil con dos niveles de usuario (usuario / coach)
- ORM
- Accesibilidad y usabilidad > 70 (evaluación online)
- Manual técnico y de usuario
- Minificación y ofuscación del lado del cliente
- Web services
- Generador de reportes y gráficas
- Evidencia de ejecución de pruebas
- Logs
- Internacionalización (opcional) — español e inglés, ver [Internacionalización](#internacionalización)

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

## Estado
🚧 Sprint 1 en curso.

## Instalación
_Pendiente de definir stack tecnológico (Sprint 0)._

## Licencia
Este proyecto está bajo la licencia MIT. Ver [LICENSE](LICENSE).
