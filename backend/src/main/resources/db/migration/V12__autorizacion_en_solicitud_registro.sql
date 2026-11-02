-- HU8: la página donde se pidió el registro avanza sola cuando el dueño autoriza desde el correo.
--   * sesion_hash: SHA-256 de un segundo token que solo conoce el navegador que hizo la solicitud;
--     con él consulta el estado y, una vez autorizado, completa el registro.
--   * fecha_autorizacion: cuándo el dueño del correo tocó "Autorizar y continuar" (null = pendiente).
alter table solicitud_registro
    add column sesion_hash        varchar(64) unique,
    add column fecha_autorizacion timestamp;
