-- HU13 y HU14: invitar a una meta. La invitación vive en la participación del invitado
-- (estado PENDIENTE). Del enlace del correo solo se guarda el SHA-256 del token, con su vencimiento.
alter table participacion add column codigo_invitador integer references usuario(codigo_usuario) on delete set null;
alter table participacion add column fecha_invitacion timestamp;
alter table participacion add column token_hash       varchar(64);
alter table participacion add column token_expira     timestamp;

create unique index uq_participacion_token on participacion (token_hash);
