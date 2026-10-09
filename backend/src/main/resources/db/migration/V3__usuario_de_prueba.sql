-- Usuario de prueba para desarrollo (HU1, HU3) hasta que exista el login real (HU8/HU9).
insert into usuario (nombre, apellido, email, password_hash, codigo_mascota_activa)
select 'Prueba', 'Usuario', 'prueba@metamorfosis.com', 'sin-login', m.codigo_item
from mascota m
where m.es_por_defecto = true
limit 1;