create table intento_login (
    email           varchar(255) primary key,
    intentos        integer      not null default 0,
    bloqueado_hasta timestamp
);