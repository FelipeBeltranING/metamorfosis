"""
Qué va en el diagrama de clases y en el de secuencia de cada HU.

Las clases se piden por nombre y se llenan con las firmas reales del código (modelo.json, que produce
extraer_modelo.py). Si un método pedido no existe en el código, el generador falla: así un diagrama no
puede quedar distinto al código sin que se note.

Secuencias: caso positivo completo y un fragmento alt con el error principal de la HU.
"""
import json
from pathlib import Path

MODELO = {}


def cargar_modelo(ruta):
    MODELO.update(json.loads(Path(ruta).read_text(encoding='utf-8')))


# ----------------------------------------------------------------------------------------------
# Clases
# ----------------------------------------------------------------------------------------------

def _es_dependencia(atributo):
    tipo = atributo.split(':', 1)[1].strip()
    if tipo in MODELO:
        return MODELO[tipo]['estereotipo'] in ('Service', 'Repository', 'Component', 'Interface', 'Class', 'Aspect')
    return tipo in ('PasswordEncoder', 'Clock')


def C(nombre, metodos=(), atributos=None):
    """Una clase del código. atributos=None: todos; [] ninguno; lista: solo esos nombres."""
    def construir():
        if nombre not in MODELO:
            raise KeyError(f'La clase {nombre} no está en el código')
        d = MODELO[nombre]
        if d['estereotipo'] == 'Enumeration':
            lista_atr = list(d['constantes'])
        elif atributos is None and d['estereotipo'] in ('Service', 'RestController', 'Component', 'Aspect', 'Class'):
            # En controladores y servicios solo las dependencias inyectadas (no constantes ni textos).
            lista_atr = [x for x in d['atributos'] if _es_dependencia(x)]
        elif atributos is None:
            lista_atr = list(d['atributos'])
        else:
            lista_atr = []
            for a in atributos:
                encontrados = [x for x in d['atributos'] if x.split(':')[0][2:].strip() == a]
                if not encontrados:
                    raise KeyError(f'{nombre} no tiene el atributo {a}')
                lista_atr += encontrados
        lista_met = []
        for m in metodos:
            encontrados = [x['firma'] for x in d['metodos'] if x['nombre'] == m]
            if not encontrados:
                raise KeyError(f'{nombre} no tiene el método {m}')
            for firma in encontrados:
                anotacion = d['auditar'].get(m)
                lista_met.append(firma + (f'  «@Auditar({anotacion})»' if anotacion else ''))
        return {'nombre': nombre, 'estereotipo': d['estereotipo'], 'atributos': lista_atr, 'metodos': lista_met}
    return construir


def S(nombre, estereotipo, atributos=(), metodos=()):
    """Una clase que no está en el código del proyecto (Spring Data, anotaciones, etc.)."""
    return lambda: {'nombre': nombre, 'estereotipo': estereotipo, 'atributos': list(atributos), 'metodos': list(metodos)}


def JPA(*metodos):
    firmas = {
        'save': '+ save(entity: T): T',
        'findById': '+ findById(id: ID): Optional<T>',
        'existsById': '+ existsById(id: ID): boolean',
        'delete': '+ delete(entity: T): void',
        'deleteById': '+ deleteById(id: ID): void',
        'getReferenceById': '+ getReferenceById(id: ID): T',
        'count': '+ count(): long',
    }
    return S('JpaRepository<T, ID>', 'Interface', [], [firmas[m] for m in metodos])


AUDITAR = S('Auditar', 'Annotation', [], ['+ accion(): AccionLog', '+ entidad(): EntidadLog'])


def AUDITORIA():
    return [C('AuditoriaAspect', ['auditar']), C('LogService', ['registrar']), AUDITAR]


REL_AUDITORIA = [
    ('AuditoriaAspect', 'Auditar', 'usa', 'intercepta los métodos con', None),
    ('AuditoriaAspect', 'LogService', 'asoc', 'logService', None),
]


# ----------------------------------------------------------------------------------------------
# Secuencias
# ----------------------------------------------------------------------------------------------

U = ('u', 'Usuario', 'actor')
ASP = ('asp', 'AuditoriaAspect', 'Aspect')
LOG = ('log', 'LogService', 'Service')
NOTA_TOKEN = ('nota', 'ctl', 'codigoUsuario sale del token JWT (JwtAuthFilter)')


def auditado(llamada, accion, entidad, cuerpo, retorno, srv='srv'):
    """Llamada a un método con @Auditar: el aspecto la envuelve y guarda la fila en log_actividad."""
    return [
        ('m', 'ctl', 'asp', f'{llamada}'),
        ('m', 'asp', srv, f'proceed() → {llamada}'),
        *cuerpo,
        ('r', srv, 'asp', retorno),
        ('m', 'asp', 'log', f'registrar(codigoUsuario, {accion}, {entidad}, codigo, EXITO, detalle)'),
        ('r', 'asp', 'ctl', retorno),
    ]


def error_auditado(excepcion, accion, entidad, http, srv='srv', front='svc', mensaje=None):
    return [
        ('r', srv, 'asp', excepcion),
        ('m', 'asp', 'log', f'registrar(codigoUsuario, {accion}, {entidad}, codigo, ERROR, motivo)'),
        ('r', 'asp', 'ctl', excepcion),
        ('r', 'ctl', front, f'{http} ErrorResponse (ManejadorExcepciones)'),
    ] + ([('r', front, 'ui', mensaje)] if mensaje else [])


HU = {}

# ================================ Metas (HU1 a HU4) ============================================

HU['HU01'] = dict(
    nombre='Crear meta',
    filas=[
        [C('MetaController', ['crearMeta'])],
        [C('CrearMetaRequest'), C('MetaService', ['crearMeta']), C('MetaResponse', ['desde'])],
        [C('MetaRepository', []), C('UsuarioRepository', []), JPA('save', 'findById'), *AUDITORIA()],
        [C('Meta', ['crear']), C('Participacion', [], ['id', 'usuario', 'meta', 'estadoInvitacion']),
         C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido'])],
        [C('Plazo'), C('TipoMeta'), C('EstadoMeta'), C('EstadoInvitacion')],
    ],
    relaciones=[
        ('MetaController', 'MetaService', 'asoc', 'metaService', None),
        ('MetaController', 'CrearMetaRequest', 'usa', 'recibe', None),
        ('MetaService', 'MetaResponse', 'usa', 'devuelve', None),
        ('MetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('MetaService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('MetaRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('UsuarioRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('MetaService', 'Auditar', 'usa', 'crearMeta anotado con', None),
        *REL_AUDITORIA,
        ('Meta', 'Participacion', 'comp', 'participaciones', '1..*'),
        ('Meta', 'Usuario', 'asoc', 'creador', '1'),
        ('Participacion', 'Usuario', 'asoc', 'usuario', '1'),
        ('Meta', 'Plazo', 'asoc', 'plazo', '1'),
        ('Meta', 'TipoMeta', 'asoc', 'tipo', '1'),
        ('Meta', 'EstadoMeta', 'asoc', 'estado', '1'),
        ('Participacion', 'EstadoInvitacion', 'asoc', '', '1'),
    ],
    participantes=[U, ('ui', 'CrearMetaPage', 'boundary'), ('svc', 'metaService.js', 'module'),
                   ('ctl', 'MetaController', 'RestController'), ASP, ('srv', 'MetaService', 'Service'),
                   ('ur', 'UsuarioRepository', 'Repository'), ('meta', 'meta : Meta', 'Entity'),
                   ('mr', 'MetaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'escribe nombre, plazo y tipo y pulsa «Crear meta»'),
        ('m', 'ui', 'ui', 'validar(formulario)'),
        ('m', 'ui', 'svc', 'crearMeta({ nombre, plazo, tipo })'),
        ('m', 'svc', 'ctl', 'POST /api/metas (Bearer token)'),
        NOTA_TOKEN,
        *auditado('crearMeta(solicitud, codigoUsuario)', 'CREAR_META', 'META', [
            ('m', 'srv', 'srv', 'validar(solicitud)'),
            ('alt', [
                ('[datos válidos]', [
                    ('m', 'srv', 'ur', 'findById(codigoUsuario)'),
                    ('r', 'ur', 'srv', 'creador : Usuario'),
                    ('m', 'srv', 'meta', '«create» new Meta(nombre, plazo, tipo)'),
                    ('m', 'srv', 'meta', 'crear(creador)  [estado ACTIVA + participación ACEPTADA]'),
                    ('m', 'srv', 'mr', 'save(meta)'),
                    ('r', 'mr', 'srv', 'meta guardada'),
                ]),
                ('[nombre vacío o mayor de 200, plazo o tipo faltantes]', [
                    ('r', 'srv', 'asp', 'ValidacionException(errores)'),
                    ('m', 'asp', 'log', 'registrar(..., CREAR_META, META, null, ERROR, errores)'),
                    ('r', 'ctl', 'svc', '400 ErrorResponse con la lista de errores'),
                    ('r', 'svc', 'ui', 'muestra cada error junto a su campo'),
                ]),
            ]),
        ], 'MetaResponse.desde(meta)'),
        ('r', 'ctl', 'svc', '201 Created + Location /api/metas/{codigo} + MetaResponse'),
        ('r', 'svc', 'ui', 'meta'),
        ('r', 'ui', 'u', 'vuelve a Mis metas con el aviso «Meta creada»'),
    ],
)

HU['HU02'] = dict(
    nombre='Editar meta',
    filas=[
        [C('MetaController', ['editarMeta'])],
        [C('CrearMetaRequest'), C('MetaService', ['editarMeta']), C('MetaResponse', ['desde'])],
        [C('MetaRepository', []), JPA('save', 'findById'), C('PermisoDenegadoException', []),
         C('ValidacionException', ['getErrores']), *AUDITORIA()],
        [C('Meta', ['editar', 'getCreador', 'getEstado']), C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido']),
         C('EstadoMeta')],
    ],
    relaciones=[
        ('MetaController', 'MetaService', 'asoc', 'metaService', None),
        ('MetaController', 'CrearMetaRequest', 'usa', 'recibe', None),
        ('MetaService', 'MetaResponse', 'usa', 'devuelve', None),
        ('MetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('MetaRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('MetaService', 'PermisoDenegadoException', 'usa', 'lanza (no es el creador)', None),
        ('MetaService', 'ValidacionException', 'usa', 'lanza (cumplida o datos inválidos)', None),
        ('MetaService', 'Auditar', 'usa', 'editarMeta anotado con', None),
        *REL_AUDITORIA,
        ('Meta', 'Usuario', 'asoc', 'creador', '1'),
        ('Meta', 'EstadoMeta', 'asoc', 'estado', '1'),
    ],
    participantes=[U, ('ui', 'EditarMetaPage', 'boundary'), ('svc', 'metaService.js', 'module'),
                   ('ctl', 'MetaController', 'RestController'), ASP, ('srv', 'MetaService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('meta', 'meta : Meta', 'Entity'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'cambia los datos y pulsa «Guardar cambios»'),
        ('m', 'ui', 'svc', 'editarMeta(codigoMeta, datos)'),
        ('m', 'svc', 'ctl', 'PUT /api/metas/{codigoMeta}'),
        NOTA_TOKEN,
        *auditado('editarMeta(codigoMeta, solicitud, codigoUsuario)', 'EDITAR_META', 'META', [
            ('m', 'srv', 'mr', 'findById(codigoMeta)'),
            ('r', 'mr', 'srv', 'meta'),
            ('m', 'srv', 'meta', 'getCreador(), getEstado()'),
            ('alt', [
                ('[es el creador y la meta está ACTIVA]', [
                    ('m', 'srv', 'srv', 'validar(solicitud)'),
                    ('m', 'srv', 'meta', 'editar(nombre, plazo, tipo)'),
                    ('m', 'srv', 'mr', 'save(meta)'),
                    ('r', 'mr', 'srv', 'meta'),
                ]),
                ('[la meta ya está CUMPLIDA]', [
                    ('r', 'srv', 'asp', 'ValidacionException("Una meta cumplida no se puede editar")'),
                    ('m', 'asp', 'log', 'registrar(..., EDITAR_META, ..., ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 ErrorResponse'),
                ]),
            ]),
        ], 'MetaResponse'),
        ('r', 'ctl', 'svc', '200 OK + MetaResponse'),
        ('r', 'svc', 'ui', 'meta editada'),
        ('r', 'ui', 'u', 'vuelve a Mis metas con el aviso «Meta actualizada»'),
    ],
)

HU['HU03'] = dict(
    nombre='Ver mis metas',
    filas=[
        [C('MetaController', ['listarMetas']), C('SubMetaController', ['resumenes'])],
        [C('FiltroEstado', ['incluye']), C('MetaService', ['listarMetas']), C('CaminoService', ['resumenes']),
         C('ResumenCaminoResponse')],
        [C('MetaRepository', ['findByParticipante', 'findActivasByParticipante']), C('MetaResponse', ['desde']),
         C('ParticipanteResponse', ['desde']), C('AvanceResponse', ['de']), *AUDITORIA()],
        [C('Meta', ['participantesVisibles', 'isOcultaPara']), C('Participacion', [], ['usuario', 'estadoInvitacion', 'fechaCumplida', 'oculta'])],
    ],
    relaciones=[
        ('MetaController', 'MetaService', 'asoc', 'metaService', None),
        ('MetaController', 'FiltroEstado', 'usa', '?estado=', None),
        ('SubMetaController', 'CaminoService', 'asoc', 'caminoService', None),
        ('MetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('CaminoService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('MetaService', 'MetaResponse', 'usa', 'devuelve', None),
        ('CaminoService', 'ResumenCaminoResponse', 'usa', 'devuelve', None),
        ('ResumenCaminoResponse', 'AvanceResponse', 'asoc', 'avance', '1'),
        ('MetaResponse', 'ParticipanteResponse', 'asoc', 'participantes', '0..*'),
        ('MetaService', 'Auditar', 'usa', 'listarMetas anotado con', None),
        *REL_AUDITORIA,
        ('Meta', 'Participacion', 'comp', 'participaciones', '1..*'),
    ],
    participantes=[U, ('ui', 'MisMetasPage', 'boundary'), ('svc', 'metaService.js', 'module'),
                   ('svc2', 'subMetaService.js', 'module'), ('ctl', 'MetaController', 'RestController'), ASP,
                   ('srv', 'MetaService', 'Service'), ('ctl2', 'SubMetaController', 'RestController'),
                   ('cam', 'CaminoService', 'Service'), ('mr', 'MetaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'abre Mis metas'),
        ('m', 'ui', 'svc', 'listarMetas({ estado: ACTIVA })'),
        ('m', 'svc', 'ctl', 'GET /api/metas'),
        NOTA_TOKEN,
        *auditado('listarMetas(ACTIVA, null, null, codigoUsuario)', 'LISTAR_METAS', 'META', [
            ('m', 'srv', 'mr', 'findByParticipante(codigoUsuario)'),
            ('r', 'mr', 'srv', 'List<Meta>'),
            ('m', 'srv', 'srv', 'filtrar por estado, tipo y plazo → MetaResponse.desde(meta, codigoUsuario)'),
        ], 'List<MetaResponse>'),
        ('r', 'ctl', 'svc', '200 OK + metas'),
        ('m', 'ui', 'svc2', 'obtenerResumenes()'),
        ('m', 'svc2', 'ctl2', 'GET /api/metas/camino-hoy'),
        ('m', 'ctl2', 'cam', 'resumenes(codigoUsuario)'),
        ('m', 'cam', 'mr', 'findActivasByParticipante(codigoUsuario)'),
        ('r', 'mr', 'cam', 'metas activas'),
        ('m', 'cam', 'cam', 'avance por hitos + hábitos de hoy + constancia de 7 días'),
        ('r', 'cam', 'ctl2', 'List<ResumenCaminoResponse>'),
        ('r', 'ctl2', 'svc2', '200 OK'),
        ('alt', [
            ('[tiene metas]', [('r', 'ui', 'u', 'tarjetas con avance, hábitos de hoy y participantes')]),
            ('[no tiene metas]', [('r', 'ui', 'u', 'estado vacío con «+ Nueva meta»')]),
        ]),
    ],
)

HU['HU04'] = dict(
    nombre='Eliminar meta',
    filas=[
        [C('MetaController', ['eliminarMeta'])],
        [C('MetaService', ['eliminarMeta']), C('PermisoDenegadoException', [])],
        [C('MetaRepository', []), JPA('findById', 'delete'), *AUDITORIA()],
        [C('Meta', ['getCreador']), C('Participacion', [], ['id', 'usuario', 'meta']),
         C('SubMeta', [], ['codigoSubMeta', 'meta', 'nombre', 'tipo']), C('RegistroSubMeta', [], ['codigoRegistro', 'subMeta', 'fecha', 'valor'])],
    ],
    relaciones=[
        ('MetaController', 'MetaService', 'asoc', 'metaService', None),
        ('MetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('MetaRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('MetaService', 'PermisoDenegadoException', 'usa', 'lanza', None),
        ('MetaService', 'Auditar', 'usa', 'eliminarMeta anotado con', None),
        *REL_AUDITORIA,
        ('Meta', 'Participacion', 'comp', 'ON DELETE CASCADE', '1..*'),
        ('SubMeta', 'Meta', 'asoc', 'meta (ON DELETE CASCADE)', '1'),
        ('RegistroSubMeta', 'SubMeta', 'asoc', 'subMeta (ON DELETE CASCADE)', '1'),
    ],
    participantes=[U, ('ui', 'MisMetasPage', 'boundary'), ('modal', 'ConfirmarEliminarModal', 'boundary'),
                   ('svc', 'metaService.js', 'module'), ('ctl', 'MetaController', 'RestController'), ASP,
                   ('srv', 'MetaService', 'Service'), ('mr', 'MetaRepository', 'Repository'), ('bd', 'PostgreSQL', 'database'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'menú ⋯ → «Eliminar»'),
        ('m', 'ui', 'modal', 'abrir(meta, resumen del camino)'),
        ('r', 'modal', 'u', '«¿Eliminar…?» con los pasos y días que se borran'),
        ('m', 'u', 'modal', 'pulsa «Eliminar meta»'),
        ('m', 'modal', 'svc', 'eliminarMeta(codigoMeta)'),
        ('m', 'svc', 'ctl', 'DELETE /api/metas/{codigoMeta}'),
        NOTA_TOKEN,
        *auditado('eliminarMeta(codigoMeta, codigoUsuario)', 'ELIMINAR_META', 'META', [
            ('m', 'srv', 'mr', 'findById(codigoMeta)'),
            ('r', 'mr', 'srv', 'meta'),
            ('alt', [
                ('[es el creador]', [
                    ('m', 'srv', 'mr', 'delete(meta)'),
                    ('m', 'mr', 'bd', 'DELETE meta → cascada: participación, sub_meta, registro_sub_meta'),
                ]),
                ('[no es el creador]', [
                    ('r', 'srv', 'asp', 'PermisoDenegadoException'),
                    ('m', 'asp', 'log', 'registrar(..., ELIMINAR_META, ..., ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '403 ErrorResponse'),
                ]),
            ]),
        ], 'void'),
        ('r', 'ctl', 'svc', '204 No Content'),
        ('r', 'svc', 'ui', 'quita la tarjeta de la lista'),
        ('r', 'ui', 'u', 'aviso «Meta eliminada»'),
    ],
)

# ================================ Camino (HU28, HU5, HU29) =====================================

HU['HU28'] = dict(
    nombre='Trazar el camino de una meta (hitos y hábitos)',
    filas=[
        [C('SubMetaController', ['crear', 'paso', 'editar', 'eliminar'])],
        [C('SubMetaRequest'), C('SubMetaService', ['crear', 'editar', 'eliminar']), C('CaminoService', ['paso']),
         C('PasoResponse')],
        [C('SubMetaRepository', ['existsByMetaCodigoMetaAndNombreIgnoreCase']), C('MetaRepository', []),
         C('SubMetaResponse', ['desde']), *AUDITORIA()],
        [C('Meta', ['getCreador', 'getEstado']), C('SubMeta', ['actualizar', 'esHito', 'esHabito']),
         C('TipoSubMeta'), C('TipoObjetivo')],
    ],
    relaciones=[
        ('SubMetaController', 'SubMetaService', 'asoc', 'subMetaService', None),
        ('SubMetaController', 'CaminoService', 'asoc', 'caminoService', None),
        ('SubMetaController', 'SubMetaRequest', 'usa', 'recibe', None),
        ('SubMetaService', 'SubMetaRepository', 'asoc', 'subMetaRepository', None),
        ('SubMetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('SubMetaService', 'SubMetaResponse', 'usa', 'devuelve', None),
        ('CaminoService', 'PasoResponse', 'usa', 'devuelve', None),
        ('PasoResponse', 'SubMetaResponse', 'asoc', 'paso', '1'),
        ('SubMetaService', 'Auditar', 'usa', 'crear, editar y eliminar anotados con', None),
        *REL_AUDITORIA,
        ('SubMeta', 'Meta', 'asoc', 'meta', '1'),
        ('SubMeta', 'TipoSubMeta', 'asoc', 'tipo', '1'),
        ('SubMeta', 'TipoObjetivo', 'asoc', 'tipoObjetivo', '0..1'),
    ],
    participantes=[U, ('ui', 'SubMetaFormPage', 'boundary'), ('svc', 'subMetaService.js', 'module'),
                   ('ctl', 'SubMetaController', 'RestController'), ASP, ('srv', 'SubMetaService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('sr', 'SubMetaRepository', 'Repository'),
                   ('sm', 'paso : SubMeta', 'Entity'), LOG],
    pasos=[
        ('m', 'u', 'ui', '«+ Agregar paso» → elige Hito (o una idea según el tipo de meta)'),
        ('m', 'u', 'ui', 'escribe el nombre y pulsa «Guardar paso»'),
        ('m', 'ui', 'svc', 'crearSubMeta(codigoMeta, { nombre, tipo: HITO })'),
        ('m', 'svc', 'ctl', 'POST /api/metas/{codigoMeta}/submetas'),
        NOTA_TOKEN,
        *auditado('crear(codigoMeta, pedido, codigoUsuario)', 'CREAR_SUB_META', 'SUB_META', [
            ('m', 'srv', 'mr', 'findById(codigoMeta)'),
            ('r', 'mr', 'srv', 'meta'),
            ('alt', [
                ('[es el creador y la meta está ACTIVA]', [
                    ('m', 'srv', 'sr', 'existsByMetaCodigoMetaAndNombreIgnoreCase(codigoMeta, nombre)'),
                    ('r', 'sr', 'srv', 'false'),
                    ('m', 'srv', 'sm', '«create» new SubMeta(meta, HITO, hoy)'),
                    ('m', 'srv', 'sm', 'actualizar(nombre, …)  [un hito no guarda pregunta, unidad ni días]'),
                    ('m', 'srv', 'sr', 'save(paso)'),
                    ('r', 'sr', 'srv', 'paso'),
                ]),
                ('[no es el creador]', [
                    ('r', 'srv', 'asp', 'PermisoDenegadoException'),
                    ('m', 'asp', 'log', 'registrar(..., CREAR_SUB_META, ..., ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '403 ErrorResponse'),
                ]),
            ]),
        ], 'SubMetaResponse'),
        ('r', 'ctl', 'svc', '201 Created + SubMetaResponse'),
        ('r', 'svc', 'ui', 'paso creado'),
        ('r', 'ui', 'u', 'vuelve a Tu camino: el hito aparece pendiente y el avance se recalcula'),
    ],
)

HU['HU05'] = dict(
    nombre='Registrar mi avance (hitos logrados y días de los hábitos)',
    filas=[
        [C('SubMetaController', ['lograrHito', 'deshacerHito', 'registrar', 'quitar'])],
        [C('SubMetaService', ['lograrHito', 'deshacerHito', 'avance']), C('RegistroDiaService', ['registrar', 'quitar']),
         C('RegistrarDiaRequest')],
        [C('LogroHitoResponse'), C('AvanceResponse', ['de', 'completo']), C('DiaResponse'),
         C('SubMetaRepository', ['findByMetaCodigoMetaOrderByCodigoSubMetaAsc']),
         C('RegistroSubMetaRepository', ['findBySubMetaCodigoSubMetaAndUsuarioCodigoUsuarioAndFecha'])],
        [C('MetaRepository', ['esParticipanteAceptado']), C('CalendarioUsuario', ['hoy']), *AUDITORIA()],
        [C('SubMeta', ['lograr', 'deshacerLogro', 'isLogrado', 'tocaEl', 'cumple']),
         C('RegistroSubMeta', ['cambiarValor']), C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido'])],
    ],
    relaciones=[
        ('SubMetaController', 'SubMetaService', 'asoc', 'subMetaService', None),
        ('SubMetaController', 'RegistroDiaService', 'asoc', 'registroDiaService', None),
        ('SubMetaController', 'RegistrarDiaRequest', 'usa', 'recibe', None),
        ('SubMetaService', 'LogroHitoResponse', 'usa', 'devuelve', None),
        ('LogroHitoResponse', 'AvanceResponse', 'asoc', 'avance', '1'),
        ('RegistroDiaService', 'DiaResponse', 'usa', 'devuelve', None),
        ('SubMetaService', 'SubMetaRepository', 'asoc', 'subMetaRepository', None),
        ('SubMetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('SubMetaService', 'CalendarioUsuario', 'asoc', 'calendario', None),
        ('RegistroDiaService', 'RegistroSubMetaRepository', 'asoc', 'registroRepository', None),
        ('RegistroDiaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('SubMetaService', 'Auditar', 'usa', 'LOGRAR_HITO / DESHACER_HITO', None),
        ('RegistroDiaService', 'Auditar', 'usa', 'REGISTRAR_DIA / QUITAR_REGISTRO', None),
        *REL_AUDITORIA,
        ('SubMeta', 'Usuario', 'asoc', 'logradoPor', '0..1'),
        ('RegistroSubMeta', 'SubMeta', 'asoc', 'subMeta', '1'),
        ('RegistroSubMeta', 'Usuario', 'asoc', 'usuario', '1'),
    ],
    participantes=[U, ('ui', 'CaminoMetaPage', 'boundary'), ('svc', 'subMetaService.js', 'module'),
                   ('ctl', 'SubMetaController', 'RestController'), ASP, ('srv', 'SubMetaService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('sm', 'hito : SubMeta', 'Entity'),
                   ('sr', 'SubMetaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'marca la casilla del hito «Terminar el libro 3»'),
        ('m', 'ui', 'svc', 'lograrHito(codigoSubMeta)'),
        ('m', 'svc', 'ctl', 'PUT /api/submetas/{codigoSubMeta}/logro'),
        NOTA_TOKEN,
        *auditado('lograrHito(codigoSubMeta, codigoUsuario)', 'LOGRAR_HITO', 'SUB_META', [
            ('m', 'srv', 'sr', 'findById(codigoSubMeta)'),
            ('r', 'sr', 'srv', 'hito'),
            ('m', 'srv', 'mr', 'esParticipanteAceptado(codigoMeta, codigoUsuario)'),
            ('r', 'mr', 'srv', 'true'),
            ('alt', [
                ('[meta ACTIVA y el paso es un hito pendiente]', [
                    ('m', 'srv', 'sm', 'lograr(usuario, hoy)'),
                    ('m', 'srv', 'sr', 'save(hito)'),
                ]),
                ('[el paso es un hábito]', [
                    ('r', 'srv', 'asp', 'ValidacionException("Solo los hitos se marcan como logrados…")'),
                    ('m', 'asp', 'log', 'registrar(..., LOGRAR_HITO, ..., ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 ErrorResponse'),
                ]),
            ]),
            ('m', 'srv', 'srv', 'avance(codigoMeta)'),
            ('m', 'srv', 'sr', 'findByMetaCodigoMetaOrderByCodigoSubMetaAsc(codigoMeta)'),
            ('r', 'sr', 'srv', 'pasos → AvanceResponse.de(logrados, total)'),
        ], 'LogroHitoResponse(hito, avance)'),
        ('r', 'ctl', 'svc', '200 OK + { hito, avance }'),
        ('r', 'svc', 'ui', 'respuesta'),
        ('m', 'ui', 'ui', 'recargar() Tu camino'),
        ('alt', [
            ('[avance.logrados < avance.total]', [('r', 'ui', 'u', 'aviso «¡Hito logrado! Vas en 75 %»')]),
            ('[todos los hitos logrados]', [('r', 'ui', 'u', '«¡Lograste todos los hitos!» + botón Marcar meta como cumplida (HU6)')]),
        ]),
    ],
)

HU['HU29'] = dict(
    nombre='Ver cuánto me falta y qué tan constante soy',
    filas=[
        [C('SubMetaController', ['camino', 'estadisticas'])],
        [C('CaminoService', ['camino', 'estadisticas']), C('CalculoCamino', ['dias', 'constancia', 'rachaActual', 'rachas', 'porDiaSemana', 'diasSeguidosCompletos'])],
        [C('CaminoResponse'), C('EstadisticasResponse'), C('SubMetaCaminoResponse')],
        [C('AvanceResponse', ['de', 'completo']), C('DiaResponse'), C('SubMetaResponse', ['desde']),
         C('SubMetaRepository', ['findByMetaCodigoMetaOrderByCodigoSubMetaAsc']),
         C('RegistroSubMetaRepository', ['findBySubMetaCodigoSubMetaInAndUsuarioCodigoUsuarioAndFechaBetween'])],
        [C('SubMeta', ['esHito', 'isLogrado', 'tocaEl', 'cumple']), C('RegistroSubMeta', [])],
    ],
    relaciones=[
        ('SubMetaController', 'CaminoService', 'asoc', 'caminoService', None),
        ('CaminoService', 'CalculoCamino', 'usa', 'reglas de los hábitos', None),
        ('CaminoService', 'CaminoResponse', 'usa', 'devuelve', None),
        ('CaminoService', 'EstadisticasResponse', 'usa', 'devuelve', None),
        ('CaminoResponse', 'AvanceResponse', 'asoc', 'avance', '1'),
        ('CaminoResponse', 'SubMetaResponse', 'asoc', 'hitos', '0..*'),
        ('CaminoResponse', 'SubMetaCaminoResponse', 'asoc', 'habitos', '0..*'),
        ('SubMetaCaminoResponse', 'DiaResponse', 'asoc', 'dias', '7'),
        ('EstadisticasResponse', 'DiaResponse', 'asoc', 'dias', '0..365'),
        ('CaminoService', 'SubMetaRepository', 'asoc', 'subMetaRepository', None),
        ('CaminoService', 'RegistroSubMetaRepository', 'asoc', 'registroRepository', None),
        ('RegistroSubMeta', 'SubMeta', 'asoc', 'subMeta', '1'),
    ],
    participantes=[U, ('ui', 'CaminoMetaPage / EstadisticasSubMetaPage', 'boundary'), ('svc', 'subMetaService.js', 'module'),
                   ('ctl', 'SubMetaController', 'RestController'), ('srv', 'CaminoService', 'Service'),
                   ('sr', 'SubMetaRepository', 'Repository'), ('rr', 'RegistroSubMetaRepository', 'Repository'),
                   ('calc', 'CalculoCamino', 'utility')],
    pasos=[
        ('m', 'u', 'ui', 'abre «Ver mi camino» de una meta'),
        ('m', 'ui', 'svc', 'obtenerCamino(codigoMeta)'),
        ('m', 'svc', 'ctl', 'GET /api/metas/{codigoMeta}/camino'),
        NOTA_TOKEN,
        ('m', 'ctl', 'srv', 'camino(codigoMeta, codigoUsuario)'),
        ('m', 'srv', 'sr', 'findByMetaCodigoMetaOrderByCodigoSubMetaAsc(codigoMeta)'),
        ('r', 'sr', 'srv', 'pasos → separa hitos y hábitos'),
        ('m', 'srv', 'srv', 'avance(hitos) = logrados / total'),
        ('m', 'srv', 'rr', 'findBySubMetaCodigoSubMetaInAndUsuarioCodigoUsuarioAndFechaBetween(hábitos, usuario, hoy-29, hoy)'),
        ('r', 'rr', 'srv', 'registros de 30 días'),
        ('m', 'srv', 'calc', 'dias(hábito, valores, desde, hasta)'),
        ('m', 'srv', 'calc', 'constancia(días, hoy, 7) y constancia(días, hoy, 30)'),
        ('m', 'srv', 'calc', 'diasSeguidosCompletos(díasPorHábito, hoy)'),
        ('r', 'srv', 'ctl', 'CaminoResponse(avance, constancias, racha, hitos, hábitos)'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('alt', [
            ('[la meta tiene hitos]', [('r', 'ui', 'u', 'anillo con el avance real (p. ej. 75 %) y «3 de 4 hitos»')]),
            ('[sin hitos]', [('r', 'ui', 'u', 'sin porcentaje engañoso: ícono del camino y la constancia como texto')]),
        ]),
        ('m', 'u', 'ui', 'toca el nombre de un hábito'),
        ('m', 'ui', 'svc', 'obtenerEstadisticas(codigoSubMeta)'),
        ('m', 'svc', 'ctl', 'GET /api/submetas/{codigoSubMeta}/estadisticas'),
        ('m', 'ctl', 'srv', 'estadisticas(codigoSubMeta, codigoUsuario)'),
        ('m', 'srv', 'rr', 'registros del hábito (hasta un año)'),
        ('m', 'srv', 'calc', 'rachas(), rachaActual(), porDiaSemana()'),
        ('r', 'srv', 'ctl', 'EstadisticasResponse'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'ui', 'u', 'constancia 7 y 30 días, rachas, calendario; historial plegado'),
    ],
)

# ================================ HU6, HU11, HU15 =============================================

HU['HU06'] = dict(
    nombre='Marcar meta como cumplida',
    filas=[
        [C('MetaController', ['marcarCumplida'])],
        [C('MetaService', ['marcarCumplida']), C('InsigniaService', ['otorgarNuevas']), C('MetaCumplidaResponse')],
        [C('MetaRepository', []), JPA('findById', 'save'), C('MetaResponse', ['desde']), C('InsigniaResponse'), *AUDITORIA()],
        [C('Meta', ['marcarCumplida', 'otrosParticipantesAceptados', 'getFechaCumplida']),
         C('Participacion', ['marcarCumplida'], ['usuario', 'estadoInvitacion', 'fechaCumplida']), C('EstadoMeta')],
    ],
    relaciones=[
        ('MetaController', 'MetaService', 'asoc', 'metaService', None),
        ('MetaService', 'InsigniaService', 'asoc', 'insigniaService', None),
        ('MetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('MetaRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('MetaService', 'MetaCumplidaResponse', 'usa', 'devuelve', None),
        ('MetaCumplidaResponse', 'MetaResponse', 'asoc', 'meta', '1'),
        ('MetaCumplidaResponse', 'InsigniaResponse', 'asoc', 'insigniasNuevas', '0..*'),
        ('MetaService', 'Auditar', 'usa', 'marcarCumplida anotado con', None),
        *REL_AUDITORIA,
        ('Meta', 'Participacion', 'comp', 'participaciones', '1..*'),
        ('Meta', 'EstadoMeta', 'asoc', 'estado', '1'),
    ],
    participantes=[U, ('ui', 'MisMetasPage / CaminoMetaPage', 'boundary'), ('modal', 'ConfirmarCumplidaModal', 'boundary'),
                   ('svc', 'metaService.js', 'module'), ('ctl', 'MetaController', 'RestController'), ASP,
                   ('srv', 'MetaService', 'Service'), ('mr', 'MetaRepository', 'Repository'),
                   ('meta', 'meta : Meta', 'Entity'), ('ins', 'InsigniaService', 'Service'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'pulsa el círculo de la tarjeta (o «Marcar meta como cumplida»)'),
        ('m', 'ui', 'modal', 'abrir(meta)'),
        ('r', 'modal', 'u', '«¿Marcar esta meta como cumplida?» (no se puede deshacer)'),
        ('m', 'u', 'modal', 'confirma'),
        ('m', 'modal', 'svc', 'marcarCumplida(codigoMeta)'),
        ('m', 'svc', 'ctl', 'PATCH /api/metas/{codigoMeta}/cumplida'),
        NOTA_TOKEN,
        *auditado('marcarCumplida(codigoMeta, codigoUsuario)', 'MARCAR_CUMPLIDA', 'META', [
            ('m', 'srv', 'mr', 'findById(codigoMeta)'),
            ('r', 'mr', 'srv', 'meta'),
            ('alt', [
                ('[es el creador y la meta está ACTIVA]', [
                    ('m', 'srv', 'meta', 'marcarCumplida(codigoUsuario, hoy)  [CUMPLIDA + fecha a cada participante aceptado]'),
                    ('m', 'srv', 'mr', 'save(meta)'),
                    ('m', 'srv', 'ins', 'otorgarNuevas(codigoUsuario)  (HU11)'),
                    ('r', 'ins', 'srv', 'insigniasNuevas'),
                    ('m', 'srv', 'meta', 'otrosParticipantesAceptados(codigoUsuario)  (HU15)'),
                    ('m', 'srv', 'ins', 'otorgarNuevas(cada participante)'),
                ]),
                ('[no es el creador]', [
                    ('r', 'srv', 'asp', 'PermisoDenegadoException("Solo el creador puede marcar la meta como cumplida")'),
                    ('m', 'asp', 'log', 'registrar(..., MARCAR_CUMPLIDA, ..., ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '403 ErrorResponse'),
                ]),
            ]),
        ], 'MetaCumplidaResponse(meta, insigniasNuevas)'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'svc', 'ui', 'meta cumplida + insignias nuevas'),
        ('r', 'ui', 'u', 'etiqueta «Cumplida», aviso «¡Meta cumplida!» y avisos de insignias'),
    ],
)

HU['HU11'] = dict(
    nombre='Recibir insignia',
    filas=[
        [C('MetaService', ['marcarCumplida']), C('InsigniaService', ['otorgarNuevas'])],
        [C('MetaRepository', ['plazosYTiposCumplidos']), C('InsigniaRepository', ['findAllByOrderByCodigoInsigniaAsc']),
         C('UsuarioInsigniaRepository', ['codigosObtenidos']), C('InsigniaResponse')],
        [C('Insignia'), C('UsuarioInsignia'), C('UsuarioInsigniaId'), C('CriterioInsignia'), C('Plazo')],
    ],
    relaciones=[
        ('MetaService', 'InsigniaService', 'asoc', 'insigniaService', None),
        ('InsigniaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('InsigniaService', 'InsigniaRepository', 'asoc', 'insigniaRepository', None),
        ('InsigniaService', 'UsuarioInsigniaRepository', 'asoc', 'usuarioInsigniaRepository', None),
        ('InsigniaService', 'InsigniaResponse', 'usa', 'devuelve', None),
        ('UsuarioInsignia', 'Insignia', 'asoc', 'insignia', '1'),
        ('UsuarioInsignia', 'UsuarioInsigniaId', 'comp', 'id', '1'),
        ('Insignia', 'CriterioInsignia', 'asoc', 'criterio', '1'),
        ('Insignia', 'Plazo', 'asoc', 'plazoRequerido', '0..1'),
    ],
    participantes=[U, ('ui', 'MisMetasPage', 'boundary'), ('avisos', 'AvisosInsignia', 'boundary'),
                   ('srv', 'MetaService', 'Service'), ('ins', 'InsigniaService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('uir', 'UsuarioInsigniaRepository', 'Repository'),
                   ('ir', 'InsigniaRepository', 'Repository')],
    pasos=[
        ('nota', 'srv', 'Se dispara al marcar una meta como cumplida (HU6)'),
        ('m', 'srv', 'ins', 'otorgarNuevas(codigoUsuario)'),
        ('m', 'ins', 'mr', 'plazosYTiposCumplidos(codigoUsuario)'),
        ('r', 'mr', 'ins', 'filas (plazo, tipo) de las metas cumplidas'),
        ('m', 'ins', 'ins', 'Conteo.desde(filas): total, por plazo y tipos distintos'),
        ('m', 'ins', 'uir', 'codigosObtenidos(codigoUsuario)'),
        ('r', 'uir', 'ins', 'insignias que ya tiene'),
        ('m', 'ins', 'ir', 'findAllByOrderByCodigoInsigniaAsc()'),
        ('r', 'ir', 'ins', 'catálogo de 6 insignias'),
        ('loop', [
            ('[cada insignia del catálogo]', [
                ('alt', [
                    ('[alcanza(insignia, conteo) y no la tiene]', [
                        ('m', 'ins', 'uir', 'save(new UsuarioInsignia(usuario, insignia, hoy))'),
                        ('m', 'ins', 'ins', 'nuevas.add(InsigniaResponse)'),
                    ]),
                    ('[ya la tiene o no alcanza]', [('m', 'ins', 'ins', 'sigue')]),
                ]),
            ]),
        ]),
        ('r', 'ins', 'srv', 'List<InsigniaResponse> insigniasNuevas'),
        ('r', 'srv', 'ui', 'MetaCumplidaResponse (vía controlador)'),
        ('m', 'ui', 'avisos', 'mostrar(insigniasNuevas)'),
        ('r', 'avisos', 'u', '«¡Ganaste la insignia Primer paso!»'),
    ],
)

HU['HU15'] = dict(
    nombre='Logro de meta compartida',
    filas=[
        [C('MetaService', ['marcarCumplida']), C('InsigniaService', ['otorgarNuevas']), C('PerfilService', ['verMiPerfil'])],
        [C('Meta', ['marcarCumplida', 'otrosParticipantesAceptados', 'getFechaCumplida']),
         C('Participacion', ['marcarCumplida'], ['usuario', 'meta', 'estadoInvitacion', 'fechaCumplida'])],
        [C('EstadoInvitacion'), C('PerfilResponse'), C('MetaResponse', ['desde'])],
    ],
    relaciones=[
        ('MetaService', 'InsigniaService', 'asoc', 'insigniaService', None),
        ('MetaService', 'Meta', 'usa', 'marca cumplida', None),
        ('Meta', 'Participacion', 'comp', 'participaciones', '1..*'),
        ('Participacion', 'EstadoInvitacion', 'asoc', 'estadoInvitacion', '1'),
        ('PerfilService', 'PerfilResponse', 'usa', 'devuelve', None),
        ('PerfilResponse', 'MetaResponse', 'asoc', 'ultimasCumplidas', '0..3'),
    ],
    participantes=[('u', 'Creador', 'actor'), ('srv', 'MetaService', 'Service'), ('meta', 'meta : Meta', 'Entity'),
                   ('p', 'participación : Participacion', 'Entity'), ('ins', 'InsigniaService', 'Service'),
                   ('perfil', 'PerfilService', 'Service'), ('u2', 'Participante', 'actor')],
    pasos=[
        ('m', 'u', 'srv', 'marcarCumplida(codigoMeta, codigoCreador)  (HU6)'),
        ('m', 'srv', 'meta', 'marcarCumplida(codigoCreador, hoy)'),
        ('loop', [
            ('[cada participación con invitación ACEPTADA]', [
                ('m', 'meta', 'p', 'marcarCumplida(hoy)  [misma fecha para todo el equipo]'),
            ]),
        ]),
        ('nota', 'meta', 'Las invitaciones PENDIENTES no cuentan y ya no se pueden aceptar'),
        ('m', 'srv', 'meta', 'otrosParticipantesAceptados(codigoCreador)'),
        ('r', 'meta', 'srv', 'códigos de los demás participantes'),
        ('loop', [
            ('[cada participante]', [('m', 'srv', 'ins', 'otorgarNuevas(codigoParticipante)  (HU11)')]),
        ]),
        ('m', 'u2', 'perfil', 'abre Mi perfil → verMiPerfil(codigoParticipante)'),
        ('m', 'perfil', 'perfil', 'metas donde participa aceptado → últimas cumplidas'),
        ('r', 'perfil', 'u2', 'la meta compartida aparece como logro en su perfil, con su fecha'),
    ],
)

# ================================ Autenticación (HU8, HU9, HU10) ===============================

HU['HU08'] = dict(
    nombre='Registrarse (con autorización por correo)',
    filas=[
        [C('AuthController', ['solicitarRegistro', 'autorizarRegistro', 'estadoSolicitud', 'consultarSolicitud', 'rechazarRegistro', 'registrar'])],
        [C('SolicitudRegistroRequest'), C('SolicitudRegistroService', ['solicitar', 'autorizar', 'estado', 'validarParaRegistro', 'consumir', 'rechazar']),
         C('AuthService', ['registrar']), C('RegistroRequest')],
        [C('SolicitudRegistroRepository', ['findByEmail', 'findByTokenHash', 'findBySesionHash']),
         C('UsuarioRepository', ['existsByEmailIgnoreCase']), C('CorreoService', ['enviar']), C('BrevoCorreoService', ['enviar']),
         C('ConsolaCorreoService', ['enviar'])],
        [C('SolicitudRegistro', ['renovar', 'autorizar', 'bloquear', 'estaAutorizada', 'estaVigente', 'estaBloqueada']),
         C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido', 'email', 'passwordHash', 'fechaRegistro']),
         C('SolicitudRegistroResponse'), C('UsuarioResponse'), C('LogService', ['registrar'])],
    ],
    relaciones=[
        ('AuthController', 'SolicitudRegistroService', 'asoc', 'solicitudRegistroService', None),
        ('AuthController', 'AuthService', 'asoc', 'authService', None),
        ('AuthService', 'SolicitudRegistroService', 'asoc', 'valida y consume la solicitud', None),
        ('AuthController', 'SolicitudRegistroRequest', 'usa', 'recibe', None),
        ('AuthController', 'RegistroRequest', 'usa', 'recibe', None),
        ('SolicitudRegistroService', 'SolicitudRegistroRepository', 'asoc', 'solicitudRepository', None),
        ('SolicitudRegistroService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('AuthService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('SolicitudRegistroService', 'CorreoService', 'asoc', 'correoService', None),
        ('BrevoCorreoService', 'CorreoService', 'impl', '', None),
        ('ConsolaCorreoService', 'CorreoService', 'impl', '', None),
        ('SolicitudRegistroService', 'SolicitudRegistroResponse', 'usa', 'devuelve', None),
        ('AuthService', 'UsuarioResponse', 'usa', 'devuelve', None),
        ('SolicitudRegistroService', 'LogService', 'asoc', 'SOLICITAR_REGISTRO / RECHAZAR_REGISTRO', None),
        ('AuthService', 'LogService', 'asoc', 'REGISTRAR_USUARIO', None),
        ('SolicitudRegistroRepository', 'SolicitudRegistro', 'usa', 'gestiona', None),
        ('UsuarioRepository', 'Usuario', 'usa', 'gestiona', None),
    ],
    participantes=[U, ('ui', 'SolicitarRegistroPage / RegistroPage', 'boundary'), ('svc', 'authService.js', 'module'),
                   ('ctl', 'AuthController', 'RestController'), ('sol', 'SolicitudRegistroService', 'Service'),
                   ('sr', 'SolicitudRegistroRepository', 'Repository'), ('correo', 'CorreoService', 'interface'),
                   ('auth', 'AuthService', 'Service'), ('ur', 'UsuarioRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'escribe su correo y pulsa «Enviar enlace de autorización»'),
        ('m', 'ui', 'svc', 'solicitarRegistro(email)'),
        ('m', 'svc', 'ctl', 'POST /api/auth/registro/solicitar'),
        ('m', 'ctl', 'sol', 'solicitar(pedido)'),
        ('m', 'sol', 'ur', 'existsByEmailIgnoreCase(email)'),
        ('r', 'ur', 'sol', 'false'),
        ('alt', [
            ('[correo libre]', [
                ('m', 'sol', 'sr', 'findByEmail(email) → nueva o renovar(tokenHash, sesionHash, vence)'),
                ('m', 'sol', 'sr', 'save(solicitud)'),
                ('m', 'sol', 'correo', 'enviar(correo con «Autorizar» y «No fui yo»)'),
                ('m', 'sol', 'log', 'registrar(null, SOLICITAR_REGISTRO, USUARIO, …, EXITO)'),
                ('r', 'sol', 'ctl', 'SolicitudRegistroResponse(email, vence, sesion)'),
                ('r', 'ctl', 'ui', '202 Accepted → «Revisa tu correo» (consulta el estado cada pocos segundos)'),
            ]),
            ('[el correo ya tiene cuenta]', [
                ('r', 'sol', 'ctl', 'EmailYaRegistradoException'),
                ('r', 'ctl', 'ui', '409 «Ya existe una cuenta con este correo»'),
            ]),
        ]),
        ('m', 'u', 'ui', 'abre el enlace del correo → AutorizarRegistroPage'),
        ('m', 'ui', 'ctl', 'POST /api/auth/registro/autorizar { token }'),
        ('m', 'ctl', 'sol', 'autorizar(token) → solicitud.autorizar(ahora)'),
        ('r', 'ctl', 'ui', '200 estado AUTORIZADA'),
        ('m', 'u', 'ui', 'completa nombre, apellido y contraseña'),
        ('m', 'ui', 'svc', 'registrar({ token, nombre, apellido, password })'),
        ('m', 'svc', 'ctl', 'POST /api/auth/registro'),
        ('m', 'ctl', 'auth', 'registrar(solicitud)'),
        ('m', 'auth', 'sol', 'validarParaRegistro(token) → solicitud autorizada y vigente'),
        ('m', 'auth', 'ur', 'save(new Usuario(nombre, apellido, email, BCrypt(password)))'),
        ('m', 'auth', 'sol', 'consumir(solicitud)'),
        ('m', 'auth', 'log', 'registrar(codigo, REGISTRAR_USUARIO, USUARIO, codigo, EXITO)'),
        ('r', 'auth', 'ctl', 'UsuarioResponse (sin contraseña)'),
        ('r', 'ctl', 'svc', '201 Created'),
        ('r', 'ui', 'u', 'cuenta creada → Iniciar sesión'),
    ],
)

HU['HU09'] = dict(
    nombre='Iniciar sesión',
    filas=[
        [C('AuthController', ['iniciarSesion'])],
        [C('LoginRequest'), C('AuthService', ['iniciarSesion']), C('LoginResponse')],
        [C('UsuarioRepository', ['findByEmailIgnoreCase']), C('BloqueoLoginService', ['segundosBloqueado', 'registrarFallo', 'limpiar']),
         C('JwtService', ['generarToken']), S('PasswordEncoder', 'Interface', [], ['+ matches(raw: CharSequence, hash: String): boolean'])],
        [C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido', 'email', 'passwordHash']), C('IntentoLoginRepository', []),
         C('IntentoLogin'), C('CredencialesInvalidasException', []), C('CuentaBloqueadaException', []), C('LogService', ['registrar'])],
    ],
    relaciones=[
        ('AuthController', 'AuthService', 'asoc', 'authService', None),
        ('AuthController', 'LoginRequest', 'usa', 'recibe', None),
        ('AuthService', 'LoginResponse', 'usa', 'devuelve', None),
        ('AuthService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('AuthService', 'BloqueoLoginService', 'asoc', 'bloqueoLoginService', None),
        ('AuthService', 'JwtService', 'asoc', 'jwtService', None),
        ('AuthService', 'PasswordEncoder', 'asoc', 'passwordEncoder (BCrypt)', None),
        ('AuthService', 'LogService', 'asoc', 'INICIAR_SESION', None),
        ('BloqueoLoginService', 'IntentoLoginRepository', 'asoc', 'repositorio', None),
        ('IntentoLoginRepository', 'IntentoLogin', 'usa', 'gestiona', None),
        ('UsuarioRepository', 'Usuario', 'usa', 'gestiona', None),
        ('AuthService', 'CredencialesInvalidasException', 'usa', 'lanza', None),
        ('AuthService', 'CuentaBloqueadaException', 'usa', 'lanza', None),
    ],
    participantes=[U, ('ui', 'LoginPage', 'boundary'), ('svc', 'authService.js', 'module'), ('ses', 'sesionService.js', 'module'),
                   ('ctl', 'AuthController', 'RestController'), ('auth', 'AuthService', 'Service'),
                   ('ur', 'UsuarioRepository', 'Repository'), ('blq', 'BloqueoLoginService', 'Service'),
                   ('jwt', 'JwtService', 'Service'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'escribe correo y contraseña y pulsa «Iniciar sesión»'),
        ('m', 'ui', 'svc', 'iniciarSesion(email, password)'),
        ('m', 'svc', 'ctl', 'POST /api/auth/login'),
        ('m', 'ctl', 'auth', 'iniciarSesion(solicitud)'),
        ('m', 'auth', 'ur', 'findByEmailIgnoreCase(email)'),
        ('r', 'ur', 'auth', 'Optional<Usuario>'),
        ('m', 'auth', 'blq', 'segundosBloqueado(email)'),
        ('r', 'blq', 'auth', '0'),
        ('alt', [
            ('[usuario existe y la contraseña coincide (BCrypt)]', [
                ('m', 'auth', 'blq', 'limpiar(email)'),
                ('m', 'auth', 'jwt', 'generarToken(usuario)'),
                ('r', 'jwt', 'auth', 'token (jti, exp)'),
                ('m', 'auth', 'log', 'registrar(codigo, INICIAR_SESION, USUARIO, codigo, EXITO)'),
                ('r', 'auth', 'ctl', 'LoginResponse(token, usuario)'),
                ('r', 'ctl', 'svc', '200 OK'),
                ('m', 'ui', 'ses', 'guardarSesion({ token, codigoUsuario, nombre, … })'),
                ('r', 'ui', 'u', 'entra a Mis metas'),
            ]),
            ('[correo o contraseña incorrectos]', [
                ('m', 'auth', 'blq', 'registrarFallo(email)  [5 fallos → bloqueo temporal]'),
                ('m', 'auth', 'log', 'registrar(…, INICIAR_SESION, …, ERROR, motivo)'),
                ('r', 'auth', 'ctl', 'CredencialesInvalidasException'),
                ('r', 'ctl', 'svc', '401 «Email o contraseña incorrectos»'),
                ('r', 'ui', 'u', 'mensaje sin revelar si el correo existe'),
            ]),
        ]),
    ],
)

HU['HU10'] = dict(
    nombre='Cerrar sesión',
    filas=[
        [C('AuthController', ['cerrarSesion'])],
        [C('AuthService', ['cerrarSesion']), C('JwtService', ['extraerId', 'extraerExpiracion', 'extraerCodigoUsuario'])],
        [C('TokenRevocadoRepository', ['existsByJti']), C('TokenRevocado'), C('LogService', ['registrar']),
         C('JwtAuthFilter', [])],
    ],
    relaciones=[
        ('AuthController', 'AuthService', 'asoc', 'authService', None),
        ('AuthService', 'JwtService', 'asoc', 'jwtService', None),
        ('AuthService', 'TokenRevocadoRepository', 'asoc', 'tokenRevocadoRepository', None),
        ('AuthService', 'LogService', 'asoc', 'CERRAR_SESION', None),
        ('TokenRevocadoRepository', 'TokenRevocado', 'usa', 'gestiona', None),
        ('JwtAuthFilter', 'TokenRevocadoRepository', 'asoc', 'rechaza tokens revocados', None),
        ('JwtAuthFilter', 'JwtService', 'asoc', 'jwtService', None),
    ],
    participantes=[U, ('ui', 'BotonCerrarSesion', 'boundary'), ('svc', 'authService.js', 'module'),
                   ('ses', 'sesionService.js', 'module'), ('ctl', 'AuthController', 'RestController'),
                   ('auth', 'AuthService', 'Service'), ('jwt', 'JwtService', 'Service'),
                   ('tr', 'TokenRevocadoRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'pulsa «Cerrar sesión»'),
        ('r', 'ui', 'u', 'confirmación «¿Cerrar sesión?»'),
        ('m', 'u', 'ui', 'confirma'),
        ('m', 'ui', 'svc', 'cerrarSesionRemota(token)'),
        ('m', 'svc', 'ctl', 'POST /api/auth/logout (Bearer token)'),
        ('m', 'ctl', 'auth', 'cerrarSesion(token)'),
        ('m', 'auth', 'jwt', 'extraerId(token), extraerExpiracion(token), extraerCodigoUsuario(token)'),
        ('r', 'jwt', 'auth', 'jti, expiración, codigoUsuario'),
        ('alt', [
            ('[token válido]', [
                ('m', 'auth', 'tr', 'existsByJti(jti)'),
                ('r', 'tr', 'auth', 'false'),
                ('m', 'auth', 'tr', 'save(new TokenRevocado(jti, expiración))'),
                ('m', 'auth', 'log', 'registrar(codigo, CERRAR_SESION, USUARIO, codigo, EXITO)'),
                ('r', 'ctl', 'svc', '204 No Content'),
            ]),
            ('[token inválido o expirado]', [
                ('r', 'jwt', 'auth', 'TokenInvalidoException'),
                ('r', 'ctl', 'svc', '401 (igual se cierra la sesión local)'),
            ]),
        ]),
        ('m', 'ui', 'ses', 'cerrarSesion()  [borra la sesión del navegador]'),
        ('r', 'ui', 'u', 'vuelve a /login con el aviso «Sesión cerrada»'),
    ],
)

# ================================ HU7 Reporte, HU27 Filtrar ===================================

HU['HU07'] = dict(
    nombre='Ver reporte general',
    filas=[
        [C('ReporteController', ['resumen'])],
        [C('ReporteService', ['resumen']), C('ReporteResponse'), C('CalendarioUsuario', ['hoy'])],
        [C('ReporteRepository', ['contarPorTipoYEstado', 'contarPorPlazoYEstado', 'fechasCumplidasDesde']),
         C('UsuarioRepository', []), *AUDITORIA()],
        [C('Meta', [], ['codigoMeta', 'tipo', 'plazo', 'estado']), C('Participacion', [], ['usuario', 'estadoInvitacion', 'fechaCumplida'])],
    ],
    relaciones=[
        ('ReporteController', 'ReporteService', 'asoc', 'reporteService', None),
        ('ReporteService', 'ReporteRepository', 'asoc', 'reporteRepository', None),
        ('ReporteService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('ReporteService', 'CalendarioUsuario', 'asoc', 'calendario', None),
        ('ReporteService', 'ReporteResponse', 'usa', 'devuelve', None),
        ('ReporteRepository', 'Meta', 'usa', 'JPQL count / group by', None),
        ('ReporteRepository', 'Participacion', 'usa', 'participante aceptado', None),
        ('ReporteService', 'Auditar', 'usa', 'resumen anotado con', None),
        *REL_AUDITORIA,
        ('Meta', 'Participacion', 'comp', 'participaciones', '1..*'),
    ],
    participantes=[U, ('ui', 'ReportePage', 'boundary'), ('chart', 'Chart.js', 'library'), ('svc', 'reporteService.js', 'module'),
                   ('ctl', 'ReporteController', 'RestController'), ASP, ('srv', 'ReporteService', 'Service'),
                   ('rr', 'ReporteRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'abre «Reporte»'),
        ('m', 'ui', 'svc', 'obtenerReporte()'),
        ('m', 'svc', 'ctl', 'GET /api/reportes/resumen'),
        NOTA_TOKEN,
        *auditado('resumen(codigoUsuario)', 'VER_REPORTE', 'USUARIO', [
            ('m', 'srv', 'rr', 'contarPorTipoYEstado(codigoUsuario)'),
            ('r', 'rr', 'srv', 'conteos por tipo'),
            ('m', 'srv', 'rr', 'contarPorPlazoYEstado(codigoUsuario)'),
            ('r', 'rr', 'srv', 'conteos por plazo'),
            ('m', 'srv', 'rr', 'fechasCumplidasDesde(codigoUsuario, hace 6 meses)'),
            ('r', 'rr', 'srv', 'fechas de cumplimiento'),
            ('m', 'srv', 'srv', 'porcentaje general, por tipo, por plazo y por mes'),
        ], 'ReporteResponse'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('alt', [
            ('[tiene metas]', [
                ('m', 'ui', 'chart', 'dibujar(tipo de reporte elegido: dona, barras, barras apiladas o línea)'),
                ('r', 'ui', 'u', 'gráfica + tabla alternativa para lectores de pantalla'),
            ]),
            ('[sin metas]', [('r', 'ui', 'u', '«Aún no hay datos para el reporte» con enlace a crear meta')]),
        ]),
    ],
)

HU['HU27'] = dict(
    nombre='Filtrar metas por estado, tipo y plazo',
    filas=[
        [C('MetaController', ['listarMetas'])],
        [C('FiltroEstado', ['incluye']), C('MetaService', ['listarMetas']), C('MetaResponse', ['desde'])],
        [C('TipoMeta'), C('Plazo'), C('EstadoMeta'), C('MetaRepository', ['findByParticipante']), *AUDITORIA()],
    ],
    relaciones=[
        ('MetaController', 'MetaService', 'asoc', 'metaService', None),
        ('MetaController', 'FiltroEstado', 'usa', '?estado=', None),
        ('MetaController', 'TipoMeta', 'usa', '?tipo=', None),
        ('MetaController', 'Plazo', 'usa', '?plazo=', None),
        ('MetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('MetaService', 'MetaResponse', 'usa', 'devuelve', None),
        ('FiltroEstado', 'EstadoMeta', 'usa', 'incluye(estado)', None),
        ('MetaService', 'Auditar', 'usa', 'listarMetas anotado con', None),
        *REL_AUDITORIA,
    ],
    participantes=[U, ('ui', 'MisMetasPage', 'boundary'), ('fil', 'FiltrosMetas', 'boundary'), ('svc', 'metaService.js', 'module'),
                   ('ctl', 'MetaController', 'RestController'), ASP, ('srv', 'MetaService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'fil', 'elige «Cumplidas», tipo «Académica» y plazo «Corto»'),
        ('m', 'fil', 'ui', 'cambio de filtros (quedan en la URL)'),
        ('m', 'ui', 'svc', 'listarMetas({ estado: CUMPLIDA, tipo: ACADEMICA, plazo: CORTO })'),
        ('m', 'svc', 'ctl', 'GET /api/metas?estado=CUMPLIDA&tipo=ACADEMICA&plazo=CORTO'),
        NOTA_TOKEN,
        ('alt', [
            ('[parámetros válidos]', [
                *auditado('listarMetas(CUMPLIDA, ACADEMICA, CORTO, codigoUsuario)', 'LISTAR_METAS', 'META', [
                    ('m', 'srv', 'mr', 'findByParticipante(codigoUsuario)'),
                    ('r', 'mr', 'srv', 'List<Meta>'),
                    ('m', 'srv', 'srv', 'filter(estado.incluye, tipo, plazo); cumplidas por fecha de cumplimiento'),
                ], 'List<MetaResponse>'),
                ('r', 'ctl', 'svc', '200 OK'),
                ('r', 'ui', 'u', 'metas filtradas o «Ninguna meta coincide con los filtros»'),
            ]),
            ('[valor desconocido, p. ej. estado=X]', [
                ('r', 'ctl', 'svc', '400 ErrorResponse (no se pudo convertir el parámetro)'),
                ('r', 'ui', 'u', 'aviso de error; la lista no cambia'),
            ]),
        ]),
    ],
)

# ================================ Perfil (HU22, HU12, HU23, HU24, HU25, HU26) ================

HU['HU22'] = dict(
    nombre='Ver mi perfil',
    filas=[
        [C('UsuarioController', ['miPerfil'])],
        [C('PerfilService', ['verMiPerfil']), C('PerfilResponse')],
        [C('UsuarioRepository', []), C('MetaRepository', ['findByParticipante']), C('UsuarioInsigniaRepository', ['insigniasDelUsuario']),
         C('InsigniaRepository', []), C('PersonalizacionPerfil', ['soloCabecera']), *AUDITORIA()],
        [C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido', 'email', 'fotoPerfil', 'perfilPrivado', 'frase', 'portada', 'mascota', 'fechaRegistro']),
         C('Meta', ['isOcultaPara'], ['codigoMeta', 'nombre', 'estado']), C('UsuarioInsignia', [])],
    ],
    relaciones=[
        ('UsuarioController', 'PerfilService', 'asoc', 'perfilService', None),
        ('PerfilService', 'PerfilResponse', 'usa', 'devuelve', None),
        ('PerfilResponse', 'PersonalizacionPerfil', 'asoc', 'personalizacion', '1'),
        ('PerfilService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('PerfilService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('PerfilService', 'UsuarioInsigniaRepository', 'asoc', 'usuarioInsigniaRepository', None),
        ('PerfilService', 'InsigniaRepository', 'asoc', 'insigniaRepository', None),
        ('PerfilService', 'Auditar', 'usa', 'verMiPerfil anotado con', None),
        *REL_AUDITORIA,
        ('UsuarioRepository', 'Usuario', 'usa', 'gestiona', None),
        ('MetaRepository', 'Meta', 'usa', 'gestiona', None),
        ('UsuarioInsigniaRepository', 'UsuarioInsignia', 'usa', 'gestiona', None),
    ],
    participantes=[U, ('ui', 'PerfilPage', 'boundary'), ('svc', 'perfilService.js', 'module'),
                   ('ctl', 'UsuarioController', 'RestController'), ASP, ('srv', 'PerfilService', 'Service'),
                   ('ur', 'UsuarioRepository', 'Repository'), ('mr', 'MetaRepository', 'Repository'),
                   ('uir', 'UsuarioInsigniaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'menú de la cuenta → «Mi perfil»'),
        ('m', 'ui', 'svc', 'obtenerPerfil()'),
        ('m', 'svc', 'ctl', 'GET /api/usuarios/me/perfil'),
        NOTA_TOKEN,
        *auditado('verMiPerfil(codigoUsuario)', 'VER_PERFIL', 'USUARIO', [
            ('m', 'srv', 'ur', 'findById(codigoUsuario)'),
            ('r', 'ur', 'srv', 'usuario'),
            ('m', 'srv', 'mr', 'findByParticipante(codigoUsuario)'),
            ('r', 'mr', 'srv', 'metas'),
            ('m', 'srv', 'srv', 'resumen(metas): activas, cumplidas y % (mismo cálculo que HU7)'),
            ('m', 'srv', 'uir', 'insigniasDelUsuario(codigoUsuario)'),
            ('r', 'uir', 'srv', 'insignias ganadas'),
            ('m', 'srv', 'srv', 'ultimasCumplidas(metas) y personalizacion(usuario) (HU19, HU20)'),
        ], 'PerfilResponse'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('alt', [
            ('[respuesta correcta]', [('r', 'ui', 'u', 'cabecera, resumen, insignias y últimas metas cumplidas')]),
            ('[sin conexión]', [('r', 'ui', 'u', 'EstadoError con «Intentar de nuevo»')]),
        ]),
    ],
)

HU['HU12'] = dict(
    nombre='Ver galería de insignias',
    filas=[
        [C('UsuarioController', ['misInsignias'])],
        [C('InsigniaService', ['verGaleria']), C('GaleriaInsigniasResponse')],
        [C('MetaRepository', ['plazosYTiposCumplidos']), C('InsigniaRepository', ['findAllByOrderByCodigoInsigniaAsc']),
         C('UsuarioInsigniaRepository', ['insigniasDelUsuario']), *AUDITORIA()],
        [C('Insignia'), C('UsuarioInsignia'), C('CriterioInsignia')],
    ],
    relaciones=[
        ('UsuarioController', 'InsigniaService', 'asoc', 'insigniaService', None),
        ('InsigniaService', 'GaleriaInsigniasResponse', 'usa', 'devuelve', None),
        ('InsigniaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('InsigniaService', 'InsigniaRepository', 'asoc', 'insigniaRepository', None),
        ('InsigniaService', 'UsuarioInsigniaRepository', 'asoc', 'usuarioInsigniaRepository', None),
        ('InsigniaService', 'Auditar', 'usa', 'verGaleria anotado con', None),
        *REL_AUDITORIA,
        ('UsuarioInsignia', 'Insignia', 'asoc', 'insignia', '1'),
        ('Insignia', 'CriterioInsignia', 'asoc', 'criterio', '1'),
    ],
    participantes=[U, ('ui', 'InsigniasPage', 'boundary'), ('svc', 'insigniaService.js', 'module'),
                   ('ctl', 'UsuarioController', 'RestController'), ASP, ('srv', 'InsigniaService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('uir', 'UsuarioInsigniaRepository', 'Repository'),
                   ('ir', 'InsigniaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'Mi perfil → «Ver galería»'),
        ('m', 'ui', 'svc', 'obtenerGaleria()'),
        ('m', 'svc', 'ctl', 'GET /api/usuarios/me/insignias'),
        NOTA_TOKEN,
        *auditado('verGaleria(codigoUsuario)', 'VER_INSIGNIAS', 'USUARIO', [
            ('m', 'srv', 'mr', 'plazosYTiposCumplidos(codigoUsuario)'),
            ('r', 'mr', 'srv', 'conteo de metas cumplidas'),
            ('m', 'srv', 'uir', 'insigniasDelUsuario(codigoUsuario)'),
            ('r', 'uir', 'srv', 'ganadas con su fecha'),
            ('m', 'srv', 'ir', 'findAllByOrderByCodigoInsigniaAsc()'),
            ('r', 'ir', 'srv', 'catálogo'),
            ('m', 'srv', 'srv', 'por insignia: ganada (fecha) o pendiente (avance actual / requerido)'),
        ], 'GaleriaInsigniasResponse'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('alt', [
            ('[ya ganó alguna]', [('r', 'ui', 'u', 'ganadas a color con fecha; pendientes en gris con barra de avance')]),
            ('[aún ninguna]', [('r', 'ui', 'u', 'todas pendientes con lo que falta para cada una')]),
        ]),
    ],
)

HU['HU23'] = dict(
    nombre='Editar perfil y foto',
    filas=[
        [C('CuentaController', ['verCuenta', 'editarDatos', 'cambiarFoto', 'quitarFoto'])],
        [C('EditarCuentaRequest'), C('CuentaService', ['verCuenta', 'editarDatos', 'cambiarFoto', 'quitarFoto']),
         C('FotoPerfilRequest'), C('CuentaResponse', ['desde'])],
        [C('UsuarioRepository', []), JPA('findById', 'save'), C('ValidacionException', ['getErrores']), *AUDITORIA()],
        [C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido', 'email', 'fotoPerfil'])],
    ],
    relaciones=[
        ('CuentaController', 'CuentaService', 'asoc', 'cuentaService', None),
        ('CuentaController', 'EditarCuentaRequest', 'usa', 'recibe', None),
        ('CuentaController', 'FotoPerfilRequest', 'usa', 'recibe', None),
        ('CuentaService', 'CuentaResponse', 'usa', 'devuelve', None),
        ('CuentaService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('UsuarioRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('CuentaService', 'ValidacionException', 'usa', 'lanza', None),
        ('CuentaService', 'Auditar', 'usa', 'EDITAR_PERFIL / CAMBIAR_FOTO_PERFIL / QUITAR_FOTO_PERFIL', None),
        *REL_AUDITORIA,
        ('UsuarioRepository', 'Usuario', 'usa', 'gestiona', None),
    ],
    participantes=[U, ('ui', 'EditarPerfilPage', 'boundary'), ('foto', 'FotoPerfilModal', 'boundary'),
                   ('svc', 'cuentaService.js', 'module'), ('ctl', 'CuentaController', 'RestController'), ASP,
                   ('srv', 'CuentaService', 'Service'), ('ur', 'UsuarioRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'elige una imagen (JPG, PNG o WebP, máximo 5 MB)'),
        ('m', 'ui', 'foto', 'abrir(archivo)'),
        ('m', 'u', 'foto', 'mueve y acerca la foto (puntero o flechas) y pulsa «Usar foto»'),
        ('r', 'foto', 'ui', 'data URL recortada a 256 × 256'),
        ('m', 'u', 'ui', 'cambia nombre o apellido y pulsa «Guardar cambios»'),
        ('m', 'ui', 'svc', 'editarDatos({ nombre, apellido })'),
        ('m', 'svc', 'ctl', 'PUT /api/usuarios/me'),
        *auditado('editarDatos(codigoUsuario, pedido)', 'EDITAR_PERFIL', 'USUARIO', [
            ('m', 'srv', 'ur', 'findById(codigoUsuario)'),
            ('m', 'srv', 'srv', 'validar nombre y apellido; usuario.setNombre / setApellido'),
            ('m', 'srv', 'ur', 'save(usuario)'),
        ], 'CuentaResponse'),
        ('m', 'ui', 'svc', 'cambiarFoto(dataUrl)'),
        ('m', 'svc', 'ctl', 'PUT /api/usuarios/me/foto'),
        ('m', 'ctl', 'asp', 'cambiarFoto(codigoUsuario, pedido)'),
        ('m', 'asp', 'srv', 'proceed()'),
        ('m', 'srv', 'srv', 'validarFoto(foto)'),
        ('alt', [
            ('[formato y tamaño válidos]', [
                ('m', 'srv', 'ur', 'save(usuario con fotoPerfil)'),
                ('m', 'asp', 'log', 'registrar(…, CAMBIAR_FOTO_PERFIL, USUARIO, …, EXITO)'),
                ('r', 'ctl', 'svc', '200 CuentaResponse'),
                ('r', 'ui', 'u', '«¡Perfil actualizado!»; la foto aparece en el encabezado'),
            ]),
            ('[no es imagen o pesa más de 5 MB]', [
                ('r', 'srv', 'asp', 'ValidacionException'),
                ('m', 'asp', 'log', 'registrar(…, CAMBIAR_FOTO_PERFIL, …, ERROR, motivo)'),
                ('r', 'ctl', 'svc', '400 ErrorResponse'),
                ('r', 'ui', 'u', 'error junto a la foto; no se guarda'),
            ]),
        ]),
    ],
)

HU['HU24'] = dict(
    nombre='Consultar el perfil de otros',
    filas=[
        [C('UsuarioController', ['buscarUsuarios', 'perfilPublico'])],
        [C('PerfilService', ['buscarUsuarios', 'verPerfilPublico']), C('UsuarioResumenResponse'),
         C('PerfilPublicoResponse')],
        [C('UsuarioRepository', ['buscarPorNombre']), C('MetaRepository', ['findByParticipante']), C('PersonalizacionPerfil', []), *AUDITORIA()],
        [C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido', 'fotoPerfil', 'perfilPrivado', 'fechaRegistro']),
         C('Meta', ['isOcultaPara'], ['codigoMeta', 'nombre', 'estado']), C('UsuarioNoEncontradoException', [])],
    ],
    relaciones=[
        ('UsuarioController', 'PerfilService', 'asoc', 'perfilService', None),
        ('PerfilService', 'UsuarioResumenResponse', 'usa', 'devuelve', None),
        ('PerfilService', 'PerfilPublicoResponse', 'usa', 'devuelve', None),
        ('PerfilPublicoResponse', 'PersonalizacionPerfil', 'asoc', 'personalizacion', '1'),
        ('PerfilService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('PerfilService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('PerfilService', 'UsuarioNoEncontradoException', 'usa', 'lanza', None),
        ('PerfilService', 'Auditar', 'usa', 'BUSCAR_USUARIOS / VER_PERFIL_PUBLICO', None),
        *REL_AUDITORIA,
        ('UsuarioRepository', 'Usuario', 'usa', 'gestiona', None),
    ],
    participantes=[U, ('ui', 'BuscarPersonasPage / PerfilPublicoPage', 'boundary'), ('svc', 'usuarioService.js / perfilService.js', 'module'),
                   ('ctl', 'UsuarioController', 'RestController'), ASP, ('srv', 'PerfilService', 'Service'),
                   ('ur', 'UsuarioRepository', 'Repository'), ('mr', 'MetaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'escribe «lau» en Personas'),
        ('m', 'ui', 'svc', 'buscarUsuarios("lau")'),
        ('m', 'svc', 'ctl', 'GET /api/usuarios?nombre=lau'),
        *auditado('buscarUsuarios("lau", codigoUsuario)', 'BUSCAR_USUARIOS', 'USUARIO', [
            ('m', 'srv', 'srv', 'patronDeBusqueda("lau")  [mínimo 2 letras]'),
            ('m', 'srv', 'ur', 'buscarPorNombre(patron, codigoUsuario)'),
            ('r', 'ur', 'srv', 'coincidencias (sin tildes ni mayúsculas)'),
        ], 'List<UsuarioResumenResponse>'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'ui', 'u', 'lista con foto o iniciales y nombre'),
        ('m', 'u', 'ui', 'abre el perfil de una persona'),
        ('m', 'ui', 'svc', 'obtenerPerfilPublico(codigoUsuario)'),
        ('m', 'svc', 'ctl', 'GET /api/usuarios/{codigo}/perfil'),
        *auditado('verPerfilPublico(codigoPerfil, codigoUsuario)', 'VER_PERFIL_PUBLICO', 'USUARIO', [
            ('m', 'srv', 'ur', 'findById(codigoPerfil)'),
            ('alt', [
                ('[existe y su perfil es público]', [
                    ('m', 'srv', 'mr', 'findByParticipante(codigoPerfil)'),
                    ('m', 'srv', 'srv', 'quita las metas ocultas (HU25) y arma resumen e insignias, sin correo'),
                ]),
                ('[no existe]', [
                    ('r', 'srv', 'asp', 'UsuarioNoEncontradoException'),
                    ('r', 'ctl', 'svc', '404 → «Esta persona no existe» y enlace para volver a buscar'),
                ]),
            ]),
        ], 'PerfilPublicoResponse'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'ui', 'u', 'perfil de la persona (sin correo ni botones propios)'),
    ],
)

HU['HU25'] = dict(
    nombre='Ocultar metas del perfil',
    filas=[
        [C('MetaController', ['cambiarVisibilidad'])],
        [C('VisibilidadRequest'), C('MetaService', ['cambiarVisibilidad']), C('MetaResponse', ['desde'])],
        [C('MetaRepository', []), JPA('findById', 'save'), *AUDITORIA()],
        [C('Meta', ['participaAceptado', 'cambiarVisibilidad', 'isOcultaPara']),
         C('Participacion', ['cambiarVisibilidad'], ['usuario', 'estadoInvitacion', 'oculta'])],
    ],
    relaciones=[
        ('MetaController', 'MetaService', 'asoc', 'metaService', None),
        ('MetaController', 'VisibilidadRequest', 'usa', 'recibe', None),
        ('MetaService', 'MetaResponse', 'usa', 'devuelve', None),
        ('MetaService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('MetaRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('MetaService', 'Auditar', 'usa', 'cambiarVisibilidad anotado con', None),
        *REL_AUDITORIA,
        ('Meta', 'Participacion', 'comp', 'participaciones (cada una con su «oculta»)', '1..*'),
    ],
    participantes=[U, ('ui', 'MisMetasPage', 'boundary'), ('svc', 'metaService.js', 'module'),
                   ('ctl', 'MetaController', 'RestController'), ASP, ('srv', 'MetaService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('meta', 'meta : Meta', 'Entity'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'menú ⋯ → «Ocultar del perfil»'),
        ('m', 'ui', 'svc', 'cambiarVisibilidad(codigoMeta, true)'),
        ('m', 'svc', 'ctl', 'PATCH /api/metas/{codigoMeta}/visibilidad { oculta: true }'),
        NOTA_TOKEN,
        *auditado('cambiarVisibilidad(codigoMeta, solicitud, codigoUsuario)', 'CAMBIAR_VISIBILIDAD', 'META', [
            ('m', 'srv', 'mr', 'findById(codigoMeta)'),
            ('r', 'mr', 'srv', 'meta'),
            ('m', 'srv', 'meta', 'participaAceptado(codigoUsuario)'),
            ('alt', [
                ('[participa en la meta]', [
                    ('m', 'srv', 'meta', 'cambiarVisibilidad(codigoUsuario, true)  [solo en su participación]'),
                    ('m', 'srv', 'mr', 'save(meta)'),
                ]),
                ('[no participa]', [
                    ('r', 'srv', 'asp', 'PermisoDenegadoException'),
                    ('m', 'asp', 'log', 'registrar(…, CAMBIAR_VISIBILIDAD, …, ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '403 ErrorResponse'),
                ]),
            ]),
        ], 'MetaResponse (oculta = true para este usuario)'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'ui', 'u', 'etiqueta «Oculta» y aviso «Meta oculta: ya no se mostrará en tu perfil público»'),
    ],
)

HU['HU26'] = dict(
    nombre='Perfil privado',
    filas=[
        [C('UsuarioController', ['cambiarPrivacidad', 'perfilPublico'])],
        [C('PrivacidadRequest'), C('PerfilService', ['cambiarPrivacidad', 'verPerfilPublico']), C('PrivacidadResponse')],
        [C('UsuarioRepository', []), C('PerfilPublicoResponse'), C('PersonalizacionPerfil', ['soloCabecera']), *AUDITORIA()],
        [C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido', 'fotoPerfil', 'perfilPrivado', 'frase', 'portada', 'mascota', 'fechaRegistro'])],
    ],
    relaciones=[
        ('UsuarioController', 'PerfilService', 'asoc', 'perfilService', None),
        ('UsuarioController', 'PrivacidadRequest', 'usa', 'recibe', None),
        ('PerfilService', 'PrivacidadResponse', 'usa', 'devuelve', None),
        ('PerfilService', 'PerfilPublicoResponse', 'usa', 'devuelve', None),
        ('PerfilPublicoResponse', 'PersonalizacionPerfil', 'asoc', 'personalizacion', '1'),
        ('PerfilService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('PerfilService', 'Auditar', 'usa', 'CAMBIAR_PRIVACIDAD / VER_PERFIL_PUBLICO', None),
        *REL_AUDITORIA,
        ('UsuarioRepository', 'Usuario', 'usa', 'gestiona', None),
    ],
    participantes=[U, ('ui', 'TarjetaPrivacidad', 'boundary'), ('svc', 'perfilService.js', 'module'),
                   ('ctl', 'UsuarioController', 'RestController'), ASP, ('srv', 'PerfilService', 'Service'),
                   ('ur', 'UsuarioRepository', 'Repository'), LOG, ('u2', 'Otra persona', 'actor')],
    pasos=[
        ('m', 'u', 'ui', 'activa el interruptor «Perfil privado»'),
        ('m', 'ui', 'svc', 'cambiarPrivacidad(true)'),
        ('m', 'svc', 'ctl', 'PATCH /api/usuarios/me/privacidad { privado: true }'),
        NOTA_TOKEN,
        *auditado('cambiarPrivacidad(solicitud, codigoUsuario)', 'CAMBIAR_PRIVACIDAD', 'USUARIO', [
            ('m', 'srv', 'ur', 'findById(codigoUsuario)'),
            ('m', 'srv', 'srv', 'usuario.setPerfilPrivado(true)'),
            ('m', 'srv', 'ur', 'save(usuario)'),
        ], 'PrivacidadResponse(privado = true)'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('alt', [
            ('[guardado]', [('r', 'ui', 'u', '«Tu perfil ahora es privado» (frase, portada y mascota se siguen viendo)')]),
            ('[sin conexión]', [('r', 'ui', 'u', 'el interruptor vuelve a su estado y aparece el error')]),
        ]),
        ('m', 'u2', 'ctl', 'GET /api/usuarios/{codigo}/perfil'),
        ('m', 'ctl', 'srv', 'verPerfilPublico(codigoPerfil, codigoVisitante)'),
        ('m', 'srv', 'srv', 'dueño privado → PersonalizacionPerfil.soloCabecera(frase, portada, mascota)'),
        ('r', 'srv', 'u2', 'cabecera + «Este perfil es privado» (sin resumen, insignias ni metas)'),
    ],
)

# ================================ Metas compartidas (HU13, HU14) y HU21 =======================

HU['HU13'] = dict(
    nombre='Invitar a una meta',
    filas=[
        [C('InvitacionController', ['invitar', 'generarCodigo', 'verCodigo', 'desactivarCodigo'])],
        [C('InvitarRequest'), C('InvitacionService', ['invitar']), C('CodigoUnionService', ['generar', 'verCodigo', 'desactivar']),
         C('InvitacionEnviadaResponse'), C('CodigoUnionResponse')],
        [C('MetaRepository', ['findByCodigoUnion']), C('UsuarioRepository', []), C('CorreoService', ['enviar']),
         C('PlantillaInvitacion', []), *AUDITORIA()],
        [C('Meta', ['invitar', 'participacionDeUsuario', 'activarCodigoUnion', 'desactivarCodigoUnion', 'codigoUnionVigente'],
           ['codigoMeta', 'nombre', 'creador', 'codigoUnion', 'codigoUnionExpira']),
         C('Participacion', ['invitar'], ['usuario', 'estadoInvitacion', 'codigoInvitador', 'tokenHash', 'tokenExpira']), C('EstadoInvitacion')],
    ],
    relaciones=[
        ('InvitacionController', 'InvitacionService', 'asoc', 'invitacionService', None),
        ('InvitacionController', 'CodigoUnionService', 'asoc', 'codigoUnionService', None),
        ('InvitacionController', 'InvitarRequest', 'usa', 'recibe', None),
        ('InvitacionService', 'InvitacionEnviadaResponse', 'usa', 'devuelve', None),
        ('CodigoUnionService', 'CodigoUnionResponse', 'usa', 'devuelve', None),
        ('InvitacionService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('InvitacionService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('InvitacionService', 'CorreoService', 'asoc', 'correoService', None),
        ('InvitacionService', 'PlantillaInvitacion', 'usa', 'arma el correo', None),
        ('CodigoUnionService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('InvitacionService', 'Auditar', 'usa', 'INVITAR_META', None),
        ('CodigoUnionService', 'Auditar', 'usa', 'GENERAR_CODIGO_META / DESACTIVAR_CODIGO_META', None),
        *REL_AUDITORIA,
        ('Meta', 'Participacion', 'comp', 'participaciones', '1..*'),
        ('Participacion', 'EstadoInvitacion', 'asoc', 'estadoInvitacion', '1'),
    ],
    participantes=[('u', 'Creador', 'actor'), ('ui', 'InvitarPersonasModal', 'boundary'), ('svc', 'metaService.js', 'module'),
                   ('ctl', 'InvitacionController', 'RestController'), ASP, ('srv', 'InvitacionService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('ur', 'UsuarioRepository', 'Repository'),
                   ('meta', 'meta : Meta', 'Entity'), ('correo', 'CorreoService', 'interface'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'menú ⋯ → «Invitar personas», busca un nombre y pulsa «Invitar»'),
        ('m', 'ui', 'svc', 'invitarAMeta(codigoMeta, codigoInvitado)'),
        ('m', 'svc', 'ctl', 'POST /api/metas/{codigoMeta}/invitaciones'),
        NOTA_TOKEN,
        *auditado('invitar(codigoMeta, pedido, codigoUsuario)', 'INVITAR_META', 'META', [
            ('m', 'srv', 'mr', 'findById(codigoMeta)'),
            ('r', 'mr', 'srv', 'meta (creador, ACTIVA)'),
            ('m', 'srv', 'ur', 'findById(codigoInvitado)'),
            ('m', 'srv', 'meta', 'participacionDeUsuario(codigoInvitado)'),
            ('alt', [
                ('[no participa ni tiene invitación pendiente]', [
                    ('m', 'srv', 'meta', 'invitar(invitado, codigoCreador, hash(token), ahora, ahora + 7 días)'),
                    ('m', 'srv', 'mr', 'save(meta)'),
                    ('m', 'srv', 'correo', 'enviar(PlantillaInvitacion con «Aceptar» y «Rechazar»)'),
                ]),
                ('[ya participa o ya está invitado]', [
                    ('r', 'srv', 'asp', 'ValidacionException("Esa persona ya participa en la meta…")'),
                    ('m', 'asp', 'log', 'registrar(…, INVITAR_META, …, ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 ErrorResponse (mensaje dentro del diálogo)'),
                ]),
            ]),
        ], 'InvitacionEnviadaResponse'),
        ('r', 'ctl', 'svc', '201 Created'),
        ('r', 'ui', 'u', 'el botón pasa a «Invitación enviada»'),
        ('nota', 'ui', 'Alternativa: el creador genera un código para unirse (CodigoUnionService.generar)'),
    ],
)

HU['HU14'] = dict(
    nombre='Responder invitación',
    filas=[
        [C('InvitacionController', ['pendientes', 'responder', 'consultarPorToken', 'aceptarPorToken', 'rechazarPorToken', 'unirse'])],
        [C('ResponderInvitacionRequest'), C('InvitacionService', ['pendientes', 'responder', 'consultarPorToken', 'aceptarPorToken', 'rechazarPorToken']),
         C('CodigoUnionService', ['unirse']), C('InvitacionRespondidaResponse')],
        [C('ParticipacionRepository', ['pendientesDe', 'findByTokenHash']), C('MetaRepository', ['findByCodigoUnion']),
         C('InvitacionResponse'), C('InvitacionCorreoResponse'), *AUDITORIA()],
        [C('Participacion', ['responder'], ['id', 'usuario', 'meta', 'estadoInvitacion', 'tokenHash', 'tokenExpira']),
         C('Meta', ['unirse', 'codigoUnionVigente'], ['codigoMeta', 'nombre', 'estado']), C('EstadoInvitacion'),
         C('InvitacionNoValidaException', [])],
    ],
    relaciones=[
        ('InvitacionController', 'InvitacionService', 'asoc', 'invitacionService', None),
        ('InvitacionController', 'CodigoUnionService', 'asoc', 'codigoUnionService', None),
        ('InvitacionController', 'ResponderInvitacionRequest', 'usa', 'recibe', None),
        ('InvitacionService', 'InvitacionRespondidaResponse', 'usa', 'devuelve', None),
        ('InvitacionService', 'InvitacionResponse', 'usa', 'pendientes', None),
        ('InvitacionService', 'InvitacionCorreoResponse', 'usa', 'enlace del correo', None),
        ('InvitacionService', 'ParticipacionRepository', 'asoc', 'participacionRepository', None),
        ('CodigoUnionService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('InvitacionService', 'InvitacionNoValidaException', 'usa', 'lanza (vencida o usada)', None),
        ('InvitacionService', 'Auditar', 'usa', 'VER_INVITACIONES / RESPONDER_INVITACION', None),
        ('CodigoUnionService', 'Auditar', 'usa', 'UNIRSE_CON_CODIGO', None),
        *REL_AUDITORIA,
        ('Participacion', 'Meta', 'asoc', 'meta', '1'),
        ('Participacion', 'EstadoInvitacion', 'asoc', 'estadoInvitacion', '1'),
    ],
    participantes=[('u', 'Invitado', 'actor'), ('ui', 'InvitacionesPage', 'boundary'), ('svc', 'invitacionService.js', 'module'),
                   ('ctl', 'InvitacionController', 'RestController'), ASP, ('srv', 'InvitacionService', 'Service'),
                   ('pr', 'ParticipacionRepository', 'Repository'), ('p', 'participación : Participacion', 'Entity'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'abre «Invitaciones» (aviso en Mis metas o menú de la cuenta)'),
        ('m', 'ui', 'svc', 'listarInvitaciones()'),
        ('m', 'svc', 'ctl', 'GET /api/invitaciones'),
        ('m', 'ctl', 'srv', 'pendientes(codigoUsuario)'),
        ('m', 'srv', 'pr', 'pendientesDe(codigoUsuario)'),
        ('r', 'srv', 'ctl', 'List<InvitacionResponse>'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('m', 'u', 'ui', 'pulsa «Aceptar»'),
        ('m', 'ui', 'svc', 'responderInvitacion(codigoMeta, true)'),
        ('m', 'svc', 'ctl', 'PATCH /api/invitaciones/{codigoMeta} { aceptar: true }'),
        *auditado('responder(codigoMeta, pedido, codigoUsuario)', 'RESPONDER_INVITACION', 'META', [
            ('m', 'srv', 'pr', 'findById(usuario, meta)'),
            ('r', 'pr', 'srv', 'participación PENDIENTE'),
            ('alt', [
                ('[la meta sigue ACTIVA]', [
                    ('m', 'srv', 'p', 'responder(true)  [ACEPTADA, borra el token]'),
                    ('m', 'srv', 'pr', 'save(participación)'),
                ]),
                ('[la meta ya se cumplió]', [
                    ('r', 'srv', 'asp', 'ValidacionException("La meta ya se cumplió…")'),
                    ('m', 'asp', 'log', 'registrar(…, RESPONDER_INVITACION, …, ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 ErrorResponse'),
                ]),
            ]),
        ], 'InvitacionRespondidaResponse'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'ui', 'u', '«Te uniste a la meta»: ya aparece en Mis metas'),
        ('nota', 'ui', 'Desde el correo: ResponderInvitacionPage → /api/invitaciones/correo/aceptar o /rechazar con el token'),
    ],
)

HU['HU21'] = dict(
    nombre='Compartir logro',
    filas=[
        [C('LogroController', ['compartirLogro', 'verLogro'])],
        [C('LogroCompartidoService', ['compartirLogro', 'obtenerLogroPublico']),
         C('LogroCompartidoResponse'), C('LogroPublicoResponse')],
        [C('LogroCompartidoRepository', ['findByMetaAndUsuario', 'findByToken']), C('MetaRepository', ['esParticipanteAceptado']),
         C('UsuarioRepository', []), *AUDITORIA()],
        [C('LogroCompartido'), C('Meta', ['getFechaCumplida'], ['codigoMeta', 'nombre', 'tipo', 'plazo', 'estado']),
         C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido'])],
    ],
    relaciones=[
        ('LogroController', 'LogroCompartidoService', 'asoc', 'logroCompartidoService', None),
        ('LogroCompartidoService', 'LogroCompartidoResponse', 'usa', 'devuelve', None),
        ('LogroCompartidoService', 'LogroPublicoResponse', 'usa', 'devuelve', None),
        ('LogroCompartidoService', 'LogroCompartidoRepository', 'asoc', 'logroRepository', None),
        ('LogroCompartidoService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('LogroCompartidoService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('LogroCompartidoService', 'Auditar', 'usa', 'COMPARTIR_LOGRO', None),
        *REL_AUDITORIA,
        ('LogroCompartido', 'Meta', 'asoc', 'meta', '1'),
        ('LogroCompartido', 'Usuario', 'asoc', 'usuario', '1'),
    ],
    participantes=[U, ('ui', 'CompartirLogroModal', 'boundary'), ('svc', 'logroService.js', 'module'),
                   ('ctl', 'LogroController', 'RestController'), ASP, ('srv', 'LogroCompartidoService', 'Service'),
                   ('mr', 'MetaRepository', 'Repository'), ('lr', 'LogroCompartidoRepository', 'Repository'), LOG,
                   ('nav', 'navigator', 'API del navegador')],
    pasos=[
        ('m', 'u', 'ui', 'Mis metas → Cumplidas → «Compartir logro»'),
        ('m', 'ui', 'svc', 'crearEnlace(codigoMeta)'),
        ('m', 'svc', 'ctl', 'POST /api/metas/{codigoMeta}/compartir'),
        NOTA_TOKEN,
        *auditado('compartirLogro(codigoMeta, codigoUsuario)', 'COMPARTIR_LOGRO', 'META', [
            ('m', 'srv', 'mr', 'findById(codigoMeta) y esParticipanteAceptado(codigoMeta, codigoUsuario)'),
            ('alt', [
                ('[la meta está CUMPLIDA]', [
                    ('m', 'srv', 'lr', 'findByMetaAndUsuario(meta, usuario)'),
                    ('r', 'lr', 'srv', 'Optional.empty() (primera vez)'),
                    ('m', 'srv', 'srv', 'generarToken()'),
                    ('m', 'srv', 'lr', 'save(new LogroCompartido(meta, usuario, token))'),
                ]),
                ('[la meta sigue activa]', [
                    ('r', 'srv', 'asp', 'ValidacionException("Solo se comparten metas cumplidas")'),
                    ('m', 'asp', 'log', 'registrar(…, COMPARTIR_LOGRO, …, ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 ErrorResponse'),
                ]),
            ]),
        ], 'LogroCompartidoResponse(token, url, nombre, fechaCumplida)'),
        ('r', 'ctl', 'svc', '201 Created'),
        ('m', 'ui', 'ui', 'generarTarjetaLogro() → imagen PNG con el logro'),
        ('m', 'ui', 'nav', 'share({ files: [imagen], url })'),
        ('r', 'nav', 'u', 'menú del sistema para compartir (WhatsApp, redes, copiar enlace)'),
        ('nota', 'ui', 'Quien abre el enlace ve /logros/{token}: GET /api/logros/{token} (público)'),
    ],
)

# ================================ HU19, HU20, HU30 ============================================

HU['HU19'] = dict(
    nombre='Personalizar mi perfil público',
    filas=[
        [C('UsuarioController', ['editarPerfilPublico'])],
        [C('PerfilPublicoRequest'), C('PerfilService', ['editarPerfilPublico']),
         C('PerfilResponse'), C('PersonalizacionPerfil', ['soloCabecera'])],
        [C('UsuarioRepository', []), C('MetaRepository', ['findByParticipante']), C('UsuarioInsigniaRepository', ['codigosObtenidos']),
         C('InsigniaObtenida', ['desde']), *AUDITORIA()],
        [C('Usuario', [], ['codigoUsuario', 'frase', 'portada', 'codigoInsigniaDestacada']),
         C('Meta', ['cambiarDestacada', 'isDestacadaPara', 'isOcultaPara'], ['codigoMeta', 'nombre']), C('Portada')],
    ],
    relaciones=[
        ('UsuarioController', 'PerfilService', 'asoc', 'perfilService', None),
        ('UsuarioController', 'PerfilPublicoRequest', 'usa', 'recibe', None),
        ('PerfilService', 'PerfilResponse', 'usa', 'devuelve', None),
        ('PerfilResponse', 'PersonalizacionPerfil', 'asoc', 'personalizacion', '1'),
        ('PersonalizacionPerfil', 'InsigniaObtenida', 'asoc', 'insigniaDestacada', '0..1'),
        ('PerfilService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('PerfilService', 'MetaRepository', 'asoc', 'metaRepository', None),
        ('PerfilService', 'UsuarioInsigniaRepository', 'asoc', 'usuarioInsigniaRepository', None),
        ('PerfilService', 'Auditar', 'usa', 'EDITAR_PERFIL_PUBLICO', None),
        *REL_AUDITORIA,
        ('Usuario', 'Portada', 'asoc', 'portada', '1'),
    ],
    participantes=[U, ('ui', 'EditarPerfilPage · SeccionPerfilPublico', 'boundary'), ('svc', 'perfilService.js', 'module'),
                   ('ctl', 'UsuarioController', 'RestController'), ASP, ('srv', 'PerfilService', 'Service'),
                   ('ur', 'UsuarioRepository', 'Repository'), ('uir', 'UsuarioInsigniaRepository', 'Repository'),
                   ('mr', 'MetaRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'escribe la frase, elige el lienzo de portada, la insignia y hasta 3 metas'),
        ('m', 'ui', 'ui', 'vista previa «Así te ven los demás»'),
        ('m', 'u', 'ui', 'pulsa «Guardar cambios»'),
        ('m', 'ui', 'svc', 'editarPerfilPublico({ frase, portada, codigoInsigniaDestacada, metasDestacadas })'),
        ('m', 'svc', 'ctl', 'PUT /api/usuarios/me/perfil-publico'),
        NOTA_TOKEN,
        *auditado('editarPerfilPublico(pedido, codigoUsuario)', 'EDITAR_PERFIL_PUBLICO', 'USUARIO', [
            ('m', 'srv', 'ur', 'findById(codigoUsuario)'),
            ('m', 'srv', 'srv', 'limpiarFrase(frase)  [máximo 100 caracteres]'),
            ('alt', [
                ('[frase, portada, insignia y metas válidas]', [
                    ('m', 'srv', 'uir', 'codigosObtenidos(codigoUsuario)  [la insignia debe ser suya]'),
                    ('m', 'srv', 'mr', 'findByParticipante(codigoUsuario)  [metas visibles, máximo 3]'),
                    ('m', 'srv', 'srv', 'meta.cambiarDestacada(...) por cada meta; usuario.setFrase/Portada/Insignia'),
                    ('m', 'srv', 'ur', 'save(usuario)'),
                ]),
                ('[frase de más de 100 caracteres]', [
                    ('r', 'srv', 'asp', 'ValidacionException'),
                    ('m', 'asp', 'log', 'registrar(…, EDITAR_PERFIL_PUBLICO, …, ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 → error junto a la frase'),
                ]),
            ]),
        ], 'PerfilResponse'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'ui', 'u', '«¡Perfil actualizado!»; Mi perfil y el perfil público muestran la portada, la frase y los destacados'),
    ],
)

HU['HU20'] = dict(
    nombre='Elegir mi mascota',
    filas=[
        [C('CuentaController', ['cambiarMascota'])],
        [C('MascotaRequest'), C('CuentaService', ['cambiarMascota', 'verCuenta']), C('CuentaResponse', ['desde'])],
        [C('UsuarioRepository', []), JPA('findById', 'save'), C('PersonalizacionPerfil', []), *AUDITORIA()],
        [C('Usuario', [], ['codigoUsuario', 'nombre', 'apellido', 'mascota']), C('Mascota'), C('ValidacionException', ['getErrores'])],
    ],
    relaciones=[
        ('CuentaController', 'CuentaService', 'asoc', 'cuentaService', None),
        ('CuentaController', 'MascotaRequest', 'usa', 'recibe', None),
        ('CuentaService', 'CuentaResponse', 'usa', 'devuelve', None),
        ('CuentaResponse', 'Mascota', 'asoc', 'mascota', '1'),
        ('PersonalizacionPerfil', 'Mascota', 'asoc', 'mascota (Mi perfil y perfil público)', '1'),
        ('CuentaService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('UsuarioRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('CuentaService', 'ValidacionException', 'usa', 'lanza', None),
        ('CuentaService', 'Auditar', 'usa', 'cambiarMascota anotado con', None),
        *REL_AUDITORIA,
        ('Usuario', 'Mascota', 'asoc', 'mascota', '1'),
    ],
    participantes=[U, ('ui', 'EditarPerfilPage · SeccionMascota', 'boundary'), ('svc', 'cuentaService.js', 'module'),
                   ('ctl', 'CuentaController', 'RestController'), ASP, ('srv', 'CuentaService', 'Service'),
                   ('ur', 'UsuarioRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'abre Editar perfil → «Tu mascota» y elige «Luciérnaga»'),
        ('m', 'u', 'ui', 'pulsa «Guardar cambios»'),
        ('m', 'ui', 'svc', 'cambiarMascota("LUCIERNAGA")'),
        ('m', 'svc', 'ctl', 'PUT /api/usuarios/me/mascota { mascota }'),
        NOTA_TOKEN,
        *auditado('cambiarMascota(codigoUsuario, pedido)', 'CAMBIAR_MASCOTA', 'USUARIO', [
            ('m', 'srv', 'ur', 'findById(codigoUsuario)'),
            ('r', 'ur', 'srv', 'usuario'),
            ('m', 'srv', 'srv', 'valorDe(Mascota, "LUCIERNAGA")'),
            ('alt', [
                ('[mascota del catálogo (todas gratis)]', [
                    ('m', 'srv', 'srv', 'usuario.setMascota(LUCIERNAGA)'),
                    ('m', 'srv', 'ur', 'save(usuario)'),
                ]),
                ('[valor que no existe]', [
                    ('r', 'srv', 'asp', 'ValidacionException("Elige una mascota válida")'),
                    ('m', 'asp', 'log', 'registrar(…, CAMBIAR_MASCOTA, …, ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 → error en la sección Tu mascota'),
                ]),
            ]),
        ], 'CuentaResponse(… mascota)'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('r', 'ui', 'u', '«¡Perfil actualizado!»; Mi perfil muestra «Te acompaña Luciérnaga»'),
    ],
)

HU['HU30'] = dict(
    nombre='Elegir la apariencia de la app',
    filas=[
        [C('CuentaController', ['cambiarApariencia', 'verCuenta'])],
        [C('AparienciaRequest'), C('CuentaService', ['cambiarApariencia', 'verCuenta']), C('CuentaResponse', ['desde'])],
        [C('UsuarioRepository', []), JPA('findById', 'save'), *AUDITORIA()],
        [C('Usuario', [], ['codigoUsuario', 'modoTema', 'colorAcento']), C('ModoTema'), C('ColorAcento')],
    ],
    relaciones=[
        ('CuentaController', 'CuentaService', 'asoc', 'cuentaService', None),
        ('CuentaController', 'AparienciaRequest', 'usa', 'recibe', None),
        ('CuentaService', 'CuentaResponse', 'usa', 'devuelve', None),
        ('CuentaService', 'UsuarioRepository', 'asoc', 'usuarioRepository', None),
        ('UsuarioRepository', 'JpaRepository<T, ID>', 'hereda', '', None),
        ('CuentaService', 'Auditar', 'usa', 'cambiarApariencia anotado con', None),
        *REL_AUDITORIA,
        ('Usuario', 'ModoTema', 'asoc', 'modoTema', '1'),
        ('Usuario', 'ColorAcento', 'asoc', 'colorAcento', '1'),
    ],
    participantes=[U, ('ui', 'EditarPerfilPage · SeccionApariencia', 'boundary'), ('tema', 'tema/apariencia.js', 'module'),
                   ('svc', 'cuentaService.js', 'module'), ('ctl', 'CuentaController', 'RestController'), ASP,
                   ('srv', 'CuentaService', 'Service'), ('ur', 'UsuarioRepository', 'Repository'), LOG],
    pasos=[
        ('m', 'u', 'ui', 'elige tema Noche (modo oscuro + acento azul)'),
        ('m', 'ui', 'tema', 'aplicarApariencia(nueva)  [vista previa inmediata]'),
        ('m', 'u', 'ui', 'pulsa «Guardar cambios»'),
        ('m', 'ui', 'svc', 'cambiarApariencia({ modo: OSCURO, acento: AZUL })'),
        ('m', 'svc', 'ctl', 'PUT /api/usuarios/me/apariencia'),
        NOTA_TOKEN,
        *auditado('cambiarApariencia(codigoUsuario, pedido)', 'CAMBIAR_APARIENCIA', 'USUARIO', [
            ('m', 'srv', 'ur', 'findById(codigoUsuario)'),
            ('alt', [
                ('[modo y acento válidos]', [
                    ('m', 'srv', 'srv', 'usuario.setModoTema(OSCURO); usuario.setColorAcento(AZUL)'),
                    ('m', 'srv', 'ur', 'save(usuario)'),
                ]),
                ('[valores no válidos]', [
                    ('r', 'srv', 'asp', 'ValidacionException'),
                    ('m', 'asp', 'log', 'registrar(…, CAMBIAR_APARIENCIA, …, ERROR, motivo)'),
                    ('r', 'ctl', 'svc', '400 → error en la sección Apariencia'),
                ]),
            ]),
        ], 'CuentaResponse(… apariencia)'),
        ('r', 'ctl', 'svc', '200 OK'),
        ('m', 'ui', 'tema', 'aplicarApariencia(guardada, true)  [se recuerda en este y otros dispositivos]'),
        ('r', 'ui', 'u', 'la app queda con el tema Noche'),
    ],
)

NOMBRES_ARCHIVO = {clave: f'{clave}_{datos["nombre"]}' for clave, datos in HU.items()}
