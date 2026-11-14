"""
Lee el código Java del backend y deja en un JSON, por clase: estereotipo, atributos y métodos públicos.
Los diagramas de clases toman de aquí las firmas, así quedan iguales al código y no se escriben a mano.

Uso: python extraer_modelo.py <ruta a backend/src/main/java/com/metamorfosis/backend> <salida.json>
"""
import json
import re
import sys
from pathlib import Path

ESTEREOTIPOS = [
    ('@RestController', 'RestController'),
    ('@Service', 'Service'),
    ('@Component', 'Component'),
    ('@Configuration', 'Configuration'),
    ('@Aspect', 'Aspect'),
    ('@Entity', 'Entity'),
    ('@Embeddable', 'Embeddable'),
    ('@RestControllerAdvice', 'ControllerAdvice'),
]

MODIFICADORES = r'(?:public|protected|private|static|final|abstract|synchronized|default|\s)*'


def limpiar_cuerpo(texto):
    """Quita comentarios, anotaciones y literales para que las regex no se confundan."""
    # Primero los literales (una URL "https://..." no es un comentario) y después los comentarios.
    texto = re.sub(r'"""[\s\S]*?"""', '""', texto)        # bloques de texto
    texto = re.sub(r'"(?:\\.|[^"\\\n])*"', '""', texto)
    texto = re.sub(r"'(?:\\.|[^'\\])'", "' '", texto)       # caracteres como '{'
    texto = re.sub(r'/\*.*?\*/', '', texto, flags=re.S)
    texto = re.sub(r'//[^\n]*', '', texto)
    return texto


def tipo_corto(tipo):
    tipo = re.sub(r'\s+', ' ', tipo.strip())
    tipo = re.sub(r'@\w+(\([^)]*\))?\s*', '', tipo)
    return tipo.replace('java.util.', '').replace('java.time.', '')


def parametros(texto):
    """'@PathVariable Integer codigoMeta, String x' -> 'codigoMeta: Integer, x: String'."""
    texto = re.sub(r'@\w+(\([^)]*\))?\s*', '', texto).strip()
    if not texto:
        return ''
    partes, nivel, actual = [], 0, ''
    for c in texto:
        if c == '<':
            nivel += 1
        elif c == '>':
            nivel -= 1
        if c == ',' and nivel == 0:
            partes.append(actual)
            actual = ''
        else:
            actual += c
    partes.append(actual)
    salida = []
    for p in partes:
        p = p.replace('final ', '').strip()
        m = re.match(r'(.+?)\s+(\w+)$', p)
        if m:
            salida.append(f'{m.group(2)}: {tipo_corto(m.group(1))}')
    return ', '.join(salida)


def cuerpo_de_clase(texto, inicio):
    """Devuelve solo el primer nivel de llaves de la clase (sin el contenido de los métodos)."""
    nivel, salida = 0, []
    for c in texto[inicio:]:
        if c == '{':
            nivel += 1
            if nivel == 1:
                continue
            if nivel == 2:
                salida.append('{')
        elif c == '}':
            nivel -= 1
            if nivel == 0:
                break
        if nivel == 1:
            salida.append(c)
        elif nivel == 2 and c not in '{}':
            pass
    return ''.join(salida)


def analizar(ruta):
    original = ruta.read_text(encoding='utf-8')
    texto = limpiar_cuerpo(original)
    clases = {}
    for m in re.finditer(r'(?:public\s+)?(?:final\s+)?(?:abstract\s+)?(class|interface|record|enum|@interface)\s+(\w+)'
                         r'(\s*<[^>{]*>)?(\s*\(([^)]*)\))?([^{]*)\{', texto):
        clase, nombre = m.group(1), m.group(2)
        if nombre in clases:
            continue
        cabecera = texto[max(0, m.start() - 400):m.start()]
        estereotipo = None
        for anotacion, nombre_est in ESTEREOTIPOS:
            if anotacion + '\n' in cabecera[-200:] + '\n' or re.search(re.escape(anotacion) + r'\b', cabecera[-200:]):
                estereotipo = nombre_est
        if clase == 'record':
            estereotipo = estereotipo or 'DTO'
        elif clase == 'enum':
            estereotipo = 'Enumeration'
        elif clase == 'interface':
            estereotipo = 'Repository' if 'Repository' in nombre else 'Interface'
        elif clase == '@interface':
            estereotipo = 'Annotation'
        extiende = m.group(6) or ''
        cuerpo = cuerpo_de_clase(texto, m.end() - 1)
        # Sin anotaciones (@Param(""), @Column(...)): sus paréntesis confunden las regex de métodos.
        cuerpo = re.sub(r'@\w+(\s*\([^()]*\))?', '', cuerpo)
        atributos, metodos, constantes = [], [], []
        if clase == 'record' and m.group(5) is not None:
            for p in parametros(m.group(5)).split(', '):
                if p:
                    atributos.append('- ' + p)
        if clase == 'enum':
            primera = cuerpo.split(';')[0]
            constantes = [c.strip().split('(')[0] for c in primera.split(',') if re.match(r'\s*[A-Z_]+', c)]
        for a in re.finditer(r'(private|protected|public)\s+((?:static\s+)?(?:final\s+)?)([\w<>,.\[\]? ]+?)\s+(\w+)\s*(=[^;]*)?;', cuerpo):
            vis = {'private': '-', 'protected': '#', 'public': '+'}[a.group(1)]
            nombre_a = a.group(4)
            if 'static' in a.group(2) and nombre_a in ('log', 'serialVersionUID'):
                continue
            atributos.append(f'{vis} {nombre_a}: {tipo_corto(a.group(3))}')
        for f in re.finditer(r'(public|protected)\s+(static\s+)?(?:<[^>]+>\s+)?([\w<>,.\[\]? ]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws[^{;]*)?[{;]', cuerpo):
            if f.group(4) in ('if', 'for', 'while', 'switch', 'return', 'new'):
                continue
            metodos.append({'nombre': f.group(4), 'firma': f'+ {f.group(4)}({parametros(f.group(5))}): {tipo_corto(f.group(3))}',
                            'estatico': bool(f.group(2))})
        if clase == 'interface':
            for f in re.finditer(r'^\s*([\w<>,.\[\]? ]+?)\s+(\w+)\s*\(([^)]*)\)\s*;', cuerpo, flags=re.M):
                metodos.append({'nombre': f.group(2), 'firma': f'+ {f.group(2)}({parametros(f.group(3))}): {tipo_corto(f.group(1))}',
                                'estatico': False})
        auditar = {}
        for au in re.finditer(r'@Auditar\(accion\s*=\s*AccionLog\.(\w+)[^)]*\)\s*(?:@\w+(?:\([^)]*\))?\s*)*public\s+[\w<>,.\[\]? ]+?\s+(\w+)\(', original):
            auditar[au.group(2)] = au.group(1)
        clases[nombre] = {
            'tipo': clase, 'estereotipo': estereotipo or 'Class', 'atributos': atributos,
            'metodos': metodos, 'constantes': constantes, 'auditar': auditar,
            'extiende': extiende.strip(), 'archivo': str(ruta.name),
        }
    return clases


def main():
    base = Path(sys.argv[1])
    modelo = {}
    for ruta in sorted(base.rglob('*.java')):
        for nombre, datos in analizar(ruta).items():
            datos['paquete'] = ruta.parent.name
            modelo.setdefault(nombre, datos)
    Path(sys.argv[2]).write_text(json.dumps(modelo, ensure_ascii=False, indent=1), encoding='utf-8')
    print(f'{len(modelo)} clases')


if __name__ == '__main__':
    main()
