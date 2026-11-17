"""
Genera la documentación de la base de datos de Metamorfosis desde la base real (PostgreSQL).

Uso (desde la raíz del repositorio, con la base local de Docker encendida):
    python docs/BaseDeDatos/generar.py [contenedor] [base]

Por defecto usa el contenedor `metamorfosis-pg-local` y la base `metamorfosis`, la misma que crea Flyway.
Escribe en docs/BaseDeDatos:
  - diccionario_de_datos.md : cada tabla con sus columnas, tipos, nulos, valores por defecto, claves y restricciones
  - diccionario_de_datos.json : lo mismo en JSON
  - modelo_er.mmd           : el modelo entidad-relación en Mermaid (erDiagram), con las llaves foráneas reales
El esquema SQL completo se saca aparte con:
    docker exec metamorfosis-pg-local pg_dump -U postgres -d metamorfosis --schema-only --no-owner --no-privileges > docs/BaseDeDatos/esquema.sql
"""
import json
import subprocess
import sys
from collections import OrderedDict, defaultdict
from pathlib import Path

AQUI = Path(__file__).resolve().parent
CONTENEDOR = sys.argv[1] if len(sys.argv) > 1 else 'metamorfosis-pg-local'
BASE = sys.argv[2] if len(sys.argv) > 2 else 'metamorfosis'

DESCRIPCION = {
    'usuario': 'Cuenta de cada persona: datos personales, contraseña (hash BCrypt), foto, apariencia, avatar y perfil público (HU8, HU19, HU20, HU23, HU26, HU30, HU32).',
    'meta': 'Meta que crea un usuario: nombre, tipo, plazo y estado ACTIVA o CUMPLIDA (HU1 a HU4, HU6).',
    'participacion': 'Relación usuario-meta: creador e invitados, estado de la invitación, fecha de cumplimiento y si la oculta de su perfil (HU6, HU13 a HU15, HU25).',
    'sub_meta': 'Paso del camino de una meta: hito (se logra una vez) o hábito (se repite ciertos días, Sí/No o medible) (HU28, HU5).',
    'registro_sub_meta': 'Registro de un hábito en una fecha: hecho/no hecho o el valor medido (HU5, HU29).',
    'insignia': 'Catálogo de insignias y el criterio para ganarlas (HU11, HU12).',
    'usuario_insignia': 'Insignias que ganó cada usuario y en qué fecha (HU11, HU12).',
    'logro_compartido': 'Enlace público de una meta cumplida (HU21).',
    'solicitud_registro': 'Solicitud de registro pendiente de autorizar desde el correo (HU8).',
    'codigo_recuperacion': 'Código de 6 dígitos para recuperar la contraseña, guardado como hash (HU33).',
    'token_revocado': 'Tokens JWT cerrados con «Cerrar sesión» (HU10).',
    'intento_login': 'Intentos fallidos de inicio de sesión por correo, para bloquear temporalmente (HU9).',
    'log_actividad': 'Bitácora de auditoría: quién hizo qué, sobre qué dato y con qué resultado. Solo se puede insertar (RQ8).',
    'flyway_schema_history': 'Control de Flyway: qué migraciones se aplicaron (V1 a V28). No es parte del modelo.',
}
GRUPOS = [
    ('cuentas', 'fill:#ede9fe,stroke:#7c3aed,stroke-width:2px,color:#2e1065',
     ['usuario', 'solicitud_registro', 'codigo_recuperacion', 'token_revocado', 'intento_login']),
    ('metas', 'fill:#dbeafe,stroke:#2563eb,stroke-width:2px,color:#172554',
     ['meta', 'participacion', 'sub_meta', 'registro_sub_meta', 'logro_compartido']),
    ('insignias', 'fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#451a03', ['insignia', 'usuario_insignia']),
    ('auditoria', 'fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#052e16', ['log_actividad']),
]
TIPO_CORTO = {
    'character varying': 'varchar', 'timestamp with time zone': 'timestamptz', 'timestamp without time zone': 'timestamp',
    'double precision': 'double', 'character': 'char',
}


def consulta(sql):
    salida = subprocess.run(['docker', 'exec', CONTENEDOR, 'psql', '-U', 'postgres', '-d', BASE, '-At', '-F', '|', '-c', sql],
                            capture_output=True, text=True, encoding='utf-8', check=True).stdout
    return [linea.split('|') for linea in salida.splitlines() if linea]


def main():
    sys.stdout.reconfigure(encoding='utf-8')
    columnas = consulta("""
        select c.table_name, c.column_name, c.data_type, coalesce(c.character_maximum_length::text, ''),
               coalesce(c.numeric_precision::text, ''), coalesce(c.numeric_scale::text, ''), c.is_nullable,
               coalesce(c.column_default, case when c.is_identity = 'YES' then 'identity (autoincremental)' else '' end)
        from information_schema.columns c
        join information_schema.tables t on t.table_name = c.table_name and t.table_schema = c.table_schema
        where c.table_schema = 'public' and t.table_type = 'BASE TABLE'
        order by c.table_name, c.ordinal_position""")
    pk = {(t, c) for t, c in consulta("""
        select k.table_name, k.column_name from information_schema.table_constraints tc
        join information_schema.key_column_usage k on k.constraint_name = tc.constraint_name and k.table_name = tc.table_name
        where tc.constraint_type = 'PRIMARY KEY' and tc.table_schema = 'public'""")}
    fk = {}
    for tabla, col, ref_tabla, ref_col, borrar in consulta("""
        select kcu.table_name, kcu.column_name, ccu.table_name, ccu.column_name, rc.delete_rule
        from information_schema.referential_constraints rc
        join information_schema.key_column_usage kcu on kcu.constraint_name = rc.constraint_name
        join information_schema.constraint_column_usage ccu on ccu.constraint_name = rc.unique_constraint_name
        where kcu.table_schema = 'public'"""):
        fk[(tabla, col)] = (ref_tabla, ref_col, borrar)
    unicas = defaultdict(list)
    checks = defaultdict(list)
    for tabla, nombre, definicion in consulta("""
        select conrelid::regclass::text, conname, pg_get_constraintdef(oid) from pg_constraint
        where contype in ('u', 'c') and connamespace = 'public'::regnamespace order by 1, 2"""):
        (unicas if definicion.startswith('UNIQUE') else checks)[tabla].append(f'{nombre}: {definicion}')
    indices = defaultdict(list)
    for tabla, nombre, definicion in consulta("""
        select tablename, indexname, indexdef from pg_indexes where schemaname = 'public'
        and indexname not like '%_pkey' order by 1, 2"""):
        indices[tabla].append(nombre)

    tablas = OrderedDict()
    for tabla, col, tipo, largo, precision, escala, nulo, defecto in columnas:
        tipo_txt = TIPO_CORTO.get(tipo, tipo)
        if largo:
            tipo_txt += f'({largo})'
        elif tipo == 'numeric' and precision:
            tipo_txt += f'({precision},{escala})'
        claves = []
        if (tabla, col) in pk:
            claves.append('PK')
        if (tabla, col) in fk:
            ref = fk[(tabla, col)]
            claves.append(f'FK → {ref[0]}.{ref[1]}' + (f' ({ref[2]})' if ref[2] != 'NO ACTION' else ''))
        tablas.setdefault(tabla, []).append((col, tipo_txt, 'Sí' if nulo == 'YES' else 'No', defecto, ', '.join(claves)))

    orden = [t for t in DESCRIPCION if t in tablas] + [t for t in tablas if t not in DESCRIPCION]

    # Diccionario de datos
    md = ['# Diccionario de datos · Metamorfosis', '',
          'Generado con `python docs/BaseDeDatos/generar.py` desde la base PostgreSQL creada por Flyway (migraciones V1 a V28). '
          'Cada tabla corresponde a una entidad JPA del paquete `com.metamorfosis.backend.model`.', '',
          '| Tabla | Columnas | Descripción |', '| --- | --- | --- |']
    md += [f'| `{t}` | {len(tablas[t])} | {DESCRIPCION.get(t, "")} |' for t in orden]
    for t in orden:
        md += ['', f'## {t}', '', DESCRIPCION.get(t, ''), '',
               '| Columna | Tipo | Nulo | Por defecto | Clave |', '| --- | --- | --- | --- | --- |']
        md += [f'| `{c}` | {ti} | {n} | {("`" + d + "`") if d else ""} | {k} |' for c, ti, n, d, k in tablas[t]]
        if unicas.get(t):
            md += ['', '**Únicas:** ' + '; '.join(f'`{u}`' for u in unicas[t])]
        if checks.get(t):
            md += ['', '**Restricciones CHECK:** ' + '; '.join(f'`{c}`' for c in checks[t])]
        if indices.get(t):
            md += ['', '**Índices:** ' + ', '.join(f'`{i}`' for i in indices[t])]
    datos = {'tablas': {t: {'descripcion': DESCRIPCION.get(t, ''), 'columnas': tablas[t], 'unicas': unicas.get(t, []),
                             'checks': checks.get(t, []), 'indices': indices.get(t, [])} for t in orden},
             'llaves_foraneas': [[t, c, *fk[(t, c)]] for t, c in sorted(fk)]}
    (AQUI / 'diccionario_de_datos.json').write_text(json.dumps(datos, ensure_ascii=False, indent=1), encoding='utf-8')
    (AQUI / 'diccionario_de_datos.md').write_text('\n'.join(md) + '\n', encoding='utf-8')

    # Modelo entidad-relación (Mermaid)
    er = ['erDiagram']
    for t in orden:
        if t == 'flyway_schema_history':
            continue
        er.append(f'    {t} {{')
        for c, ti, n, d, k in tablas[t]:
            marca = ', '.join(m for m in (('PK' if 'PK' in k else ''), ('FK' if 'FK' in k else '')) if m)
            tipo_mmd = ti.split('(')[0].replace(' ', '_')
            er.append(f'        {tipo_mmd} {c}{(" " + marca) if marca else ""}')
        er.append('    }')
    for (tabla, col), (ref_tabla, ref_col, _) in sorted(fk.items()):
        # Si la llave foránea es toda la llave primaria, la relación es 1 a 0..1; si no, 1 a N
        solo_pk = (tabla, col) in pk and sum(1 for t, _ in pk if t == tabla) == 1
        er.append(f'    {ref_tabla} ||--{"o|" if solo_pk else "o{"} {tabla} : "{col}"')
    # Colores por módulo: cuentas y acceso, metas, insignias y auditoría
    for nombre, estilo, miembros in GRUPOS:
        er.append(f'    classDef {nombre} {estilo}')
        er.append(f'    class {",".join(m for m in miembros if m in tablas)} {nombre}')
    (AQUI / 'modelo_er.mmd').write_text('\n'.join(er) + '\n', encoding='utf-8')
    print(f'{len(orden)} tablas, {sum(len(v) for v in tablas.values())} columnas, {len(fk)} llaves foráneas')


if __name__ == '__main__':
    main()
