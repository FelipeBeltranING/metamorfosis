-- HU32: cambiar la contraseña. Se guarda cuándo se cambió por última vez (en UTC) para cerrar las
-- demás sesiones: un token emitido antes de ese momento deja de servir. Las cuentas que nunca la
-- han cambiado quedan en null y sus sesiones siguen como siempre.
alter table usuario add column contrasena_cambiada_en timestamp;
