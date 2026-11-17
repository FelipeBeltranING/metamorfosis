# Diccionario de datos · Metamorfosis

Generado con `python docs/BaseDeDatos/generar.py` desde la base PostgreSQL creada por Flyway (migraciones V1 a V28). Cada tabla corresponde a una entidad JPA del paquete `com.metamorfosis.backend.model`.

| Tabla | Columnas | Descripción |
| --- | --- | --- |
| `usuario` | 20 | Cuenta de cada persona: datos personales, contraseña (hash BCrypt), foto, apariencia, avatar y perfil público (HU8, HU19, HU20, HU23, HU26, HU30, HU32). |
| `meta` | 9 | Meta que crea un usuario: nombre, tipo, plazo y estado ACTIVA o CUMPLIDA (HU1 a HU4, HU6). |
| `participacion` | 10 | Relación usuario-meta: creador e invitados, estado de la invitación, fecha de cumplimiento y si la oculta de su perfil (HU6, HU13 a HU15, HU25). |
| `sub_meta` | 13 | Paso del camino de una meta: hito (se logra una vez) o hábito (se repite ciertos días, Sí/No o medible) (HU28, HU5). |
| `registro_sub_meta` | 5 | Registro de un hábito en una fecha: hecho/no hecho o el valor medido (HU5, HU29). |
| `insignia` | 7 | Catálogo de insignias y el criterio para ganarlas (HU11, HU12). |
| `usuario_insignia` | 3 | Insignias que ganó cada usuario y en qué fecha (HU11, HU12). |
| `logro_compartido` | 5 | Enlace público de una meta cumplida (HU21). |
| `solicitud_registro` | 8 | Solicitud de registro pendiente de autorizar desde el correo (HU8). |
| `codigo_recuperacion` | 5 | Código de 6 dígitos para recuperar la contraseña, guardado como hash (HU33). |
| `token_revocado` | 3 | Tokens JWT cerrados con «Cerrar sesión» (HU10). |
| `intento_login` | 3 | Intentos fallidos de inicio de sesión por correo, para bloquear temporalmente (HU9). |
| `log_actividad` | 8 | Bitácora de auditoría: quién hizo qué, sobre qué dato y con qué resultado. Solo se puede insertar (RQ8). |
| `flyway_schema_history` | 10 | Control de Flyway: qué migraciones se aplicaron (V1 a V28). No es parte del modelo. |

## usuario

Cuenta de cada persona: datos personales, contraseña (hash BCrypt), foto, apariencia, avatar y perfil público (HU8, HU19, HU20, HU23, HU26, HU30, HU32).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_usuario` | integer | No |  | PK |
| `nombre` | varchar(100) | No |  |  |
| `apellido` | varchar(100) | No |  |  |
| `email` | varchar(255) | No |  |  |
| `password_hash` | varchar(255) | No |  |  |
| `foto_perfil` | text | Sí |  |  |
| `perfil_privado` | boolean | No | `false` |  |
| `fecha_registro` | date | No | `CURRENT_DATE` |  |
| `modo_tema` | varchar(10) | No | `'CLARO'::character varying` |  |
| `color_acento` | varchar(10) | No | `'VIOLETA'::character varying` |  |
| `frase` | varchar(100) | Sí |  |  |
| `portada` | varchar(12) | No | `'NINGUNA'::character varying` |  |
| `codigo_insignia_destacada` | integer | Sí |  | FK → insignia.codigo_insignia (SET NULL) |
| `avatar_piel` | varchar(6) | No | `'f5d7b1'::character varying` |  |
| `avatar_peinado` | varchar(20) | No | `'shortHair'::character varying` |  |
| `avatar_color_pelo` | varchar(6) | No | `'71472d'::character varying` |  |
| `avatar_ojos` | varchar(12) | No | `'cheery'::character varying` |  |
| `avatar_boca` | varchar(14) | No | `'openedSmile'::character varying` |  |
| `avatar_accesorio` | varchar(16) | Sí |  |  |
| `contrasena_cambiada_en` | timestamp | Sí |  |  |

**Únicas:** `usuario_email_key: UNIQUE (email)`

**Restricciones CHECK:** `ck_usuario_avatar_accesorio: CHECK (((avatar_accesorio IS NULL) OR ((avatar_accesorio)::text = ANY ((ARRAY['catEars'::character varying, 'glasses'::character varying, 'sailormoonCrown'::character varying, 'clownNose'::character varying, 'sleepMask'::character varying, 'sunglasses'::character varying, 'faceMask'::character varying, 'mustache'::character varying])::text[]))))`; `ck_usuario_avatar_boca: CHECK (((avatar_boca)::text = ANY ((ARRAY['openedSmile'::character varying, 'unimpressed'::character varying, 'gapSmile'::character varying, 'openSad'::character varying, 'teethSmile'::character varying, 'awkwardSmile'::character varying, 'braces'::character varying, 'kawaii'::character varying])::text[])))`; `ck_usuario_avatar_color_pelo: CHECK (((avatar_color_pelo)::text = ANY ((ARRAY['220f00'::character varying, '3a1a00'::character varying, '71472d'::character varying, 'e2ba87'::character varying, '605de4'::character varying, '238d80'::character varying, 'd56c0c'::character varying, 'e9b729'::character varying])::text[])))`; `ck_usuario_avatar_ojos: CHECK (((avatar_ojos)::text = ANY ((ARRAY['cheery'::character varying, 'normal'::character varying, 'confused'::character varying, 'starstruck'::character varying, 'winking'::character varying, 'sleepy'::character varying, 'sad'::character varying, 'angry'::character varying])::text[])))`; `ck_usuario_avatar_peinado: CHECK (((avatar_peinado)::text = ANY ((ARRAY['shortHair'::character varying, 'mohawk'::character varying, 'wavyBob'::character varying, 'bowlCutHair'::character varying, 'curlyBob'::character varying, 'straightHair'::character varying, 'braids'::character varying, 'shavedHead'::character varying, 'bunHair'::character varying, 'froBun'::character varying, 'bangs'::character varying, 'halfShavedHead'::character varying, 'curlyShortHair'::character varying])::text[])))`; `ck_usuario_avatar_piel: CHECK (((avatar_piel)::text = ANY ((ARRAY['ffe4c0'::character varying, 'f5d7b1'::character varying, 'efcc9f'::character varying, 'e2ba87'::character varying, 'c99c62'::character varying, 'a47539'::character varying, '8c5a2b'::character varying, '643d19'::character varying])::text[])))`

**Índices:** `usuario_email_key`

## meta

Meta que crea un usuario: nombre, tipo, plazo y estado ACTIVA o CUMPLIDA (HU1 a HU4, HU6).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_meta` | integer | No |  | PK |
| `codigo_creador` | integer | No |  | FK → usuario.codigo_usuario (CASCADE) |
| `nombre` | varchar(200) | No |  |  |
| `plazo` | varchar(10) | No |  |  |
| `tipo` | varchar(20) | No |  |  |
| `estado` | varchar(10) | No | `'ACTIVA'::character varying` |  |
| `fecha_creacion` | date | No | `CURRENT_DATE` |  |
| `codigo_union` | varchar(8) | Sí |  |  |
| `codigo_union_expira` | timestamp | Sí |  |  |

**Restricciones CHECK:** `meta_estado_check: CHECK (((estado)::text = ANY ((ARRAY['ACTIVA'::character varying, 'CUMPLIDA'::character varying])::text[])))`; `meta_plazo_check: CHECK (((plazo)::text = ANY ((ARRAY['CORTO'::character varying, 'MEDIANO'::character varying, 'LARGO'::character varying])::text[])))`; `meta_tipo_check: CHECK (((tipo)::text = ANY ((ARRAY['PERSONAL'::character varying, 'PROFESIONAL'::character varying, 'FINANCIERA'::character varying, 'SOCIAL_FAMILIAR'::character varying, 'SALUD_BIENESTAR'::character varying, 'ACADEMICA'::character varying, 'RECREATIVA_OCIO'::character varying, 'OTRO'::character varying])::text[])))`

**Índices:** `idx_meta_creador`, `uq_meta_codigo_union`

## participacion

Relación usuario-meta: creador e invitados, estado de la invitación, fecha de cumplimiento y si la oculta de su perfil (HU6, HU13 a HU15, HU25).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_usuario` | integer | No |  | PK, FK → usuario.codigo_usuario (CASCADE) |
| `codigo_meta` | integer | No |  | PK, FK → meta.codigo_meta (CASCADE) |
| `estado_invitacion` | varchar(10) | No | `'PENDIENTE'::character varying` |  |
| `fecha_cumplida` | date | Sí |  |  |
| `oculta` | boolean | No | `false` |  |
| `codigo_invitador` | integer | Sí |  | FK → usuario.codigo_usuario (SET NULL) |
| `fecha_invitacion` | timestamp | Sí |  |  |
| `token_hash` | varchar(64) | Sí |  |  |
| `token_expira` | timestamp | Sí |  |  |
| `destacada` | boolean | No | `false` |  |

**Restricciones CHECK:** `participacion_estado_invitacion_check: CHECK (((estado_invitacion)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ACEPTADA'::character varying, 'RECHAZADA'::character varying])::text[])))`

**Índices:** `idx_participacion_meta`, `uq_participacion_token`

## sub_meta

Paso del camino de una meta: hito (se logra una vez) o hábito (se repite ciertos días, Sí/No o medible) (HU28, HU5).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_sub_meta` | integer | No |  | PK |
| `codigo_meta` | integer | No |  | FK → meta.codigo_meta (CASCADE) |
| `nombre` | varchar(100) | No |  |  |
| `pregunta` | varchar(200) | Sí |  |  |
| `tipo` | varchar(10) | No |  |  |
| `unidad` | varchar(30) | Sí |  |  |
| `objetivo` | numeric(12,2) | Sí |  |  |
| `tipo_objetivo` | varchar(12) | Sí |  |  |
| `dias` | smallint | No | `127` |  |
| `notas` | varchar(500) | Sí |  |  |
| `fecha_creacion` | date | No | `CURRENT_DATE` |  |
| `fecha_logrado` | date | Sí |  |  |
| `codigo_usuario_logro` | integer | Sí |  | FK → usuario.codigo_usuario (SET NULL) |

**Restricciones CHECK:** `ck_sub_meta_logro: CHECK ((((tipo)::text = 'HITO'::text) OR ((fecha_logrado IS NULL) AND (codigo_usuario_logro IS NULL))))`; `ck_sub_meta_tipo: CHECK (((((tipo)::text = ANY ((ARRAY['SI_NO'::character varying, 'HITO'::character varying])::text[])) AND (unidad IS NULL) AND (objetivo IS NULL) AND (tipo_objetivo IS NULL)) OR (((tipo)::text = 'MEDIBLE'::text) AND (unidad IS NOT NULL) AND (objetivo > (0)::numeric) AND (tipo_objetivo IS NOT NULL))))`; `sub_meta_dias_check: CHECK (((dias >= 1) AND (dias <= 127)))`; `sub_meta_tipo_check: CHECK (((tipo)::text = ANY ((ARRAY['SI_NO'::character varying, 'MEDIBLE'::character varying, 'HITO'::character varying])::text[])))`; `sub_meta_tipo_objetivo_check: CHECK (((tipo_objetivo)::text = ANY ((ARRAY['AL_MENOS'::character varying, 'COMO_MAXIMO'::character varying])::text[])))`

**Índices:** `idx_sub_meta_meta`

## registro_sub_meta

Registro de un hábito en una fecha: hecho/no hecho o el valor medido (HU5, HU29).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_registro` | integer | No |  | PK |
| `codigo_sub_meta` | integer | No |  | FK → sub_meta.codigo_sub_meta (CASCADE) |
| `codigo_usuario` | integer | No |  | FK → usuario.codigo_usuario (CASCADE) |
| `fecha` | date | No |  |  |
| `valor` | numeric(12,2) | No |  |  |

**Únicas:** `uq_registro_sub_meta_dia: UNIQUE (codigo_sub_meta, codigo_usuario, fecha)`

**Restricciones CHECK:** `registro_sub_meta_valor_check: CHECK ((valor >= (0)::numeric))`

**Índices:** `uq_registro_sub_meta_dia`

## insignia

Catálogo de insignias y el criterio para ganarlas (HU11, HU12).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_insignia` | integer | No |  | PK |
| `nombre` | varchar(60) | No |  |  |
| `descripcion` | varchar(150) | No |  |  |
| `icono` | varchar(30) | No |  |  |
| `criterio` | varchar(25) | No |  |  |
| `plazo_requerido` | varchar(10) | Sí |  |  |
| `cantidad_requerida` | integer | No |  |  |

**Únicas:** `insignia_nombre_key: UNIQUE (nombre)`

**Restricciones CHECK:** `ck_insignia_plazo: CHECK ((((criterio)::text = 'METAS_CUMPLIDAS_PLAZO'::text) = (plazo_requerido IS NOT NULL)))`; `insignia_cantidad_requerida_check: CHECK ((cantidad_requerida > 0))`; `insignia_criterio_check: CHECK (((criterio)::text = ANY ((ARRAY['METAS_CUMPLIDAS'::character varying, 'METAS_CUMPLIDAS_PLAZO'::character varying, 'TIPOS_DISTINTOS'::character varying])::text[])))`; `insignia_plazo_requerido_check: CHECK (((plazo_requerido)::text = ANY ((ARRAY['CORTO'::character varying, 'MEDIANO'::character varying, 'LARGO'::character varying])::text[])))`

**Índices:** `insignia_nombre_key`

## usuario_insignia

Insignias que ganó cada usuario y en qué fecha (HU11, HU12).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_usuario` | integer | No |  | PK, FK → usuario.codigo_usuario (CASCADE) |
| `codigo_insignia` | integer | No |  | PK, FK → insignia.codigo_insignia (CASCADE) |
| `fecha_obtenida` | date | No | `CURRENT_DATE` |  |

## logro_compartido

Enlace público de una meta cumplida (HU21).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_logro` | integer | No |  | PK |
| `token` | varchar(16) | No |  |  |
| `codigo_meta` | integer | No |  | FK → meta.codigo_meta (CASCADE) |
| `codigo_usuario` | integer | No |  | FK → usuario.codigo_usuario (CASCADE) |
| `fecha_creacion` | date | No | `CURRENT_DATE` |  |

**Únicas:** `logro_compartido_token_key: UNIQUE (token)`; `uq_logro_meta_usuario: UNIQUE (codigo_meta, codigo_usuario)`

**Índices:** `logro_compartido_token_key`, `uq_logro_meta_usuario`

## solicitud_registro

Solicitud de registro pendiente de autorizar desde el correo (HU8).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_solicitud` | integer | No |  | PK |
| `email` | varchar(255) | No |  |  |
| `token_hash` | varchar(64) | Sí |  |  |
| `fecha_expiracion` | timestamp | No |  |  |
| `fecha_ultimo_envio` | timestamp | No |  |  |
| `bloqueado_hasta` | timestamp | Sí |  |  |
| `sesion_hash` | varchar(64) | Sí |  |  |
| `fecha_autorizacion` | timestamp | Sí |  |  |

**Únicas:** `solicitud_registro_email_key: UNIQUE (email)`; `solicitud_registro_sesion_hash_key: UNIQUE (sesion_hash)`; `solicitud_registro_token_hash_key: UNIQUE (token_hash)`

**Índices:** `solicitud_registro_email_key`, `solicitud_registro_sesion_hash_key`, `solicitud_registro_token_hash_key`

## codigo_recuperacion

Código de 6 dígitos para recuperar la contraseña, guardado como hash (HU33).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_usuario` | integer | No |  | PK, FK → usuario.codigo_usuario (CASCADE) |
| `codigo_hash` | varchar(100) | No |  |  |
| `expira_en` | timestamp | No |  |  |
| `enviado_en` | timestamp | No |  |  |
| `intentos` | integer | No | `0` |  |

**Restricciones CHECK:** `codigo_recuperacion_intentos_check: CHECK ((intentos >= 0))`

## token_revocado

Tokens JWT cerrados con «Cerrar sesión» (HU10).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_token` | integer | No |  | PK |
| `jti` | varchar(64) | No |  |  |
| `fecha_expiracion` | timestamp | No |  |  |

**Únicas:** `token_revocado_jti_key: UNIQUE (jti)`

**Índices:** `idx_token_revocado_expiracion`, `token_revocado_jti_key`

## intento_login

Intentos fallidos de inicio de sesión por correo, para bloquear temporalmente (HU9).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `email` | varchar(255) | No |  | PK |
| `intentos` | integer | No | `0` |  |
| `bloqueado_hasta` | timestamp | Sí |  |  |

## log_actividad

Bitácora de auditoría: quién hizo qué, sobre qué dato y con qué resultado. Solo se puede insertar (RQ8).

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `codigo_log` | integer | No |  | PK |
| `fecha_hora` | timestamptz | No | `now()` |  |
| `codigo_usuario` | integer | Sí |  |  |
| `accion` | varchar(30) | No |  |  |
| `entidad` | varchar(30) | No |  |  |
| `codigo_entidad` | integer | Sí |  |  |
| `resultado` | varchar(10) | No |  |  |
| `detalle` | text | Sí |  |  |

**Restricciones CHECK:** `log_actividad_resultado_check: CHECK (((resultado)::text = ANY ((ARRAY['EXITO'::character varying, 'ERROR'::character varying])::text[])))`

**Índices:** `idx_log_entidad`, `idx_log_fecha`, `idx_log_usuario_fecha`

## flyway_schema_history

Control de Flyway: qué migraciones se aplicaron (V1 a V28). No es parte del modelo.

| Columna | Tipo | Nulo | Por defecto | Clave |
| --- | --- | --- | --- | --- |
| `installed_rank` | integer | No |  | PK |
| `version` | varchar(50) | Sí |  |  |
| `description` | varchar(200) | No |  |  |
| `type` | varchar(20) | No |  |  |
| `script` | varchar(1000) | No |  |  |
| `checksum` | integer | Sí |  |  |
| `installed_by` | varchar(100) | No |  |  |
| `installed_on` | timestamp | No | `now()` |  |
| `execution_time` | integer | No |  |  |
| `success` | boolean | No |  |  |

**Índices:** `flyway_schema_history_pk`, `flyway_schema_history_s_idx`
