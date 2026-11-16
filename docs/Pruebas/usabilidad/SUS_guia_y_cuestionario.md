# Prueba de usabilidad con SUS — guía para aplicarla

**Para qué:** la sección 6.2 de la entrega final («Indicador de usabilidad (SUS)») pide medir la usabilidad
con el System Usability Scale. El requisito del curso es un puntaje **mayor que 70**. Esta prueba **la tienen que
responder personas reales**, mínimo 5, que no sean del equipo.

**Qué hay en esta carpeta:**

| Archivo | Para qué sirve |
|---|---|
| `SUS_guia_y_cuestionario.md` | Este archivo: cómo aplicar la prueba, las tareas y el cuestionario. |
| `sus_respuestas.csv` | Plantilla donde se anotan las respuestas (1 a 5) de cada persona. |
| `calcular_sus.py` | Calcula el puntaje SUS de cada persona y el promedio, y dice si se cumple el requisito. |

---

## 1. Antes de empezar

- **Dónde:** la app publicada, `https://metamorfosis-mu.vercel.app`. La mitad de las personas la usa en el
  celular y la otra mitad en el computador.
- **Cuenta:** cada persona se registra con su propio correo (HU8) o usa una cuenta de prueba que el equipo le
  entrega. No usen cuentas con datos de otra persona.
- **Quién aplica la prueba:** un integrante lee las tareas y anota el tiempo y los tropiezos. **No ayuda** mientras
  la persona hace la tarea; si se bloquea más de 2 minutos, se anota «no completada» y se pasa a la siguiente.
- **Consentimiento:** antes de empezar, decirle a la persona que se evalúa la app, no a ella, y que sus
  respuestas se guardan sin nombre (P1, P2…).

## 2. Tareas (unos 10 minutos)

| # | Tarea que se le lee a la persona | HU que evalúa |
|---|---|---|
| 1 | Crea una meta llamada «Leer 3 libros» de tipo Personal y mediano plazo. | HU1 |
| 2 | Abre la meta y agrega un hito «Terminar el libro 1» y un hábito «Leer 15 páginas» que se cumpla con al menos 15. | HU28 |
| 3 | Registra que hoy leíste 20 páginas y marca el hito como logrado. | HU5 |
| 4 | Mira cuánto avanzaste y qué tan constante has sido. | HU29 |
| 5 | Marca la meta como cumplida. | HU6 |
| 6 | Busca tus metas cumplidas usando los filtros. | HU27 |
| 7 | Abre tu reporte y cambia a la gráfica «por plazo». | HU7 |
| 8 | Cambia la apariencia de la app al modo oscuro. | HU30 |

Para cada tarea se anota: completada (sí / no), tiempo aproximado y comentarios.

## 3. Cuestionario SUS (justo después de las tareas)

La persona responde cada frase de **1 (totalmente en desacuerdo)** a **5 (totalmente de acuerdo)**, sin pensarlo mucho
y sin dejar ninguna en blanco.

| # | Frase |
|---|---|
| 1 | Creo que me gustaría usar esta aplicación con frecuencia. |
| 2 | Encontré la aplicación innecesariamente compleja. |
| 3 | Pienso que la aplicación es fácil de usar. |
| 4 | Creo que necesitaría la ayuda de una persona con conocimientos técnicos para poder usarla. |
| 5 | Las funciones de la aplicación están bien integradas. |
| 6 | Pienso que la aplicación tiene demasiadas inconsistencias. |
| 7 | Imagino que la mayoría de las personas aprendería a usarla muy rápido. |
| 8 | Encontré la aplicación muy difícil de usar. |
| 9 | Me sentí muy seguro(a) usando la aplicación. |
| 10 | Necesité aprender muchas cosas antes de poder usar la aplicación. |

Se puede pasar a un Google Forms con preguntas de escala lineal de 1 a 5, en este mismo orden.

## 4. Cómo se calcula (lo hace `calcular_sus.py`)

- Frases impares (1, 3, 5, 7, 9): aporte = respuesta − 1.
- Frases pares (2, 4, 6, 8, 10): aporte = 5 − respuesta.
- Puntaje SUS de la persona = suma de los aportes × 2,5 (va de 0 a 100).
- Puntaje de la app = promedio de los puntajes de todas las personas.

Como referencia, 68 es el promedio de muchas aplicaciones evaluadas con SUS. El requisito del curso es **más de 70**.

```bash
python docs/Pruebas/usabilidad/calcular_sus.py docs/Pruebas/usabilidad/sus_respuestas.csv
```

## 5. Qué va en el documento (sección 6.2)

1. Tabla con el puntaje de cada persona (P1 a P5 o más), su dispositivo y el promedio que da el script.
2. Tabla de tareas: cuántas personas completaron cada una y los tropiezos más repetidos.
3. Captura de la salida de `calcular_sus.py` y, si usaron Google Forms, del resumen de respuestas.
4. Mejoras hechas (o propuestas) a partir de los comentarios.
