# Plan de cierre — Entrega final de Metamorfosis

> Revisión: 14 de noviembre de 2026 (entrega: 18 de noviembre). Versión 2, con las decisiones del equipo.
> Fuentes revisadas:
> - **Repositorio** `main` (commit `3c85949`): backend, frontend, `docs/`.
> - **Drive › Metamorfosis**: `Entrega_final_-_Metamorfosis` (modificado el 9 nov), `Sprint 0 - Metamorfosis`, `Sprint 1 - Metamorfosis`.
> - **Requisitos del profesor** (Entrega final, Sprint 0 y Sprint 1).
>
> Leyenda: ✅ listo · 🟡 parcial o desactualizado · ❌ falta · ❓ decisión pendiente

---

## Avance (14 de noviembre de 2026)

Bloque 1 (código) terminado en ramas locales, cada una con su sustento en el mensaje del commit. Falta subirlas y abrir los PR.

| Rama | Qué resuelve | Requisito / HU | Verificación |
|---|---|---|---|
| `rq6-ofuscacion` | Plugin de ofuscación en el build, sin source maps | RQ6 | JS: sin minificar 1.725 kB → minificado 843 kB → min + ofuscado 949 kB; `npm run preview` sin errores |
| `rq8-log-archivo` | `logs/metamorfosis.log` con rotación y WARN en accesos rechazados | RQ8 | Archivo creado y WARN registrado con un token falso |
| `rq9-pruebas` | JaCoCo en `mvnw verify` | RQ9 | 159 pruebas, 0 fallos; cobertura 50,7 % de líneas |
| `rq5-accesibilidad` | Widget Sienna + `eslint-plugin-jsx-a11y`; lint en 0 errores | RQ5 | Widget en español con 28 opciones; `npm ci` limpio |
| `hu28-camino-hitos` | Rediseño del camino: hitos + hábitos, avance real, formulario corto (sección 3) | HU28, HU5, HU29 (+HU3, HU4, HU6) | V24 en PostgreSQL limpio; 28 pruebas nuevas; probado en el navegador (es y en) |
| `hu20-mascota` (sobre `hu28-camino-hitos`) | Mascota gratis entre 6 opciones | HU20 | V25; 5 pruebas nuevas; elegir, guardar y verla en Mi perfil; auditoría CAMBIAR_MASCOTA |
| `docs-readme` | README actualizado, salida de Cristian, este plan | — | — |

**Orden de fusión:** `hu28-camino-hitos` antes que `hu20-mascota` (las migraciones V24 y V25 van en ese orden). Las demás ramas son independientes.

**Pendiente de revisar a ojo:** el aspecto visual de las pantallas nuevas y de los dibujos de las mascotas. Se verificaron por contenido y funcionamiento, pero no hubo captura de pantalla.

## 0. Resumen

1. **El código va muy por delante del documento de Drive.** El doc se quedó en el 9 de noviembre: la sección 16 termina a mitad de la HU13 y faltan HU14, HU15, HU19, HU21, HU23 y las sub-metas.
2. **Faltan 4 requisitos técnicos en el código:** ofuscación (RQ6), complemento de accesibilidad (RQ5), log de aplicación a archivo (RQ8) y pruebas automatizadas con evidencia (JaCoCo, Postman, frontend) (RQ9).
3. **Falta la evidencia online:** PageSpeed/WAVE y SUS (RQ4), acceso desde otro equipo (RQ1) y los **manuales** (RQ10).
4. **Diagramas:** se entregan **dos archivos draw.io por HU** (clases y secuencia): **52 archivos** para 26 HU (sección 2.1).
5. **Sub-metas:** se rediseñan para que tengan sentido para el usuario. El camino tiene **hitos** (pasos que se logran una vez y dan el avance real de la meta) y **hábitos** (lo que se hace cada día). Quedan en 3 HU que cubren el requisito RF3.1 del Sprint 0 (sección 3).
6. **Alcance:** HU15 va (ya está hecha); HU17 y HU18 se retiran; **HU20 vuelve como mascota gratis** con varias opciones para escoger (sección 4).
7. **Cristian sale del proyecto:** se ajustan el README, la nota en la Entrega final y la RACI (sección 5).
8. **Complemento de accesibilidad:** opción C, una librería open source instalada con npm (sección 6).

---

## 1. Requisitos del profesor vs. estado real

### 1.1 Entrega final

| # | Requisito | Estado en el código | Estado en el doc de Drive | Qué falta |
|---|---|---|---|---|
| RQ1 | **Dos niveles** — según el profesor: un equipo o servidor ejecuta la aplicación y otro equipo entra a usarla sin problema | ✅ Arquitectura cliente-servidor: API Spring Boot + PostgreSQL (Supabase) en el servidor; React en el navegador de cualquier equipo. `docs/DESPLIEGUE.md` documenta Render + Vercel | 🟡 La sección 3 lo explica bien, pero no tiene evidencia de acceso remoto | **Evidencia:** la app desplegada (Render/Vercel) abierta desde **otro computador y desde un celular**, con la URL visible. Diagrama de despliegue (qué corre en qué máquina). Plan B para la sustentación: backend en un PC y otro PC o el celular entrando por la IP de la red local. |
| RQ2 | **ORM** | ✅ JPA/Hibernate, Flyway V1–V22 | 🟡 La sección 4 dice V1–V9, lista `Registro`, `ItemPersonalizacion` y `Mascota` (no existen) y le faltan `SubMeta`, `RegistroSubMeta`, `SolicitudRegistro` y `LogroCompartido` | Actualizar 4.1 y 4.2 y agregar un modelo entidad-relación actual en draw.io. |
| RQ3 | **Web services** | ✅ 46 endpoints REST en 8 controladores | 🟡 La sección 5 lista unos 20, incluye HU18/HU20 (no existen) y la HU5 vieja | Regenerar la tabla desde el código. ❌ Colección de Postman. Opcional: Swagger (springdoc). |
| RQ4 | **Accesibilidad y usabilidad online > 70** | 🟡 Despliegue documentado | 🟡 Solo hay línea base local (100/100 en /login y /registro); la tabla SUS está vacía | PageSpeed Insights + WAVE sobre la URL pública de cada pantalla; SUS con 5 personas o más. Capturas con fecha y URL. |
| RQ5 | **Complemento de accesibilidad** | ❌ No hay panel ni `eslint-plugin-jsx-a11y` | "Por hacer" | Ver opciones en la sección 6. |
| RQ6 | **Minificar y ofuscar el cliente** | 🟡 Minifica (Vite). ❌ **No ofusca**: `vite.config.js` no tiene el plugin | La sección 7 muestra una configuración que **no está en el repo** | Instalar `vite-plugin-javascript-obfuscator`, poner `sourcemap:false`, probar con `npm run preview` y tomar capturas de `dist/` y de la pestaña Sources. |
| RQ7 | **Reportes y gráficas** | ✅ HU7 con Chart.js (4 gráficas) y estadísticas por sub-meta | ✅ Sección 8 | Capturas por tipo de reporte. Recomendado: exportar el reporte a PDF o CSV. |
| RQ8 | **Logs** | 🟡 Auditoría `log_actividad` ✅ (37 acciones). ❌ No hay `logging.file.*` | La sección 9 lista solo 13 acciones | Log a archivo con rotación; lista real de `AccionLog`; capturas. |
| RQ9 | **Evidencia de pruebas** | 🟡 147 `@Test` en 14 clases (solo servicios). ❌ Sin JaCoCo, MockMvc, Postman/Newman ni pruebas de frontend | Menciona `RegistroServiceTest` (ya no existe) y dice "[completar]"; los 24 casos de la HU10 no se ejecutaron | JaCoCo, `mvnw verify`, Postman + Newman, los 24 casos de la HU10 y una tabla de casos por HU. |
| RQ10 | **Manual técnico y de usuario** | ❌ | Solo hay índice | Dos PDF con capturas de la versión final. |
| RQ11 | **Internacionalización (opcional)** | ❌ | Plan en la sección 11 | Solo si sobra tiempo. |
| — | **Web y móvil** | ✅ Responsive (375 px y 1280 px) | ✅ | Opcional: PWA (instalable en el celular). |

### 1.2 Sprint 0 (ya entregado)

| Requisito | Estado | Ajuste para la entrega final |
|---|---|---|
| RACI | ✅ | Nota de cambio: Cristian sale; sus HU (HU5, HU15, HU19, HU26…) se reasignan (sección 5). |
| Product Backlog MoSCoW | 🟡 | Agregar HU27, HU28 y HU29; redefinir HU5, HU15 y HU19; retirar HU17, HU18 y HU20. |
| Estimación tiempo / costo / esfuerzo | ✅ | Recalcular los totales de la entrega final con los cambios de alcance. |
| Planificación en herramienta web | ✅ GitHub Projects | Cerrar o mover las issues de las HU retiradas y crear las de HU28 y HU29. |

### 1.3 Artefactos por HU (formato del Sprint 1)

Cada HU debe tener: historia con criterios, Sprint Backlog, **mockup**, **diagrama de clases**, **diagrama de secuencia**, DoR, DoD, riesgos, plan de pruebas, código limpio y responsive.

---

## 2. Verificación por HU

**Cód.** = implementado en `main` · **Mock.** = mockups en `docs/Mockups por HU` · **Clases / Sec.** = formato actual · **Doc** = sección en la Entrega final.

| HU | Nombre | Cód. | Mock. | Clases | Sec. | Doc Drive | Problemas detectados |
|---|---|---|---|---|---|---|---|
| HU1 | Crear meta | ✅ | ✅ | draw.io 🟡 | draw.io ✅ | Sprint 1 | `Meta.oculta` ya no es campo de `Meta` (pasó a `Participacion` en V16); falta `participantes` en `MetaResponse`. |
| HU2 | Editar meta | ✅ | ✅ | draw.io ❌ | draw.io 🟡 | Sprint 1 | Todavía aparece la entidad **`Registro`** (eliminada). Falta la regla "meta cumplida no editable". |
| HU3 | Mis metas | ✅ | ✅ | draw.io 🟡 | draw.io ✅ | Sprint 1 | Falta `participantes` y el resumen del camino en la tarjeta. |
| HU4 | Eliminar meta | ✅ | ✅ | draw.io 🟡 | draw.io 🟡 | Sprint 1 | Agregar el borrado en cascada de `SubMeta` y `RegistroSubMeta`. |
| **HU5** | **Registrar el avance del día** (redefinida, sección 3) | ✅ | ✅ (Sub-metas) | ❌ nuevo | ❌ nuevo | ❌ | Los draw.io viejos de la HU5 van a "Histórico". |
| HU6 | Marcar cumplida | ✅ | ✅ | Mermaid | Mermaid | §16.1 | Pasar a draw.io. La meta compartida se cumple para todos (commit `a6c4f55`): ajustar el criterio 5 y el diagrama. |
| HU7 | Ver reporte | ✅ | ✅ | ❌ | Mermaid | §16.3 | Crear el diagrama de clases. |
| HU8 | Registrarse | ✅ | ✅ | draw.io ❌ | draw.io ❌ | Sprint 1 | Muestra el flujo viejo; falta la autorización por correo (`SolicitudRegistro`, Brevo, 6 endpoints). **Rehacer.** |
| HU9 | Iniciar sesión | ✅ | ✅ | draw.io 🟡 | draw.io 🟡 | Sprint 1 | Agregar `BloqueoLoginService` e `IntentoLogin`. |
| HU10 | Cerrar sesión | ✅ | ✅ | draw.io ✅ | draw.io 🟡 | Sprint 1 | Debe empezar en `BotonCerrarSesion` (con confirmación). **24 pruebas sin ejecutar.** |
| HU11 | Recibir insignia | ✅ | ✅ | ❌ | Mermaid | §16.4 | Crear el diagrama de clases. |
| HU12 | Galería de insignias | ✅ | ✅ | PNG | PNG | §16.6 | Pasar a draw.io. |
| HU13 | Invitar a una meta | ✅ | ✅ | Mermaid/PNG | Mermaid/PNG | §16.10 incompleta | Faltan diagramas, pruebas y DoD en el doc. Los nombres de las figuras del doc no coinciden con los del repo. Falta documentar el **código de invitación**. |
| HU14 | Responder invitación | ✅ | ✅ | Mermaid/PNG | Mermaid/PNG | ❌ | Escribir la sección. |
| **HU15** | Logro de meta compartida | ✅ (sección 4) | ❌ | ❌ | ❌ | ❌ | Mockups (perfil de un participante con la meta compartida cumplida), diagramas y sección. |
| HU19 | Personalizar perfil (redefinida) | ✅ | 🟡 solo Apariencia | ❌ | ❌ | ❌ | Redefinir sin los ítems de la HU18 (sección 4). Faltan los mockups del perfil público (portada, frase, destacados). |
| HU21 | Compartir logro | ✅ | ✅ | draw.io ✅ | draw.io ✅ | ❌ | Escribir la sección. |
| HU22 | Ver mi perfil | ✅ | ✅ | ❌ | Mermaid | §16.5 | Crear el diagrama de clases. |
| HU23 | Editar perfil y foto | ✅ | ✅ | ❌ | ❌ | ❌ | Diagramas y sección. |
| HU24 | Perfil de otros | ✅ | ✅ | PNG | PNG | §16.8 | "FIGURA PENDIENTE": las imágenes ya existen, falta pegarlas. |
| HU25 | Ocultar metas | ✅ | ✅ | PNG | PNG | §16.7 | Igual que HU24. |
| HU26 | Perfil privado | ✅ | ✅ | PNG | PNG | §16.9 | Igual; además el estado dice "Haciendo" y ya está hecha. |
| HU27 | Filtrar metas | ✅ | ✅ | ❌ | Mermaid | §16.2 | Crear el diagrama de clases. |
| **HU28** | **Definir el camino de una meta** (nueva, sección 3) | ✅ | ✅ (Sub-metas) | ❌ | ❌ | ❌ | Todo. |
| **HU29** | **Ver la constancia de una sub-meta** (nueva, sección 3) | ✅ | ✅ (Sub-metas) | ❌ | ❌ | ❌ | Todo. |
| **HU20** | **Elegir mi mascota** (redefinida, gratis; sección 4) | ❌ por hacer | ❌ | ❌ | ❌ | ❌ | Todo. |
| HU30 | Mi mascota se transforma (opcional, sección 4) | ❌ | — | — | — | — | Solo si hay tiempo. |
| HU16 | Cambiar idioma | ❌ | — | — | — | Opcional | Opcional. |
| HU17, HU18 | Puntos y tienda de ítems | ❌ | — | — | — | — | **Retiradas** (sección 4). Sin diagramas. |

### 2.1 Diagramas en draw.io (dos archivos por HU)

Nombres: `HU01_clases.drawio` y `HU01_secuencia.drawio`. Cada uno se verifica contra el código de `main`.

| Acción | HU | Archivos |
|---|---|---|
| Corregir draw.io existente | HU1, HU2, HU3, HU4, HU9, HU10, HU21 | 14 |
| Rehacer | HU8 | 2 |
| Convertir de Mermaid/PNG | HU6, HU12, HU13, HU14, HU24, HU25, HU26 (ambos); HU7, HU11, HU22, HU27 (secuencia) | 18 |
| Crear desde cero | HU7, HU11, HU22, HU27 (clases); HU5, HU15, HU19, HU20, HU23, HU28, HU29 (ambos) | 18 |
| **Total** | **26 HU** (+ HU30 si se hace) | **52** (54) |

Además: `modelo_ER.drawio` (RQ2) y `despliegue_dos_niveles.drawio` (RQ1).

---

## 3. Sub-metas: qué está mal y cómo se arregla

### 3.1 Diagnóstico

- **Contradicción con el propio documento.** La Entrega final §2.1 retira la HU5 porque "trataba las metas como hábitos diarios, que no es el propósito del producto". Ocho días después (2 de noviembre) se agregaron las sub-metas, que vuelven a registrar el avance día a día, y ningún documento explica por qué.
- **No existen en ningún artefacto.** No están en el Product Backlog, la RACI, la estimación, el CPM ni GitHub Projects. En el código aparecen como "HU-A", "HU-B" y "HU-C", que son nombres que no corresponden a nada.
- **Se colaron dentro de otras HU.** Las sub-metas aparecen repartidas en los commits de HU3 (la tarjeta), HU4 (el aviso al eliminar) y HU6 (el camino en solo lectura), sin una historia propia que las agrupe.
- **El requisito que sí las justifica quedó huérfano.** La visión del Sprint 0 dice que el usuario "lleve el seguimiento diario de su cumplimiento", y el requisito **RF3.1** ("registrar si una meta se cumplió o no en una fecha") era de la HU5. Al retirar la HU5, RF3.1 quedó sin HU que lo cubra.

### 3.2 Sustentación (la idea que sí tiene sentido)

> **La meta es el destino y las sub-metas son el camino.**
> Una meta como "Correr una media maratón" no se cumple en un día: se cumple (HU6) una sola vez. Lo que sí se hace cada día son los pasos: "salir a correr" (Sí/No) o "kilómetros recorridos ≥ 5" (medible).
> El error de la HU5 era registrar el día **sobre la meta**; las sub-metas lo registran **sobre cada paso**, y la meta sigue teniendo un solo estado (activa o cumplida).

Con esto RF3.1 vuelve a quedar cubierto, la retirada de la HU5 tiene sentido (se corrige su diseño, no se elimina el requisito) y la ruta crítica del Sprint 0 (T0 → HU1 → HU3 → **HU5** → HU6) sigue siendo válida.

### 3.3 Qué no funciona para el usuario (revisando las pantallas)

| # | Problema | Dónde se ve | Por qué confunde |
|---|---|---|---|
| 1 | **El porcentaje de la meta no es avance.** "Leer 20 libros · 56%" parece que lleva el 56 % de los libros, pero es la constancia de los últimos 30 días. | Tarjeta de Mis metas y cabecera de Tu camino | El usuario nunca sabe **cuánto le falta para llegar**, que es lo único que importa de una meta. |
| 2 | **Las sub-metas no conectan con la meta.** "Leer 20 libros" tiene "Tiempo en redes ≤ 60 min" y "Leer 1.032 páginas", pero en ningún lado dice cuántos libros lleva. | Tu camino, estadísticas | Todo son hábitos diarios; no hay forma de registrar pasos que se logran una vez ("Terminar el libro 3", "Abrir la cuenta de ahorro"). |
| 3 | **Tres círculos que significan cosas distintas** en la misma tarjeta: el de marcar la meta cumplida, el anillo del 56 % y los checks de las sub-metas de hoy. | Tarjeta de Mis metas | No se sabe cuál tocar. |
| 4 | **Formulario largo y técnico:** "Pregunta del día", "Tipo de objetivo: Al menos / Como máximo", "Notas", días con máscara de bits. | Nueva sub-meta | Para la mayoría de pasos basta un nombre. |
| 5 | **Estadísticas copiadas de una app de hábitos** (historial, calendario, mejores rachas, qué días cumples más) y ninguna sobre la meta. | Estadísticas de la sub-meta | Mucha información y ninguna responde "¿voy a lograr mi meta?". |
| 6 | **"Días seguidos al 100 %: 0"** y la palabra "Pendiente" suelta al pie de la tarjeta. | Tu camino, tarjeta | Desmotiva y no se entiende a qué se refiere. |
| 7 | **El camino no tiene final:** cumplir todos los pasos no lleva a nada. | Tu camino | Se pierde el momento de "¡llegaste!", que es el corazón de la app (y de su nombre). |

### 3.4 Rediseño propuesto: **hitos + hábitos**

El camino de una meta tiene dos tipos de pasos, con nombres que cualquier persona entiende:

| | **Hito** (nuevo) | **Hábito** (lo que hoy es una sub-meta) |
|---|---|---|
| Qué es | Un paso que se logra **una vez** | Algo que se repite en ciertos días |
| Ejemplo ("Leer 20 libros") | "Terminar *Cien años de soledad*" | "Leer 15 páginas al día" |
| Ejemplo ("Ahorrar para un portátil") | "Juntar el primer $1.000.000" | "Guardar $20.000 cada lunes" |
| Cómo se registra | Un check (con la fecha en que se logró) | Sí/No o una cantidad, por día |
| Qué mide | **Avance de la meta**: hitos logrados / total | **Constancia**: días cumplidos / días que tocaban |

Cambios en la experiencia:

1. **El anillo de la meta muestra el avance real** ("7 de 20 hitos · 35 %"). La constancia va aparte, como texto ("Constancia 58 % este mes"). Si la meta no tiene hitos, no se muestra un porcentaje que engañe.
2. **Cuando se logra el último hito**, la app propone: "¡Llegaste a tu destino! ¿Marcar la meta como cumplida?" (conecta con la HU6, las insignias y compartir el logro).
3. **Formulario corto:** nombre y tipo (hito o hábito). Para el hábito: "¿cuántas veces o cuánto?" y los días; la pregunta del día, el "como máximo" y las notas pasan a "Más opciones".
4. **Sugerencias según el tipo de meta** al crear el camino (Financiera → "Guardar $___ cada semana"; Salud → "Caminar 30 minutos"), para que el camino no empiece vacío.
5. **Tarjeta más limpia:** un solo anillo (avance), la lista "Hoy" con los hábitos que tocan y se pueden marcar ahí mismo, y el círculo de "cumplida" pasa al menú "⋯" o a un botón con texto.
6. **Estadísticas enfocadas:** se conservan constancia, racha actual, mejor racha y calendario; el historial por día/semana/mes y "qué días cumples más" quedan en una sección plegable.
7. **Textos que motivan:** "Racha: 0 días" pasa a "Hoy puedes empezar una racha"; "Pendiente" se reemplaza por lo que falta ("Faltan 13 hitos").
8. **Insignias del camino** (HU11 ya tiene el mecanismo): "Primer hito", "Racha de 7 días", "Mitad del camino".

### 3.5 Historias resultantes

| HU | Historia | Requisito | Prioridad | Pts | R / A |
|---|---|---|---|---|---|
| **HU28** (nueva) | Como usuario quiero **trazar el camino de una meta** con hitos y hábitos, para saber qué pasos me llevan a lograrla. | RF3.1 | Should have | 5 | Daniel / Felipe |
| **HU5** (redefinida) | Como usuario quiero **registrar mi avance**: marcar los hitos que logro y lo que hice cada día en mis hábitos, para llevar el seguimiento de mi meta. | RF3.1 | Must have | 5 | Daniel / Felipe |
| **HU29** (nueva) | Como usuario quiero **ver cuánto me falta y qué tan constante soy**, para saber si voy por buen camino. | RF3.3 | Could have | 3 | Daniel / Felipe |

### 3.6 Impacto técnico

| Capa | Cambio |
|---|---|
| Base de datos | Migración **V23**: `sub_meta.tipo` admite `HITO`; columna `fecha_logrado` (date, nula) para los hitos. No se borra nada; las sub-metas actuales quedan como hábitos. |
| Backend | `SubMeta` con el tipo `HITO` y `lograr()/deshacer()`; `CaminoService` devuelve `avance` (hitos logrados / total) aparte de `constancia`; endpoint `PATCH /api/submetas/{id}/logrado`; nuevas acciones de log; criterios de insignias del camino. |
| Frontend | Formulario corto con "Más opciones"; sección "Hitos" en Tu camino; anillo de avance en la tarjeta y la cabecera; aviso de "llegaste"; sugerencias por tipo de meta; textos nuevos. |
| Pruebas | Hoy **no hay ninguna** de `SubMetaService`, `RegistroDiaService`, `CaminoService` ni `CalculoCamino`: se agregan con el rediseño. |
| Documentos | Comentarios "HU-A/B/C" → HU28/HU5/HU29; mockups nuevos en `HU28 Trazar camino`, `HU05 Registrar avance`, `HU29 Avance y constancia`; sección en §2.1 y en §16; issues en GitHub Projects. |

**Esfuerzo estimado:** 8–10 h (backend 3 h, frontend 4–5 h, pruebas 2 h).

---

## 4. HU fuera de alcance y redefinidas

Las cuatro HU que estaban sin resolver vienen del Sprint 0:

| HU | Historia original (Sprint 0) | Depende de | Decisión |
|---|---|---|---|
| **HU15** Logro de meta compartida | "Como participante de una meta compartida quiero que, al cumplirla, quede registrada como un logro en mi perfil, para recordar lo que logré en equipo." | HU6, HU14, HU22 | **Hecha.** El código ya lo hace: cuando el creador marca la meta compartida como cumplida, cada participante con invitación aceptada recibe la fecha de cumplimiento, gana sus insignias y la meta aparece en su perfil. Diferencia con el Sprint 0: la fecha es la misma para todos (no "cada uno con su propia fecha"). Se documenta como redefinida. |
| **HU17** Ganar puntos | "…ganar puntos al cumplir una meta, para sentirme recompensado." | HU6 | **Retirada.** No quedó bien definida (¿cuántos puntos?, ¿por qué?) y complica el código. Las insignias (HU11 y HU12) ya cubren la recompensa. |
| **HU18** Gastar puntos en ítems | "…gastar mis puntos en ítems de personalización (colores, temas, mascotas)." | HU17 | **Retirada.** Sin puntos no hay tienda. |
| **HU19** Personalizar perfil | "…personalizar mi perfil con los colores, temas y elementos **que ya obtuve**." | HU18 | **Redefinida** sin la tienda: temas (Crisálida, Noche, Bosque, Amanecer), modo claro/oscuro, portada, frase, insignia destacada y metas destacadas, **disponibles para todos**. |
| **HU20** Elegir mascota | "…elegir y cambiar la mascota de mi cuenta entre las que ya tengo." | HU18 | **Redefinida, gratis:** todas las mascotas están disponibles desde el principio (ver 4.1). |

### 4.1 HU20: ¿una o dos HU?

Elegir una mascota gratis es **una sola HU**: catálogo fijo, elegir una, guardarla y mostrarla. Lo que daría una segunda HU es que la mascota **reaccione al avance**, que encaja con el nombre de la app (huevo → oruga → crisálida → mariposa). Propuesta:

| HU | Historia | Qué incluye | Prioridad | Pts |
|---|---|---|---|---|
| **HU20** (redefinida) | Como usuario quiero **elegir mi mascota entre varias opciones gratis** y cambiarla cuando quiera, para que me acompañe en la app. | 6 mascotas del tema de la metamorfosis (mariposa, mariquita, abeja, luciérnaga, libélula, caracol) en SVG propio; mascota por defecto al registrarse; selector en Editar perfil; se muestra en Mi perfil, en el perfil público y en Mis metas. Backend: columna `usuario.mascota` (enum, V24), `PUT /api/usuarios/me/mascota`. | Could have | 5 |
| **HU30** (nueva, opcional) | Como usuario quiero que **mi mascota se transforme** a medida que cumplo metas, para ver mi avance de forma divertida. | 4 etapas según las metas cumplidas (0 huevo, 1 oruga, 3 crisálida, 5 mariposa); mensajes según la constancia del día ("¡Hoy cumpliste todo!"). Sin tablas nuevas: se calcula. | Could have | 3 |

**Recomendación:** hacer la **HU20** sí o sí y la **HU30** solo si queda tiempo después de los requisitos P0.

### 4.2 HU20 rediseñada: avatar en lugar de mascota (14 de noviembre de 2026)

Al ver la mascota en la app, el equipo decidió quitarla: los dibujos no conectaban y no decían nada de la persona. La HU20 pasa a ser **Personalizar mi avatar**: *como usuario quiero armar un avatar que se parezca a mí, para que las demás personas me reconozcan sin ver mi foto.*

| Qué | Cómo |
|---|---|
| Estilo | «Big Smile» de DiceBear (ilustraciones de Ashley Seo, CC BY 4.0; código MIT). Se dibuja en el navegador, sin servicios externos. El crédito va en el editor y en el README. |
| Piezas | 8 tonos de piel, 13 peinados, 8 colores de pelo, 8 ojos, 8 bocas y 8 accesorios opcionales. Todo gratis. |
| Editor | Sección «Tu avatar» de Editar perfil: vista previa, una pestaña por pieza con la miniatura de cada opción y «Sorpréndeme». Se guarda con el formulario. |
| Dónde se ve | Asomado detrás del panel de la cabecera, sobre la portada, en Mi perfil, en el perfil de otra persona y en la vista previa; también con el perfil privado (HU26). |
| Backend | V26 quita `usuario.mascota` y agrega `avatar_*` con restricciones check; `Avatar` (@Embeddable), `CatalogoAvatar`, `AvatarPerfil`; `PUT /api/usuarios/me/avatar`; auditoría `CAMBIAR_AVATAR` (los registros viejos con `CAMBIAR_MASCOTA` se conservan: la bitácora es de solo escritura). |
| Pruebas | `CuentaServiceAvatarTest` (6 casos de caja blanca). |

La **HU30 opcional** (la mascota que se transforma) se retira con la mascota.

**Efecto en el backlog:** salen 8 puntos (HU17 3 + HU18 5); la HU20 baja de 13 a 5; entran 13 (HU28 5 + HU29 3 + HU20 5) y 3 opcionales (HU30). Se documenta en §2.1 y en los riesgos (EF-01 se materializó y se mitigó respetando el orden MoSCoW). RF4.2 (puntos) queda **no implementado**; RF4.3 se cumple con la personalización gratuita.

---

## 5. Equipo: salida de Cristian

| Dónde | Cambio |
|---|---|
| Portada de la Entrega final | Ya lista 3 integrantes ✅. Agregar una nota en §1: "Cristian Villalobos dejó el proyecto durante la entrega final; sus HU se reasignaron". |
| RACI (en la Entrega final, no editar el Sprint 0 ya entregado) | Reasignar sus R: HU15 → Daniel, HU19 → Daniel (ya figura así), HU26 → Felipe (ya figura así). HU5 (redefinida), HU28 y HU29 → Daniel, que hizo los 33 commits de sub-metas. HU20 y HU30 → Felipe (como la HU20 en el Sprint 0). |
| `README.md` | Quitar a Cristian de la tabla de roles, quitar "usuario / coach" y explicar los dos niveles como cliente-servidor. |
| Riesgos | Registrar el riesgo materializado "abandono de un integrante" (estaba en riesgos humanos del Sprint 1) y su mitigación. |

---

## 6. Complemento de accesibilidad: opciones

El profesor pide "utilizar complemento de accesibilidad en el proyecto". Las opciones son:

| Opción | Qué es | A favor | En contra |
|---|---|---|---|
| **A. Panel propio** | Componente nuestro (`PanelAccesibilidad.jsx`): botón flotante con tamaño de letra, alto contraste, subrayar enlaces, fuente legible y reducir animaciones; se guarda en el navegador. | Sin dependencias ni cuentas; respeta los 4 temas de la HU19; se puede explicar línea por línea en la sustentación. | 3–4 h de trabajo. Se puede discutir si "complemento" significa algo hecho por terceros. |
| **B. Widget externo comercial** (UserWay, accessiBe) | Una etiqueta `<script>` que agrega un menú de accesibilidad hecho por una empresa. | 15 minutos; muchas funciones. | Requiere cuenta; la versión gratis muestra su marca; carga código de otro servidor (baja el puntaje de rendimiento y choca con la ofuscación y la privacidad); no conoce nuestros temas. |
| **C. Librería open source instalada con npm** (p. ej. Sienna Accessibility, licencia MIT) | Un widget de terceros que se instala como dependencia del proyecto y se empaqueta con nuestro build. | Es literalmente un "complemento"; sin cuenta ni servidor externo; 1 h de trabajo. | Hay que comprobar que funcione bien con los temas oscuros y con Tailwind; verificar la versión antes de instalar. |

**En las tres** se agrega `eslint-plugin-jsx-a11y` (complemento para el desarrollo) y `npm run lint` debe quedar en 0 errores.

**Decisión: C.** Si al probarla choca con los temas oscuros, la respaldamos con A.

---

## 7. Estructura propuesta en Drive

Hoy `Metamorfosis/` solo tiene los 3 documentos.

```
Metamorfosis/
├── Entrega_final_-_Metamorfosis   (doc)
├── Sprint 0 - Metamorfosis        (doc)
├── Sprint 1 - Metamorfosis        (doc)
├── Diagramas/
│   ├── Clases/          HU01_clases.drawio … HU29_clases.drawio        (26)
│   ├── Secuencia/       HU01_secuencia.drawio … HU29_secuencia.drawio  (26)
│   ├── Arquitectura/    modelo_ER.drawio, despliegue_dos_niveles.drawio
│   └── Histórico/       HU05 vieja (registro por fecha sobre la meta)
├── Mockups/             una carpeta por HU con sus PNG
├── Manuales/            Manual técnico.pdf, Manual de usuario.pdf
└── Evidencias/          Pruebas/, Accesibilidad/, Ofuscación/, Logs/, Dos niveles/
```

Los mismos `.drawio` se guardan en el repo (`docs/Diagramas/Clases` y `docs/Diagramas/Secuencia`), y se borran los duplicados, los `.bkp` y las carpetas vacías `HU1`–`HU4`.

> La carpeta **Orion › Diagramas** de tu Drive es de otro proyecto: no se toca.

---

## 8. Plan de trabajo

**Orden:** primero el **código** (rediseño de sub-metas, HU20 y requisitos técnicos), porque los diagramas, los mockups, los manuales y las mediciones tienen que hacerse sobre la versión final. Cada bloque de código va en su propia rama y su propio PR.

### Bloque 1 — Código (14 y 15 de noviembre)

| # | Tarea | Rama | Esfuerzo |
|---|---|---|---|
| 1 | Ofuscación + `sourcemap:false` + medición de tamaños | `rq6-ofuscacion` | 1 h |
| 2 | Complemento de accesibilidad (opción C) + `eslint-plugin-jsx-a11y` + lint en 0 errores | `rq5-accesibilidad` | 1–2 h |
| 3 | Log de aplicación a archivo con rotación | `rq8-log-archivo` | 0,5 h |
| 4 | Rediseño de sub-metas: hitos + hábitos (sección 3), con sus pruebas | `hu28-camino-hitos` | 8–10 h |
| 5 | HU20 Elegir mascota (gratis) | `hu20-mascota` | 4–5 h |
| 6 | JaCoCo + `mvnw verify` | `rq9-pruebas` | 1 h |
| 7 | README y nota de la salida de Cristian | `docs-readme` | 0,5 h |
| 8 | (Opcional) HU30 La mascota se transforma | `hu30-transformacion` | 2–3 h |

### Bloque 2 — Artefactos por HU (16 de noviembre)

| # | Tarea |
|---|---|
| 9 | Los 52 draw.io + modelo ER + despliegue, verificados contra el código final, en `docs/Diagramas/` y en `Drive › Metamorfosis › Diagramas`. |
| 10 | Mockups: capturas nuevas de sub-metas, HU15, HU19 (perfil público) y HU20; subir todos a `Drive › Metamorfosis › Mockups`. |
| 11 | Colección de Postman de todos los endpoints + corrida con Newman. |
| 12 | Ejecutar los 24 casos de la HU10. |
| 13 | Limpiar `docs/` y actualizar `docs/LEEME.txt`. |

### Bloque 3 — Documentos y evidencia (17 de noviembre)

| # | Tarea | Quién |
|---|---|---|
| 14 | Actualizar el doc de Drive (tabla de la sección 9) y completar §16 con todas las HU. | Claude, revisa el equipo |
| 15 | Manual técnico y manual de usuario (PDF). | Claude, revisa el equipo |
| 16 | Desplegar la versión final; evidencia de dos niveles (otro PC y celular). | Equipo |
| 17 | PageSpeed + WAVE sobre la URL pública. | Claude o equipo |
| 18 | SUS con 5 personas o más (el formulario se puede enviar desde el 16). | Equipo |
| 19 | GitHub Projects: estados finales, HU retiradas y nuevas. | Equipo |

### 18 de noviembre — revisión final y entrega

### Opcionales si sobra tiempo

Exportar el reporte a PDF/CSV · PWA · Swagger · Internacionalización es/en.

---

## 9. Documento de Drive: desfases a corregir

| Sección | Problema | Corrección |
|---|---|---|
| §1 | Sin nota sobre la salida de Cristian | Agregarla (sección 5). |
| §1.1 Matriz | Estados del 9 nov | Usar la tabla 1.1 de este plan. |
| §2.1 | HU5 "se retira" sin reemplazo; no menciona sub-metas, HU19 redefinida, HU15, ni el código de invitación | Secciones 3 y 4 de este plan. |
| §2.1 y §4.2 | Dicen que la tabla `registro` se conserva, pero V13 la borra (`drop table if exists registro`) | La reemplaza `registro_sub_meta`. |
| §2.2 | Estados viejos; HU17, HU18 y HU20 "Por hacer" | Estados finales; HU retiradas; HU28 y HU29. |
| §3 | Sin evidencia de acceso desde otro equipo | Diagrama de despliegue + capturas. |
| §4 ORM | V1–V9; entidades que no existen y otras que faltan | Modelo real (V1–V22). |
| §5 Web services | Unas 20 rutas, algunas que no existen | Los 46 endpoints. |
| §7 Ofuscación | Configuración que no está en el repo | Implementarla y medir de nuevo. |
| §9.1 Log | 13 acciones | Las 37 reales. |
| §10 Pruebas | `RegistroServiceTest` no existe; "[completar]" | 14+ clases de prueba y su ejecución. |
| §14 Riesgos | EF-01 sin desenlace; falta el abandono de un integrante | Agregar los riesgos materializados. |
| §16 | Se corta en HU13; faltan 9 HU; "FIGURA PENDIENTE" en HU13, HU24, HU25 y HU26 | Completar (sección 8, #13). |
| §16 (HU6–HU22) | El texto exportado sale vacío bajo "Mockups" y "Diagramas": hay que confirmar que las imágenes estén pegadas | Revisar en PDF. |
| Anexos | Manuales inexistentes | Crearlos y enlazarlos. |

---

## 10. Decisiones

| # | Tema | Decisión |
|---|---|---|
| 1 | Dos niveles | ✅ Cliente-servidor: un servidor ejecuta la app y otro equipo entra. Se demuestra con el despliegue y con acceso desde otro PC y desde un celular. |
| 2 | HU15, HU17, HU18, HU20 | ✅ HU15 va (hecha, redefinida); HU17 y HU18 retiradas; HU20 gratis con varias mascotas; HU19 redefinida. ❓ Confirmar si se intenta la HU30 (mascota que se transforma). |
| 3 | Sub-metas | ✅ Rediseñarlas para que sean útiles: hitos + hábitos, avance real de la meta, formulario corto (sección 3). ❓ Confirmar la propuesta antes de programar. |
| 4 | Diagramas | ✅ Dos archivos draw.io por HU: clases y secuencia. |
| 5 | Complemento de accesibilidad | ✅ Opción C (librería open source con npm). |
| 6 | Equipo | ✅ Cristian sale del proyecto; se gestiona en el README, la Entrega final y la RACI. |
| 7 | Secuencias | ❓ ¿Solo el caso positivo (como dice `docs/LEEME.txt`) o con `alt` para los errores principales? Recomendado: caso positivo + un `alt` con el error más importante de cada HU. |
| 8 | HU20 | ✅ La mascota se quita: la HU20 es **Personalizar mi avatar** con el estilo Big Smile (sección 4.2). La HU30 opcional se retira. |
