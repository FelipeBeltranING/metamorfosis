/** Sub-metas: textos que se repiten en varias pantallas. */
import { localeActual, t } from '../i18n';

/** Nombres largos de los días, lunes primero. */
export function diasLargos() {
  return t('fechas.diasLargos', { returnObjects: true });
}

export function formatearNumero(valor) {
  if (valor === null || valor === undefined) return '';
  return new Intl.NumberFormat(localeActual(), { maximumFractionDigits: 2 }).format(Number(valor));
}

/** "Al menos 15 páginas", "Como máximo 60 min" o "Sí / No". */
export function textoObjetivo(subMeta) {
  if (subMeta.tipo !== 'MEDIBLE') return t('subMetas.siNo');
  const clave = subMeta.tipoObjetivo === 'COMO_MAXIMO' ? 'subMetas.comoMaximo' : 'subMetas.alMenos';
  return t(clave, { valor: formatearNumero(subMeta.objetivo), unidad: subMeta.unidad });
}

/** [1..7] -> "diariamente", "lunes a viernes", "lunes, miércoles y viernes". */
export function textoDias(dias) {
  if (!dias || dias.length === 7) return t('subMetas.diariamente');
  const orden = [...dias].sort((a, b) => a - b);
  if (orden.join() === '1,2,3,4,5') return t('subMetas.lunesAViernes');
  if (orden.join() === '6,7') return t('subMetas.finesDeSemana');
  const largos = diasLargos();
  const nombres = orden.map((d) => largos[d - 1]);
  if (nombres.length === 1) return t('subMetas.soloUnDia', { dia: nombres[0] });
  return t('subMetas.variosDias', { dias: nombres.slice(0, -1).join(', '), ultimo: nombres.at(-1) });
}

export function detalleSubMeta(subMeta) {
  return `${textoObjetivo(subMeta)} · ${textoDias(subMeta.dias)}`;
}
