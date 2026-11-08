-- HU19: las portadas pasan de degradados a lienzos ilustrados. Quien ya había elegido una queda
-- con el lienzo más parecido; los demás valores (AURORA, OCEANO, BOSQUE, ATARDECER) se conservan.
update usuario set portada = 'METAMORFOSIS' where portada = 'CRISALIDA';
update usuario set portada = 'ESTRELLADA'   where portada = 'NOCHE';
update usuario set portada = 'FIESTA'       where portada = 'CONFETI';
