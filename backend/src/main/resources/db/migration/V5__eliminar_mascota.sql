-- La mascota deja de existir en el modelo.

-- 1. Los usuarios ya no apuntan a una mascota.
alter table usuario drop column if exists codigo_mascota_activa;

-- 2. Se borra el ítem "Mascota por defecto" y las demás mascotas.
do $$
begin
    if to_regclass('mascota') is not null then
        delete from item_personalizacion
        where codigo_item in (select codigo_item from mascota);
    end if;
end $$;

-- 3. Se elimina la tabla (arrastra su índice uq_mascota_por_defecto).
drop table if exists mascota;