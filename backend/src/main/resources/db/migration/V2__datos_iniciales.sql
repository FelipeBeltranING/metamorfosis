-- Mascota por defecto (la reciben todos los usuarios nuevos)
insert into item_personalizacion (nombre, tipo, costo_puntos, recurso)
values ('Mascota por defecto', 'ELEMENTO_VISUAL', 0, 'mascotas/default.png');

insert into mascota (codigo_item, es_por_defecto)
select codigo_item, true
from item_personalizacion
where nombre = 'Mascota por defecto';

-- Una insignia de ejemplo (H10): 5 metas cumplidas de corto plazo
insert into insignia (nombre, descripcion, icono, plazo_requerido, tipo_requerido, cantidad_requerida)
values ('Corredor de corto plazo', 'Cumple 5 metas de corto plazo',
        'insignias/corto5.png', 'CORTO', null, 5);