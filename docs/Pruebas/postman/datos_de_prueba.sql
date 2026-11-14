-- RQ9: usuarios para correr la colección de Postman/Newman en una base LOCAL o de pruebas.
-- No ejecutar en la base de producción.
-- Contraseña de los dos: Prueba123!  (hash BCrypt, el mismo formato que guarda AuthService)
insert into usuario (nombre, apellido, email, password_hash)
values ('Ana',  'Newman', 'ana.newman@metamorfosis.test',  '$2a$10$Cl05JoxV8ghMZ6yfpTxQquwSvc2ZchfLHIeiiNHPrnW4t31EfPHVC'),
       ('Beto', 'Newman', 'beto.newman@metamorfosis.test', '$2a$10$Cl05JoxV8ghMZ6yfpTxQquwSvc2ZchfLHIeiiNHPrnW4t31EfPHVC')
on conflict (email) do nothing;

-- Cada corrida empieza con los perfiles públicos (por si una corrida anterior se cortó a la mitad).
update usuario set perfil_privado = false where email like '%.newman@metamorfosis.test';
