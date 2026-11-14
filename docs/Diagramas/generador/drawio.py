"""
Genera diagramas UML en formato draw.io (.drawio, XML sin comprimir) a partir de una especificación.

- Diagrama de clases: cajas UML de tres secciones (estereotipo y nombre, atributos, métodos) por filas,
  con relaciones (dependencia, asociación, composición, realización, herencia).
- Diagrama de secuencia: líneas de vida, mensajes síncronos, retornos punteados, mensajes a sí mismo,
  notas y fragmentos combinados (alt / opt).

Los archivos se abren y se editan en draw.io (app.diagrams.net) sin perder nada.
"""
import re
from html import escape
from xml.sax.saxutils import quoteattr

FUENTE = 11            # tamaño de letra
ANCHO_LETRA = 6.3      # ancho promedio de un carácter con esa letra (para calcular el ancho de las cajas)
ALTO_LINEA = 15


class Lienzo:
    """Acumula celdas mxCell y produce el XML del archivo."""

    def __init__(self, nombre_pagina):
        self.nombre_pagina = nombre_pagina
        self.celdas = []
        self.siguiente = 2

    def nuevo_id(self):
        self.siguiente += 1
        return f'c{self.siguiente}'

    def vertice(self, valor, estilo, x, y, ancho, alto, padre='1'):
        ident = self.nuevo_id()
        self.celdas.append(
            f'<mxCell id="{ident}" value={quoteattr(valor)} style={quoteattr(estilo)} vertex="1" parent="{padre}">'
            f'<mxGeometry x="{x:.0f}" y="{y:.0f}" width="{ancho:.0f}" height="{alto:.0f}" as="geometry"/></mxCell>')
        return ident

    def arista(self, valor, estilo, origen=None, destino=None, puntos=None, desde=None, hasta=None):
        ident = self.nuevo_id()
        atributos = f'id="{ident}" value={quoteattr(valor)} style={quoteattr(estilo)} edge="1" parent="1"'
        if origen:
            atributos += f' source="{origen}"'
        if destino:
            atributos += f' target="{destino}"'
        geometria = '<mxGeometry relative="1" as="geometry">'
        if desde:
            geometria += f'<mxPoint x="{desde[0]:.0f}" y="{desde[1]:.0f}" as="sourcePoint"/>'
        if hasta:
            geometria += f'<mxPoint x="{hasta[0]:.0f}" y="{hasta[1]:.0f}" as="targetPoint"/>'
        if puntos:
            geometria += '<Array as="points">' + ''.join(
                f'<mxPoint x="{px:.0f}" y="{py:.0f}"/>' for px, py in puntos) + '</Array>'
        geometria += '</mxGeometry>'
        self.celdas.append(f'<mxCell {atributos}>{geometria}</mxCell>')
        return ident

    def xml(self, ancho, alto):
        cuerpo = ''.join(self.celdas)
        return (
            '<mxfile host="app.diagrams.net" agent="Metamorfosis generador" version="24.7.0">'
            f'<diagram id="d1" name={quoteattr(self.nombre_pagina)}>'
            f'<mxGraphModel dx="{ancho:.0f}" dy="{alto:.0f}" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" '
            f'arrows="1" fold="1" page="1" pageScale="1" pageWidth="{max(1169, ancho + 80):.0f}" '
            f'pageHeight="{max(827, alto + 80):.0f}" math="0" shadow="0">'
            '<root><mxCell id="0"/><mxCell id="1" parent="0"/>'
            f'{cuerpo}</root></mxGraphModel></diagram></mxfile>\n')


def _ancho_texto(lineas, minimo=120):
    largo = max([len(linea) for linea in lineas] + [10])
    return max(minimo, largo * ANCHO_LETRA + 24)


# ----------------------------------------------------------------------------------------------
# Diagrama de clases
# ----------------------------------------------------------------------------------------------

COLORES_CLASE = {
    'RestController': ('#dae8fc', '#6c8ebf'),
    'Service': ('#d5e8d4', '#82b366'),
    'Repository': ('#fff2cc', '#d6b656'),
    'Entity': ('#f8cecc', '#b85450'),
    'Embeddable': ('#f8cecc', '#b85450'),
    'DTO': ('#e1d5e7', '#9673a6'),
    'Enumeration': ('#f5f5f5', '#666666'),
    'Aspect': ('#ffe6cc', '#d79b00'),
    'Annotation': ('#ffe6cc', '#d79b00'),
    'Component': ('#d5e8d4', '#82b366'),
    'Interface': ('#fff2cc', '#d6b656'),
    'Class': ('#ffffff', '#000000'),
}

ESTILOS_RELACION = {
    # dependencia: usa al otro en un método (línea punteada, flecha abierta)
    'usa': 'endArrow=open;endFill=0;dashed=1;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;',
    # asociación dirigida: lo tiene como atributo
    'asoc': 'endArrow=open;endFill=0;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;',
    # composición: el todo controla la vida de la parte (rombo lleno en el origen)
    'comp': 'endArrow=open;endFill=0;startArrow=diamondThin;startFill=1;startSize=14;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;',
    # agregación
    'agr': 'endArrow=open;endFill=0;startArrow=diamondThin;startFill=0;startSize=14;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;',
    # realización de una interfaz
    'impl': 'endArrow=block;endFill=0;dashed=1;endSize=12;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;',
    # herencia
    'hereda': 'endArrow=block;endFill=0;endSize=12;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;',
}


FUENTE_MIEMBRO = 10
ANCHO_MONO = 6.05      # ancho de un carácter de Courier New a 10 px
MAX_CARACTERES = 84    # las firmas más largas se parten en varias líneas


def partir(linea, maximo=MAX_CARACTERES):
    """Parte una firma larga después de una coma o un paréntesis; las líneas siguientes van con sangría."""
    if len(linea) <= maximo:
        return [linea]
    partes, actual = [], ''
    for trozo in re.split(r'(?<=[,(])\s*', linea):
        separador = ' ' if actual and not actual.endswith('(') else ''
        candidato = actual + separador + trozo
        if len(candidato) > maximo and actual.strip():
            partes.append(actual)
            actual = '      ' + trozo
        else:
            actual = candidato
    partes.append(actual)
    return partes


def _lineas(miembros):
    salida = []
    for m in miembros:
        salida += partir(m)
    return salida


def medir_clase(c):
    atributos, metodos = _lineas(c['atributos']), _lineas(c['metodos'])
    largo = max([len(x) for x in atributos + metodos] + [0])
    ancho = max(150, largo * ANCHO_MONO + 26, len(c['nombre']) * 7.4 + 30)
    alto_atr = max(1, len(atributos)) * 14 + 10
    alto_met = (len(metodos) * 14 + 10) if metodos else 0
    alto = 40 + alto_atr + (8 + alto_met if metodos else 0)
    return ancho, alto, atributos, metodos


def _html_miembros(lineas):
    return '<br>'.join(escape(x).replace('  ', '&nbsp;&nbsp;') for x in lineas)


def caja_clase(lienzo, c, x, y):
    """Dibuja una clase UML (estereotipo y nombre, atributos, métodos). Devuelve (id, ancho, alto)."""
    ancho, alto, atributos, metodos = medir_clase(c)
    estereotipo, nombre = c['estereotipo'], c['nombre']
    titulo = f'«{estereotipo}»<br><b>{escape(nombre)}</b>'
    alto_atr = max(1, len(atributos)) * 14 + 10
    relleno, borde = COLORES_CLASE.get(estereotipo, COLORES_CLASE['Class'])
    contenedor = lienzo.vertice(
        titulo,
        f'swimlane;fontStyle=0;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;'
        f'startSize=40;horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;'
        f'marginBottom=0;html=1;whiteSpace=wrap;fillColor={relleno};strokeColor={borde};fontSize={FUENTE};',
        x, y, ancho, alto)
    estilo_texto = (f'text;strokeColor=none;fillColor=none;align=left;verticalAlign=top;spacingLeft=6;spacingRight=4;'
                    f'spacingTop=2;overflow=hidden;rotatable=0;points=[[0,0.5],[1,0.5]];portConstraint=eastwest;html=1;'
                    f'whiteSpace=nowrap;fontSize={FUENTE_MIEMBRO};fontFamily=Courier New;')
    lienzo.vertice(_html_miembros(atributos) or ' ', estilo_texto, 0, 40, ancho, alto_atr, padre=contenedor)
    if metodos:
        lienzo.vertice('', 'line;strokeWidth=1;fillColor=none;align=left;verticalAlign=middle;spacingTop=-1;'
                           'spacingLeft=3;spacingRight=3;rotatable=0;labelPosition=right;points=[];'
                           'portConstraint=eastwest;strokeColor=inherit;',
                       0, 40 + alto_atr, ancho, 8, padre=contenedor)
        lienzo.vertice(_html_miembros(metodos), estilo_texto, 0, 40 + alto_atr + 8, ancho, len(metodos) * 14 + 10,
                       padre=contenedor)
    return contenedor, ancho, alto


CAPA_BASE = {
    'RestController': 0, 'ControllerAdvice': 0,
    'Service': 1, 'Component': 1, 'Aspect': 1, 'Class': 1, 'Configuration': 1,
    'DTO': 2, 'Repository': 2, 'Interface': 2, 'Annotation': 2,
    'Entity': 3, 'Embeddable': 4, 'Enumeration': 4,
}


def _capas(nodos, relaciones):
    """Capa de cada clase: base según su estereotipo y, además, debajo de quien la usa."""
    capa = {n['nombre']: CAPA_BASE.get(n['estereotipo'], 1) for n in nodos}
    base = dict(capa)
    aristas = []
    for o, d, tipo, *_ in relaciones:
        aristas.append((d, o) if tipo in ('hereda', 'impl') else (o, d))  # el padre o la interfaz va arriba
    for _ in range(len(nodos) + 2):
        cambio = False
        for o, d in aristas:
            if base[o] == base[d]:
                continue  # servicio → servicio o entidad → entidad: van lado a lado
            if capa[d] < capa[o] + 1 <= 9:
                capa[d] = capa[o] + 1
                cambio = True
        if not cambio:
            break
    usadas = sorted(set(capa.values()))
    return {n: usadas.index(c) for n, c in capa.items()}, aristas


def _canal_libre(x, cajas_intermedias, holgura=18):
    """La x más cercana a la pedida que no cae dentro de ninguna caja de las capas intermedias."""
    def libre(cx):
        return all(not (bx - holgura <= cx <= bx + bw + holgura) for bx, _, bw, _ in cajas_intermedias)
    if libre(x):
        return x
    candidatos = []
    for bx, _, bw, _ in cajas_intermedias:
        candidatos += [bx - holgura - 6, bx + bw + holgura + 6]
    candidatos = [c for c in candidatos if libre(c)]
    return min(candidatos, key=lambda c: abs(c - x)) if candidatos else x


def diagrama_clases(titulo, filas, relaciones, notas=()):
    """
    filas: clases a dibujar (el orden de las filas sirve de orden inicial dentro de cada capa).
    relaciones: lista de (origen, destino, tipo, etiqueta, multiplicidad_destino).
    La ubicación se calcula sola: capas de arriba abajo y orden por baricentro para reducir cruces.
    """
    lienzo = Lienzo(titulo)
    margen, sep_x, sep_y = 40, 70, 140
    nodos = [c for fila in filas for c in fila]
    nombres = {c['nombre'] for c in nodos}
    for o, d, *_ in relaciones:
        if o not in nombres or d not in nombres:
            raise ValueError(f'Relación con una clase que no está en el diagrama: {o} -> {d}')
    capa, aristas = _capas(nodos, relaciones)
    medidas = {c['nombre']: medir_clase(c)[:2] for c in nodos}
    por_capa = {}
    for c in nodos:
        por_capa.setdefault(capa[c['nombre']], []).append(c['nombre'])

    def ubicar():
        centros = {}
        anchos = {k: sum(medidas[n][0] for n in v) + sep_x * (len(v) - 1) for k, v in por_capa.items()}
        total = max(anchos.values())
        for k, v in por_capa.items():
            x = margen + (total - anchos[k]) / 2
            for n in v:
                centros[n] = x + medidas[n][0] / 2
                x += medidas[n][0] + sep_x
        return centros, total

    vecinos = {}
    for o, d in aristas:
        vecinos.setdefault(o, []).append(d)
        vecinos.setdefault(d, []).append(o)
    for _ in range(4):  # baricentro: cada clase cerca de las clases con que se relaciona
        centros, _ = ubicar()
        for k in sorted(por_capa):
            def peso(n, k=k, centros=centros):
                otros = [centros[v] for v in vecinos.get(n, []) if capa[v] != k]
                return sum(otros) / len(otros) if otros else centros[n]
            por_capa[k].sort(key=peso)
    centros, ancho_total = ubicar()

    lienzo.vertice(f'<b>{escape(titulo)}</b>', 'text;html=1;align=left;verticalAlign=middle;fontSize=16;', margen,
                   margen - 10, max(400, ancho_total), 30)
    ids, cajas, techo = {}, {}, {}
    y = margen + 50
    por_nombre = {c['nombre']: c for c in nodos}
    for k in sorted(por_capa):
        techo[k] = y
        alto_capa = 0
        for n in por_capa[k]:
            ancho, alto = medidas[n]
            x = centros[n] - ancho / 2
            ident, w, h = caja_clase(lienzo, por_nombre[n], x, y)
            ids[n] = ident
            cajas[n] = (x, y, w, h)
            alto_capa = max(alto_capa, h)
        y += alto_capa + sep_y

    # Puertos: las líneas que salen por abajo (o entran por arriba) se reparten a lo ancho del borde.
    salidas, entradas, planes = {}, {}, []
    for o, d, tipo, etiqueta, multiplicidad in relaciones:
        arriba, abajo = (d, o) if tipo in ('hereda', 'impl') else (o, d)
        mismo = capa[o] == capa[d]
        planes.append((o, d, tipo, etiqueta, multiplicidad, arriba, abajo, mismo))
        if not mismo:
            salidas.setdefault(arriba, []).append(abajo)
            entradas.setdefault(abajo, []).append(arriba)

    # Cada línea que baja por un hueco entre capas tiene su propia altura, para que no se pisen.
    niveles, por_hueco = {}, {}
    for plan in planes:
        if not plan[7]:
            por_hueco.setdefault(capa[plan[5]], []).append(plan)
    for k, lista in por_hueco.items():
        lista.sort(key=lambda p: cajas[p[5]][0] + cajas[p[5]][2] / 2)
        paso = min(14, (sep_y - 40) / max(1, len(lista)))
        for i, plan in enumerate(lista):
            niveles[id(plan)] = techo[k + 1] - sep_y + 22 + i * paso

    arcos = {}

    def fraccion(lista, elemento):
        orden = sorted(lista, key=lambda n: cajas[n][0] + cajas[n][2] / 2)
        return (orden.index(elemento) + 1) / (len(orden) + 1)

    for o, d, tipo, etiqueta, multiplicidad, arriba, abajo, mismo in planes:
        estilo = ESTILOS_RELACION[tipo] + f'fontSize={FUENTE - 2};labelBackgroundColor=#ffffff;'
        puntos = None
        if mismo:
            fila = por_capa[capa[o]]
            juntas = abs(fila.index(o) - fila.index(d)) == 1
            derecha = cajas[d][0] > cajas[o][0]
            if juntas:
                estilo += (f'exitX={1 if derecha else 0};exitY=0.5;exitDx=0;exitDy=0;'
                           f'entryX={0 if derecha else 1};entryY=0.5;entryDx=0;entryDy=0;')
            else:
                # No están juntas: la línea pasa por encima de la fila para no cruzar la clase del medio.
                arcos[capa[o]] = arcos.get(capa[o], 0) + 1
                altura = techo[capa[o]] - 12 - 9 * (arcos[capa[o]] - 1)
                xa = cajas[o][0] + cajas[o][2] * (0.75 if derecha else 0.25)
                xb = cajas[d][0] + cajas[d][2] * (0.25 if derecha else 0.75)
                fx_o = 0.75 if derecha else 0.25
                fx_d = 0.25 if derecha else 0.75
                estilo += (f'exitX={fx_o};exitY=0;exitDx=0;exitDy=0;entryX={fx_d};entryY=0;entryDx=0;entryDy=0;')
                puntos = [(xa, altura), (xb, altura)]
        else:
            fx_salida = fraccion(salidas[arriba], abajo)
            fx_entrada = fraccion(entradas[abajo], arriba)
            if arriba == o:
                estilo += (f'exitX={fx_salida:.3f};exitY=1;exitDx=0;exitDy=0;'
                           f'entryX={fx_entrada:.3f};entryY=0;entryDx=0;entryDy=0;')
            else:
                estilo += (f'exitX={fx_entrada:.3f};exitY=0;exitDx=0;exitDy=0;'
                           f'entryX={fx_salida:.3f};entryY=1;entryDx=0;entryDy=0;')
        plan_actual = (o, d, tipo, etiqueta, multiplicidad, arriba, abajo, mismo)
        for plan in planes:
            if plan == plan_actual and id(plan) in niveles:
                nivel = niveles[id(plan)]
                xa = cajas[arriba][0] + cajas[arriba][2] * fraccion(salidas[arriba], abajo)
                xb = cajas[abajo][0] + cajas[abajo][2] * fraccion(entradas[abajo], arriba)
                ka, kb = capa[arriba], capa[abajo]
                if kb - ka > 1:
                    # Salta capas: baja por un canal donde no haya cajas en las capas intermedias.
                    intermedias = [cajas[n] for k in range(ka + 1, kb) for n in por_capa.get(k, [])]
                    canal = _canal_libre(xb, intermedias)
                    nivel_final = techo[kb] - 26
                    puntos = [(xa, nivel), (canal, nivel), (canal, nivel_final), (xb, nivel_final)]
                else:
                    puntos = [(xa, nivel), (xb, nivel)]
                break
        arista = lienzo.arista(etiqueta or '', estilo, ids[o], ids[d], puntos=puntos)
        if multiplicidad:
            lienzo.celdas.append(
                f'<mxCell id="{lienzo.nuevo_id()}" value={quoteattr(multiplicidad)} '
                f'style="edgeLabel;resizable=0;html=1;align=left;verticalAlign=bottom;fontSize={FUENTE - 1};'
                f'labelBackgroundColor=#ffffff;" connectable="0" vertex="1" parent="{arista}">'
                f'<mxGeometry x="0.8" relative="1" as="geometry"><mxPoint x="6" y="0" as="offset"/></mxGeometry></mxCell>')
    for texto in notas:
        lineas = texto.split('\n')
        ancho = _ancho_texto(lineas, 200)
        alto = len(lineas) * ALTO_LINEA + 16
        lienzo.vertice('<br>'.join(escape(x) for x in lineas),
                       f'shape=note;whiteSpace=wrap;html=1;backgroundOutline=1;darkOpacity=0.05;size=14;'
                       f'fillColor=#fffbe6;strokeColor=#b8a000;align=left;spacingLeft=8;fontSize={FUENTE};',
                       margen, y, ancho, alto)
        y += alto + 20
    return lienzo.xml(ancho_total + 2 * margen, y)


# ----------------------------------------------------------------------------------------------
# Diagrama de secuencia
# ----------------------------------------------------------------------------------------------

ESTILO_LLAMADA = f'html=1;verticalAlign=bottom;endArrow=block;endFill=1;rounded=0;fontSize={FUENTE};'
ESTILO_RETORNO = f'html=1;verticalAlign=bottom;endArrow=open;endFill=0;dashed=1;rounded=0;fontSize={FUENTE};'
ESTILO_CREAR = f'html=1;verticalAlign=bottom;endArrow=open;endFill=0;dashed=1;rounded=0;fontSize={FUENTE};'


def diagrama_secuencia(titulo, participantes, pasos):
    """
    participantes: lista de (clave, nombre, estereotipo). Estereotipo 'actor' dibuja una persona.
    pasos: lista de tuplas:
      ('m', origen, destino, texto)     mensaje síncrono (destino == origen: mensaje a sí mismo)
      ('r', origen, destino, texto)     retorno (punteado)
      ('nota', clave, texto)            nota sobre una línea de vida
      ('alt', [(guarda, [pasos...]), (guarda, [pasos...])])   fragmento alt (o 'opt' / 'loop' con una guarda)
    """
    lienzo = Lienzo(titulo)
    margen = 40
    y0 = margen + 50
    alto_cabeza = 50
    paso_y = 34

    # Ancho de cada columna según el texto más largo que sale de ella.
    anchos = {}
    for clave, nombre, estereotipo in participantes:
        anchos[clave] = max(130, _ancho_texto([nombre, f'«{estereotipo}»'], 130))

    def recorrer(lista):
        for p in lista:
            if p[0] in ('m', 'r'):
                yield p
            elif p[0] in ('alt', 'opt', 'loop'):
                for _, sub in p[1]:
                    yield from recorrer(sub)

    claves = [c for c, _, _ in participantes]
    separacion = {i: 40 for i in range(len(claves))}
    for p in recorrer(pasos):
        _, o, d, texto = p
        if o == d:
            i = claves.index(o)
            separacion[i] = max(separacion[i], len(texto) * ANCHO_LETRA * 0.9 + 30)
            continue
        i, j = sorted((claves.index(o), claves.index(d)))
        necesario = len(texto) * ANCHO_LETRA + 30
        disponible = sum(anchos[claves[k]] for k in range(i + 1, j)) + sum(separacion[k] for k in range(i, j)) \
            + (anchos[claves[i]] + anchos[claves[j]]) / 2
        if disponible < necesario:
            extra = (necesario - disponible) / (j - i)
            for k in range(i, j):
                separacion[k] += extra

    centros, x = {}, margen
    for i, (clave, nombre, estereotipo) in enumerate(participantes):
        centros[clave] = x + anchos[clave] / 2
        x += anchos[clave] + separacion[i]
    ancho_total = x - separacion[len(claves) - 1]

    # Primero se calcula la altura total para dibujar las líneas de vida con el largo correcto.
    def contar(lista):
        total = 0
        for p in lista:
            if p[0] in ('m', 'r'):
                total += paso_y + (14 if p[1] == p[2] else 0)
            elif p[0] == 'nota':
                total += paso_y + 10
            else:
                for _, sub in p[1]:
                    total += 26 + contar(sub)
                total += 16
        return total

    alto_vida = alto_cabeza + 30 + contar(pasos) + 30
    lienzo.vertice(f'<b>{escape(titulo)}</b>', 'text;html=1;align=left;verticalAlign=middle;fontSize=16;',
                   margen, margen - 10, max(400, ancho_total), 30)
    for clave, nombre, estereotipo in participantes:
        ancho = anchos[clave]
        if estereotipo == 'actor':
            estilo = (f'shape=umlLifeline;participant=umlActor;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;'
                      f'container=0;collapsible=0;recursiveResize=0;verticalAlign=top;spacingTop=36;outlineConnect=0;'
                      f'portConstraint=eastwest;size=40;fontSize={FUENTE};')
            lienzo.vertice(escape(nombre), estilo, centros[clave] - 15, y0, 30, alto_vida)
        else:
            etiqueta = f'«{escape(estereotipo)}»<br><b>{escape(nombre)}</b>' if estereotipo else f'<b>{escape(nombre)}</b>'
            estilo = (f'shape=umlLifeline;perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=0;dropTarget=0;'
                      f'collapsible=0;recursiveResize=0;outlineConnect=0;portConstraint=eastwest;newEdgeStyle='
                      f'{{"curved":0,"rounded":0}};size={alto_cabeza};fontSize={FUENTE};fillColor=#dae8fc;strokeColor=#6c8ebf;')
            lienzo.vertice(etiqueta, estilo, centros[clave] - ancho / 2, y0, ancho, alto_vida)

    y = [y0 + alto_cabeza + 30]

    def dibujar(lista, nivel=0):
        for p in lista:
            if p[0] in ('m', 'r'):
                _, o, d, texto = p
                estilo = ESTILO_LLAMADA if p[0] == 'm' else ESTILO_RETORNO
                if o == d:
                    cx = centros[o]
                    lienzo.arista(escape(texto), estilo + 'align=left;spacingLeft=4;', desde=(cx, y[0]),
                                  hasta=(cx, y[0] + 16), puntos=[(cx + 40, y[0]), (cx + 40, y[0] + 16)])
                    y[0] += paso_y + 14
                else:
                    lienzo.arista(escape(texto), estilo, desde=(centros[o], y[0]), hasta=(centros[d], y[0]))
                    y[0] += paso_y
            elif p[0] == 'nota':
                _, clave, texto = p
                ancho = _ancho_texto([texto], 120)
                lienzo.vertice(escape(texto), f'shape=note;whiteSpace=wrap;html=1;size=10;fillColor=#fffbe6;'
                                              f'strokeColor=#b8a000;fontSize={FUENTE - 1};',
                               centros[clave] + 8, y[0] - 14, ancho, 30)
                y[0] += paso_y + 10
            else:
                tipo, ramas = p
                inicio = y[0] - 10
                involucrados = [c for r in recorrer([p]) for c in (r[1], r[2])]
                xs = [centros[c] for c in involucrados] or list(centros.values())
                izquierda = max(4, min(xs) - 70 + nivel * 14)
                derecha = max(xs) + 90 - nivel * 14
                guardas = []
                for k, (guarda, sub) in enumerate(ramas):
                    if k > 0:
                        guardas.append(('linea', y[0] - 6))
                    guardas.append(('texto', y[0] + 4, guarda))
                    y[0] += 26
                    dibujar(sub, nivel + 1)
                fin = y[0] + 2
                lienzo.vertice(tipo, f'shape=umlFrame;whiteSpace=wrap;html=1;pointerEvents=0;width=46;height=20;'
                                     f'fontSize={FUENTE};fontStyle=1;fillColor=none;',
                               izquierda, inicio, derecha - izquierda, fin - inicio)
                for g in guardas:
                    if g[0] == 'linea':
                        lienzo.arista('', 'endArrow=none;dashed=1;html=1;rounded=0;strokeColor=#666666;',
                                      desde=(izquierda, g[1]), hasta=(derecha, g[1]))
                    else:
                        lienzo.vertice(escape(g[2]), f'text;html=1;align=left;verticalAlign=middle;fontSize={FUENTE};'
                                                     f'fontStyle=2;',
                                       izquierda + 52, g[1] - 10, max(120, len(g[2]) * ANCHO_LETRA + 10), 20)
                y[0] = fin + 24

    dibujar(pasos)
    return lienzo.xml(ancho_total + margen, y0 + alto_vida + margen)
