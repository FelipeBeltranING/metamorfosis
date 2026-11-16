"""
RQ3 y RQ9: genera la colección de Postman de la API de Metamorfosis (formato v2.1) y su entorno local.

Cada petición lleva pruebas: el código HTTP esperado y los campos clave de la respuesta. Las peticiones
van en el orden de un recorrido real (iniciar sesión, crear una meta, trazar su camino, compartirla…)
y se pasan datos entre ellas con variables de entorno (tokenA, codigoMeta, codigoHito…).

Uso:
    python docs/Pruebas/postman/generar_coleccion.py
    npx newman run docs/Pruebas/postman/Metamorfosis.postman_collection.json \
        -e docs/Pruebas/postman/local.postman_environment.json
"""
import json
from pathlib import Path

AQUI = Path(__file__).resolve().parent


def js(*lineas):
    return list(lineas)


def estado(codigo):
    return f"pm.test('responde {codigo}', () => pm.response.to.have.status({codigo}));"


def mensaje(texto):
    return (f"pm.test('mensaje: {texto}', () => pm.expect(pm.response.json().mensaje).to.eql({json.dumps(texto)}));")


def error(texto):
    """El cuerpo de un 400 trae la lista `errores`; comprueba que incluya este texto exacto."""
    return (f"pm.test('error: {texto}', () => pm.expect(pm.response.json().errores).to.include({json.dumps(texto)}));")


def guardar(variable, expresion):
    return f"pm.environment.set('{variable}', {expresion});"


def peticion(nombre, metodo, ruta, *, token='A', cuerpo=None, codigo=200, pruebas=(), previas=(), cabeceras=None):
    encabezados = [{'key': 'Accept-Language', 'value': 'es'}]
    if token:
        valor = token if token.startswith(('Bearer', '{{', 'abc')) else f'Bearer {{{{token{token}}}}}'
        if token.startswith('{{'):
            valor = f'Bearer {token}'
        encabezados.append({'key': 'Authorization', 'value': valor})
    if cuerpo is not None:
        encabezados.append({'key': 'Content-Type', 'value': 'application/json'})
    for clave, valor in (cabeceras or {}).items():
        encabezados.append({'key': clave, 'value': valor})
    item = {
        'name': nombre,
        'request': {
            'method': metodo,
            'header': encabezados,
            'url': {'raw': '{{baseUrl}}' + ruta, 'host': ['{{baseUrl}}'],
                    'path': [p for p in ruta.split('?')[0].strip('/').split('/')],
                    **({'query': [{'key': k, 'value': v} for k, v in
                                  (q.split('=') for q in ruta.split('?')[1].split('&'))]} if '?' in ruta else {})},
        },
        'event': [{'listen': 'test', 'script': {'type': 'text/javascript', 'exec': [estado(codigo), *pruebas]}}],
    }
    if cuerpo is not None:
        item['request']['body'] = {'mode': 'raw', 'raw': json.dumps(cuerpo, ensure_ascii=False, indent=2),
                                   'options': {'raw': {'language': 'json'}}}
    if previas:
        item['event'].append({'listen': 'prerequest', 'script': {'type': 'text/javascript', 'exec': list(previas)}})
    return item


def carpeta(nombre, descripcion, *items):
    return {'name': nombre, 'description': descripcion, 'item': list(items)}


LOGIN_A = {'email': '{{emailA}}', 'password': '{{password}}'}
LOGIN_B = {'email': '{{emailB}}', 'password': '{{password}}'}
# PNG real de 1×1 píxel: CuentaService revisa la firma de los bytes, no solo el tipo de la Data URL.
FOTO_PNG = ('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==')
HOY = "pm.environment.set('hoy', new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10));"

coleccion = {
    'info': {
        'name': 'Metamorfosis API',
        'description': ('RQ3 (web services) y RQ9 (evidencia de pruebas). Recorre los servicios REST de todas las HU '
                        'con dos usuarios de prueba (docs/Pruebas/postman/datos_de_prueba.sql). Solo para una base local '
                        'o de pruebas: crea metas, invitaciones y registros.'),
        'schema': 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
    },
    'item': [
        carpeta('HU9 · Iniciar sesión', 'POST /api/auth/login',
                peticion('Ana inicia sesión', 'POST', '/api/auth/login', token=None, cuerpo=LOGIN_A, pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('trae token y usuario', () => { pm.expect(r.token).to.be.a('string'); pm.expect(r.codigoUsuario).to.be.a('number'); });",
                    guardar('tokenA', 'r.token'), guardar('codigoA', 'r.codigoUsuario'))),
                peticion('Beto inicia sesión', 'POST', '/api/auth/login', token=None, cuerpo=LOGIN_B, pruebas=js(
                    "const r = pm.response.json();", guardar('tokenB', 'r.token'), guardar('codigoB', 'r.codigoUsuario'))),
                peticion('Contraseña incorrecta', 'POST', '/api/auth/login', token=None, codigo=401,
                         cuerpo={'email': '{{emailB}}', 'password': 'equivocada'},
                         pruebas=js(mensaje('Email o contraseña incorrectos'))),
                peticion('Beto vuelve a entrar (limpia el intento fallido)', 'POST', '/api/auth/login', token=None, cuerpo=LOGIN_B,
                         pruebas=js(guardar('tokenB', 'pm.response.json().token'))),
                peticion('Sin datos', 'POST', '/api/auth/login', token=None, cuerpo={}, codigo=400),
                ),
        carpeta('Seguridad · rutas protegidas', 'El filtro JWT exige el token en todo lo que no es /api/auth',
                peticion('Mis metas sin token', 'GET', '/api/metas', token=None, codigo=401,
                         pruebas=js(mensaje('Autenticación requerida'))),
                # Un servicio de cada HU de la entrega final, sin token (SeguridadConfig: /api/** exige sesión).
                *[peticion(f'Sin token · {nombre}', metodo, ruta, token=None, codigo=401, cuerpo=cuerpo,
                           pruebas=js(mensaje('Autenticación requerida')))
                  for nombre, metodo, ruta, cuerpo in [
                      ('HU29 Tu camino', 'GET', '/api/metas/1/camino', None),
                      ('HU13 invitar', 'POST', '/api/metas/1/invitaciones', {'codigoUsuario': 1}),
                      ('HU14 mis invitaciones', 'GET', '/api/invitaciones', None),
                      ('HU21 compartir', 'POST', '/api/metas/1/compartir', None),
                      ('HU23 mi cuenta', 'GET', '/api/usuarios/me', None),
                      ('HU19 perfil público', 'PUT', '/api/usuarios/me/perfil-publico', {'frase': 'x'}),
                      ('HU32 cambiar la contraseña', 'PUT', '/api/usuarios/me/contrasena', {'nueva': 'x', 'confirmacion': 'x'}),
                  ]],
                ),
        carpeta('HU1 · Crear meta', 'POST /api/metas',
                peticion('Crear meta', 'POST', '/api/metas', codigo=201,
                         cuerpo={'nombre': 'Leer 12 libros (Newman)', 'plazo': 'MEDIANO', 'tipo': 'PERSONAL'},
                         pruebas=js("const r = pm.response.json();",
                                    "pm.test('queda ACTIVA', () => pm.expect(r.estado).to.eql('ACTIVA'));",
                                    "pm.test('cabecera Location', () => pm.expect(pm.response.headers.get('Location')).to.include('/api/metas/'));",
                                    guardar('codigoMeta', 'r.codigoMeta'))),
                peticion('Nombre vacío', 'POST', '/api/metas', codigo=400, cuerpo={'nombre': '', 'plazo': 'CORTO', 'tipo': 'OTRO'},
                         pruebas=js("pm.test('dice qué campo falla', () => pm.expect(pm.response.json().errores.length).to.be.above(0));")),
                ),
        carpeta('HU3 y HU27 · Ver y filtrar mis metas', 'GET /api/metas?estado=&tipo=&plazo=',
                peticion('Mis metas (activas)', 'GET', '/api/metas', pruebas=js(
                    "pm.test('incluye la meta creada', () => pm.expect(pm.response.json().map(m => m.codigoMeta)).to.include(pm.environment.get('codigoMeta')));")),
                peticion('Filtro estado=TODAS&tipo=PERSONAL&plazo=MEDIANO', 'GET', '/api/metas?estado=TODAS&tipo=PERSONAL&plazo=MEDIANO', pruebas=js(
                    "pm.test('todas son PERSONAL y MEDIANO', () => pm.response.json().forEach(m => { pm.expect(m.tipo).to.eql('PERSONAL'); pm.expect(m.plazo).to.eql('MEDIANO'); }));")),
                peticion('Filtro con valor inválido', 'GET', '/api/metas?estado=X', codigo=400),
                ),
        carpeta('HU2 · Editar meta', 'PUT /api/metas/{codigoMeta}',
                peticion('Editar', 'PUT', '/api/metas/{{codigoMeta}}',
                         cuerpo={'nombre': 'Leer 15 libros (Newman)', 'plazo': 'MEDIANO', 'tipo': 'PERSONAL'},
                         pruebas=js("pm.test('nombre nuevo', () => pm.expect(pm.response.json().nombre).to.eql('Leer 15 libros (Newman)'));")),
                peticion('Beto no puede editar la meta de Ana', 'PUT', '/api/metas/{{codigoMeta}}', token='B', codigo=403,
                         cuerpo={'nombre': 'x', 'plazo': 'CORTO', 'tipo': 'OTRO'}),
                peticion('Meta inexistente', 'PUT', '/api/metas/999999', codigo=404,
                         cuerpo={'nombre': 'x', 'plazo': 'CORTO', 'tipo': 'OTRO'}),
                ),
        carpeta('HU28 · Trazar el camino (hitos y hábitos)', 'POST/GET/PUT/DELETE /api/metas/{codigo}/submetas y /api/submetas/{codigo}',
                peticion('Agregar hito 1', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=201,
                         cuerpo={'nombre': 'Terminar el libro 1', 'tipo': 'HITO'},
                         pruebas=js("const r = pm.response.json();",
                                    "pm.test('es hito y sin días', () => { pm.expect(r.tipo).to.eql('HITO'); pm.expect(r.dias).to.eql([]); });",
                                    guardar('codigoHito', 'r.codigoSubMeta'))),
                peticion('Agregar hito 2', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=201,
                         cuerpo={'nombre': 'Terminar el libro 2', 'tipo': 'HITO'},
                         pruebas=js(guardar('codigoHito2', 'pm.response.json().codigoSubMeta'))),
                peticion('Agregar hábito medible', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=201,
                         cuerpo={'nombre': 'Leer 15 páginas', 'tipo': 'MEDIBLE', 'unidad': 'páginas', 'objetivo': 15,
                                 'tipoObjetivo': 'AL_MENOS'},
                         pruebas=js(guardar('codigoHabito', 'pm.response.json().codigoSubMeta'))),
                peticion('Sin tipo', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=400, cuerpo={'nombre': 'x'},
                         pruebas=js("pm.test('pide elegir', () => pm.expect(pm.response.json().errores).to.include('Elige si el paso es un hito o un hábito'));")),
                peticion('Nombre repetido', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=400,
                         cuerpo={'nombre': 'terminar el libro 1', 'tipo': 'HITO'},
                         pruebas=js(error('Ya tienes un paso con este nombre en esta meta'))),
                peticion('Objetivo en cero', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=400,
                         cuerpo={'nombre': 'Correr', 'tipo': 'MEDIBLE', 'unidad': 'km', 'objetivo': 0, 'tipoObjetivo': 'AL_MENOS'},
                         pruebas=js(error('El objetivo debe ser mayor que 0'))),
                peticion('Día de la semana inválido', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=400,
                         cuerpo={'nombre': 'Meditar', 'tipo': 'SI_NO', 'dias': [8]},
                         pruebas=js(error('Los días van del 1 (lunes) al 7 (domingo)'))),
                peticion('Leer un paso', 'GET', '/api/submetas/{{codigoHito}}', pruebas=js(
                    "pm.test('trae la meta', () => pm.expect(pm.response.json().nombreMeta).to.eql('Leer 15 libros (Newman)'));")),
                peticion('Paso que no existe', 'GET', '/api/submetas/999999', codigo=404,
                         pruebas=js(mensaje('No se encontró el paso 999999'))),
                peticion('El tipo de un paso no se cambia', 'PUT', '/api/submetas/{{codigoHito}}', codigo=400,
                         cuerpo={'nombre': 'Terminar el libro 1', 'tipo': 'MEDIBLE'},
                         pruebas=js(error('El tipo de un paso no se puede cambiar'))),
                peticion('Agregar un hito para borrarlo', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=201,
                         cuerpo={'nombre': 'Hito temporal', 'tipo': 'HITO'},
                         pruebas=js(guardar('codigoHitoBorrar', 'pm.response.json().codigoSubMeta'))),
                peticion('Eliminar un paso', 'DELETE', '/api/submetas/{{codigoHitoBorrar}}', codigo=204),
                peticion('Beto no puede agregar pasos', 'POST', '/api/metas/{{codigoMeta}}/submetas', token='B', codigo=403,
                         cuerpo={'nombre': 'x', 'tipo': 'HITO'},
                         pruebas=js(mensaje('Solo el creador de la meta puede cambiar su camino'))),
                ),
        carpeta('HU5 · Registrar mi avance', 'PUT/DELETE /api/submetas/{codigo}/logro y /registros/{fecha}',
                peticion('Lograr el hito 1', 'PUT', '/api/submetas/{{codigoHito}}/logro', previas=js(HOY), pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('queda logrado hoy', () => pm.expect(r.hito.fechaLogrado).to.eql(pm.environment.get('hoy')));",
                    "pm.test('avance 1 de 2 = 50 %', () => pm.expect(r.avance).to.eql({ logrados: 1, total: 2, porcentaje: 50 }));")),
                peticion('Deshacer el hito 1', 'DELETE', '/api/submetas/{{codigoHito}}/logro', pruebas=js(
                    "pm.test('vuelve a pendiente', () => pm.expect(pm.response.json().hito.fechaLogrado).to.be.null);")),
                peticion('Un hábito no se logra', 'PUT', '/api/submetas/{{codigoHabito}}/logro', codigo=400,
                         pruebas=js(error('Solo los hitos se marcan como logrados; los hábitos se registran por día'))),
                peticion('Registrar 20 páginas hoy', 'PUT', '/api/submetas/{{codigoHabito}}/registros/{{hoy}}', cuerpo={'valor': 20},
                         previas=js(HOY), pruebas=js("pm.test('cumple el objetivo', () => pm.expect(pm.response.json().cumplido).to.be.true);")),
                peticion('Un hito no se registra por día', 'PUT', '/api/submetas/{{codigoHito}}/registros/{{hoy}}', codigo=400,
                         cuerpo={'valor': 1}, pruebas=js(error('Un hito no se registra por día: se marca como logrado'))),
                peticion('Día futuro', 'PUT', '/api/submetas/{{codigoHabito}}/registros/2099-01-01', codigo=400, cuerpo={'valor': 5},
                         pruebas=js(error('No puedes registrar días que aún no llegan'))),
                peticion('Valor negativo', 'PUT', '/api/submetas/{{codigoHabito}}/registros/{{hoy}}', codigo=400, cuerpo={'valor': -1},
                         pruebas=js(error('El valor no puede ser negativo'))),
                peticion('Beto no participa: no registra', 'PUT', '/api/submetas/{{codigoHabito}}/registros/{{hoy}}', token='B',
                         codigo=403, cuerpo={'valor': 5},
                         pruebas=js(mensaje('Solo los participantes de la meta pueden registrar su día'))),
                peticion('Quitar el registro de hoy', 'DELETE', '/api/submetas/{{codigoHabito}}/registros/{{hoy}}', codigo=204),
                peticion('Registrar de nuevo 20 páginas', 'PUT', '/api/submetas/{{codigoHabito}}/registros/{{hoy}}', cuerpo={'valor': 20},
                         pruebas=js("pm.test('cumple el objetivo', () => pm.expect(pm.response.json().cumplido).to.be.true);")),
                # Hábito Sí / No temporal: se crea, se prueba y se borra para no cambiar las cuentas de la HU29.
                peticion('Agregar un hábito Sí / No', 'POST', '/api/metas/{{codigoMeta}}/submetas', codigo=201,
                         cuerpo={'nombre': 'Leer antes de dormir', 'tipo': 'SI_NO'},
                         pruebas=js(guardar('codigoSiNo', 'pm.response.json().codigoSubMeta'))),
                peticion('Sí / No con valor 2', 'PUT', '/api/submetas/{{codigoSiNo}}/registros/{{hoy}}', codigo=400, cuerpo={'valor': 2},
                         pruebas=js(error('En un hábito Sí / No el valor es 1 (hecho) o 0 (no hecho)'))),
                peticion('Borrar el hábito Sí / No', 'DELETE', '/api/submetas/{{codigoSiNo}}', codigo=204),
                peticion('Lograr el hito 1 otra vez', 'PUT', '/api/submetas/{{codigoHito}}/logro'),
                ),
        carpeta('HU29 · Avance y constancia', 'GET /api/metas/{codigo}/camino, /api/metas/camino-hoy, /api/submetas/{codigo}/estadisticas',
                peticion('Tu camino', 'GET', '/api/metas/{{codigoMeta}}/camino', pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('2 hitos y 1 hábito', () => { pm.expect(r.hitos.length).to.eql(2); pm.expect(r.habitos.length).to.eql(1); });",
                    "pm.test('avance 50 %', () => pm.expect(r.avance.porcentaje).to.eql(50));",
                    "pm.test('constancia de hoy al 100 %', () => pm.expect(r.constancia7).to.eql(100));")),
                peticion('Resumen para las tarjetas', 'GET', '/api/metas/camino-hoy', pruebas=js(
                    "const r = pm.response.json().find(x => x.codigoMeta === pm.environment.get('codigoMeta'));",
                    "pm.test('trae el avance', () => pm.expect(r.avance.total).to.eql(2));")),
                peticion('Estadísticas del hábito', 'GET', '/api/submetas/{{codigoHabito}}/estadisticas', pruebas=js(
                    "pm.test('racha de 1 día', () => pm.expect(pm.response.json().rachaActual).to.eql(1));")),
                peticion('Un hito no tiene estadísticas por día', 'GET', '/api/submetas/{{codigoHito}}/estadisticas', codigo=400,
                         pruebas=js(error('Un hito no tiene estadísticas por día'))),
                peticion('Beto no participa: no ve el camino', 'GET', '/api/metas/{{codigoMeta}}/camino', token='B', codigo=403,
                         pruebas=js(mensaje('No participas en esta meta'))),
                ),
        carpeta('HU13 · Invitar a una meta', 'POST /api/metas/{codigo}/invitaciones y código para unirse',
                peticion('Pedido sin persona', 'POST', '/api/metas/{{codigoMeta}}/invitaciones', codigo=400, cuerpo={},
                         pruebas=js(error('Elige a quién quieres invitar'))),
                peticion('Beto no es el creador: no invita', 'POST', '/api/metas/{{codigoMeta}}/invitaciones', token='B', codigo=403,
                         cuerpo={'codigoUsuario': '{{codigoA}}'},
                         pruebas=js(mensaje('Solo el creador puede invitar personas a la meta'))),
                peticion('Invitarse a sí misma', 'POST', '/api/metas/{{codigoMeta}}/invitaciones', codigo=400,
                         cuerpo={'codigoUsuario': '{{codigoA}}'}, pruebas=js(error('No puedes invitarte a ti mismo'))),
                peticion('Persona que no existe', 'POST', '/api/metas/{{codigoMeta}}/invitaciones', codigo=404,
                         cuerpo={'codigoUsuario': 999999}, pruebas=js(mensaje('No existe el usuario con código 999999'))),
                peticion('Ana invita a Beto', 'POST', '/api/metas/{{codigoMeta}}/invitaciones', codigo=201,
                         cuerpo={'codigoUsuario': '{{codigoB}}'}, pruebas=js(
                             "pm.test('queda PENDIENTE', () => pm.expect(pm.response.json().participante.estado).to.eql('PENDIENTE'));")),
                peticion('Invitarlo dos veces', 'POST', '/api/metas/{{codigoMeta}}/invitaciones', codigo=409,
                         cuerpo={'codigoUsuario': '{{codigoB}}'},
                         pruebas=js(mensaje('Beto Newman ya tiene una invitación pendiente a esta meta'))),
                peticion('Generar código para unirse', 'POST', '/api/metas/{{codigoMeta}}/codigo', codigo=201, pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('código de 8 caracteres sin ambiguos', () => pm.expect(r.codigo).to.match(/^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}$/));",
                    # CodigoUnionService usa Clock.systemUTC(): `expira` es una hora UTC sin zona.
                    "pm.test('vence en 7 días', () => { const dias = (new Date(r.expira + 'Z') - Date.now()) / 86400000; pm.expect(dias).to.be.within(6.9, 7.1); });")),
                peticion('Ver el código', 'GET', '/api/metas/{{codigoMeta}}/codigo'),
                peticion('Beto no maneja el código', 'GET', '/api/metas/{{codigoMeta}}/codigo', token='B', codigo=403,
                         pruebas=js(mensaje('Solo el creador puede manejar el código de la meta'))),
                peticion('Desactivar el código', 'DELETE', '/api/metas/{{codigoMeta}}/codigo', codigo=204),
                peticion('Unirse con un código que no existe', 'POST', '/api/metas/unirse', token='B', codigo=400,
                         cuerpo={'codigo': 'ZZZZZZ'}, pruebas=js(error('Ese código no existe o ya venció. Revisa que esté bien escrito.'))),
                peticion('Unirse sin código', 'POST', '/api/metas/unirse', token='B', codigo=400, cuerpo={},
                         pruebas=js(error('Escribe el código que te compartieron'))),
                ),
        carpeta('HU14 · Responder invitación', 'GET /api/invitaciones y PATCH /api/invitaciones/{codigoMeta}',
                peticion('Beto ve su invitación', 'GET', '/api/invitaciones', token='B', pruebas=js(
                    "pm.test('incluye la meta', () => pm.expect(pm.response.json().map(i => i.codigoMeta)).to.include(pm.environment.get('codigoMeta')));")),
                peticion('Responder sin decir si acepta', 'PATCH', '/api/invitaciones/{{codigoMeta}}', token='B', codigo=400,
                         cuerpo={}, pruebas=js(error('Indica si aceptas o rechazas la invitación'))),
                peticion('Ana no tiene invitación a su propia meta', 'PATCH', '/api/invitaciones/{{codigoMeta}}', codigo=404,
                         cuerpo={'aceptar': True}, pruebas=js(mensaje('No tienes una invitación pendiente a esta meta'))),
                peticion('Enlace del correo que no existe (sin sesión)', 'GET', '/api/invitaciones/correo?token=enlace-falso',
                         token=None, codigo=410),
                peticion('Rechazar desde el correo con un enlace falso (sin sesión)', 'POST', '/api/invitaciones/correo/rechazar',
                         token=None, codigo=410, cuerpo={'token': 'enlace-falso'}),
                peticion('Beto acepta', 'PATCH', '/api/invitaciones/{{codigoMeta}}', token='B', cuerpo={'aceptar': True}),
                peticion('Aceptar otra vez', 'PATCH', '/api/invitaciones/{{codigoMeta}}', token='B', codigo=404, cuerpo={'aceptar': True},
                         pruebas=js(mensaje('No tienes una invitación pendiente a esta meta'))),
                peticion('Beto ya la ve en Mis metas', 'GET', '/api/metas', token='B', pruebas=js(
                    "pm.test('está en su lista', () => pm.expect(pm.response.json().map(m => m.codigoMeta)).to.include(pm.environment.get('codigoMeta')));")),
                peticion('Ana genera otro código', 'POST', '/api/metas/{{codigoMeta}}/codigo', codigo=201,
                         pruebas=js(guardar('codigoUnion', 'pm.response.json().codigo'))),
                peticion('Beto ya participa: no se une con el código', 'POST', '/api/metas/unirse', token='B', codigo=409,
                         cuerpo={'codigo': '{{codigoUnion}}'}, pruebas=js(mensaje('Ya participas en esta meta'))),
                peticion('Ana desactiva el código', 'DELETE', '/api/metas/{{codigoMeta}}/codigo', codigo=204),
                peticion('Beto logra el hito 2 (el hito es del equipo)', 'PUT', '/api/submetas/{{codigoHito2}}/logro', token='B', pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('queda quién lo logró', () => pm.expect(r.hito.logradoPor).to.eql('Beto Newman'));",
                    "pm.test('avance 100 %', () => pm.expect(r.avance.porcentaje).to.eql(100));")),
                ),
        carpeta('HU25 · Ocultar metas', 'PATCH /api/metas/{codigo}/visibilidad',
                peticion('Ana oculta la meta de su perfil', 'PATCH', '/api/metas/{{codigoMeta}}/visibilidad', cuerpo={'oculta': True},
                         pruebas=js("pm.test('oculta', () => pm.expect(pm.response.json().oculta).to.be.true);")),
                peticion('Ana la vuelve a mostrar', 'PATCH', '/api/metas/{{codigoMeta}}/visibilidad', cuerpo={'oculta': False}),
                peticion('Sin el campo oculta', 'PATCH', '/api/metas/{{codigoMeta}}/visibilidad', codigo=400, cuerpo={},
                         pruebas=js(error('Indica si la meta queda oculta'))),
                peticion('Meta que no existe', 'PATCH', '/api/metas/999999/visibilidad', codigo=404, cuerpo={'oculta': True}),
                ),
        carpeta('HU6, HU11 y HU15 · Cumplir la meta', 'PATCH /api/metas/{codigo}/cumplida',
                peticion('HU21 antes de cumplirla: una meta activa no se comparte', 'POST', '/api/metas/{{codigoMeta}}/compartir',
                         codigo=400, pruebas=js(error('Solo se puede compartir una meta cumplida'))),
                peticion('Beto no es el creador', 'PATCH', '/api/metas/{{codigoMeta}}/cumplida', token='B', codigo=403,
                         pruebas=js(mensaje('Solo el creador puede marcar la meta como cumplida'))),
                peticion('Ana la marca como cumplida', 'PATCH', '/api/metas/{{codigoMeta}}/cumplida', pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('CUMPLIDA', () => pm.expect(r.meta.estado).to.eql('CUMPLIDA'));",
                    "pm.test('lista de insignias nuevas (HU11)', () => pm.expect(r.insigniasNuevas).to.be.an('array'));")),
                peticion('Marcarla otra vez no cambia nada (criterio 4)', 'PATCH', '/api/metas/{{codigoMeta}}/cumplida', previas=js(HOY), pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('conserva la fecha original', () => pm.expect(r.meta.fechaCumplida).to.eql(pm.environment.get('hoy')));",
                    "pm.test('no da insignias otra vez', () => pm.expect(r.insigniasNuevas).to.eql([]));")),
                peticion('HU15: aparece en el perfil de Beto', 'GET', '/api/usuarios/me/perfil', token='B', pruebas=js(
                    "pm.test('entre sus últimas cumplidas', () => pm.expect(pm.response.json().ultimasCumplidas.map(m => m.codigoMeta)).to.include(pm.environment.get('codigoMeta')));")),
                peticion('El camino queda en solo lectura', 'PUT', '/api/submetas/{{codigoHito}}/logro', codigo=400,
                         pruebas=js(error('La meta ya está cumplida: su camino es solo de lectura'))),
                peticion('Tampoco se registran días', 'PUT', '/api/submetas/{{codigoHabito}}/registros/{{hoy}}', codigo=400,
                         cuerpo={'valor': 5}, pruebas=js(error('La meta ya está cumplida: su camino es solo de lectura'))),
                ),
        carpeta('HU21 · Compartir logro', 'POST /api/metas/{codigo}/compartir y GET /api/logros/{token} (público)',
                peticion('Compartir', 'POST', '/api/metas/{{codigoMeta}}/compartir', codigo=201, pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('token de 10 caracteres sin ambiguos', () => pm.expect(r.token).to.match(/^[ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789]{10}$/));",
                    "pm.test('URL del enlace', () => pm.expect(r.url).to.match(new RegExp('/logros/' + r.token + '$')));",
                    guardar('tokenLogro', 'r.token'))),
                peticion('Compartir otra vez reutiliza el enlace', 'POST', '/api/metas/{{codigoMeta}}/compartir', codigo=201, pruebas=js(
                    "pm.test('mismo token', () => pm.expect(pm.response.json().token).to.eql(pm.environment.get('tokenLogro')));")),
                peticion('Beto también puede compartirlo (participa)', 'POST', '/api/metas/{{codigoMeta}}/compartir', token='B',
                         codigo=201, pruebas=js(
                             "pm.test('su propio enlace', () => pm.expect(pm.response.json().token).to.not.eql(pm.environment.get('tokenLogro')));")),
                peticion('Página pública del logro (sin sesión)', 'GET', '/api/logros/{{tokenLogro}}', token=None, pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('nombre de la meta', () => pm.expect(r.nombreMeta).to.eql('Leer 15 libros (Newman)'));",
                    "pm.test('nombre visible sin apellido completo', () => pm.expect(r.nombreVisible).to.eql('Ana N.'));")),
                peticion('Logro que no existe', 'GET', '/api/logros/no-existe', token=None, codigo=404,
                         pruebas=js(mensaje('Este logro no existe o el enlace no es válido'))),
                ),
        carpeta('HU7 · Reporte general', 'GET /api/reportes/resumen',
                peticion('Reporte', 'GET', '/api/reportes/resumen', pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('al menos una cumplida', () => pm.expect(r.metasCumplidas).to.be.at.least(1));",
                    "pm.test('porcentaje de 0 a 100', () => pm.expect(r.porcentajeCumplimiento).to.be.within(0, 100));")),
                ),
        carpeta('HU22 y HU12 · Mi perfil e insignias', 'GET /api/usuarios/me/perfil y /me/insignias',
                peticion('Mi perfil', 'GET', '/api/usuarios/me/perfil', pruebas=js(
                    "pm.test('resumen de metas', () => pm.expect(pm.response.json().resumen.metasCumplidas).to.be.at.least(1));")),
                peticion('Galería de insignias', 'GET', '/api/usuarios/me/insignias', pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('catálogo de 6', () => pm.expect(r.total).to.eql(6));",
                    "pm.test('ganó al menos una', () => pm.expect(r.ganadas).to.be.at.least(1));",
                    # Para la HU19: una insignia que Ana ya ganó y una que no.
                    guardar('insigniaGanada', 'r.insignias.find(i => i.obtenida).codigoInsignia'),
                    guardar('insigniaPendiente', 'r.insignias.find(i => !i.obtenida).codigoInsignia'))),
                ),
        carpeta('HU24 y HU26 · Perfil de otros y perfil privado', 'GET /api/usuarios?nombre=, /api/usuarios/{codigo}/perfil, PATCH /me/privacidad',
                peticion('Buscar «bet»', 'GET', '/api/usuarios?nombre=bet', pruebas=js(
                    "pm.test('encuentra a Beto', () => pm.expect(pm.response.json().map(u => u.codigoUsuario)).to.include(pm.environment.get('codigoB')));")),
                peticion('Búsqueda de una letra', 'GET', '/api/usuarios?nombre=b', codigo=400),
                peticion('Perfil público de Beto', 'GET', '/api/usuarios/{{codigoB}}/perfil', pruebas=js(
                    "pm.test('sin correo', () => pm.expect(pm.response.json().email).to.be.undefined);")),
                peticion('Persona que no existe', 'GET', '/api/usuarios/999999/perfil', codigo=404),
                peticion('Beto hace su perfil privado', 'PATCH', '/api/usuarios/me/privacidad', token='B', cuerpo={'privado': True},
                         pruebas=js("pm.test('privado', () => pm.expect(pm.response.json().privado).to.be.true);")),
                peticion('Ana ve el perfil privado de Beto', 'GET', '/api/usuarios/{{codigoB}}/perfil', pruebas=js(
                    "const r = pm.response.json();",
                    "pm.test('marcado privado y sin metas', () => { pm.expect(r.privado).to.be.true; pm.expect(r.ultimasCumplidas).to.eql([]); });")),
                peticion('Beto lo vuelve público', 'PATCH', '/api/usuarios/me/privacidad', token='B', cuerpo={'privado': False}),
                ),
        carpeta('HU19 · Perfil público', 'PUT /api/usuarios/me/perfil-publico',
                peticion('Frase y portada', 'PUT', '/api/usuarios/me/perfil-publico',
                         cuerpo={'frase': 'Paso a paso', 'portada': 'AURORA', 'codigoInsigniaDestacada': None, 'metasDestacadas': []},
                         pruebas=js("pm.test('frase guardada', () => pm.expect(pm.response.json().personalizacion.frase).to.eql('Paso a paso'));")),
                peticion('Frase de más de 100 caracteres', 'PUT', '/api/usuarios/me/perfil-publico', codigo=400,
                         cuerpo={'frase': 'x' * 101, 'portada': 'NINGUNA', 'codigoInsigniaDestacada': None, 'metasDestacadas': []},
                         pruebas=js(error('La frase puede tener hasta 100 caracteres'))),
                peticion('Portada que no existe', 'PUT', '/api/usuarios/me/perfil-publico', codigo=400,
                         cuerpo={'frase': 'Paso a paso', 'portada': 'ARCOIRIS', 'codigoInsigniaDestacada': None, 'metasDestacadas': []},
                         pruebas=js(error('Elige una portada válida'))),
                peticion('Insignia que no ganó', 'PUT', '/api/usuarios/me/perfil-publico', codigo=400,
                         cuerpo={'frase': 'Paso a paso', 'portada': 'AURORA', 'codigoInsigniaDestacada': '{{insigniaPendiente}}', 'metasDestacadas': []},
                         pruebas=js(error('Elige una insignia que hayas ganado'))),
                peticion('Cuatro metas destacadas', 'PUT', '/api/usuarios/me/perfil-publico', codigo=400,
                         cuerpo={'frase': 'Paso a paso', 'portada': 'AURORA', 'codigoInsigniaDestacada': None, 'metasDestacadas': [1, 2, 3, 4]},
                         pruebas=js(error('Puedes destacar hasta 3 metas'))),
                peticion('Destacar la insignia y la meta cumplida', 'PUT', '/api/usuarios/me/perfil-publico',
                         cuerpo={'frase': 'Paso a paso', 'portada': 'AURORA', 'codigoInsigniaDestacada': '{{insigniaGanada}}',
                                 'metasDestacadas': ['{{codigoMeta}}']},
                         pruebas=js("const p = pm.response.json().personalizacion;",
                                    "pm.test('insignia destacada', () => pm.expect(p.insigniaDestacada.codigoInsignia).to.eql(pm.environment.get('insigniaGanada')));",
                                    "pm.test('una meta destacada', () => pm.expect(p.metasDestacadas.length).to.eql(1));")),
                peticion('Sin cuerpo', 'PUT', '/api/usuarios/me/perfil-publico', codigo=400,
                         pruebas=js(mensaje('Solicitud mal formada'))),
                peticion('Dejar el perfil como estaba', 'PUT', '/api/usuarios/me/perfil-publico',
                         cuerpo={'frase': '', 'portada': 'NINGUNA', 'codigoInsigniaDestacada': None, 'metasDestacadas': []}),
                ),
        carpeta('HU23, HU30 y HU20 · Mi cuenta', 'GET/PUT /api/usuarios/me, /foto, /apariencia, /avatar',
                peticion('Ver mi cuenta', 'GET', '/api/usuarios/me', pruebas=js(
                    "pm.test('avatar por defecto o elegido', () => pm.expect(pm.response.json().avatar.peinado).to.be.a('string'));")),
                peticion('Editar nombre', 'PUT', '/api/usuarios/me', cuerpo={'nombre': '  Ana  ', 'apellido': 'Newman'}, pruebas=js(
                    "pm.test('sin espacios de más', () => pm.expect(pm.response.json().nombre).to.eql('Ana'));")),
                peticion('Nombre y apellido vacíos', 'PUT', '/api/usuarios/me', codigo=400, cuerpo={'nombre': ' ', 'apellido': ''},
                         pruebas=js("pm.test('los dos errores', () => pm.expect(pm.response.json().errores).to.eql(['Escribe tu nombre', 'Escribe tu apellido']));")),
                peticion('Apellido de 51 caracteres', 'PUT', '/api/usuarios/me', codigo=400, cuerpo={'nombre': 'Ana', 'apellido': 'n' * 51},
                         pruebas=js(error('El apellido puede tener máximo 50 caracteres'))),
                peticion('Foto que no es imagen', 'PUT', '/api/usuarios/me/foto', codigo=400, cuerpo={'foto': 'no-es-una-imagen'},
                         pruebas=js(error('La foto debe ser una imagen JPG, PNG o WebP'))),
                peticion('Sin foto', 'PUT', '/api/usuarios/me/foto', codigo=400, cuerpo={}, pruebas=js(error('Elige una foto'))),
                peticion('Foto de más de 512 KB', 'PUT', '/api/usuarios/me/foto', codigo=400, cuerpo={'foto': '{{fotoPesada}}'},
                         previas=js("pm.environment.set('fotoPesada', 'data:image/png;base64,iVBORw0KGgo' + 'A'.repeat(720000));"),
                         pruebas=js(error('La foto pesa demasiado; elige una más liviana'))),
                peticion('Foto PNG válida', 'PUT', '/api/usuarios/me/foto', cuerpo={'foto': FOTO_PNG},
                         pruebas=js(f"pm.test('foto guardada', () => pm.expect(pm.response.json().foto).to.eql({json.dumps(FOTO_PNG)}));")),
                peticion('Quitar la foto', 'DELETE', '/api/usuarios/me/foto',
                         pruebas=js("pm.test('sin foto', () => pm.expect(pm.response.json().foto).to.be.null);")),
                peticion('Apariencia Noche', 'PUT', '/api/usuarios/me/apariencia', cuerpo={'modo': 'OSCURO', 'acento': 'AZUL'},
                         pruebas=js("pm.test('guardada', () => pm.expect(pm.response.json().apariencia).to.eql({ modo: 'OSCURO', acento: 'AZUL' }));")),
                peticion('Apariencia inválida', 'PUT', '/api/usuarios/me/apariencia', codigo=400, cuerpo={'modo': 'NEON', 'acento': 'X'},
                         pruebas=js("pm.test('dice qué falla', () => pm.expect(pm.response.json().errores).to.eql(['Elige un modo válido', 'Elige un color de acento válido']));")),
                peticion('Apariencia en minúsculas y con espacios', 'PUT', '/api/usuarios/me/apariencia', cuerpo={'modo': ' oscuro ', 'acento': 'coral'},
                         pruebas=js("pm.test('se normaliza', () => pm.expect(pm.response.json().apariencia).to.eql({ modo: 'OSCURO', acento: 'CORAL' }));")),
                peticion('Armar el avatar', 'PUT', '/api/usuarios/me/avatar',
                         cuerpo={'piel': 'c99c62', 'peinado': 'curlyBob', 'colorPelo': '3a1a00', 'ojos': 'winking', 'boca': 'openedSmile', 'accesorio': None},
                         pruebas=js("pm.test('guardado', () => pm.expect(pm.response.json().avatar.peinado).to.eql('curlyBob'));")),
                peticion('Pieza de avatar que no existe', 'PUT', '/api/usuarios/me/avatar', codigo=400,
                         cuerpo={'piel': 'c99c62', 'peinado': 'cresta-de-dragon', 'colorPelo': '3a1a00', 'ojos': 'winking', 'boca': 'openedSmile', 'accesorio': None},
                         pruebas=js("pm.test('solo el peinado falla', () => pm.expect(pm.response.json().errores).to.eql(['Elige un peinado válido']));")),
                peticion('Volver a la apariencia clara', 'PUT', '/api/usuarios/me/apariencia', cuerpo={'modo': 'CLARO', 'acento': 'VIOLETA'}),
                ),
        carpeta('HU4 · Eliminar meta', 'DELETE /api/metas/{codigo}',
                peticion('Crear una meta para borrar', 'POST', '/api/metas', codigo=201,
                         cuerpo={'nombre': 'Meta para borrar (Newman)', 'plazo': 'CORTO', 'tipo': 'OTRO'},
                         pruebas=js(guardar('codigoMetaBorrar', 'pm.response.json().codigoMeta'))),
                peticion('HU25: Beto no participa, no la oculta', 'PATCH', '/api/metas/{{codigoMetaBorrar}}/visibilidad', token='B',
                         codigo=403, cuerpo={'oculta': True},
                         pruebas=js(mensaje('Solo los participantes de la meta pueden ocultarla'))),
                peticion('HU21: Beto no participa, no comparte', 'POST', '/api/metas/{{codigoMetaBorrar}}/compartir', token='B',
                         codigo=403, pruebas=js(mensaje('Solo un participante de la meta puede compartir el logro'))),
                peticion('Beto no puede borrarla', 'DELETE', '/api/metas/{{codigoMetaBorrar}}', token='B', codigo=403),
                peticion('Ana la borra', 'DELETE', '/api/metas/{{codigoMetaBorrar}}', codigo=204),
                peticion('Ya no existe', 'DELETE', '/api/metas/{{codigoMetaBorrar}}', codigo=404),
                ),
        carpeta('HU8 · Registrarse', 'El flujo completo necesita el enlace del correo; aquí se prueba lo que no depende de él.',
                peticion('Pedir registro con un correo nuevo', 'POST', '/api/auth/registro/solicitar', token=None, codigo=202,
                         cuerpo={'email': 'nuevo.{{$timestamp}}@metamorfosis.test'},
                         pruebas=js(guardar('sesionRegistro', 'pm.response.json().sesion'))),
                peticion('Estado de la solicitud', 'GET', '/api/auth/registro/estado/{{sesionRegistro}}', token=None, pruebas=js(
                    "pm.test('pendiente de autorizar', () => pm.expect(pm.response.json().estado).to.eql('PENDIENTE'));")),
                peticion('Correo que ya tiene cuenta', 'POST', '/api/auth/registro/solicitar', token=None, codigo=409,
                         cuerpo={'email': '{{emailA}}'}),
                peticion('Autorizar con un enlace falso', 'POST', '/api/auth/registro/autorizar', token=None, codigo=410,
                         cuerpo={'token': 'enlace-falso'}),
                peticion('Registrarse con un enlace falso', 'POST', '/api/auth/registro', token=None, codigo=410,
                         cuerpo={'token': 'enlace-falso', 'nombre': 'X', 'apellido': 'Y', 'password': 'Prueba123!'}),
                ),
        carpeta('HU10 · Cerrar sesión (caja negra e integración)',
                'Los 8 casos de caja negra por API y 3 de integración del plan de pruebas del Sprint 1 (sección 8.8).',
                peticion('Sesión 1 de Ana', 'POST', '/api/auth/login', token=None, cuerpo=LOGIN_A,
                         pruebas=js(guardar('sesion1', 'pm.response.json().token'))),
                peticion('Sesión 2 de Ana', 'POST', '/api/auth/login', token=None, cuerpo=LOGIN_A,
                         pruebas=js(guardar('sesion2', 'pm.response.json().token'))),
                peticion('CN1 · Cierre con token válido', 'POST', '/api/auth/logout', token='{{sesion1}}', codigo=204,
                         pruebas=js("pm.test('sin cuerpo', () => pm.expect(pm.response.text()).to.eql(''));")),
                peticion('CN2 · Usar el token después de cerrar sesión', 'GET', '/api/metas', token='{{sesion1}}', codigo=401,
                         pruebas=js(mensaje('Autenticación requerida'))),
                peticion('CN3 · Cerrar sesión dos veces', 'POST', '/api/auth/logout', token='{{sesion1}}', codigo=401,
                         pruebas=js(mensaje('La sesión ya fue cerrada'))),
                peticion('CN4 · Sin cabecera Authorization', 'POST', '/api/auth/logout', token=None, codigo=401,
                         pruebas=js(mensaje('Falta el token de autorización'))),
                peticion('CN5 · Cabecera sin el prefijo Bearer', 'POST', '/api/auth/logout', token=None, codigo=401,
                         cabeceras={'Authorization': 'abc123'}, pruebas=js(mensaje('Falta el token de autorización'))),
                peticion('CN6 · Token alterado', 'POST', '/api/auth/logout', token='{{tokenAlterado}}', codigo=401,
                         previas=js("const t = pm.environment.get('sesion2').split('.');",
                                    "const c = t[1].charAt(10) === 'A' ? 'B' : 'A';",
                                    "t[1] = t[1].slice(0, 10) + c + t[1].slice(11);",
                                    "pm.environment.set('tokenAlterado', t.join('.'));"),
                         pruebas=js(mensaje('Token inválido o expirado'))),
                peticion('CN7 · Token vencido', 'POST', '/api/auth/logout', token='{{tokenVencido}}', codigo=401,
                         previas=js(
                             "// Token firmado con el secreto de pruebas pero con fecha de expiración pasada.",
                             "const b64u = p => CryptoJS.enc.Base64.stringify(p).replace(/=+$/, '').replace(/\\+/g, '-').replace(/\\//g, '_');",
                             "const parte = o => b64u(CryptoJS.enc.Utf8.parse(JSON.stringify(o)));",
                             "const ahora = Math.floor(Date.now() / 1000);",
                             "const cabecera = parte({ alg: 'HS256' });",
                             "const carga = parte({ jti: 'vencido-' + ahora, sub: String(pm.environment.get('codigoA')), iat: ahora - 7200, exp: ahora - 3600 });",
                             "const firma = b64u(CryptoJS.HmacSHA256(cabecera + '.' + carga, pm.environment.get('jwtSecreto')));",
                             "pm.environment.set('tokenVencido', cabecera + '.' + carga + '.' + firma);"),
                         pruebas=js(mensaje('Token inválido o expirado'))),
                peticion('CN8 · Otra sesión del mismo usuario sigue activa', 'GET', '/api/metas', token='{{sesion2}}'),
                peticion('Integración · la sesión 2 también se puede cerrar', 'POST', '/api/auth/logout', token='{{sesion2}}', codigo=204),
                ),
        carpeta('HU32 · Cambiar la contraseña',
                'PUT /api/usuarios/me/contrasena { nueva, confirmacion }: con la sesión iniciada no se pide la actual. '
                'Va casi al final porque cambiarla cierra las demás sesiones de Ana; al terminar vuelve a la contraseña '
                'de prueba y deja tokenA con el token nuevo.',
                peticion('Nueva que no cumple la regla ni coincide', 'PUT', '/api/usuarios/me/contrasena', codigo=400,
                         cuerpo={'nueva': 'corta', 'confirmacion': 'otra'},
                         pruebas=js("pm.test('dos errores', () => pm.expect(pm.response.json().errores).to.have.length(2));")),
                peticion('Nueva igual a la actual', 'PUT', '/api/usuarios/me/contrasena', codigo=400,
                         cuerpo={'nueva': '{{password}}', 'confirmacion': '{{password}}'},
                         pruebas=js("pm.test('debe ser distinta', () => pm.expect(pm.response.json().errores).to.eql(['La contraseña nueva debe ser distinta de la actual']));")),
                peticion('Sin la contraseña nueva', 'PUT', '/api/usuarios/me/contrasena', codigo=400,
                         cuerpo={'nueva': '', 'confirmacion': ''}, pruebas=js(error('Escribe la contraseña nueva'))),
                peticion('Cambiar la contraseña', 'PUT', '/api/usuarios/me/contrasena',
                         cuerpo={'nueva': 'Cambio2026x', 'confirmacion': 'Cambio2026x'},
                         pruebas=js("pm.test('trae un token nuevo', () => pm.expect(pm.response.json().token).to.be.a('string'));",
                                    guardar('tokenCambio', 'pm.response.json().token'))),
                peticion('La sesión anterior ya no sirve', 'GET', '/api/usuarios/me', codigo=401),
                peticion('El token nuevo sí sirve', 'GET', '/api/usuarios/me', token='{{tokenCambio}}'),
                peticion('Ya no entra con la contraseña vieja', 'POST', '/api/auth/login', token=None, codigo=401, cuerpo=LOGIN_A,
                         pruebas=js(mensaje('Email o contraseña incorrectos'))),
                peticion('Volver a la contraseña de prueba', 'PUT', '/api/usuarios/me/contrasena', token='{{tokenCambio}}',
                         cuerpo={'nueva': '{{password}}', 'confirmacion': '{{password}}'},
                         pruebas=js(guardar('tokenA', 'pm.response.json().token'))),
                ),
        carpeta('HU33 · Recuperar la contraseña olvidada',
                'POST /api/auth/contrasena/codigo y /restablecer (públicos). El código llega por correo, así que aquí se '
                'prueba lo que no depende de leerlo.',
                peticion('Pedir el código', 'POST', '/api/auth/contrasena/codigo', token=None, codigo=202,
                         cuerpo={'email': '{{emailA}}'}),
                peticion('Correo sin cuenta: la misma respuesta', 'POST', '/api/auth/contrasena/codigo', token=None,
                         codigo=202, cuerpo={'email': 'nadie.{{$timestamp}}@metamorfosis.test'}),
                peticion('Correo con formato inválido', 'POST', '/api/auth/contrasena/codigo', token=None, codigo=400,
                         cuerpo={'email': 'no-es-un-correo'}, pruebas=js(error('El formato del email no es válido'))),
                peticion('Código incorrecto', 'POST', '/api/auth/contrasena/restablecer', token=None, codigo=400,
                         cuerpo={'email': '{{emailA}}', 'codigo': '000 000', 'nueva': 'Cambio2026x', 'confirmacion': 'Cambio2026x'},
                         pruebas=js("pm.test('no cambia la contraseña', () => pm.expect(pm.response.json().errores[0]).to.match(/^El código no es correcto/));")),
                peticion('Sin código y con la nueva mal', 'POST', '/api/auth/contrasena/restablecer', token=None, codigo=400,
                         cuerpo={'email': '{{emailA}}', 'codigo': '', 'nueva': 'corta', 'confirmacion': 'otra'},
                         pruebas=js("pm.test('tres errores', () => pm.expect(pm.response.json().errores).to.have.length(3));")),
                peticion('La contraseña de prueba sigue sirviendo', 'POST', '/api/auth/login', token=None, cuerpo=LOGIN_A),
                ),
    ],
}

entorno = {
    'name': 'Metamorfosis local',
    'values': [
        {'key': 'baseUrl', 'value': 'http://localhost:8080', 'enabled': True},
        {'key': 'emailA', 'value': 'ana.newman@metamorfosis.test', 'enabled': True},
        {'key': 'emailB', 'value': 'beto.newman@metamorfosis.test', 'enabled': True},
        {'key': 'password', 'value': 'Prueba123!', 'enabled': True},
        {'key': 'jwtSecreto', 'value': 'secreto-de-pruebas-newman-32-bytes!!', 'enabled': True},
    ],
}

(AQUI / 'Metamorfosis.postman_collection.json').write_text(json.dumps(coleccion, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
(AQUI / 'local.postman_environment.json').write_text(json.dumps(entorno, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
total = sum(len(c['item']) for c in coleccion['item'])
print(f'{len(coleccion["item"])} carpetas, {total} peticiones')
