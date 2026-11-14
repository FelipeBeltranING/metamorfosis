-- HU33: recuperar la contraseña olvidada desde Iniciar sesión. Se manda al correo un código de 6
-- dígitos que vence a los 15 minutos. Cada cuenta tiene a lo sumo un código vigente (pedir otro
-- reemplaza al anterior). El código se guarda cifrado con BCrypt, nunca en claro; intentos cuenta
-- las veces que se escribió mal (a las 5 se borra y hay que pedir otro).
create table codigo_recuperacion (
    codigo_usuario integer      primary key references usuario (codigo_usuario) on delete cascade,
    codigo_hash    varchar(100) not null,
    expira_en      timestamp    not null,
    enviado_en     timestamp    not null,
    intentos       integer      not null default 0 check (intentos >= 0)
);
