const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto',
  'septiembre', 'octubre', 'noviembre', 'diciembre'];

/** HU22: "Felipe" + "Beltrán" → "FB". Sin datos, "?". */
export function iniciales(nombre, apellido) {
  const letras = [nombre, apellido]
    .map((parte) => (parte ?? '').trim().charAt(0))
    .filter(Boolean)
    .join('');
  return letras ? letras.toLocaleUpperCase('es-CO') : '?';
}

/** HU22: "2026-09-20" → "septiembre de 2026". */
export function mesYAnio(iso) {
  if (!iso) return '';
  const [anio, mes] = iso.split('-').map(Number);
  return `${MESES[mes - 1]} de ${anio}`;
}
