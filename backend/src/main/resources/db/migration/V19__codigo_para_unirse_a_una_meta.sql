-- Unirse a una meta con un código: el creador lo genera, vence y se puede desactivar.
alter table meta add column codigo_union        varchar(8);
alter table meta add column codigo_union_expira timestamp;

create unique index uq_meta_codigo_union on meta (codigo_union);
