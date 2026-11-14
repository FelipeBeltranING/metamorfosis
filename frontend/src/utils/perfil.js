import { localeActual, t } from '../i18n';

/** HU22: "Felipe" + "Beltrán" -> "FB". Sin datos, "?". */
export function iniciales(nombre, apellido) {
  const letras = [nombre, apellido]
    .map((parte) => (parte ?? '').trim().charAt(0))
    .filter(Boolean)
    .join('');
  return letras ? letras.toLocaleUpperCase(localeActual()) : '?';
}

/** HU22: "2026-09-20" -> "septiembre de 2026" (o "September 2026"). */
export function mesYAnio(iso) {
  if (!iso) return '';
  const [anio, mes] = iso.split('-').map(Number);
  return t('fechas.mesYAnio', { mes: t('fechas.mesesLargos', { returnObjects: true })[mes - 1], anio });
}
