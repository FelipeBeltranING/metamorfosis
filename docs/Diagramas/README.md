# Diagramas de Metamorfosis

Dos diagramas por historia de usuario, en draw.io (se abren y se editan en [app.diagrams.net](https://app.diagrams.net) o en draw.io de escritorio):

- **Clases** (`Clases/HUxx_clases.drawio`): las clases que participan en la HU, con sus atributos y métodos **tal como están en el código** y sus relaciones (asociación, dependencia, composición, realización, herencia). Colores por capa: controlador azul, servicio verde, repositorio amarillo, DTO morado, entidad rojo, aspecto naranja.
- **Secuencia** (`Secuencia/HUxx_secuencia.drawio`): desde la acción del usuario en la pantalla hasta la base de datos y de vuelta. Caso positivo completo y un fragmento `alt` con el error principal de la HU. Las llamadas auditadas pasan por `AuditoriaAspect`, que guarda la fila en `log_actividad` (RQ8).

| HU | Historia | Clases | Secuencia |
|---|---|---|---|
| HU1 | Crear meta | [clases](Clases/HU01_clases.drawio) | [secuencia](Secuencia/HU01_secuencia.drawio) |
| HU2 | Editar meta | [clases](Clases/HU02_clases.drawio) | [secuencia](Secuencia/HU02_secuencia.drawio) |
| HU3 | Ver mis metas | [clases](Clases/HU03_clases.drawio) | [secuencia](Secuencia/HU03_secuencia.drawio) |
| HU4 | Eliminar meta | [clases](Clases/HU04_clases.drawio) | [secuencia](Secuencia/HU04_secuencia.drawio) |
| HU5 | Registrar mi avance (hitos logrados y días de los hábitos) | [clases](Clases/HU05_clases.drawio) | [secuencia](Secuencia/HU05_secuencia.drawio) |
| HU6 | Marcar meta como cumplida | [clases](Clases/HU06_clases.drawio) | [secuencia](Secuencia/HU06_secuencia.drawio) |
| HU7 | Ver reporte general | [clases](Clases/HU07_clases.drawio) | [secuencia](Secuencia/HU07_secuencia.drawio) |
| HU8 | Registrarse (con autorización por correo) | [clases](Clases/HU08_clases.drawio) | [secuencia](Secuencia/HU08_secuencia.drawio) |
| HU9 | Iniciar sesión | [clases](Clases/HU09_clases.drawio) | [secuencia](Secuencia/HU09_secuencia.drawio) |
| HU10 | Cerrar sesión | [clases](Clases/HU10_clases.drawio) | [secuencia](Secuencia/HU10_secuencia.drawio) |
| HU11 | Recibir insignia | [clases](Clases/HU11_clases.drawio) | [secuencia](Secuencia/HU11_secuencia.drawio) |
| HU12 | Ver galería de insignias | [clases](Clases/HU12_clases.drawio) | [secuencia](Secuencia/HU12_secuencia.drawio) |
| HU13 | Invitar a una meta | [clases](Clases/HU13_clases.drawio) | [secuencia](Secuencia/HU13_secuencia.drawio) |
| HU14 | Responder invitación | [clases](Clases/HU14_clases.drawio) | [secuencia](Secuencia/HU14_secuencia.drawio) |
| HU15 | Logro de meta compartida | [clases](Clases/HU15_clases.drawio) | [secuencia](Secuencia/HU15_secuencia.drawio) |
| HU19 | Personalizar mi perfil público | [clases](Clases/HU19_clases.drawio) | [secuencia](Secuencia/HU19_secuencia.drawio) |
| HU20 | Personalizar mi avatar | [clases](Clases/HU20_clases.drawio) | [secuencia](Secuencia/HU20_secuencia.drawio) |
| HU21 | Compartir logro | [clases](Clases/HU21_clases.drawio) | [secuencia](Secuencia/HU21_secuencia.drawio) |
| HU22 | Ver mi perfil | [clases](Clases/HU22_clases.drawio) | [secuencia](Secuencia/HU22_secuencia.drawio) |
| HU23 | Editar perfil y foto | [clases](Clases/HU23_clases.drawio) | [secuencia](Secuencia/HU23_secuencia.drawio) |
| HU24 | Consultar el perfil de otros | [clases](Clases/HU24_clases.drawio) | [secuencia](Secuencia/HU24_secuencia.drawio) |
| HU25 | Ocultar metas del perfil | [clases](Clases/HU25_clases.drawio) | [secuencia](Secuencia/HU25_secuencia.drawio) |
| HU26 | Perfil privado | [clases](Clases/HU26_clases.drawio) | [secuencia](Secuencia/HU26_secuencia.drawio) |
| HU27 | Filtrar metas por estado, tipo y plazo | [clases](Clases/HU27_clases.drawio) | [secuencia](Secuencia/HU27_secuencia.drawio) |
| HU28 | Trazar el camino de una meta (hitos y hábitos) | [clases](Clases/HU28_clases.drawio) | [secuencia](Secuencia/HU28_secuencia.drawio) |
| HU29 | Ver cuánto me falta y qué tan constante soy | [clases](Clases/HU29_clases.drawio) | [secuencia](Secuencia/HU29_secuencia.drawio) |
| HU30 | Elegir la apariencia de la app | [clases](Clases/HU30_clases.drawio) | [secuencia](Secuencia/HU30_secuencia.drawio) |

**No tienen diagramas:** HU16 (internacionalización, transversal: ver el README del repositorio), HU17 y HU18 (retiradas) y HU31 (opcional, no implementada).
**HU30** es la que el equipo había dejado como "HU sin número - Apariencia de la app": se numera para que tenga el mismo sustento que las demás.

## Cómo se generan

Los diagramas salen del código, no se dibujan a mano, para que no vuelvan a quedar desactualizados:

```bash
python docs/Diagramas/generador/generar.py
```

1. `extraer_modelo.py` lee `backend/src/main/java` y saca, por clase, su estereotipo (`@RestController`, `@Service`, `@Entity`, record → DTO…), atributos, métodos públicos y la acción de `@Auditar`.
2. `especificaciones.py` dice qué clases, relaciones y pasos de secuencia van en cada HU. Si pide un método que ya no existe en el código, **el generador falla y dice qué HU revisar**.
3. `drawio.py` dibuja: clases por capas (controlador → servicio → repositorio/DTO → entidad → enum), ordenadas para reducir cruces; secuencias con líneas de vida, retornos punteados y fragmentos `alt` / `loop`.
4. Cada archivo se valida como XML antes de escribirse.

Para exportar un diagrama a imagen con draw.io de escritorio:

```bash
"C:/Program Files/draw.io/draw.io.exe" -x -f png -o HU05_clases.png docs/Diagramas/Clases/HU05_clases.drawio
```

## Histórico

`Historico/` guarda los diagramas a mano del Sprint 1 (HU1 a HU5, HU8 a HU10 y HU21) tal como se entregaron. Varios quedaron desactualizados (la entidad `Registro` de la HU5 retirada, el registro sin autorización por correo de la HU8…); los vigentes son los de `Clases/` y `Secuencia/`.
