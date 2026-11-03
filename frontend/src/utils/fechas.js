const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

/** "2026-11-01" → "1 nov 2026". Se arma con la fecha local para no correr el día por la zona horaria. */
export function formatearFecha(iso) {
  if (!iso) return '';
  const [anio, mes, dia] = iso.split('-').map(Number);
  return FORMATO_FECHA.format(new Date(anio, mes - 1, dia)).replace('.', '');
}

const DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
const MESES_CORTOS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/** "2026-11-02" → Date local (sin correr el día por la zona horaria). */
export function aFecha(iso) {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return new Date(anio, mes - 1, dia);
}

/** Date → "2026-11-02". */
export function aIso(fecha) {
  const m = String(fecha.getMonth() + 1).padStart(2, '0');
  const d = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${m}-${d}`;
}

/** Sub-metas: "Lun", "Mar"… (lunes primero). */
export function diaCorto(iso) {
  return DIAS_CORTOS[(aFecha(iso).getDay() + 6) % 7];
}

/** "2026-11-02" → "2 nov". */
export function diaYMes(iso) {
  const f = aFecha(iso);
  return `${f.getDate()} ${MESES_CORTOS[f.getMonth()]}`;
}

export const NOMBRES_DIAS = DIAS_CORTOS;
export const NOMBRES_MESES = MESES_CORTOS;
