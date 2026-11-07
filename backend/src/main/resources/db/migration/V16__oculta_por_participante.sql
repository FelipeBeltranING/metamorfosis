-- HU25: ocultar una meta del perfil. En una meta compartida cada participante decide en su
-- propio perfil, así que la marca pasa de la meta a la participación. Las metas que ya estaban
-- ocultas quedan ocultas para todos sus participantes.
alter table participacion add column oculta boolean not null default false;

update participacion p
   set oculta = true
  from meta m
 where m.codigo_meta = p.codigo_meta
   and m.oculta;

alter table meta drop column oculta;
