-- HU28 y HU5 (redefinida): el camino de una meta tiene dos tipos de pasos.
--   * Hábito (SI_NO o MEDIBLE): lo que ya existía; se registra por día y mide la constancia.
--   * Hito (HITO, nuevo): un paso que se logra una sola vez; los hitos logrados sobre el total
--     son el avance real de la meta (HU29). En una meta compartida el hito es del equipo:
--     lo marca cualquier participante y queda quién lo logró.
-- No se borra nada: las sub-metas que ya existen siguen siendo hábitos.

-- 1. El tipo admite HITO (la restricción en línea de V13 se llama sub_meta_tipo_check).
alter table sub_meta drop constraint sub_meta_tipo_check;
alter table sub_meta add constraint sub_meta_tipo_check check (tipo in ('SI_NO', 'MEDIBLE', 'HITO'));

-- 2. Un hito no tiene unidad, objetivo ni tipo de objetivo (igual que un Sí/No).
alter table sub_meta drop constraint ck_sub_meta_tipo;
alter table sub_meta add constraint ck_sub_meta_tipo check (
    (tipo in ('SI_NO', 'HITO') and unidad is null and objetivo is null and tipo_objetivo is null)
    or (tipo = 'MEDIBLE' and unidad is not null and objetivo > 0 and tipo_objetivo is not null));

-- 3. Cuándo se logró el hito y quién lo marcó (si esa persona borra su cuenta, el logro se conserva).
alter table sub_meta add column fecha_logrado date;
alter table sub_meta add column codigo_usuario_logro integer
    references usuario(codigo_usuario) on delete set null;

-- 4. Solo un hito puede estar logrado.
alter table sub_meta add constraint ck_sub_meta_logro check (
    tipo = 'HITO' or (fecha_logrado is null and codigo_usuario_logro is null));
