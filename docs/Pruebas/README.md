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
| Caso | Resultado |
|---|---|
| CN1–CN8 (caja negra por API) | Pasan en Newman (carpeta HU10) |
| Integración: login → logout → GET /api/metas | 204 y luego 401 (CN1–CN2) |
| Integración: 4 cierres simultáneos del mismo token | Uno 204 y tres 401 «La sesión ya fue cerrada»; 1 sola fila en token_revocado |
| Integración: log_actividad | CERRAR_SESION con EXITO, ERROR TOKEN_INVALIDO y ERROR TOKEN_YA_REVOCADO |
| Integración: otra sesión del mismo usuario sigue activa | Pasa (CN8) |
| Integración: persistencia tras reiniciar el backend | **Pendiente** |
| Caja blanca (6) e interfaz (4 + 1) | **Pendientes** |
