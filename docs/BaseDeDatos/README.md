# Base de datos y ORM

Documentación de la base de datos de Metamorfosis **generada por comandos** desde la base real (PostgreSQL 16 creada por
Flyway, migraciones V1 a V28). Las tablas se mapean con JPA / Hibernate: cada una es una entidad del paquete
`com.metamorfosis.backend.model` (ver sección 4 del documento de la entrega final).

| Archivo | Qué es | Cómo se genera |
|---|---|---|
| `esquema.sql` | Esquema completo: tablas, llaves, índices, restricciones y triggers | `docker exec metamorfosis-pg-local pg_dump -U postgres -d metamorfosis --schema-only --no-owner --no-privileges > docs/BaseDeDatos/esquema.sql` |
| `diccionario_de_datos.md` | Cada tabla con sus columnas, tipo, si acepta nulos, valor por defecto, PK/FK, únicas, CHECK e índices | `python docs/BaseDeDatos/generar.py` |
| `modelo_er.mmd` | Modelo entidad-relación en Mermaid (`erDiagram`) con las llaves foráneas reales, coloreado por módulo | `python docs/BaseDeDatos/generar.py` |
| `modelo_er.png` | El modelo E-R dibujado | `node render_er.mjs modelo_er.mmd modelo_er.png` (puppeteer-core y Google Chrome) |
| `evidencias/` | Capturas de los comandos (`\dt`, `\d meta`, `pg_dump` y el generador) | — |

Colores del modelo E-R: **morado** cuentas y acceso, **azul** metas y su camino, **ámbar** insignias, **verde** auditoría.

`generar.py` consulta `information_schema` y `pg_catalog`, así que siempre refleja la base tal como la deja Flyway.
Para volver a generar todo, con la base local de Docker encendida:

```bash
python docs/BaseDeDatos/generar.py
```
