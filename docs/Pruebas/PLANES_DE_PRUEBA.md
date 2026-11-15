# Pruebas de la entrega final — planes por HU y evidencia

Fuente en Markdown de lo que va en el documento *Entrega_final_-_Metamorfosis*: la sección 10 completa,
una nota para la sección 11 y el «Plan de pruebas» de cada HU de la sección 16.
Todos los resultados son de la ejecución del **15 de noviembre de 2026** (commit de la rama `pruebas-entrega-final`).
Las salidas completas están en `docs/Evidencias entrega final/datos/`.

---

## 10. Evidencia de la ejecución de pruebas

Cada HU se prueba con casos de caja negra, caja blanca e integración, y el responsable de cada HU los diseña y ejecuta. Daniel, como QA, define el formato y consolida la evidencia del equipo. En la entrega final las pruebas quedaron automatizadas en tres suites que se repiten con un solo comando: las pruebas unitarias de caja blanca del backend (JUnit 5 y Mockito), las pruebas de caja negra e integración de los web services (colección de Postman ejecutada con Newman contra la API y PostgreSQL) y las pruebas unitarias y de componentes del frontend (Vitest y React Testing Library). Lo que no está automatizado, como el recorrido visual de cada pantalla, se prueba a mano en el navegador.

### 10.1 Herramientas

| Tipo | Herramienta | Qué se prueba | Comando |
|---|---|---|---|
| Unitarias del backend (caja blanca) | JUnit 5 y Mockito | Ramas de los servicios, del filtro JWT y de las plantillas de correo, con los repositorios simulados | `mvnw test` |
| Arranque del backend | Spring Boot Test (`BackendApplicationTests`) | El contexto completo levanta y Flyway aplica las 28 migraciones sobre PostgreSQL | `mvnw verify` |
| Cobertura del backend | JaCoCo 0.8.13 | Instrucciones, ramas y líneas cubiertas | `mvnw verify` (informe en `backend/target/site/jacoco/index.html`) |
| Web services (caja negra e integración) | Postman y Newman | Filtro JWT, controlador, servicio y PostgreSQL: código HTTP, mensaje y datos de cada respuesta | `newman run docs/Pruebas/postman/Metamorfosis.postman_collection.json -e docs/Pruebas/postman/local.postman_environment.json` |
| Log de auditoría | Consulta SQL a `log_actividad` | Que cada acción deja su fila EXITO o ERROR | `select accion, resultado, count(*) from log_actividad group by 1, 2` |
| Unitarias y de componentes del frontend | Vitest, React Testing Library y jsdom | Utilidades de `src/utils` y los componentes de HU6 y HU27 | `npm test` / `npm run test:cobertura` |
| Interfaz | Prueba manual en el navegador | Recorrido de cada pantalla en 375 px y 1280 px y con teclado | — |
| Accesibilidad | ESLint con `eslint-plugin-jsx-a11y`, Lighthouse y PageSpeed Insights | Reglas del linter y puntaje de accesibilidad (sección 6) | `npm run lint` |

### 10.2 Estado de las pruebas (15 de noviembre de 2026)

| Alcance | Casos | Estado |
|---|---|---|
| HU1 a HU5, HU8 y HU9 (Sprint 1) | 212 | Ejecutados en el Sprint 1, todos pasan (documento del Sprint 1) |
| HU10 (pendiente del Sprint 1) | 24 | Pasan. Automatizados: 6 de caja blanca (`AuthServiceCerrarSesionTest`) y 11 peticiones de la carpeta HU10 de Postman |
| Pruebas JUnit del backend | 253 pruebas en 29 clases | 0 fallos (`mvnw clean verify`, BUILD SUCCESS) |
| Cobertura del backend (JaCoCo) | 13.700 instrucciones, 1.038 ramas | 69,3 % de instrucciones, 58,4 % de ramas y 70,0 % de líneas |
| Colección de Postman | 162 peticiones, 291 aserciones, 23 carpetas | 0 fallos (Newman) |
| Pruebas del frontend | 32 pruebas en 2 archivos | 0 fallos (Vitest); cobertura de `src/utils`: 95,0 % de sentencias y 93,7 % de ramas |
| Linter y build del frontend | `npm run lint` y `npm run build` | 0 errores y build correcto |
| Requisitos: accesibilidad, ofuscación y logs | Ver secciones 6, 7 y 9 | Medidos el 14 de noviembre de 2026 |

### 10.3 Inventario de las pruebas automatizadas

| Suite | HU | Pruebas |
|---|---|---|
| `MetaServiceMarcarCumplidaTest` | HU6, HU11, HU15 | 10 |
| `MetaServiceListarMetasTest` | HU27 | 10 |
| `MetaServiceVisibilidadTest` | HU25 | 11 |
| `ReporteServiceTest` | HU7 | 9 |
| `InsigniaServiceTest` | HU11 | 11 |
| `InsigniaServiceGaleriaTest` | HU12 | 9 |
| `PerfilServiceTest` | HU22 | 11 |
| `PerfilServicePublicoTest` | HU24 | 10 |
| `PerfilServicePrivacidadTest` | HU26 | 8 |
| `PerfilServicePersonalizacionTest` | HU19 | 13 |
| `InvitacionServiceInvitarTest` | HU13 | 11 |
| `InvitacionServiceResponderTest` | HU14 | 14 |
| `CodigoUnionServiceTest` | HU13, HU14 | 13 |
| `LogroCompartidoServiceTest` | HU21 | 10 |
| `CuentaServiceDatosYFotoTest` | HU23 | 14 |
| `CuentaServiceAvatarTest` | HU20 | 6 |
| `CuentaServiceAparienciaTest` | HU30 | 6 |
| `SubMetaServiceHitosTest` | HU28, HU5, HU29, HU15 | 10 |
| `RegistroDiaServiceTest` | HU5 | 6 |
| `CalculoCaminoTest` | HU29 | 12 |
| `ContrasenaServiceTest` | HU32 | 7 |
| `JwtAuthFilterTest` | HU32 | 4 |
| `PlantillaContrasenaCambiadaTest` | HU32, idiomas | 3 |
| `RecuperacionContrasenaServiceTest` | HU33 | 13 |
| `PlantillaCodigoRecuperacionTest` | HU33, idiomas | 3 |
| `AuthServiceCerrarSesionTest` | HU10 | 6 |
| `TraductorMensajesTest` | Internacionalización | 8 |
| `PlantillasCorreoTest` | Internacionalización (HU8, HU13) | 4 |
| `BackendApplicationTests` | Arranque y migraciones | 1 |
| **Total JUnit** | | **253** |
| `frontend/src/utils/utilidades.test.js` (Vitest) | HU5, HU7, HU8, HU12, HU13, HU22, HU27, HU28, HU32, HU33 | 23 |
| `frontend/src/components/componentes.test.jsx` (Vitest y React Testing Library) | HU6, HU27 | 9 |
| **Total frontend** | | **32** |

### 10.4 Limitaciones conocidas

El aspecto de auditoría (`AuditoriaAspect`) no tiene prueba JUnit propia; se verifica con la consulta a `log_actividad` después de correr la colección de Postman. Los servicios de las HU del Sprint 1 (HU1, HU2, HU4, HU8 y HU9) se prueban con Postman y con los casos documentados en el Sprint 1. En el frontend se automatizaron las utilidades y dos componentes; el resto de pantallas se prueba a mano. La colección no cubre los casos que necesitan leer un correo real (el enlace de registro de HU8, el código de HU33) ni los que piden muchas cuentas (el máximo de 10 personas por meta); esos casos los cubren las pruebas JUnit.

**Evidencia.** `docs/Evidencias entrega final/`: `10_pruebas_mvnw_verify.png`, `10_pruebas_cobertura_jacoco.png`, `05_web_services_newman.png`, `10_pruebas_frontend_vitest.png` y `10_pruebas_frontend_cobertura.png`; salidas completas en `datos/mvnw-verify.txt`, `datos/newman-salida.txt`, `datos/vitest-salida.txt` y `datos/log_actividad_tras_newman.txt`.

---

## 11. Nota para la sección de internacionalización

**Pruebas.** Los mensajes traducidos y los correos en los dos idiomas se prueban con `TraductorMensajesTest` (8 pruebas), `PlantillasCorreoTest` (4), `PlantillaContrasenaCambiadaTest` (3) y `PlantillaCodigoRecuperacionTest` (3). Las pruebas del frontend corren en español.

---

## 16. Planes de prueba por HU

En cada HU, la tabla **Automatización y evidencia** dice qué prueba automatizada respalda el plan y su resultado del 15 de noviembre. Lo que no aparece en esa tabla se prueba a mano.

### HU6 — Marcar meta como cumplida

**Correcciones al plan:** ninguna. El plan coincide con el código.

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `MetaServiceMarcarCumplidaTest`: `metaInexistenteLanza404`, `usuarioQueNoEsElCreadorLanza403`, `elPermisoSeRevisaAntesQueElEstado`, `marcarUnaMetaYaCumplidaNoCambiaNadaYConservaLaFecha`, `marcarDejaLaMetaCumplidaConFechaDeHoy`, `laRespuestaTraeLaFechaDeLaParticipacionDelCreador`, `unaMetaActivaNoTieneFechaDeCumplimiento` | 7 de 7 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU6, HU11 y HU15 · Cumplir la meta»: «Beto no es el creador» (403 con su mensaje), «Ana la marca como cumplida», «Marcarla otra vez no cambia nada (criterio 4)», «El camino queda en solo lectura»; carpeta «Seguridad»: «Mis metas sin token» (401) | 0 fallos |
| Interfaz | Vitest, `componentes.test.jsx` (HU6): el diálogo se anuncia como `alertdialog` con el foco en «Marcar como cumplida»; confirmar llama una vez; Cancelar, la X y Esc cierran sin marcar; mientras guarda se deshabilitan los botones; el error se muestra como alerta | 5 de 5 pasan |
| Log de auditoría | Consulta a `log_actividad`: filas `MARCAR_CUMPLIDA` con EXITO y con ERROR | Comprobado |

### HU27 — Filtrar metas por estado, tipo y plazo

**Correcciones al plan:** ninguna.

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `MetaServiceListarMetasTest` (las 10 filas de la tabla, una prueba por fila): `usuarioInexistenteLanzaExcepcionSinConsultarMetas`, `sinFiltrosDevuelveSoloLasActivas`, `estadoCumplidaDevuelveSoloLasCumplidasConSuFecha`, `estadoTodasDevuelveActivasYCumplidas`, `filtraPorTipo`, `filtraPorPlazo`, `combinaLosTresFiltros`, `sinCoincidenciasDevuelveListaVacia`, `elLogGuardaLosFiltrosUsados`, `filtroEstadoIncluyeSegunElEstadoDeLaMeta` | 10 de 10 pasan |
| Caja negra (API) | Postman, carpeta «HU3 y HU27 · Ver y filtrar mis metas»: activas, filtro combinado y 400 con un valor inválido | 0 fallos |
| Interfaz | Vitest: `filtrosDesdeUrl` ignora valores que no existen (fila «URL inválida»), `filtrosAUrl` y `hayFiltros`; `FiltrosMetas` marca «Activas» con `aria-pressed`, avisa cada cambio, muestra «Limpiar filtros» solo con filtros y anuncia «Buscando metas...» | 9 de 9 pasan |
| Log de auditoría | Filas `LISTAR_METAS` EXITO | Comprobado |

### HU7 — Ver reporte general

**Correcciones al plan:** en la caja blanca, fila «Rama cumplida / activa por tipo», columna Entrada: cambiar «SALUD cumplida 3» por «SALUD_BIENESTAR cumplida 3».

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `ReporteServiceTest` (las 9 filas, una prueba por fila): `usuarioInexistenteLanzaExcepcionSinConsultarElReporte`, `sinMetasTodoEnCeroPeroConLosTresPlazosYLosSeisMeses`, `totalesYPorcentajeGeneral`, `porTipoSoloIncluyeTiposConMetasEnElOrdenDelEnum`, `porPlazoSeparaActivasYCumplidas`, `cumplidasPorMesCuentaLasFechasDeLosUltimosSeisMeses`, `consultaLasFechasDesdeElPrimerDiaDelMesMasAntiguo`, `elLogGuardaTotalYCumplidas`, `porcentajeRedondeaYEvitaDividirPorCero` | 9 de 9 pasan |
| Caja negra (API) | Postman, carpeta «HU7 · Reporte general»: al menos una cumplida y porcentaje entre 0 y 100 | 0 fallos |
| Interfaz | Vitest: `tipoReporteValido` (un valor que no existe vuelve a «general»), `etiquetaMes` y `tablasDelReporte` (la tabla alternativa tiene los mismos datos que las gráficas) | 3 de 3 pasan |
| Log de auditoría | Filas `VER_REPORTE` EXITO | Comprobado |

### HU11 — Recibir insignia

**Correcciones al plan:** agregar a la caja blanca tres filas que ya tienen prueba: «Quinta meta» (5 cumplidas, otorga En racha), «Tercera de corto plazo» (otorga Corredor de corto plazo) y «Método alcanza» (compara el conteo con `cantidadRequerida`).

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `InsigniaServiceTest`: `sinMetasCumplidasNoOtorgaNada`, `laPrimeraMetaDaPrimerPasoConLaFechaDeHoy`, `cuatroMetasNoAlcanzanEnRacha`, `laQuintaMetaDaEnRacha`, `unaInsigniaYaObtenidaNoSeVuelveAGuardar`, `elCriterioDePlazoSoloCuentaEsePlazo`, `laTerceraDeCortoPlazoDaCorredor`, `tiposRepetidosCuentanComoUnSoloTipo`, `tresTiposDistintosDanTodoterreno`, `variasALaVezSeGuardanTodasEnElOrdenDelCatalogo`, `alcanzaComparaContraLaCantidadRequerida`; `MetaServiceMarcarCumplidaTest`: `alMarcarSeRevisanLasInsigniasYQuedanEnElLog`, `marcarUnaMetaYaCumplidaNoCambiaNadaYConservaLaFecha`, `conMetaAjenaNoSeRevisanInsignias` | 14 de 14 pasan |
| Caja negra (API) | Postman: «Ana la marca como cumplida» (trae la lista de insignias nuevas) y «Marcarla otra vez no cambia nada» (no da insignias otra vez) | 0 fallos |
| Interfaz | Prueba manual (aviso, cierre a los 6 segundos, varios avisos, lector de pantalla) | Manual |

### HU22 — Ver mi perfil

**Correcciones al plan:** agregar a la caja blanca «Empate de fecha» (primero la de código mayor), «Activas fuera de las últimas cumplidas» y «Contexto de auditoría» (el log recibe el resumen).

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `PerfilServiceTest`: `usuarioInexistenteLanzaExcepcionSinConsultarMetasNiInsignias`, `sinMetasElResumenQuedaEnCeroSinDividirPorCero`, `metasActivasYCumplidasDanElResumenYElPorcentaje`, `lasCumplidasVanDeLaMasRecienteALaMasAntigua`, `conMismaFechaVaPrimeroLaDeCodigoMayor`, `soloSeMuestranLasTresCumplidasMasRecientes`, `lasActivasNoAparecenEntreLasUltimasCumplidas`, `sinInsigniasLaListaVaVaciaYElTotalEsElCatalogo`, `conInsigniasSeRespetaElOrdenDeLaConsulta`, `devuelveLosDatosPersonalesDelUsuario`, `dejaElResumenEnElContextoDeAuditoria` | 11 de 11 pasan |
| Caja negra (API) | Postman: «Mi perfil» y «HU15: aparece en el perfil de Beto» | 0 fallos |
| Interfaz | Vitest: `iniciales` y `mesYAnio` (fecha de registro) | 2 de 2 pasan |
| Log de auditoría | Filas `VER_PERFIL` EXITO | Comprobado |

### HU12 — Ver galería de insignias

**Correcciones al plan:** agregar a la caja blanca «InsigniaService.avance directo», que ya tiene prueba.

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `InsigniaServiceGaleriaTest`: `sinMetasCumplidasTodasPendientesEnCero`, `criterioMetasCumplidasCuentaElTotal`, `criterioPorPlazoCuentaSoloElPlazoPedido`, `criterioTiposDistintosCuentaCadaTipoUnaVez`, `elAvanceNoPasaDeLoQuePideLaInsignia`, `insigniaGanadaTraeSuFechaYElAvanceCompleto`, `insigniaGanadaSeConservaAunqueYaNoHayaMetasCumplidas`, `sigueElOrdenDelCatalogoYCuentaLasGanadas`, `avanceDirectoPorCriterio` | 9 de 9 pasan |
| Caja negra (API) | Postman: «Galería de insignias» (catálogo de 6 y al menos una ganada) | 0 fallos |
| Interfaz | Vitest: `comoGanarla` (texto de cada criterio, por ejemplo «Cumple 5 metas») y `porcentajeAvance` (0 a 100, sin dividir por cero) | 2 de 2 pasan |
| Log de auditoría | Filas `VER_INSIGNIAS` EXITO | Comprobado |

### HU25 — Ocultar metas

**Correcciones al plan:** agregar a la caja blanca «Solicitud nula», «Ocultar una meta cumplida» (no cambia el estado ni la fecha) y «Ocultar dos veces».

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `MetaServiceVisibilidadTest`: `solicitudSinValorLanza400SinConsultarLaMeta`, `solicitudNulaLanza400`, `metaInexistenteLanza404`, `usuarioQueNoParticipaLanza403`, `invitacionPendienteLanza403`, `elCreadorOcultaLaMeta`, `ocultarNoCambiaElEstadoNiLaFechaDeUnaMetaCumplida`, `mostrarDeNuevoQuitaLaMarca`, `ocultarDosVecesLaDejaOculta`, `enUnaMetaCompartidaSoloCambiaLaParticipacionDeQuienLaOculta` (filas «Meta compartida» y «MetaResponse por usuario»), `elLogRecibeElValorDeOculta` | 11 de 11 pasan |
| Caja negra (API) | Postman, carpeta «HU25 · Ocultar metas»: ocultar, mostrar, 400 «Indica si la meta queda oculta» y 404 con una meta que no existe; carpeta HU4: «HU25: Beto no participa, no la oculta» (403 «Solo los participantes de la meta pueden ocultarla») | 0 fallos |
| Log de auditoría | Filas `CAMBIAR_VISIBILIDAD` con EXITO y con ERROR | Comprobado |

### HU24 — Consultar perfil de otros

**Correcciones al plan:** la fila de caja blanca «Código de entidad del log» no tiene prueba JUnit (ninguna prueba cubre `AuditoriaAspect`): pasarla a la tabla de integración, donde ya está la fila «Aspecto de auditoría y log_actividad». Agregar a la caja blanca «Máximo 20 resultados».

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `PerfilServicePublicoTest`: `textoNuloOCortoLanza400`, `busquedaCortaNoConsultaElRepositorio`, `elPatronQuedaSinMayusculasTildesNiEspaciosDeMas`, `seQuitanLosComodines`, `laBusquedaDevuelvePersonasSinCorreoYExcluyeAQuienBusca`, `laBusquedaPideMaximoVeintePersonas`, `personaInexistenteLanza404`, `lasMetasOcultasNoAparecenNiCuentan`, `sinMetasElResumenQuedaEnCero`, `elPerfilPublicoNoLlevaCorreo` | 10 de 10 pasan |
| Caja negra (API) | Postman, carpeta «HU24 y HU26»: «Buscar «bet»», «Búsqueda de una letra» (400), «Perfil público de Beto» (sin correo), «Persona que no existe» (404) | 0 fallos |
| Log de auditoría | Filas `BUSCAR_USUARIOS` y `VER_PERFIL_PUBLICO` con EXITO y con ERROR | Comprobado |

### HU26 — Perfil privado

**Correcciones al plan:** ninguna. Una sola prueba (`activarGuardaElPerfilPrivadoYLoRegistraEnElLog`) cubre las filas «Activar» y «Log».

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `PerfilServicePrivacidadTest`: `sinValorLanza400SinConsultarElUsuario`, `usuarioInexistenteLanza404`, `activarGuardaElPerfilPrivadoYLoRegistraEnElLog`, `desactivarVuelveAPublico`, `unPerfilPrivadoSoloEnviaLaCabecera`, `unPerfilPublicoSigueIgualQueEnLaHu24`, `laBusquedaMarcaElPerfilPrivado`, `elDuenoVeTodoSuPerfilConElInterruptorEncendido` | 8 de 8 pasan |
| Caja negra (API) | Postman: «Beto hace su perfil privado», «Ana ve el perfil privado de Beto» (privado y sin metas), «Beto lo vuelve público» | 0 fallos |
| Log de auditoría | Filas `CAMBIAR_PRIVACIDAD` EXITO | Comprobado |

---

Los trece planes siguientes se reescriben completos, con el mismo nivel de detalle que el de HU6.

### HU13 — Invitar a una meta

Servicios: `POST /api/metas/{codigoMeta}/invitaciones` con cuerpo `{"codigoUsuario": n}`, y `GET`, `POST` y `DELETE /api/metas/{codigoMeta}/codigo` para el código de unión. Quien invita sale del token. Criterio central: invitar responde 201 con `{participante, correoEnviado}` y la participación PENDIENTE; 409 si la persona ya participa o ya tiene una invitación pendiente; 403 si quien invita no es el creador; 404 si la persona o la meta no existen; 400 si la meta está cumplida, el pedido está vacío, la persona es uno mismo o la meta ya tiene 10 personas; 401 sin token. El código de unión se genera con 201 y `{codigo, expira}`: 8 caracteres sin letras ambiguas, que vencen en 7 días.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Invitar a una persona | Ana invita a Beto en su meta activa | 201; la participación de Beto queda PENDIENTE |
| Invitarla dos veces | La misma invitación otra vez | 409 «Beto Newman ya tiene una invitación pendiente a esta meta»; no se crea otra participación |
| Invitar a quien ya participa | Beto ya aceptó | 409 «Beto Newman ya participa en esta meta» |
| Quien invita no es el creador | Beto invita a Ana a la meta de Ana | 403 «Solo el creador puede invitar personas a la meta» |
| Meta cumplida | Invitar a una meta CUMPLIDA | 400 «Solo se puede invitar a metas activas» |
| Invitarse a sí mismo | `codigoUsuario` igual al de Ana | 400 «No puedes invitarte a ti mismo» |
| Pedido sin persona | Cuerpo `{}` | 400 «Elige a quién quieres invitar» |
| Persona que no existe | `codigoUsuario` 999999 | 404 «No existe el usuario con código 999999» |
| Meta llena | La meta ya tiene 10 personas | 400 «La meta ya tiene el máximo de 10 personas» |
| Generar el código para unirse | `POST /codigo` como creadora | 201 con un código de 8 caracteres del alfabeto sin ambiguos y `expira` dentro de 7 días |
| Ver el código | `GET /codigo` | 200 con el código vigente |
| Otro participante pide el código | `GET /codigo` con el token de Beto | 403 «Solo el creador puede manejar el código de la meta» |
| Desactivar el código | `DELETE /codigo` | 204; el código deja de servir |
| Unirse con un código que no existe | `POST /api/metas/unirse` con `{"codigo": "ZZZZZZ"}` | 400 «Ese código no existe o ya venció. Revisa que esté bien escrito.» |
| Unirse sin código | `POST /api/metas/unirse` con `{}` | 400 «Escribe el código que te compartieron» |
| Petición sin token | Sin Authorization | 401 «Autenticación requerida» |
| Interfaz: invitar | Menú ⋯ → Invitar personas, buscar «bet» y pulsar Invitar | El botón cambia a «Invitado» y aparece el aviso «Le enviamos la invitación a Beto por correo. Cuando la acepte, aparecerá en tu meta.» |
| Interfaz: correo que no se envió | Invitar con el proveedor de correo caído | Aviso «Invitaste a Beto. No pudimos enviarle el correo, pero verá la invitación al entrar a Metamorfosis.» |
| Interfaz: meta cumplida | Abrir el menú ⋯ de una meta cumplida | La opción «Invitar personas» no aparece |
| Interfaz: celular | La ventana de invitar en 375 px | Ocupa toda la pantalla y se usa sin desplazamiento horizontal |

**Caja blanca.** Sobre `InvitacionService.invitar` y `CodigoUnionService`. Meta de cobertura: todas las ramas de cada decisión.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Ruta feliz de invitar | Meta activa del creador y una persona nueva | Participación PENDIENTE y correo enviado con los enlaces Aceptar y Rechazar |
| Falla el envío del correo | `CorreoService` lanza una excepción | La invitación igual queda guardada y `correoEnviado` es false |
| Decisión de creador | Invita alguien que no creó la meta | `PermisoDenegadoException` |
| Decisión de estado de la meta | Meta CUMPLIDA | «Solo se puede invitar a metas activas» |
| Invitarse a sí mismo | `codigoUsuario` igual al del creador | «No puedes invitarte a ti mismo» |
| Pedido vacío | `InvitarRequest` sin `codigoUsuario` | «Elige a quién quieres invitar» |
| Persona inexistente | `codigoUsuario` que no existe | `UsuarioNoEncontradoException` (404) |
| Duplicado pendiente | Persona con invitación PENDIENTE | `ConflictoException`; no se guarda nada |
| Duplicado aceptado | Persona que ya participa | `ConflictoException` |
| Volver a invitar | Persona que rechazó antes | Su participación vuelve a PENDIENTE |
| Máximo de personas | Meta con 10 participantes visibles (`MAXIMO_PARTICIPANTES = 10`) | «La meta ya tiene el máximo de 10 personas» |
| Generar código | Meta activa del creador | 8 caracteres y vence en 7 días |
| Código repetido | El código generado ya existe | Se genera otro |
| Permiso sobre el código | Otro participante | «Solo el creador puede manejar el código de la meta» |
| Código de una meta cumplida | Meta CUMPLIDA | «Solo se puede invitar a metas activas» |
| Ver y desactivar | Código vigente | Se muestra; al desactivarlo deja de existir |
| Código vencido | Código con más de 7 días | No se muestra |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| Filtro JWT, controlador, servicio, repositorios y PostgreSQL | Ana invita a Beto | 201 y fila PENDIENTE en `participacion` |
| Restricción de duplicados con la base | Repetir la invitación | 409 y una sola fila de Beto en `participacion` |
| Correo (Brevo) y enlace de respuesta | Invitar desde la app desplegada | Llega el correo con «Aceptar» y «Rechazar»; los enlaces abren la app pública |
| Aspecto de auditoría y `log_actividad` | Invitar con éxito y con errores | Filas `INVITAR_META` con EXITO y con ERROR |
| HU13 con HU14 | Beto acepta la invitación | La meta aparece en Mis metas de Beto |
| Interfaz React, API y base de datos | Invitar desde la ventana y unirse con el código | El botón cambia a «Invitado» y la persona aparece en Participantes |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `InvitacionServiceInvitarTest`: `invitaQuedaPendienteYEnviaElCorreoConLosDosEnlaces`, `siElCorreoFallaLaInvitacionQuedaIgual`, `soloElCreadorInvita`, `noSeInvitaAUnaMetaCumplida`, `noPuedesInvitarteATiMismo`, `pedidoSinPersonaEsInvalido`, `personaInexistente`, `noSeInvitaDosVeces`, `noSeInvitaAQuienYaParticipa`, `aQuienRechazoSeLePuedeVolverAInvitar`, `maximoDePersonasPorMeta`; `CodigoUnionServiceTest`: `generaUnCodigoDeOchoCaracteresQueVenceEnSieteDias`, `siElCodigoYaExisteGeneraOtro`, `soloElCreadorManejaElCodigo`, `noSeGeneraCodigoParaUnaMetaCumplida`, `verYDesactivar`, `unCodigoVencidoNoSeMuestra` | 17 de 17 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU13 · Invitar a una meta» (12 peticiones): todas las filas de caja negra salvo «Invitar a quien ya participa», «Meta cumplida» y «Meta llena», que quedan en JUnit; «Sin token · HU13 invitar» en la carpeta «Seguridad» | 0 fallos |
| Log de auditoría | Filas `INVITAR_META` con EXITO y con ERROR; `GENERAR_CODIGO_META` y `DESACTIVAR_CODIGO_META` EXITO | Comprobado |
| Interfaz y correo | Prueba manual | Manual |

### HU14 — Responder invitación

Servicios: `GET /api/invitaciones`; `PATCH /api/invitaciones/{codigoMeta}` con `{"aceptar": true|false}`; desde el correo, `GET /api/invitaciones/correo?token=`, `POST /api/invitaciones/correo/aceptar` y `POST /api/invitaciones/correo/rechazar`; y `POST /api/metas/unirse` con `{"codigo": "..."}`. El `GET` y el rechazo desde el correo son públicos (`SeguridadConfig`). Criterio central: al aceptar, 200 y la meta aparece en `GET /api/metas` del invitado; sin invitación pendiente, 404 «No tienes una invitación pendiente a esta meta»; con un enlace del correo vencido, usado o que no existe, 410; rechazar desde el correo responde 204.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Ver mis invitaciones | Beto con una invitación de Ana | 200 con la meta, quién invitó y cuántos participan |
| Respuesta vacía | `PATCH` con `{}` | 400 «Indica si aceptas o rechazas la invitación» |
| Sin invitación | Ana responde sobre su propia meta | 404 «No tienes una invitación pendiente a esta meta» |
| Enlace del correo que no existe | `GET /api/invitaciones/correo?token=enlace-falso` sin sesión | 410 |
| Rechazar con un enlace falso | `POST /api/invitaciones/correo/rechazar` sin sesión | 410 |
| Aceptar | `PATCH` con `{"aceptar": true}` | 200; Beto queda participando |
| Aceptar otra vez | El mismo `PATCH` | 404 «No tienes una invitación pendiente a esta meta» |
| Meta en Mis metas | `GET /api/metas` de Beto | 200 y la meta aparece en la lista |
| Ya participa | Beto se une con un código de la misma meta | 409 «Ya participas en esta meta» |
| Trabajo en equipo | Beto logra el hito 2 de la meta | 200; queda quién lo logró y el avance llega a 100 % |
| Rechazar | `PATCH` con `{"aceptar": false}` | 200; la participación queda RECHAZADA |
| Aceptar una meta ya cumplida | La meta se cumplió antes de responder | 400 «Esta meta ya se cumplió: ya no se pueden unir más personas» |
| Otra cuenta con el enlace | Aceptar desde el correo con la sesión de otra persona | 403 «Esta invitación es para otra cuenta (…)» con el correo enmascarado |
| Demasiados intentos con códigos | 10 códigos falsos en 15 minutos | 429 «Demasiados intentos con códigos. Espera unos minutos e inténtalo de nuevo.» |
| Petición sin token | `GET /api/invitaciones` sin Authorization | 401 «Autenticación requerida» |
| Interfaz: rechazar desde el correo | Abrir «Rechazar» sin sesión | Página de confirmación y la invitación desaparece |
| Interfaz: código no válido | Escribir un código falso | «Ese código no existe o ya venció. Revisa que esté bien escrito.» |
| Interfaz: celular | Invitaciones en 375 px | Se usa sin desplazamiento horizontal |

**Caja blanca.** Sobre `InvitacionService` (responder desde la app y desde el correo) y `CodigoUnionService.unirse`. Meta de cobertura: todas las ramas.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Lista de pendientes | Usuario con invitaciones | Cada una con quién invitó y cuántos participan |
| Aceptar desde la app | Invitación pendiente | Participación ACEPTADA y el enlace del correo queda anulado |
| Rechazar desde la app | Invitación pendiente | Participación RECHAZADA |
| Sin invitación | Meta sin invitación pendiente | `InvitacionNoEncontradaException` (404) |
| Ya respondida | Invitación ACEPTADA o RECHAZADA | No se vuelve a responder |
| Respuesta vacía | `aceptar` null | «Indica si aceptas o rechazas la invitación» |
| Meta cumplida | Aceptar o rechazar | Aceptar se rechaza; rechazar sí funciona |
| Página del correo | Token válido | Muestra la meta con el correo enmascarado |
| Enlace vencido o inexistente | Token vencido o que no existe | `InvitacionNoValidaException` (410) |
| Aceptar con la cuenta invitada | Token y sesión de Beto | Participación ACEPTADA |
| Otra cuenta | Token de Beto y sesión de otra persona | `PermisoDenegadoException` |
| Rechazar sin sesión | Token válido sin sesión | RECHAZADA y registro manual en el log |
| Un solo uso | Usar el mismo enlace dos veces | La segunda vez se rechaza |
| Enmascarar el correo | Correos de distinto largo | Se ocultan los caracteres del medio |
| Unirse con código | Código en minúsculas o con espacios | Se normaliza y se une |
| Ya participa | Participante aceptado | `ConflictoException` |
| Invitación pendiente y código | Persona con invitación pendiente | Se acepta la invitación |
| Código inválido | Inexistente, vencido o vacío | «Ese código no existe o ya venció…» o «Escribe el código que te compartieron» |
| Bloqueo por intentos | 10 fallos en 15 minutos | `SolicitudBloqueadaException` (429) |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| Filtro JWT, controlador, servicio y PostgreSQL | Beto acepta | 200 y `participacion` en ACEPTADA |
| HU14 con HU3 | Beto consulta Mis metas | La meta compartida aparece en su lista |
| HU14 con HU5 | Beto logra un hito | El hito queda logrado para todos los participantes |
| Rutas públicas | Enlaces del correo sin sesión | `SeguridadConfig` deja pasar el `GET` y el rechazo; responden 410 con un enlace falso |
| Correo, página pública y API | Rechazar desde el enlace del correo sin sesión | Queda RECHAZADA y hay una fila `RESPONDER_INVITACION` en el log |
| Aspecto de auditoría | Ver invitaciones y responder | Filas `VER_INVITACIONES` EXITO y `RESPONDER_INVITACION` con EXITO y con ERROR |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `InvitacionServiceResponderTest` (14): `listaLasPendientesConQuienInvitoYCuantosParticipan`, `aceptarDesdeLaAppLaDejaParticipandoYAnulaElEnlace`, `rechazarDesdeLaApp`, `sinInvitacionPendiente`, `unaInvitacionYaRespondidaNoSeVuelveAResponder`, `respuestaVaciaEsInvalida`, `noSeAceptaUnaMetaYaCumplidaPeroSiSePuedeRechazar`, `laPaginaDelCorreoMuestraLaMetaYElCorreoEnmascarado`, `enlaceVencidoOInexistente`, `aceptarDesdeElCorreoConLaCuentaInvitada`, `otraCuentaNoPuedeAceptarConElEnlace`, `rechazarDesdeElCorreoSinSesionQuedaEnElLog`, `elEnlaceSirveUnaSolaVez`, `enmascarar`; `CodigoUnionServiceTest`: `seUneConElCodigoEscritoComoSea`, `quienYaParticipaNoSeUneDeNuevo`, `siTeniaUnaInvitacionPendienteLaAcepta`, `codigoInexistenteVencidoOVacio`, `conDiezFallosSeBloqueaUnRato`, `maximoDePersonas`, `normalizar` | 21 de 21 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU14 · Responder invitación» (12 peticiones): las diez primeras filas de caja negra; «Sin token · HU14 mis invitaciones» en «Seguridad» | 0 fallos |
| Log de auditoría | Filas `VER_INVITACIONES` y `RESPONDER_INVITACION` | Comprobado |
| Interfaz, correo real y bloqueo por intentos | Prueba manual (el bloqueo también lo cubre JUnit) | Manual |

### HU15 — Logro de meta compartida

Servicios: `PATCH /api/metas/{codigoMeta}/cumplida` sobre una meta compartida, y `GET /api/usuarios/me/perfil` de cada participante. Criterio central: solo el creador marca la meta como cumplida (403 para otro participante); la meta queda cumplida para todos los que aceptaron, con la misma fecha; después el camino es de solo lectura (400 «La meta ya está cumplida: su camino es solo de lectura»).

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Beto no es el creador | Beto marca la meta de Ana | 403 «Solo el creador puede marcar la meta como cumplida» |
| Ana marca la meta cumplida | `PATCH /cumplida` | 200 con estado CUMPLIDA y la lista de insignias nuevas |
| Marcar otra vez | Segundo `PATCH` | 200; conserva la fecha original y no da insignias otra vez |
| Aparece en el perfil de Beto | `GET /api/usuarios/me/perfil` como Beto | 200 y la meta está entre sus últimas cumplidas |
| Camino en solo lectura | Lograr un hito después de cumplida | 400 «La meta ya está cumplida: su camino es solo de lectura» |
| Tampoco se registran días | Registrar el hábito después de cumplida | 400 con el mismo mensaje |
| Interfaz: hito del compañero | Beto logra un hito; Ana abre Tu camino | El hito muestra «Logrado el {fecha} por Beto» |

**Caja blanca.** Sobre `MetaService.marcarCumplida` y `SubMetaService`.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Meta compartida | Dos participantes que aceptaron y uno pendiente | Se cumple para los dos que aceptaron |
| Insignias | Marcar la meta cumplida | Se revisan las insignias y quedan en el contexto del log |
| Quién logró el hito | Un participante logra un hito | Queda su nombre y la fecha |
| Solo lectura | Lograr o deshacer en una meta cumplida | `ValidacionException` con el mensaje de solo lectura |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Flujo de la carpeta «HU6, HU11 y HU15» | La meta queda cumplida en el perfil de Ana y en el de Beto |
| HU15 con HU7 | Beto abre su reporte | La meta cuenta como cumplida |
| Aspecto de auditoría | Marcar la meta compartida | Fila `MARCAR_CUMPLIDA` EXITO |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `MetaServiceMarcarCumplidaTest`: `enUnaMetaCompartidaSeCumpleParaTodosLosQueAceptaron`, `alMarcarSeRevisanLasInsigniasYQuedanEnElLog`; `SubMetaServiceHitosTest`: `unParticipanteLograElHitoHoyYQuedaQuienLoLogro`, `enUnaMetaCumplidaLosHitosSonSoloLectura` | 4 de 4 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU6, HU11 y HU15 · Cumplir la meta» (7 peticiones): las seis filas de API | 0 fallos |
| Interfaz y reporte | Prueba manual | Manual |

### HU21 — Compartir logro

Servicios: `POST /api/metas/{codigoMeta}/compartir` responde 201 con `{token, url, nombreMeta, fechaCumplida}`; `GET /api/logros/{token}` es público y responde 200 con `{nombreMeta, tipo, plazo, fechaCumplida, nombreVisible}`. Criterio central: solo se comparte una meta CUMPLIDA (400) y solo lo hace un participante (403); si la misma persona comparte otra vez se reutiliza su enlace; quien abre el enlace ve el nombre sin el apellido completo («Ana N.»); un token que no existe responde 404.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Meta activa | Compartir una meta sin cumplir | 400 «Solo se puede compartir una meta cumplida» |
| Usuario ajeno | Compartir una meta en la que no participa | 403 «Solo un participante de la meta puede compartir el logro» |
| Compartir | Meta cumplida de Ana | 201 con un token de 10 caracteres sin ambiguos y la URL `<frontend>/logros/<token>` |
| Compartir otra vez | Segundo `POST` de Ana | 201 con el mismo token |
| Otro participante comparte | Beto, que participa, comparte la meta | 201 con su propio enlace |
| Página pública sin sesión | `GET /api/logros/{token}` sin Authorization | 200 con el nombre de la meta y «Ana N.» |
| Logro que no existe | `GET /api/logros/no-existe` | 404 «Este logro no existe o el enlace no es válido» |
| Petición sin token | Compartir sin Authorization | 401 «Autenticación requerida» |
| Interfaz: compartir en el celular | Pulsar «Compartir» | Se abre el menú de compartir del sistema con la imagen de la tarjeta |

**Caja blanca.** Sobre `LogroCompartidoService`. Meta de cobertura: todas las ramas.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Meta inexistente | `findById` vacío | `MetaNoEncontradaException`; no se busca el logro |
| Participante | Usuario sin participación | `PermisoDenegadoException`; no se guarda nada |
| Prioridad entre decisiones | Meta activa y usuario ajeno | 403: el permiso se revisa antes que el estado |
| Estado de la meta | Meta ACTIVA | `ValidacionException` «Solo se puede compartir una meta cumplida» |
| Primer enlace | Meta cumplida sin enlace previo | Se guarda un token de 10 caracteres del alfabeto sin 0/O ni 1/l/I y la URL sin «/» doble |
| Enlace existente | Misma meta y mismo usuario | Se reutiliza; no se guarda otro |
| Token repetido | El primer token generado ya existe | Se genera otro |
| Logro público | Token existente | Meta, tipo, plazo, fecha y «Laura G.» |
| Token inexistente | `findByToken` vacío | `LogroNoEncontradoException` (404) |
| Nombre visible | Sin apellido o en minúscula | «Mateo» o «Mateo r.» |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Compartir y abrir la página pública | 201 y luego 200 sin sesión |
| Seguridad | `GET /api/logros/**` sin token | Permitido por `SeguridadConfig`; compartir sí pide sesión |
| Aspecto de auditoría | Compartir | Filas `COMPARTIR_LOGRO` con EXITO y con ERROR |
| Interfaz React y página pública | Abrir el enlace en otro navegador | Se ve el logro sin iniciar sesión |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `LogroCompartidoServiceTest` (nueva): `metaInexistenteLanza404SinBuscarElLogro`, `quienNoParticipaNoComparteYNoSeGuardaNada`, `elPermisoSeRevisaAntesQueElEstado`, `unaMetaActivaNoSeComparte`, `compartirPorPrimeraVezCreaElEnlaceConUnTokenDeDiezCaracteresSinAmbiguos`, `compartirOtraVezReutilizaElEnlaceSinGuardarOtro`, `siElTokenGeneradoYaExisteSeGeneraOtro`, `elLogroPublicoMuestraLaMetaYElNombreSinElApellidoCompleto`, `unTokenQueNoExisteLanza404`, `nombreVisibleSinApellidoQuedaSoloElNombre` | 10 de 10 pasan; cobertura del servicio: 98 % de instrucciones, 88 % de ramas |
| Caja negra e integración (API) | Postman: carpeta «HU21 · Compartir logro» (5 peticiones), «HU21 antes de cumplirla» en la carpeta HU6, «HU21: Beto no participa, no comparte» en la carpeta HU4 y «Sin token · HU21 compartir» | 0 fallos |
| Interfaz | Prueba manual (la imagen de la tarjeta se dibuja en un `<canvas>`) | Manual |

### HU23 — Editar perfil y foto

Servicios: `GET /api/usuarios/me`; `PUT /api/usuarios/me` con `{nombre, apellido}`; `PUT /api/usuarios/me/foto` con `{"foto": "data:image/...;base64,..."}`; `DELETE /api/usuarios/me/foto`. Todos responden 200 con la cuenta. Criterio central: nombre y apellido son obligatorios, sin espacios de más y con máximo 50 caracteres; la foto tiene que ser JPG, PNG o WebP de verdad (se revisan los bytes) y pesar máximo 512 KB; una foto inválida no reemplaza la anterior; sin token, 401.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Ver mi cuenta | `GET /api/usuarios/me` | 200 con nombre, correo, foto, apariencia y avatar |
| Editar nombre | `PUT` con `"  Ana  "` y apellido | 200 con «Ana», sin espacios de más |
| Nombre y apellido vacíos | `PUT` con `" "` y `""` | 400 con «Escribe tu nombre» y «Escribe tu apellido» |
| Apellido largo | Apellido de 51 caracteres | 400 «El apellido puede tener máximo 50 caracteres» |
| Foto que no es imagen | `PUT /foto` con un texto | 400 «La foto debe ser una imagen JPG, PNG o WebP» |
| Sin foto | `PUT /foto` con `{}` | 400 «Elige una foto» |
| Foto pesada | Data URL de más de 512 KB | 400 «La foto pesa demasiado; elige una más liviana» |
| Foto válida | PNG real de 1×1 píxel | 200 con la foto guardada |
| Quitar la foto | `DELETE /foto` | 200 con la foto vacía |
| Petición sin token | Sin Authorization | 401 «Autenticación requerida» |
| Interfaz: recortar | Elegir una foto y recortarla | La foto recortada aparece en el encabezado |
| Interfaz: archivo que no abre | Elegir un archivo que no es imagen | «… no se pudo abrir como imagen. Usa JPG, PNG o WebP de máximo 2 MB.» |

**Caja blanca.** Sobre `CuentaService.verCuenta`, `editarDatos`, `cambiarFoto`, `quitarFoto` y `validarFoto`. Meta de cobertura: todas las ramas de la validación.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Ver cuenta | Usuario existente | Nombre, apellido, correo y foto vacía |
| Usuario inexistente | Código que no existe | `UsuarioNoEncontradoException`; no se guarda nada |
| Editar | Nombre y apellido con espacios de más | Se guardan limpios |
| Texto vacío | Nombre en blanco y apellido nulo | Los dos errores juntos; no se guarda |
| Sin pedido | Pedido nulo | Los dos errores |
| Valor límite | 50 caracteres y 51 | El primero se acepta; el segundo da «El apellido puede tener máximo 50 caracteres» |
| Formatos permitidos | JPG, PNG y WebP con su firma real | Se guardan |
| Sin foto | Nula o en blanco | «Elige una foto» |
| Data URL inválida | Texto, GIF o `text/plain` | «La foto debe ser una imagen JPG, PNG o WebP» |
| Bytes que no coinciden | Declara PNG y los bytes son de JPG | «La foto debe ser una imagen JPG, PNG o WebP» |
| Tamaño límite | 512 KB exactos y un byte más | El primero se acepta; el segundo da «La foto pesa demasiado; elige una más liviana» |
| Foto inválida | Usuario con foto anterior | La anterior se conserva |
| Quitar | Con foto y sin foto | Con foto la quita y guarda; sin foto no guarda nada |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Carpeta «HU23, HU30 y HU20 · Mi cuenta» | 200 al editar y 400 en cada caso inválido |
| Aspecto de auditoría | Editar el nombre, cambiar y quitar la foto | Filas `EDITAR_PERFIL` y `CAMBIAR_FOTO_PERFIL` con EXITO y con ERROR, y `QUITAR_FOTO_PERFIL` EXITO |
| HU23 con HU24 | Cambiar la foto y buscar a la persona | La búsqueda y el perfil público muestran la foto nueva |
| Interfaz React y encabezado | Guardar el nombre y la foto | El encabezado se actualiza sin recargar |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `CuentaServiceDatosYFotoTest` (nueva): `verCuentaDevuelveLosDatosDelUsuario`, `usuarioInexistenteLanza404`, `editarGuardaElNombreYElApellidoSinEspaciosDeMas`, `nombreYApellidoVaciosDanLosDosErroresYNoSeGuarda`, `sinPedidoFaltanLosDosCampos`, `cincuentaCaracteresSePermitenYCincuentaYUnoNo`, `guardaUnaFotoDeCadaFormatoPermitido`, `sinFotoPideElegirUna`, `unaDataUrlQueNoEsDeImagenSeRechaza`, `losBytesTienenQueCoincidirConElFormatoDeclarado`, `unaFotoEnElLimiteSePermiteYUnByteMasNo`, `unaFotoInvalidaNoReemplazaLaAnterior`, `quitarLaFotoLaDejaVaciaYGuarda`, `quitarSinFotoNoGuardaNada` | 14 de 14 pasan; cobertura de `CuentaService`: 96 % de instrucciones, 79 % de ramas |
| Caja negra e integración (API) | Postman, carpeta «HU23, HU30 y HU20 · Mi cuenta»: las diez filas de API; «Sin token · HU23 mi cuenta» en «Seguridad» | 0 fallos |
| Interfaz | Prueba manual (recorte de la foto y encabezado) | Manual |

### HU19 — Personalizar perfil público

Servicio: `PUT /api/usuarios/me/perfil-publico` con `{frase, portada, codigoInsigniaDestacada, metasDestacadas}`; responde 200 con la personalización. Criterio central: la frase tiene máximo 100 caracteres; la portada es un valor de `Portada` (NINGUNA, ESTRELLADA, AURORA, NEBULOSA, ATARDECER, OCEANO, BOSQUE, METAMORFOSIS, LLUVIA, TOPOGRAFIA o FIESTA); solo se destaca una insignia ganada y hasta 3 metas propias visibles; cada error se devuelve en la lista `errores` con 400.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Frase y portada | Frase «Paso a paso» y portada AURORA | 200 con la frase guardada |
| Frase de más de 100 caracteres | Frase de 101 caracteres | 400 «La frase puede tener hasta 100 caracteres» |
| Portada que no existe | `portada: "ARCOIRIS"` | 400 «Elige una portada válida» |
| Insignia que no ganó | Código de una insignia pendiente | 400 «Elige una insignia que hayas ganado» |
| Cuatro metas destacadas | Cuatro códigos de meta | 400 «Puedes destacar hasta 3 metas» |
| Destacar la insignia y la meta cumplida | Insignia ganada y la meta propia | 200 con la insignia destacada y una meta destacada |
| Meta ajena u oculta | Meta de otra persona o meta oculta (HU25) | 400 «Solo puedes destacar metas tuyas que se vean en tu perfil» |
| Sin cuerpo | `PUT` sin JSON | 400 «Solicitud mal formada» |
| Petición sin token | Sin Authorization | 401 «Autenticación requerida» |
| Interfaz: vista previa | Cambiar la portada | La vista previa cambia antes de guardar |

**Caja blanca.** Sobre `PerfilService.editarPerfilPublico` y lo que muestran los perfiles.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Frase limpia | `"  Un   libro a la vez "` y portada válida | Se guarda «Un libro a la vez» y la portada |
| Frase vacía y sin portada | Frase vacía, portada nula | La frase se borra y la portada queda NINGUNA |
| Frase larga o portada inválida | 101 caracteres o portada desconocida | No se guarda; se devuelven los dos errores |
| Sin datos | Pedido nulo | «Faltan los datos del perfil» |
| Insignia destacada | Insignia ganada | Se guarda y se muestra en los perfiles |
| Insignia no ganada | Insignia pendiente | Se rechaza |
| Quitar la insignia | `codigoInsigniaDestacada` nulo | Se quita |
| Metas destacadas | Tres metas propias | Se guardan; primero la más reciente |
| Más de tres | Cuatro metas | Se rechaza |
| Meta ajena u oculta | Meta de otra persona u oculta | Se rechaza |
| Ocultar una meta destacada | HU25 sobre una meta destacada | Deja de estar destacada |
| Perfil privado | HU26 activo | Solo se muestran la frase y la portada |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Carpeta «HU19 · Perfil público» | 200 y 400 según el caso |
| HU19 con HU12 | Destacar la insignia que da la galería | La insignia ganada se acepta y la pendiente no |
| Aspecto de auditoría | Guardar la personalización | Filas `EDITAR_PERFIL_PUBLICO` con EXITO y con ERROR |
| HU19 con HU24 y HU26 | Otra persona abre el perfil | Ve la personalización según la privacidad |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `PerfilServicePersonalizacionTest` (13): `guardaLaFraseLimpiaYLaPortada`, `limpiarFrase`, `fraseVaciaSeBorraYSinPortadaQuedaNinguna`, `fraseMuyLargaYPortadaInvalidaNoSeGuardan`, `sinDatosEsInvalido`, `destacaUnaInsigniaQueGanoYSeMuestraEnLosPerfiles`, `noSeDestacaUnaInsigniaQueNoGano`, `sinInsigniaDestacadaSeQuita`, `destacaHastaTresMetasYSeMuestranLaMasRecientePrimero`, `masDeTresMetasNoSePueden`, `noSeDestacaUnaMetaAjenaNiUnaOculta`, `alOcultarUnaMetaDejaDeEstarDestacada`, `unPerfilPrivadoMuestraLaFraseYLaPortadaPeroNadaMas` | 13 de 13 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU19 · Perfil público» (8 peticiones): todas las filas de API salvo «Meta ajena u oculta», que cubre JUnit; «Sin token · HU19 perfil público» | 0 fallos |
| Interfaz | Prueba manual | Manual |

### HU20 — Personalizar mi avatar

Servicio: `PUT /api/usuarios/me/avatar` con `{piel, peinado, colorPelo, ojos, boca, accesorio}`; cada pieza tiene que existir en `CatalogoAvatar` y el accesorio es opcional. Responde 200 con la cuenta. Criterio central: una pieza fuera del catálogo responde 400 con un mensaje por cada pieza inválida, por ejemplo «Elige un peinado válido»; sin token, 401.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Ver mi cuenta | `GET /api/usuarios/me` | 200 con el avatar por defecto o el elegido |
| Armar el avatar | Piezas del catálogo, sin accesorio | 200 con el avatar guardado |
| Pieza que no existe | Peinado «cresta-de-dragon» | 400 con un solo error: «Elige un peinado válido» |
| Petición sin token | Sin Authorization | 401 «Autenticación requerida» |
| Interfaz: armar | Elegir cada pieza en Editar perfil y guardar | El avatar se ve en el menú de la cuenta y en la búsqueda de personas |

**Caja blanca.** Sobre `CuentaService.cambiarAvatar`.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Usuario nuevo | Sin avatar guardado | Tiene el avatar por defecto |
| Guardar | Todas las piezas válidas | Se guardan y vuelven en la cuenta |
| Accesorio | Sin accesorio | Se acepta |
| Piezas inválidas | Piezas fuera del catálogo | No se guarda y se explica cada una |
| Sin pedido | Cuerpo vacío | Faltan todas las piezas obligatorias |
| Usuario inexistente | Código que no existe | `UsuarioNoEncontradoException` (404) |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Carpeta «HU23, HU30 y HU20 · Mi cuenta» | 200 y 400 |
| Aspecto de auditoría | Guardar el avatar | Filas `CAMBIAR_AVATAR` con EXITO y con ERROR |
| HU20 con HU24 | Ver el perfil de otra persona sin foto | Se ve su avatar |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `CuentaServiceAvatarTest` (6, una por fila): `todoUsuarioNuevoEmpiezaConElAvatarPorDefecto`, `guardaTodasLasPiezasYLasDevuelveEnLaCuenta`, `elAccesorioEsOpcional`, `piezasQueNoEstanEnElCatalogoNoSeGuardanYSeExplicaCadaUna`, `sinPedidoFaltanTodasLasPiezasObligatorias`, `usuarioInexistenteLanza404` | 6 de 6 pasan |
| Caja negra e integración (API) | Postman: «Ver mi cuenta», «Armar el avatar» y «Pieza de avatar que no existe» | 0 fallos |
| Interfaz | Prueba manual | Manual |

### HU30 — Elegir la apariencia de la app

Servicio: `PUT /api/usuarios/me/apariencia` con `{modo, acento}`. `modo` acepta CLARO, OSCURO o AUTOMATICO (`ModoTema`) y `acento` acepta VIOLETA, AZUL, ESMERALDA o CORAL (`ColorAcento`), sin importar mayúsculas ni espacios. Criterio central: 200 con la apariencia guardada; 400 con «Elige un modo válido» y «Elige un color de acento válido» si un valor no existe; sin token, 401.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Apariencia Noche | modo OSCURO, acento AZUL | 200 con la apariencia guardada |
| Apariencia inválida | modo NEON, acento X | 400 con los dos errores |
| Minúsculas y espacios | modo « oscuro », acento «coral» | 200 con OSCURO y CORAL |
| Volver a la clara | modo CLARO, acento VIOLETA | 200 |
| Petición sin token | Sin Authorization | 401 «Autenticación requerida» |
| Interfaz: cambiar el tema | Elegir Noche en Editar perfil | La app cambia de colores sin recargar y se mantiene al volver a entrar |

**Caja blanca.** Sobre `CuentaService.cambiarApariencia`.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Usuario nuevo | Sin apariencia guardada | CLARO y VIOLETA |
| Guardar | Modo y acento válidos | Se guardan |
| Mayúsculas y espacios | « oscuro » | Se acepta |
| Valores inválidos | Modo o acento desconocidos | No se guarda y se dice qué falla |
| Sin pedido | Cuerpo vacío | Inválido |
| Usuario inexistente | Código que no existe | `UsuarioNoEncontradoException` |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Carpeta «HU23, HU30 y HU20 · Mi cuenta» | 200 y 400 |
| Aspecto de auditoría | Cambiar la apariencia | Filas `CAMBIAR_APARIENCIA` con EXITO y con ERROR |
| Interfaz y accesibilidad | Lighthouse con el tema oscuro | Contraste sin errores (sección 6) |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `CuentaServiceAparienciaTest` (6): `usuarioNuevoTieneLaAparienciaDeSiempre`, `guardaModoYAcento`, `aceptaMinusculasYEspacios`, `valoresInvalidosNoGuardanYDicenQueFalla`, `sinPedidoEsInvalido`, `usuarioInexistente` | 6 de 6 pasan |
| Caja negra e integración (API) | Postman: «Apariencia Noche», «Apariencia inválida», «Apariencia en minúsculas y con espacios», «Volver a la apariencia clara» | 0 fallos |
| Interfaz | Prueba manual y Lighthouse | Manual |

### HU28 — Trazar el camino de una meta

Servicios: `POST /api/metas/{codigoMeta}/submetas` (201) y `GET`, `PUT` y `DELETE /api/submetas/{codigoSubMeta}` (200, 200 y 204), con cuerpo `{nombre, pregunta, tipo, unidad, objetivo, tipoObjetivo, dias, notas}`. `tipo` acepta HITO, MEDIBLE o SI_NO (`TipoSubMeta`): un hito se logra una sola vez y MEDIBLE y SI_NO son hábitos. Criterio central: solo el creador cambia el camino (403 «Solo el creador de la meta puede cambiar su camino»); en una meta cumplida, 400 de solo lectura; un paso que no existe da 404; cada dato inválido se devuelve en `errores` con 400.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Agregar hito 1 y 2 | `tipo: HITO` con nombre | 201 cada uno; el hito no tiene días |
| Agregar hábito medible | `tipo: MEDIBLE`, objetivo 15, unidad «páginas», `tipoObjetivo: AL_MENOS` | 201 |
| Sin tipo | Pedido sin `tipo` | 400 «Elige si el paso es un hito o un hábito» |
| Nombre repetido | El nombre del hito 1 en minúsculas | 400 «Ya tienes un paso con este nombre en esta meta» |
| Objetivo en cero | Hábito MEDIBLE con `objetivo: 0` | 400 «El objetivo debe ser mayor que 0» |
| Día de la semana inválido | Hábito SI_NO con `dias: [8]` | 400 «Los días van del 1 (lunes) al 7 (domingo)» |
| Leer un paso | `GET /api/submetas/{codigo}` | 200 con la meta del paso |
| Paso que no existe | `GET /api/submetas/999999` | 404 «No se encontró el paso 999999» |
| Cambiar el tipo al editar | `PUT` del hito con `tipo: MEDIBLE` | 400 «El tipo de un paso no se puede cambiar» |
| Eliminar un paso | `DELETE /api/submetas/{codigo}` | 204 |
| Beto no puede agregar pasos | Beto en la meta de Ana | 403 «Solo el creador de la meta puede cambiar su camino» |
| Interfaz: camino vacío | Abrir una meta sin pasos | Invita a trazar el camino |

**Caja blanca.** Sobre `SubMetaService` (crear, editar y validar).

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Crear un hito | Hito con campos de hábito (unidad, objetivo, días) | Se ignoran los campos de hábito |
| Sin tipo | `tipo` nulo | «Elige si el paso es un hito o un hábito» |
| Solo el creador | Otro participante agrega un paso | `PermisoDenegadoException` |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Carpeta «HU28 · Trazar el camino» | 201, 204, 400, 403 y 404 según el caso |
| Aspecto de auditoría | Crear, editar y eliminar pasos | Filas `CREAR_SUB_META` con EXITO y con ERROR, `EDITAR_SUB_META` ERROR y `ELIMINAR_SUB_META` EXITO |
| HU28 con HU5 y HU29 | Agregar pasos y registrar el avance | El camino muestra el avance |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `SubMetaServiceHitosTest`: `crearHitoIgnoraLosCamposDeLosHabitos`, `crearSinTipoPideElegirHitoOHabito`, `soloElCreadorAgregaPasos` | 3 de 3 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU28 · Trazar el camino (hitos y hábitos)» (13 peticiones): todas las filas de API. Las validaciones de unidad, objetivo y días no tienen prueba JUnit propia y quedan cubiertas aquí | 0 fallos |
| Interfaz | Vitest: `textoObjetivo` y `textoDias` (por ejemplo «Al menos 15 páginas» y «lunes, miércoles y viernes») | 2 de 2 pasan |

### HU5 — Registrar mi avance (rediseñada)

Servicios: `PUT` y `DELETE /api/submetas/{codigoSubMeta}/logro` (lograr y deshacer un hito, 200); `PUT /api/submetas/{codigoSubMeta}/registros/{fecha}` con `{"valor": n}` (registrar el día de un hábito, 200) y `DELETE` de la misma ruta (204). Criterio central: los hitos se logran y los hábitos se registran por día; en un hábito SI_NO el valor es 1 o 0 y en uno MEDIBLE va de 0 a 1.000.000 con máximo 2 decimales; no se registran días futuros, días que el hábito no toca ni días anteriores a su creación; solo registran los participantes (403).

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Lograr el hito 1 | `PUT /logro` | 200; queda logrado hoy y el avance es 1 de 2 = 50 % |
| Deshacer el hito 1 | `DELETE /logro` | 200; vuelve a pendiente |
| Un hábito no se logra | `PUT /logro` sobre el hábito | 400 «Solo los hitos se marcan como logrados; los hábitos se registran por día» |
| Registrar 20 páginas hoy | `PUT /registros/{hoy}` con `{"valor": 20}` | 200 y cumple el objetivo |
| Un hito no se registra por día | `PUT /registros/{hoy}` sobre el hito | 400 «Un hito no se registra por día: se marca como logrado» |
| Día futuro | `PUT /registros/2099-01-01` | 400 «No puedes registrar días que aún no llegan» |
| Valor negativo | `{"valor": -1}` | 400 «El valor no puede ser negativo» |
| No participa | Beto registra antes de ser invitado | 403 «Solo los participantes de la meta pueden registrar su día» |
| Quitar un registro | `DELETE /registros/{hoy}` | 204 |
| Sí / No con 2 | Hábito SI_NO con `{"valor": 2}` | 400 «En un hábito Sí / No el valor es 1 (hecho) o 0 (no hecho)» |
| Día que no toca | Fecha que no está en `dias` | 400 «Este hábito no toca ese día» |
| Interfaz: registrar | Registrar el valor del día desde Tu camino | El registro se guarda y la constancia de la cabecera de Tu camino se actualiza |

**Caja blanca.** Sobre `SubMetaService` (lograr y deshacer) y `RegistroDiaService`.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Lograr | Un participante marca un hito | Logrado hoy con su nombre |
| Lograr de nuevo | Hito ya logrado | Conserva la fecha original |
| Deshacer | Hito logrado | Vuelve a pendiente |
| No participa (hito) | Usuario ajeno | `PermisoDenegadoException` |
| Hábito como hito | Lograr un hábito | Se rechaza |
| Registrar medible | Valor contra el objetivo | Dice si cumple |
| Hito por día | Registrar un hito | Se rechaza |
| Día futuro | Fecha posterior a hoy (`CalendarioUsuario`) | Se rechaza |
| Día que no toca | Fecha fuera de `dias` | Se rechaza |
| Sí / No | Valor 2 | Solo 1 o 0 |
| No participa (registro) | Usuario ajeno | `PermisoDenegadoException` |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Carpeta «HU5 · Registrar mi avance» | 200, 204, 400 y 403 según el caso |
| Índice único por fecha (V7) | Registrar, quitar y volver a registrar el mismo día | Una sola fila por día |
| Aspecto de auditoría | Lograr, deshacer, registrar y quitar | Filas `LOGRAR_HITO` y `REGISTRAR_DIA` con EXITO y con ERROR; `DESHACER_HITO` y `QUITAR_REGISTRO` EXITO |
| HU5 con HU29 | Registrar y abrir las estadísticas | La constancia se actualiza |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `SubMetaServiceHitosTest`: `unParticipanteLograElHitoHoyYQuedaQuienLoLogro`, `lograrDeNuevoConservaLaFechaOriginal`, `deshacerDevuelveElHitoAPendiente`, `quienNoParticipaNoMarcaHitos`, `unHabitoNoSeMarcaComoLogrado`; `RegistroDiaServiceTest` (6): `registrarMedibleDiceSiCumpleElObjetivo`, `unHitoNoSeRegistraPorDia`, `noSeRegistranDiasFuturos`, `noSeRegistraUnDiaQueNoToca`, `enSiNoSoloSeAceptaUnoOCero`, `quienNoParticipaNoRegistra` | 11 de 11 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU5 · Registrar mi avance» (14 peticiones): todas las filas de API salvo «Día que no toca», que cubre JUnit | 0 fallos |
| Interfaz | Vitest: `aIso`, `aFecha`, `diaCorto` y `diaYMes` (fechas sin correr el día por la zona horaria); el resto es manual | 2 de 2 pasan |

### HU29 — Ver cuánto me falta y qué tan constante soy

Servicios: `GET /api/metas/{codigoMeta}/camino`, `GET /api/metas/camino-hoy` y `GET /api/submetas/{codigoSubMeta}/estadisticas` (200). El camino trae los últimos 30 días (`DIAS_CAMINO`) y las estadísticas como máximo 365 días (`DIAS_ESTADISTICAS`). Criterio central: el avance es la cantidad de hitos logrados sobre el total, y sin hitos no hay porcentaje; la constancia es la cantidad de días cumplidos sobre los días que cuentan, y hoy sin registrar no cuenta como fallado; un hito no tiene estadísticas por día (400); quien no participa recibe 403 «No participas en esta meta».

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Tu camino | `GET /api/metas/{codigo}/camino` | 200 con 2 hitos y 1 hábito, avance 50 % y constancia de hoy al 100 % |
| Resumen de las tarjetas | `GET /api/metas/camino-hoy` | 200 con el avance de la meta |
| Estadísticas del hábito | `GET /estadisticas` del hábito | 200 con la racha actual de 1 día |
| Un hito no tiene estadísticas | `GET /estadisticas` del hito | 400 «Un hito no tiene estadísticas por día» |
| No participa | Beto consulta el camino de la meta de Ana | 403 «No participas en esta meta» |
| Petición sin token | Sin Authorization | 401 «Autenticación requerida» |
| Interfaz: estadísticas | Abrir un hábito desde Tu camino | Constancia de los últimos 7 y 30 días, rachas y cumplimiento por día de la semana |

**Caja blanca.** Sobre `CalculoCamino` y `SubMetaService`.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Hito | Hito en cualquier día | Nunca toca ningún día |
| Días marcados | Hábito de lunes y miércoles | Solo tocan esos días |
| Antes de existir | Días anteriores a la creación | No tocan |
| Objetivo «al menos» | Valor igual o mayor que el objetivo | Cumple |
| Objetivo «como máximo» | Valor igual o menor que el objetivo | Cumple |
| Hoy sin registrar | Hoy vacío | No cuenta como fallado |
| Constancia | Cumplidos sobre días que cuentan | Valor correcto |
| Constancia sin días | Sin días que cuenten | null |
| Rachas | Días que no tocan en medio | No cortan la racha |
| Orden de rachas | Varias rachas | De la más larga a la más corta |
| Días completos | Varios hábitos el mismo día | Exige todos los que tocan |
| Por día de la semana | Historial | Empieza en lunes; null si nunca tocó |
| Sin hitos | Meta solo con hábitos | No hay porcentaje de avance |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, servicio y PostgreSQL | Carpeta «HU29 · Avance y constancia» | 200, 400 y 403 |
| HU29 con HU5 | Registrar un día y volver a Tu camino | La constancia cambia |
| Zona horaria (`CalendarioUsuario`, America/Bogota) | Consultar cerca de la medianoche | «Hoy» es el día de Colombia |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `CalculoCaminoTest` (12): `unHitoNuncaTocaNingunDia`, `soloTocanLosDiasMarcados`, `noTocaAntesDeQueElHabitoExistiera`, `medibleAlMenosCumpleDesdeElObjetivo`, `medibleComoMaximoCumpleHastaElObjetivo`, `hoySinRegistrarNoCuentaComoFallado`, `constanciaEsCumplidosSobreDiasQueCuentan`, `constanciaEsNullSiNoHayDiasQueContar`, `losDiasQueNoTocanNoCortanLaRacha`, `lasRachasSeOrdenanDeLaMasLargaALaMasCorta`, `diasSeguidosCompletosExigeTodosLosHabitosQueTocan`, `porDiaSemanaEmpiezaEnLunesYEsNullSiNuncaToco`; `SubMetaServiceHitosTest#sinHitosNoHayPorcentajeDeAvance` | 13 de 13 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU29 · Avance y constancia» (5 peticiones) y «Sin token · HU29 Tu camino» | 0 fallos |
| Zona horaria e interfaz | Prueba manual | Manual |

### HU32 — Cambiar la contraseña

Servicio: `PUT /api/usuarios/me/contrasena` con `{nueva, confirmacion}`. No pide la contraseña actual, porque el usuario ya inició sesión. Responde 200 con `{token}`, un token nuevo. Criterio central: la nueva cumple `ReglaContrasena` (al menos 8 caracteres, una mayúscula, una minúscula y un número), coincide con la confirmación y es distinta de la actual; al cambiarla, los tokens emitidos antes dejan de servir (`JwtAuthFilter`) y se envía un correo de aviso; si el correo falla, el cambio igual se guarda.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| No cumple la regla ni coincide | Nueva «corta» y confirmación distinta | 400 con dos errores |
| Igual a la actual | La misma contraseña | 400 «La contraseña nueva debe ser distinta de la actual» |
| Sin la nueva | `nueva` vacía | 400 «Escribe la contraseña nueva» |
| Cambiar | Nueva válida y confirmación igual | 200 con un token nuevo |
| La sesión anterior ya no sirve | Token viejo en `GET /api/usuarios/me` | 401 |
| El token nuevo sí sirve | Token nuevo en `GET /api/usuarios/me` | 200 |
| Contraseña vieja | Iniciar sesión con la anterior | 401 «Email o contraseña incorrectos» |
| Petición sin token | Sin Authorization | 401 «Autenticación requerida» |
| Interfaz: requisitos | Escribir la nueva en Cambiar contraseña | Cada requisito se marca en verde mientras se escribe |

**Caja blanca.** Sobre `ContrasenaService`, `JwtAuthFilter` y la plantilla del correo de aviso.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Ruta feliz | Nueva válida | Cambia el hash, anota el momento del cambio y devuelve un token nuevo |
| Sin la actual | Solo nueva y confirmación | Se acepta |
| Correo de aviso falla | `CorreoService` lanza una excepción | La contraseña igual queda cambiada |
| Igual a la actual | La misma contraseña | «La contraseña nueva debe ser distinta de la actual» |
| Sin la nueva | `nueva` nula | «Escribe la contraseña nueva» |
| Regla y confirmación | Nueva débil o distinta | Mensaje de la regla y «Las contraseñas nuevas no coinciden» |
| Usuario inexistente | Código que no existe | `UsuarioNoEncontradoException` |
| Sin cambio de contraseña | Token sin cambio posterior | Sirve |
| Token anterior | Emitido antes del cambio | No sirve |
| Mismo segundo | Emitido en el mismo segundo del cambio | Sirve |
| Token posterior | Emitido después del cambio | Sirve |
| Correo de aviso | Español, inglés y nombre con HTML | Sale en el idioma de quien la cambió y escapa el nombre |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API, filtro JWT y PostgreSQL | Carpeta «HU32 · Cambiar la contraseña» | 200 y luego 401 con el token viejo |
| Aspecto de auditoría | Cambiar la contraseña | Filas `CAMBIAR_CONTRASENA` con EXITO y con ERROR |
| Correo (Brevo) | Cambiar la contraseña desde la app desplegada | Llega el aviso en el idioma del usuario |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `ContrasenaServiceTest` (7): `cambiaLaContrasenaAnotaElMomentoYDevuelveUnTokenNuevo`, `noPideLaContrasenaActual`, `siElCorreoDeAvisoFallaLaContrasenaIgualQuedaCambiada`, `laNuevaDebeSerDistintaDeLaActual`, `sinLaNuevaLaPide`, `laNuevaDebeCumplirLaReglaYCoincidirConLaConfirmacion`, `usuarioQueNoExiste`; `JwtAuthFilterTest` (4): `sinCambioDeContrasenaElTokenSirve`, `unTokenEmitidoAntesDelCambioYaNoSirve`, `elTokenEmitidoEnElMismoSegundoDelCambioSirve`, `unTokenEmitidoDespuesDelCambioSirve`; `PlantillaContrasenaCambiadaTest` (3) | 14 de 14 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU32 · Cambiar la contraseña» (8 peticiones) y «Sin token · HU32 cambiar la contraseña» | 0 fallos |
| Interfaz | Vitest: `validarContrasenaNueva` y `REQUISITOS_CONTRASENA` (los mismos de `ReglaContrasena`); correo real: manual | 4 de 4 pasan |

### HU33 — Recuperar la contraseña olvidada

Servicios públicos (bajo `/api/auth/**`): `POST /api/auth/contrasena/codigo` con `{"email": "..."}` responde 202 tenga o no cuenta ese correo; `POST /api/auth/contrasena/restablecer` con `{email, codigo, nueva, confirmacion}` responde 204. Criterio central: el código tiene 6 dígitos y vence en 15 minutos; no se manda otro antes de 60 segundos; hay 5 intentos y al quinto fallo el código se borra; un código vencido y un correo sin cuenta dan el mismo error, «El código no es correcto o ya venció. Pide uno nuevo.», para no revelar qué correos tienen cuenta.

**Caja negra**

| Escenario | Entrada | Respuesta esperada |
|---|---|---|
| Pedir el código | Correo con cuenta | 202 |
| Correo sin cuenta | Correo que no existe | 202, la misma respuesta |
| Formato inválido | `no-es-un-correo` | 400 «El formato del email no es válido» |
| Código incorrecto | Código «000 000» | 400 «El código no es correcto. Te quedan N intento(s)»; la contraseña no cambia |
| Sin código y nueva mal | Pedido incompleto | 400 con tres errores |
| La contraseña sigue sirviendo | Iniciar sesión con la de prueba | 200 |
| Restablecer con el código correcto | Código del correo y nueva válida | 204; entra con la contraseña nueva |
| Quinto intento fallido | Cinco códigos falsos | 400 «Escribiste mal el código demasiadas veces. Pide uno nuevo.» |
| Interfaz: «¿Olvidaste tu contraseña?» | Desde Iniciar sesión | Lleva a la pantalla del código y avisa «Si hay una cuenta con ese correo, le enviamos un código de 6 dígitos a…» |

**Caja blanca.** Sobre `RecuperacionContrasenaService` y la plantilla del correo.

| Ruta o decisión que cubre | Entrada | Respuesta esperada |
|---|---|---|
| Enviar código | Correo con cuenta | 6 dígitos que vencen en 15 minutos |
| Largo del código | Generación repetida | Siempre 6 dígitos |
| Sin cuenta | Correo que no existe | No manda nada ni lo dice |
| Formato del correo | Correo inválido | «El formato del email no es válido» |
| Reenvío temprano | Antes de 60 segundos | No manda otro |
| Reenvío tardío | Después de 60 segundos | El nuevo reemplaza al anterior |
| Código correcto | Código y nueva válidos | Cambia la contraseña y gasta el código |
| Código incorrecto | Código falso | Cuenta el intento y dice cuántos quedan |
| Quinto intento | 5 fallos | El código se borra |
| Código vencido | Más de 15 minutos | «El código no es correcto o ya venció. Pide uno nuevo.» |
| Sin cuenta al restablecer | Correo que no existe | El mismo error que un código vencido |
| Nueva igual a la actual | Contraseña igual | Se rechaza y el código sigue sirviendo |
| Validación previa | Código o nueva inválidos | Se valida antes de consultar la base |
| Correo con el código | Español, inglés y nombre con HTML | Sale en el idioma de quien lo pidió, con el código en dos grupos y el nombre escapado |

**Pruebas de integración**

| Componentes integrados | Flujo | Respuesta esperada |
|---|---|---|
| API y PostgreSQL | Carpeta «HU33 · Recuperar la contraseña olvidada» | 202 y 400 según el caso |
| Log de actividad (registro manual, sin `@Auditar`) | Pedir el código y restablecer | Filas `SOLICITAR_CODIGO_CONTRASENA` con EXITO y con ERROR y `RESTABLECER_CONTRASENA` ERROR |
| API, correo y PostgreSQL | Pedir el código en la app desplegada | Llega el correo con el código en dos grupos |
| Flujo completo | Código, contraseña nueva e inicio de sesión | Entra con la contraseña nueva |

**Automatización y evidencia**

| Nivel | Prueba automatizada | Resultado |
|---|---|---|
| Caja blanca | `RecuperacionContrasenaServiceTest` (13): `mandaUnCodigoDeSeisDigitosQueVenceEn15Minutos`, `elCodigoGeneradoSiempreTieneSeisDigitos`, `sinCuentaNoMandaNadaNiLoDice`, `elCorreoDebeTenerFormatoValido`, `noMandaOtroCodigoAntesDeUnMinuto`, `pasadoElMinutoElCodigoNuevoReemplazaAlAnterior`, `conElCodigoCorrectoCambiaLaContrasenaYGastaElCodigo`, `conElCodigoIncorrectoCuentaElIntentoYDiceCuantosQuedan`, `alQuintoIntentoFallidoElCodigoSeBorra`, `unCodigoVencidoNoSirve`, `unCorreoSinCuentaDaElMismoErrorQueUnCodigoVencido`, `siLaNuevaEsIgualALaActualElCodigoSigueSirviendo`, `validaElCodigoYLaContrasenaNuevaAntesDeBuscarNada`; `PlantillaCodigoRecuperacionTest` (3) | 16 de 16 pasan |
| Caja negra e integración (API) | Postman, carpeta «HU33 · Recuperar la contraseña olvidada» (6 peticiones): las seis primeras filas. Las que necesitan leer el código del correo las cubre JUnit | 0 fallos |
| Interfaz y correo real | Vitest: `REGEX_EMAIL`; el resto es manual | 1 de 1 pasa |
