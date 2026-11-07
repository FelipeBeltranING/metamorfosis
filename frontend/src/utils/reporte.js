import { PLAZOS, TIPOS } from '../constants/opcionesMeta';
import { NOMBRES_MESES } from './fechas';

/** HU7: colores de las gráficas (contraste suficiente sobre blanco y entre sí). */
// HU19 (prototipo): se leen de la paleta activa para que las gráficas sigan el tema.
const css = (v, r) => getComputedStyle(document.documentElement).getPropertyValue(v).trim() || r;
export const COLORES = {
  get cumplidas() { return css('--color-violet-600', '#7C3AED'); },
  get activas() { return css('--color-slate-400', '#94A3B8'); },
  get linea() { return css('--color-emerald-600', '#059669'); },
};

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t.etiqueta]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p.etiqueta]));

/**
 * Tipos de reporte del selector. El primero es el que se muestra por defecto.
 * `valor` es el que va en la URL (?reporte=...).
 */
export const TIPOS_REPORTE = [
  { valor: 'general', etiqueta: 'Cumplimiento general' },
  { valor: 'por-tipo', etiqueta: 'Cumplimiento por tipo de meta' },
  { valor: 'por-plazo', etiqueta: 'Metas por plazo y estado' },
  { valor: 'por-mes', etiqueta: 'Metas cumplidas por mes' },
];

export const REPORTE_POR_DEFECTO = TIPOS_REPORTE[0].valor;

/** Valor de la URL → tipo de reporte; si falta o no existe, el de por defecto. */
export function tipoReporteValido(valor) {
  return TIPOS_REPORTE.some((t) => t.valor === valor) ? valor : REPORTE_POR_DEFECTO;
}

export const etiquetaTipo = (valor) => TIPO_POR_VALOR[valor] ?? valor;
export const etiquetaPlazo = (valor) => PLAZO_POR_VALOR[valor] ?? valor;

/** "2026-11" → "nov 2026". */
export function etiquetaMes(mes) {
  const [anio, numero] = mes.split('-').map(Number);
  return `${NOMBRES_MESES[numero - 1]} ${anio}`;
}

/** Filas de la tabla alternativa de cada gráfica ("Ver datos en tabla"). */
export function tablasDelReporte(reporte) {
  return {
    general: {
      columnas: ['Estado', 'Metas'],
      filas: [['Cumplidas', reporte.metasCumplidas], ['Activas', reporte.metasActivas], ['Total', reporte.totalMetas]],
    },
    porTipo: {
      columnas: ['Tipo', 'Metas', 'Cumplidas', '% cumplido'],
      filas: reporte.porTipo.map((t) => [etiquetaTipo(t.tipo), t.total, t.cumplidas, `${t.porcentaje} %`]),
    },
    porPlazo: {
      columnas: ['Plazo', 'Activas', 'Cumplidas'],
      filas: reporte.porPlazo.map((p) => [etiquetaPlazo(p.plazo), p.activas, p.cumplidas]),
    },
    porMes: {
      columnas: ['Mes', 'Metas cumplidas'],
      filas: reporte.cumplidasPorMes.map((m) => [etiquetaMes(m.mes), m.cumplidas]),
    },
  };
}
