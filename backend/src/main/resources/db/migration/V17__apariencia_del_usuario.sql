-- HU19: apariencia elegida por el usuario. Se guarda en la cuenta para que lo siga en
-- cualquier dispositivo. Los valores válidos los controlan los enums ModoTema y ColorAcento.
alter table usuario add column modo_tema    varchar(10) not null default 'CLARO';
alter table usuario add column color_acento varchar(10) not null default 'VIOLETA';
