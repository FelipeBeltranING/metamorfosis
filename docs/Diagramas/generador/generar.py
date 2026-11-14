"""
Genera los diagramas de clases y de secuencia de todas las HU en formato draw.io.

Uso (desde la raíz del repositorio):
    python docs/Diagramas/generador/generar.py

1. extrae las firmas del código (backend/src/main/java/...) a modelo.json,
2. arma cada diagrama con especificaciones.py,
3. valida que cada archivo sea XML bien formado,
4. escribe docs/Diagramas/Clases/HUxx_clases.drawio y docs/Diagramas/Secuencia/HUxx_secuencia.drawio.
   Si la HU tiene otros flujos (secuencias_extra), cada uno va en HUxx_secuencia_<sufijo>.drawio.

Si el código cambia (un método se renombra o desaparece) el paso 2 falla y avisa qué diagrama revisar.
"""
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

AQUI = Path(__file__).resolve().parent
RAIZ = AQUI.parents[2]
sys.path.insert(0, str(AQUI))

import drawio  # noqa: E402
import especificaciones as esp  # noqa: E402


def main():
    sys.stdout.reconfigure(encoding='utf-8')
    modelo = AQUI / 'modelo.json'
    subprocess.run([sys.executable, str(AQUI / 'extraer_modelo.py'),
                    str(RAIZ / 'backend/src/main/java/com/metamorfosis/backend'), str(modelo)], check=True)
    esp.cargar_modelo(modelo)
    destino_clases = RAIZ / 'docs/Diagramas/Clases'
    destino_secuencia = RAIZ / 'docs/Diagramas/Secuencia'
    destino_clases.mkdir(parents=True, exist_ok=True)
    destino_secuencia.mkdir(parents=True, exist_ok=True)

    errores = []
    total = 0
    for clave, datos in sorted(esp.HU.items()):
        titulo = f'{clave.replace("HU0", "HU")} · {datos["nombre"]}'
        try:
            filas = [[fabrica() for fabrica in fila] for fila in datos['filas']]
            relaciones = list(datos['relaciones'])
            # Cada repositorio se une con la entidad que gestiona si las dos están en el diagrama.
            presentes = {c['nombre'] for fila in filas for c in fila}
            ya = {(o, d) for o, d, *_ in relaciones}
            for nombre in sorted(presentes):
                entidad = nombre.removesuffix('Repository')
                if nombre.endswith('Repository') and entidad in presentes and (nombre, entidad) not in ya:
                    relaciones.append((nombre, entidad, 'usa', 'gestiona', None))
            xml_clases = drawio.diagrama_clases(f'Diagrama de clases · {titulo}', filas, relaciones)
            xml_secuencia = None
            if datos.get('pasos'):
                xml_secuencia = drawio.diagrama_secuencia(f'Diagrama de secuencia · {titulo}', datos['participantes'],
                                                          datos['pasos'])
            extras = {}
            for extra in datos.get('secuencias_extra', []):
                extras[extra['sufijo']] = drawio.diagrama_secuencia(
                    f'Diagrama de secuencia · {titulo} · {extra["nombre"]}', extra['participantes'], extra['pasos'])
            for xml in (xml_clases, *([xml_secuencia] if xml_secuencia else []), *extras.values()):
                ET.fromstring(xml)  # falla si el XML no está bien formado
        except Exception as error:  # noqa: BLE001 - se reportan todos juntos
            errores.append(f'{clave}: {error}')
            continue
        (destino_clases / f'{clave}_clases.drawio').write_text(xml_clases, encoding='utf-8')
        if xml_secuencia:
            (destino_secuencia / f'{clave}_secuencia.drawio').write_text(xml_secuencia, encoding='utf-8')
        for sufijo, xml in extras.items():
            (destino_secuencia / f'{clave}_secuencia_{sufijo}.drawio').write_text(xml, encoding='utf-8')
        total += 1 + bool(xml_secuencia) + len(extras)
        otros = f' (+{len(extras)} secuencias de otros flujos)' if extras else ''
        print(f'{clave}: {len(filas)} filas de clases, {len(datos.get("pasos", []))} pasos de secuencia{otros}')

    if errores:
        print('\nERRORES:\n' + '\n'.join(errores))
        sys.exit(1)
    print(f'\n{len(esp.HU)} HU → {total} archivos .drawio')


if __name__ == '__main__':
    main()
