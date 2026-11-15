"""
Sección 6.2 (usabilidad): calcula el puntaje SUS (System Usability Scale) de cada persona y el de la app.

Uso:
    python docs/Pruebas/usabilidad/calcular_sus.py docs/Pruebas/usabilidad/sus_respuestas.csv

El CSV tiene una fila por persona: participante, dispositivo y las respuestas p1 a p10 (de 1 a 5).
Regla de SUS: en las frases impares el aporte es respuesta - 1; en las pares, 5 - respuesta.
La suma de los aportes por 2,5 da un puntaje de 0 a 100. El requisito del curso es más de 70.
"""
import csv
import sys

MINIMO_PERSONAS = 5
REQUISITO = 70


def puntaje(respuestas):
    """Puntaje SUS (0 a 100) de una persona a partir de sus 10 respuestas."""
    if len(respuestas) != 10:
        raise ValueError(f'se esperaban 10 respuestas y hay {len(respuestas)}')
    for r in respuestas:
        if r not in (1, 2, 3, 4, 5):
            raise ValueError(f'respuesta fuera de la escala 1 a 5: {r}')
    aportes = [(r - 1) if i % 2 == 0 else (5 - r) for i, r in enumerate(respuestas)]
    return sum(aportes) * 2.5


def main(ruta):
    with open(ruta, encoding='utf-8') as archivo:
        filas = list(csv.DictReader(archivo))
    if not filas:
        print('El CSV no tiene respuestas todavía.')
        return 1

    puntajes = []
    print(f'{"Participante":<14}{"Dispositivo":<14}{"SUS":>6}')
    for fila in filas:
        try:
            respuestas = [int(fila[f'p{i}']) for i in range(1, 11)]
            valor = puntaje(respuestas)
        except (KeyError, ValueError) as error:
            print(f'{fila.get("participante", "?")}: fila inválida ({error})')
            return 1
        puntajes.append(valor)
        print(f'{fila["participante"]:<14}{fila.get("dispositivo", ""):<14}{valor:>6.1f}')

    promedio = sum(puntajes) / len(puntajes)
    print('-' * 34)
    print(f'{"Promedio":<28}{promedio:>6.1f}')
    print(f'Personas: {len(puntajes)} (mínimo {MINIMO_PERSONAS})')
    cumple = promedio > REQUISITO and len(puntajes) >= MINIMO_PERSONAS
    print(f'Requisito (> {REQUISITO} con al menos {MINIMO_PERSONAS} personas): {"CUMPLE" if cumple else "NO CUMPLE"}')
    return 0 if cumple else 2


if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    sys.exit(main(sys.argv[1]))
