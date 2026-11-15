# Evidencias de la entrega final

Capturas tomadas el **14 de noviembre de 2026** para llenar los recuadros "EVIDENCIA PENDIENTE" del documento
*Entrega_final_-_Metamorfosis*. El prefijo de cada archivo es la sección del documento donde va.
Las de pruebas (secciones 5 y 10) se volvieron a tomar el **15 de noviembre de 2026**, después de agregar las
pruebas de HU21, HU23 y del frontend y de ampliar la colección de Postman.

Todo se ejecutó en local: API Spring Boot y PostgreSQL 16 en Docker; el frontend es el build de producción.
Los reportes y el resto de pantallas usan la cuenta de demostración `valentina.demo@metamorfosis.test`,
que solo existe en la base local. Sus metas se crearon por la API, y las fechas de cumplimiento se
repartieron entre junio y noviembre para que la gráfica por mes muestre una tendencia.

| Sección del doc | Archivo | Qué muestra |
|---|---|---|
| 5 Web services | `05_web_services_newman.png` | Colección de Postman con Newman (15 nov): 162 peticiones, 291 aserciones, **0 fallos**, en 23 carpetas |
| 6.1 Accesibilidad (online) | `06_pagespeed_online_login_movil.png`, `06_pagespeed_online_login_escritorio.png` | **PageSpeed Insights sobre la app publicada** (`https://metamorfosis-mu.vercel.app/login`): accesibilidad **100** en celular y escritorio. Informe: https://pagespeed.web.dev/analysis/https-metamorfosis-mu-vercel-app-login/y6bfj059k4 |
| 6.1 Accesibilidad (online) | `06_pagespeed_online_login_*_auditorias.png` | Detalle de las auditorías de accesibilidad de ese informe |
| 6.1 Accesibilidad | `06_lighthouse_resumen_11_pantallas.png` | Lighthouse 12.6 en 11 pantallas, en celular y escritorio: accesibilidad **mínima 96, promedio 99,6** |
| 6.1 Accesibilidad | `06_lighthouse_login_movil.png`, `06_lighthouse_mis-metas_escritorio.png`, `06_lighthouse_reporte_movil.png` | Encabezado del informe de Lighthouse de tres pantallas |
| 6.3 Complemento | `06_sienna_menu_abierto.png`, `06_sienna_menu_abierto_movil.png` | Widget Sienna abierto (escritorio y celular) |
| 6.3 Complemento | `06_sienna_alto_contraste_y_fuente.png` | App con alto contraste, fuente para dislexia y enlaces destacados activados desde Sienna |
| 6.3 Complemento | `06_lint_jsx_a11y.png` | `eslint-plugin-jsx-a11y` en `eslint.config.js` y `npm run lint` sin errores |
| 7 Ofuscación | `07_build_minificado_ofuscado.png` | Salida de `npm run build` |
| 7 Ofuscación | `07_codigo_ofuscado_navegador.png` | Archivo JS de `dist/assets` abierto en el navegador: minificado y ofuscado |
| 7 Ofuscación | `07_tamanos_build.png` | Tabla de tamaños: sin minificar, minificado, y minificado y ofuscado |
| 8 Reportes | `08_reporte_*_escritorio.png`, `08_reporte_*_movil.png` | Los 4 tipos de reporte (general, por tipo, por plazo, por mes), en escritorio y celular |
| 9 Logs | `09_logs_auditoria_registros.png` | Consulta a `log_actividad` con filas EXITO y ERROR de varias acciones |
| 9 Logs | `09_logs_auditoria_resumen.png` | Conteo de EXITO y ERROR por acción |
| 9 Logs | `09_logs_archivo_aplicacion.png` | `backend/logs/metamorfosis.log`: INFO de arranque y Flyway, WARN del filtro JWT |
| 10 Pruebas | `10_pruebas_mvnw_verify.png` | `mvnw clean verify` (15 nov): **253 pruebas en 29 clases, 0 fallos**, BUILD SUCCESS |
| 10 Pruebas | `10_pruebas_cobertura_jacoco.png` | Informe de JaCoCo (15 nov): 69 % de instrucciones, 58 % de ramas |
| 10 Pruebas | `10_pruebas_frontend_vitest.png` | `npm run test:cobertura` (15 nov): **32 pruebas, 0 fallos**; cobertura de `src/utils` 95 % |
| 10 Pruebas | `10_pruebas_frontend_cobertura.png` | Informe HTML de cobertura de Vitest |
| 11 Idiomas | `11_idioma_espanol.png`, `11_idioma_ingles*.png` | La app en español y en inglés (Mis metas, reporte, inicio de sesión) |

`datos/` tiene las salidas completas: `newman-salida.txt`, `mvnw-verify.txt`, `vitest-salida.txt`, `log_actividad_tras_newman.txt`
(conteo de filas del log de auditoría después de correr la colección), `npm-lint.txt`, `npm-build.txt` y
`lighthouse-resumen.json` (los puntajes de las 22 mediciones). Los 22 informes HTML de Lighthouse y el JSON de Newman
pesan unos 14 MB y no se suben al repositorio (`.gitignore`); quedan solo en el equipo que los generó.

## Pendiente
- **WAVE:** la versión web (wave.webaim.org) no dibuja la app de React dentro de su marco y analiza una
  página casi vacía, así que su "0 errores" no sirve como evidencia. Hay que usar la extensión de WAVE para
  Chrome sobre `https://metamorfosis-mu.vercel.app`, con la sesión iniciada para las pantallas privadas.
- La URL pública del frontend es `https://metamorfosis-mu.vercel.app`; `metamorfosis.vercel.app` es el sitio de
  otra persona (corregido en `docs/DESPLIEGUE.md`).
- **SUS (6.2):** hay que aplicarlo a 5 personas reales. La guía, las tareas, el cuestionario y la calculadora del
  puntaje están en `docs/Pruebas/usabilidad/`.
- **WAVE con la extensión:** la lista de pantallas y cómo nombrar las capturas está en
  `docs/Pruebas/usabilidad/WAVE_lista_de_pantallas.md`.

## Cómo se corrieron las pruebas del 15 de noviembre
- Backend: JDK 21 y PostgreSQL 16 en Docker (`127.0.0.1:55432`), con `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET` (el de `local.postman_environment.json`)
  y `APP_CORREO_PROVEEDOR=consola` como variables de entorno, para no tocar la base de Supabase.
  `mvnw clean verify` y luego la API con `PORT=8090`.
- Postman: `datos_de_prueba.sql` en esa base y `newman run ... --env-var baseUrl=http://localhost:8090`.
- Frontend: `npm run test:cobertura`, `npm run lint` y `npm run build`.
