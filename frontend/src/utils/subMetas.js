/** Sub-metas: textos que se repiten en varias pantallas. */

const FORMATO_NUMERO = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 });
const LARGOS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];

export function formatearNumero(valor) {
  return valor === null || valor === undefined ? '' : FORMATO_NUMERO.format(Number(valor));
}

/** "Al menos 15 páginas", "Como máximo 60 min" o "Sí / No". */
export function textoObjetivo(subMeta) {
  if (subMeta.tipo !== 'MEDIBLE') return 'Sí / No';
  const prefijo = subMeta.tipoObjetivo === 'COMO_MAXIMO' ? 'Como máximo' : 'Al menos';
  return `${prefijo} ${formatearNumero(subMeta.objetivo)} ${subMeta.unidad}`;
}

/** [1..7] → "diariamente", "lunes a viernes", "lunes, miércoles y viernes". */
export function textoDias(dias) {
  if (!dias || dias.length === 7) return 'diariamente';
  const orden = [...dias].sort((a, b) => a - b);
  if (orden.join() === '1,2,3,4,5') return 'lunes a viernes';
  if (orden.join() === '6,7') return 'fines de semana';
  const nombres = orden.map((d) => LARGOS[d - 1]);
  return nombres.length === 1 ? `solo ${nombres[0]}` : `${nombres.slice(0, -1).join(', ')} y ${nombres.at(-1)}`;
}

export function detalleSubMeta(subMeta) {
  return `${textoObjetivo(subMeta)} · ${textoDias(subMeta.dias)}`;
}

export { LARGOS as DIAS_LARGOS };
