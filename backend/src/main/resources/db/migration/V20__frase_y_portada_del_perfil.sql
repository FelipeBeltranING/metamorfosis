-- HU19: lo que los demás ven del perfil. Una frase corta bajo el nombre y una portada elegida
-- de un catálogo fijo (el enum Portada; no se suben imágenes).
alter table usuario add column frase   varchar(100);
alter table usuario add column portada varchar(12) not null default 'NINGUNA';
