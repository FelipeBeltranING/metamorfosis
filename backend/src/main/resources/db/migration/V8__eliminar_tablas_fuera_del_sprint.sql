-- Se eliminan las tablas y columnas de las épicas que no entran al Sprint 1
-- (insignias, puntos e ítems de personalización).
-- Con "if exists" es seguro en bases donde ya se borraron a mano.
drop table if exists usuario_item;
drop table if exists usuario_insignia;
drop table if exists insignia;
drop table if exists item_personalizacion;
alter table usuario drop column if exists puntos;