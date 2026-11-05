-- HU-23: la foto de perfil se guarda como imagen en data URL
-- ("data:image/jpeg;base64,..."). El navegador la recorta y la reduce antes de subirla,
-- así que pesa pocos KB, pero no cabe en varchar(500).
alter table usuario alter column foto_perfil type text;
