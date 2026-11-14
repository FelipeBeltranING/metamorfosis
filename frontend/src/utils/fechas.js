import { localeActual, t } from '../i18n';

/** "2026-11-01" -> "1 nov 2026" (o "Nov 1, 2026" en inglés). Se arma con la fecha local para no correr el día. */
export function formatearFecha(iso) {
  if (!iso) return '';
  const [anio, mes, dia] = iso.split('-').map(Number);
  return new Intl.DateTimeFormat(localeActual(), { day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(anio, mes - 1, dia)).replace('.', '');
}

/** "2026-11-02" -> Date local (sin correr el día por la zona horaria). */
export function aFecha(iso) {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return new Date(anio, mes - 1, dia);
}

/** Date -> "2026-11-02". */
export function aIso(fecha) {
  const m = String(fecha.getMonth() + 1).padStart(2, '0');
  const d = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${m}-${d}`;
}

/** Nombres cortos de los días, lunes primero: ["Lun", "Mar"...] o ["Mon", "Tue"...]. */
export function nombresDias() {
  return t('fechas.diasCortos', { returnObjects: true });
}

/** Nombres cortos de los meses: ["ene", "feb"...] o ["Jan", "Feb"...]. */
export function nombresMeses() {
  return t('fechas.mesesCortos', { returnObjects: true });
}

/** Sub-metas: "Lun", "Mar"... (lunes primero). */
export function diaCorto(iso) {
  return nombresDias()[(aFecha(iso).getDay() + 6) % 7];
}

/** "2026-11-02" -> "2 nov" (o "Nov 2"). */
export function diaYMes(iso) {
  const f = aFecha(iso);
  return t('fechas.diaYMes', { dia: f.getDate(), mes: nombresMeses()[f.getMonth()] });
}
