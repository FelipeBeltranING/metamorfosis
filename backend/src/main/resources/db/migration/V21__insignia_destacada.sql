-- HU19: la insignia que el usuario eligió mostrar junto a su nombre (una de las que ganó).
alter table usuario add column codigo_insignia_destacada integer
    references insignia(codigo_insignia) on delete set null;
