const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

/** "2026-11-01" → "1 nov 2026". Se arma con la fecha local para no correr el día por la zona horaria. */
export function formatearFecha(iso) {
  if (!iso) return '';
  const [anio, mes, dia] = iso.split('-').map(Number);
  return FORMATO_FECHA.format(new Date(anio, mes - 1, dia)).replace('.', '');
}
