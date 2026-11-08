-- HU19: metas que el usuario fija en su perfil (máximo 3). Como "oculta" (HU25), la marca vive en
-- la participación: en una meta compartida cada participante decide en su propio perfil.
alter table participacion add column destacada boolean not null default false;
