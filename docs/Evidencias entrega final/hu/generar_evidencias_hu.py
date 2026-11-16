"""
Sección 16: genera una imagen de evidencia por HU a partir de las salidas reales del 15 de noviembre de 2026.

Cada imagen junta, para su HU:
  1. Las clases JUnit que la prueban, tal como salen en datos/mvnw-verify.txt.
  2. Las peticiones de Postman que la prueban, tal como salen en datos/newman-salida.txt.
  3. Las filas del log de auditoría de sus acciones, de datos/log_actividad_tras_newman.txt.
No ejecuta nada: solo da formato a esas salidas. Necesita Microsoft Edge para tomar la captura.

Uso: python "docs/Evidencias entrega final/hu/generar_evidencias_hu.py" [HU1 HU2 ...]
"""
import html
import re
import subprocess
import sys
from pathlib import Path

AQUI = Path(__file__).resolve().parent
DATOS = AQUI.parent / 'datos'
EDGE = r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'

# HU: (clases JUnit, [(carpeta de Postman, filtro del nombre de la petición o None)], acciones del log)
HU = {
    # Sprint 1 (pruebas JUnit agregadas el 16 de noviembre de 2026)
    'HU1': (['MetaServiceCrearMetaTest'], [('HU1 ', None), ('Seguridad', r'^Mis metas sin token')], ['CREAR_META']),
    'HU2': (['MetaServiceEditarMetaTest'], [('HU2 ', None)], ['EDITAR_META']),
    'HU3': (['MetaServiceListarMetasTest'], [('HU3 y HU27', r'^Mis metas'), ('Seguridad', r'^Mis metas sin token')], ['LISTAR_METAS']),
    'HU4': (['MetaServiceEliminarMetaTest'], [('HU4 ', r'^(?!HU2[15])')], ['ELIMINAR_META']),
    'HU8': (['AuthServiceRegistrarTest', 'SolicitudRegistroServiceTest'], [('HU8 ', None)], ['SOLICITAR_REGISTRO', 'REGISTRAR_USUARIO', 'RECHAZAR_REGISTRO']),
    'HU9': (['AuthServiceIniciarSesionTest', 'BloqueoLoginServiceTest', 'JwtAuthFilterTest'], [('HU9 ', None), ('Seguridad', None)], ['INICIAR_SESION']),
    'HU10': (['AuthServiceCerrarSesionTest'], [('HU10 ', None)], ['CERRAR_SESION']),
    'HU6': (['MetaServiceMarcarCumplidaTest'], [('HU6, HU11 y HU15', None), ('Seguridad', r'^Mis metas sin token')], ['MARCAR_CUMPLIDA']),
    'HU27': (['MetaServiceListarMetasTest'], [('HU3 y HU27', None)], ['LISTAR_METAS']),
    'HU7': (['ReporteServiceTest'], [('HU7 ', None)], ['VER_REPORTE']),
    'HU11': (['InsigniaServiceTest', 'MetaServiceMarcarCumplidaTest'], [('HU6, HU11 y HU15', r'^(Ana la marca|Marcarla otra vez)')], ['MARCAR_CUMPLIDA']),
    'HU22': (['PerfilServiceTest'], [('HU22 y HU12', r'^Mi perfil'), ('HU6, HU11 y HU15', r'^HU15: aparece')], ['VER_PERFIL']),
    'HU12': (['InsigniaServiceGaleriaTest'], [('HU22 y HU12', r'^Galería')], ['VER_INSIGNIAS']),
    'HU25': (['MetaServiceVisibilidadTest'], [('HU25', None), ('HU4', r'^HU25:')], ['CAMBIAR_VISIBILIDAD']),
    'HU24': (['PerfilServicePublicoTest'], [('HU24 y HU26', r'^(Buscar|Búsqueda|Perfil público|Persona que)')], ['BUSCAR_USUARIOS', 'VER_PERFIL_PUBLICO']),
    'HU26': (['PerfilServicePrivacidadTest'], [('HU24 y HU26', r'^(Beto hace|Ana ve|Beto lo vuelve)')], ['CAMBIAR_PRIVACIDAD']),
    'HU13': (['InvitacionServiceInvitarTest', 'CodigoUnionServiceTest'], [('HU13', None), ('Seguridad', r'HU13')], ['INVITAR_META', 'GENERAR_CODIGO_META', 'DESACTIVAR_CODIGO_META']),
    'HU14': (['InvitacionServiceResponderTest', 'CodigoUnionServiceTest'], [('HU14', None), ('Seguridad', r'HU14')], ['VER_INVITACIONES', 'RESPONDER_INVITACION', 'UNIRSE_CON_CODIGO']),
    'HU15': (['MetaServiceMarcarCumplidaTest', 'SubMetaServiceHitosTest'], [('HU6, HU11 y HU15', None)], ['MARCAR_CUMPLIDA']),
    'HU21': (['LogroCompartidoServiceTest'], [('HU21', None), ('HU6, HU11 y HU15', r'^HU21'), ('HU4', r'^HU21'), ('Seguridad', r'HU21')], ['COMPARTIR_LOGRO']),
    'HU23': (['CuentaServiceDatosYFotoTest'], [('HU23, HU30 y HU20', r'^(Ver mi cuenta|Editar nombre|Nombre y apellido|Apellido|Foto|Sin foto|Quitar la foto)'), ('Seguridad', r'HU23')], ['EDITAR_PERFIL', 'CAMBIAR_FOTO_PERFIL', 'QUITAR_FOTO_PERFIL']),
    'HU19': (['PerfilServicePersonalizacionTest'], [('HU19', None), ('Seguridad', r'HU19')], ['EDITAR_PERFIL_PUBLICO']),
    'HU20': (['CuentaServiceAvatarTest'], [('HU23, HU30 y HU20', r'^(Ver mi cuenta|Armar el avatar|Pieza de avatar)')], ['CAMBIAR_AVATAR']),
    'HU30': (['CuentaServiceAparienciaTest'], [('HU23, HU30 y HU20', r'^(Apariencia|Volver a la apariencia)')], ['CAMBIAR_APARIENCIA']),
    'HU28': (['SubMetaServiceHitosTest'], [('HU28', None)], ['CREAR_SUB_META', 'EDITAR_SUB_META', 'ELIMINAR_SUB_META']),
    'HU5': (['SubMetaServiceHitosTest', 'RegistroDiaServiceTest'], [('HU5 ', None)], ['LOGRAR_HITO', 'DESHACER_HITO', 'REGISTRAR_DIA', 'QUITAR_REGISTRO']),
    'HU29': (['CalculoCaminoTest', 'SubMetaServiceHitosTest'], [('HU29', None), ('Seguridad', r'HU29')], []),
    'HU32': (['ContrasenaServiceTest', 'JwtAuthFilterTest', 'PlantillaContrasenaCambiadaTest'], [('HU32', None), ('Seguridad', r'HU32')], ['CAMBIAR_CONTRASENA']),
    'HU33': (['RecuperacionContrasenaServiceTest', 'PlantillaCodigoRecuperacionTest'], [('HU33', None)], ['SOLICITAR_CODIGO_CONTRASENA', 'RESTABLECER_CONTRASENA']),
}


# Las HU del Sprint 1 tienen JUnit del 16 de noviembre; Postman y el log son de la ejecución del 15.
FECHA = {hu: 'JUnit del 16 y Postman y log del 15 de noviembre de 2026'
         for hu in ('HU1', 'HU2', 'HU3', 'HU4', 'HU8', 'HU9', 'HU10')}


def leer(nombre):
    return re.sub(r'\x1b\[[0-9;]*m', '', (DATOS / nombre).read_text(encoding='utf-8', errors='replace'))


def junit(clases):
    lineas = []
    for l in leer('mvnw-verify.txt').splitlines():
        m = re.search(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+).* in com\.metamorfosis\.backend\.\w+\.(\w+)', l)
        if m and m.group(5) in clases:
            lineas.append((f'Tests run: {m.group(1)}, Failures: {m.group(2)}, Errors: {m.group(3)}, Skipped: {m.group(4)} -- {m.group(5)}',
                           m.group(2) == '0' and m.group(3) == '0'))
    return lineas


def newman_peticiones():
    """[(carpeta, nombre, metodo_y_ruta, estado, aserciones_ok, aserciones_total)]"""
    res, carpeta, actual = [], None, None
    for l in leer('newman-salida.txt').splitlines():
        if l.startswith('□ '):
            carpeta = l[2:].strip()
        elif l.startswith('└ '):
            actual = [carpeta, l[2:].strip(), '', '', 0, 0]
            res.append(actual)
        elif actual and re.match(r'^\s+(GET|POST|PUT|PATCH|DELETE) ', l):
            m = re.match(r'^\s+(\w+) http://localhost:\d+(\S+) \[([^,\]]+)', l)
            actual[2], actual[3] = f'{m.group(1)} {m.group(2)}', m.group(3)
        elif actual and re.match(r'^\s+√', l):
            actual[4] += 1; actual[5] += 1
        elif actual and re.match(r'^\s+\d+\. ', l):
            actual[5] += 1
    return res


def log(acciones):
    filas = []
    for l in leer('log_actividad_tras_newman.txt').splitlines():
        partes = [p.strip() for p in l.split('|')]
        if len(partes) == 3 and partes[0] in acciones:
            filas.append(f'{partes[0]:<28} {partes[1]:<6} {partes[2]:>3} filas')
    return filas


def imagen(hu, clases, filtros, acciones, peticiones):
    elegidas = [p for p in peticiones
                if any(c in p[0] and (f is None or re.search(f, p[1])) for c, f in filtros)]
    j = junit(clases)
    lg = log(acciones)
    total_ok = sum(p[4] for p in elegidas); total = sum(p[5] for p in elegidas)
    bloques = ['<div class="t">JUnit · mvnw clean verify</div>']
    bloques += [f'<div class="{"ok" if ok else "er"}">{"✓" if ok else "✗"} {html.escape(x)}</div>' for x, ok in j]
    n = len(elegidas)
    bloques.append(f'<div class="t">Postman · newman run ({n} {"petición" if n == 1 else "peticiones"}, {total_ok} de {total} aserciones)</div>')
    for carpeta, nombre, ruta, estado, ok, tot in elegidas:
        clase = 'ok' if ok == tot else 'er'
        bloques.append(f'<div class="{clase}">{"✓" if ok == tot else "✗"} {html.escape(nombre)}'
                       f' <span class="g">— {html.escape(ruta)} → {html.escape(estado)} ({ok}/{tot})</span></div>')
    if lg:
        bloques.append('<div class="t">Log de auditoría · select accion, resultado, count(*) from log_actividad</div>')
        bloques += [f'<div class="g2">{html.escape(x)}</div>' for x in lg]
    alto = 52 + 21 * len(bloques)
    pagina = f'''<!doctype html><meta charset="utf-8"><style>
body{{margin:0;background:#0c0c0c;color:#d4d4d4;font:13px/1.55 Consolas,monospace}}
.bar{{background:#2d2d2d;color:#fff;padding:6px 12px;font:12px Segoe UI,sans-serif}}
.c{{padding:8px 14px}} .t{{color:#9cdcfe;margin-top:6px}} .ok{{color:#6ccf6c}} .er{{color:#f48771}}
.g{{color:#a0a0a0}} .g2{{color:#d4d4d4;white-space:pre}}</style>
<div class="bar">Evidencia {hu} — {FECHA.get(hu, 'ejecución del 15 de noviembre de 2026')}</div><div class="c">{"".join(bloques)}</div>'''
    fuente = AQUI / f'_{hu}.html'
    fuente.write_text(pagina, encoding='utf-8')
    salida = AQUI / f'16_evidencia_{hu}.png'
    subprocess.run([EDGE, '--headless=new', '--disable-gpu', '--hide-scrollbars', f'--screenshot={salida}',
                    f'--window-size=1000,{alto}', fuente.as_uri()], capture_output=True, timeout=120)
    fuente.unlink()
    return len(j), len(elegidas), total_ok, total, len(lg)


if __name__ == '__main__':
    peticiones = newman_peticiones()
    # Sin argumentos genera todas; con argumentos, solo esas HU (p. ej. HU1 HU2).
    for hu, (clases, filtros, acciones) in HU.items():
        if sys.argv[1:] and hu not in sys.argv[1:]:
            continue
        print(hu, imagen(hu, clases, filtros, acciones, peticiones))
