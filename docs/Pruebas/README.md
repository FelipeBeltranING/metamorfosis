# Pruebas de la API (RQ3 y RQ9)

## Colección de Postman
- `postman/Metamorfosis.postman_collection.json`: 93 peticiones en 21 carpetas (una por HU), con 147 aserciones (código HTTP y campos clave).
- `postman/generar_coleccion.py` la genera; `postman/datos_de_prueba.sql` crea los dos usuarios de prueba (solo en una base local o de pruebas).

```bash
psql ... < docs/Pruebas/postman/datos_de_prueba.sql
npx newman@6.2.1 run docs/Pruebas/postman/Metamorfosis.postman_collection.json \
  -e docs/Pruebas/postman/local.postman_environment.json --env-var baseUrl=http://localhost:8080
```
El backend debe correr con `APP_JWT_SECRET=secreto-de-pruebas-newman-32-bytes!!` para el caso del token vencido.

## Evidencia (14 nov 2026, PostgreSQL 16 limpio, rama hu20-mascota)
- `evidencia/newman-salida.txt` y `evidencia/newman-junit.xml`: **93 peticiones, 147 aserciones, 0 fallos**.

## HU10 · Cerrar sesión (plan de pruebas del Sprint 1, sección 8.8)

Los 24 casos que quedaron pendientes al cierre del Sprint 1 se ejecutaron el 14 de noviembre de 2026 contra
PostgreSQL 16 local. **Pasan los 24.**

### Caja negra (12)
| Caso | Cómo | Resultado |
|---|---|---|
| CN1 Cierre con token válido | Newman | 204 sin cuerpo |
| CN2 Usar el token después de cerrar | Newman | 401 «Autenticación requerida» |
| CN3 Cerrar dos veces | Newman | 401 «La sesión ya fue cerrada» |
| CN4 Sin cabecera Authorization | Newman | 401 «Falta el token de autorización» |
| CN5 Cabecera sin «Bearer » | Newman | 401 «Falta el token de autorización» |
| CN6 Token alterado | Newman | 401 «Token inválido o expirado» |
| CN7 Token vencido | Newman (token firmado con exp pasada) | 401 «Token inválido o expirado» |
| CN8 Otra sesión del mismo usuario | Newman | la segunda sesión sigue en 200 |
| CN9 Interfaz: menú de perfil | Navegador | muestra nombre, correo y «Cerrar sesión» |
| CN10 Interfaz: cerrar sesión | Navegador | confirmación, `/login` con «Sesión cerrada», sesión borrada del navegador |
| CN11 Interfaz: botón «atrás» | Navegador | vuelve a `/login`; no se ve Mis metas |
| CN12 Interfaz: escribir `/metas` | Navegador | redirige a «Iniciar sesión» |

### Caja blanca (6) · `backend/src/test/.../AuthServiceCerrarSesionTest.java`, 6/6
| Caso | Resultado |
|---|---|
| CB1 Controlador: cabecera nula, sin «Bearer » o con espacios | las dos primeras lanzan TokenInvalidoException sin llamar al servicio; la tercera llega recortada |
| CB2 Falla la extracción del token | log ERROR TOKEN_INVALIDO sin usuario; no se guarda nada |
| CB3 existsByJti verdadero | log ERROR TOKEN_YA_REVOCADO; «La sesión ya fue cerrada»; save no se invoca |
| CB4 save lanza DataIntegrityViolationException | log ERROR TOKEN_YA_REVOCADO; «La sesión ya fue cerrada» |
| CB5 Ruta feliz | save una vez con el jti y la expiración; log CERRAR_SESION EXITO |
| CB6 Expiración en UTC | JwtService real: ahora + 60 min en UTC (±2 s) |

### Integración (6)
| Caso | Resultado |
|---|---|
| Login → logout → GET /api/metas | 204 y luego 401 |
| 4 cierres simultáneos del mismo token | uno 204, tres 401 «La sesión ya fue cerrada»; una sola fila en token_revocado |
| log_actividad | CERRAR_SESION con EXITO, ERROR {"motivo":"TOKEN_INVALIDO"} y ERROR {"motivo":"TOKEN_YA_REVOCADO"} |
| Revocación por token | con dos sesiones, cerrar una no afecta la otra (CN8) |
| Persistencia tras reiniciar el backend | el token cerrado sigue en 401 después del reinicio (la revocación está en la base) |
| Interfaz, API y base | tras cerrar desde el menú, el token de la UI responde 401 y su jti consta en token_revocado |
