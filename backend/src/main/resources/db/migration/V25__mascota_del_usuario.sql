-- HU20 (redefinida): cada usuario elige su mascota entre varias opciones gratis. Ya no hay
-- tienda ni puntos (HU17 y HU18 se retiraron), así que basta una columna en usuario; la tabla
-- mascota que existía en la V1 se eliminó en la V5 y no se vuelve a crear.
-- Todo usuario empieza con la mariposa, la mascota del nombre de la app.
alter table usuario add column mascota varchar(12) not null default 'MARIPOSA';
alter table usuario add constraint ck_usuario_mascota check (
    mascota in ('MARIPOSA', 'MARIQUITA', 'ABEJA', 'LUCIERNAGA', 'LIBELULA', 'CARACOL'));
