# Revisión de accesibilidad con WAVE — lista de pantallas

**Por qué hace falta.** La versión web de WAVE (wave.webaim.org) no dibuja la app de React dentro de su marco y
analiza una página casi vacía, así que su «0 errores» no sirve como evidencia (ver `docs/Evidencias entrega final/README.md`).
Hay que usar la **extensión de WAVE** en Chrome o Edge, que analiza la página ya dibujada y con la sesión iniciada.

## Cómo hacerlo

1. Instalar la extensión «WAVE Evaluation Tool» de WebAIM desde la tienda de extensiones de Chrome o Edge.
2. Abrir `https://metamorfosis-mu.vercel.app` e iniciar sesión con una cuenta de prueba que ya tenga metas, una meta
   con hitos y hábitos, y al menos una meta cumplida (así las pantallas no salen vacías).
3. En cada pantalla de la tabla: abrir la extensión, esperar a que termine y tomar una captura del panel **Summary**
   (Errors, Contrast Errors, Alerts, Features, Structure, ARIA).
4. Repetir en vista de celular: herramientas de desarrollador (F12) → modo dispositivo → 375 px de ancho → recargar → WAVE.
5. Guardar las capturas en `docs/Evidencias entrega final/` con el nombre `06_wave_<pantalla>_<escritorio|movil>.png`.
6. Si hay **Errors** o **Contrast Errors**, anotar cuál es, corregirlo en el código y volver a medir.
   Las **Alerts** no son errores: se revisan y se anota si se dejan o no, y por qué.

## Pantallas (las mismas 11 que se midieron con Lighthouse, más las de la entrega final)

| # | Pantalla | Ruta | Escritorio: errores / contraste | Celular: errores / contraste |
|---|---|---|---|---|
| 1 | Iniciar sesión | `/login` | | |
| 2 | Pedir registro | `/registro` | | |
| 3 | Recuperar contraseña (HU33) | `/recuperar-contrasena` | | |
| 4 | Mis metas, con filtros (HU3, HU27) | `/metas` | | |
| 5 | Crear meta (HU1) | `/metas/nueva` | | |
| 6 | Tu camino de una meta (HU28, HU29) | `/metas/{código}` | | |
| 7 | Nuevo paso del camino (HU28) | `/metas/{código}/submetas/nueva` | | |
| 8 | Estadísticas de un hábito (HU29) | `/submetas/{código}` | | |
| 9 | Reporte (HU7) | `/reporte` | | |
| 10 | Mi perfil (HU22, HU26) | `/perfil` | | |
| 11 | Galería de insignias (HU12) | `/insignias` | | |
| 12 | Editar perfil, apariencia y avatar (HU23, HU30, HU20) | `/perfil/editar` | | |
| 13 | Cambiar contraseña (HU32) | `/perfil/contrasena` | | |
| 14 | Invitaciones (HU14) | `/invitaciones` | | |
| 15 | Buscar personas (HU24) | `/usuarios` | | |
| 16 | Perfil de otra persona (HU24, HU19) | `/usuarios/{código}` | | |
| 17 | Logro compartido, sin sesión (HU21) | `/logros/{token}` | | |

**Repetir las pantallas 4, 6 y 9 con el tema oscuro** (HU30), porque el contraste cambia con el tema.

## Qué va en el documento (sección 6.1)

- La tabla anterior llena, con la fecha de la medición.
- Las capturas de al menos 3 pantallas (por ejemplo, Mis metas, Tu camino y Reporte) en escritorio y celular.
- Los errores encontrados y cómo se corrigieron, o «0 errores y 0 errores de contraste» si así salió.
