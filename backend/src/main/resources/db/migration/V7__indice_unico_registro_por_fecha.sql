-- HU5: un usuario registra como máximo un cumplimiento por meta y por día.
-- Evita duplicados cuando se reintenta registrar el cumplimiento de la misma fecha.
create unique index uq_registro_meta_usuario_fecha
    on registro (codigo_meta, codigo_usuario, fecha_registro);
