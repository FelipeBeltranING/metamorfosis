# Manuales de Metamorfosis

Manuales de la entrega final (requisito "manual técnico y de usuario"), en PDF, con portada, índice con
enlaces a cada sección y capturas de la app. Versión del 17 de noviembre de 2026, verificada contra el código
de `main`.

| Manual | Archivo | Páginas | Contenido |
|---|---|---|---|
| Manual técnico | `Manual_tecnico_Metamorfosis.pdf` | 140 | Arquitectura, tecnologías, estructura del repositorio, backend, base de datos (modelo E-R, diccionario de datos, llaves, migraciones V1–V28), API REST (53 endpoints y 55 DTO), seguridad, reglas de negocio, auditoría y logs, frontend, app móvil, pruebas, instalación, despliegue, mantenimiento, solución de problemas. Anexos: glosario, variables, comandos, diccionario de clases (108 clases), 30 diagramas de clases y el script completo de la base de datos |
| Manual de usuario · versión web | `Manual_de_usuario_web_Metamorfosis.pdf` | 19 | Cuenta (incluye cambiar y recuperar la contraseña), Mis metas, trazar el camino, registrar el avance, constancia y reportes, metas compartidas y logros, perfil y avatar, insignias, preguntas frecuentes |
| Manual de usuario · app móvil | `Manual_de_usuario_movil_Metamorfosis.pdf` | 10 | Requisitos, instalación de la APK, primer uso, la app en el celular, tareas principales, actualizar y desinstalar, preguntas frecuentes |
| Manual técnico · app móvil | `Manual_tecnico_movil_Metamorfosis.pdf` | 15 | Arquitectura de la APK, versiones, estructura del proyecto Android, configuración, conexión con la API, compilación, GitHub Actions, pruebas en emulador, firma y versiones, solución de problemas |

Las tablas de datos del manual técnico (endpoints, DTO, diccionario de datos, migraciones, pruebas y
diccionario de clases) se generaron desde el código y desde `docs/BaseDeDatos`, así que coinciden con `main`.

`version_anterior/` guarda los PDF del 15 de noviembre de 2026 (manual de usuario de 24 páginas y manual técnico
de 18 páginas) como referencia.
