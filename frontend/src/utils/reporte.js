import { PLAZOS, TIPOS } from '../constants/opcionesMeta';
import { nombresMeses } from './fechas';
import { t } from '../i18n';

/** HU7: colores de las gráficas (contraste suficiente sobre blanco y entre sí). */
// HU19 (prototipo): se leen de la paleta activa para que las gráficas sigan el tema.
const css = (v, r) => getComputedStyle(document.documentElement).getPropertyValue(v).trim() || r;
export const COLORES = {
  get cumplidas() { return css('--color-violet-600', '#7C3AED'); },
  get activas() { return css('--color-slate-400', '#94A3B8'); },
  get linea() { return css('--color-emerald-600', '#059669'); },
};


/**
 * Tipos de reporte del selector. El primero es el que se muestra por defecto.
 * `valor` es el que va en la URL (?reporte=...).
 */
export const TIPOS_REPORTE = [
  { valor: 'general', get etiqueta() { return t('reporte.tipos.general'); } },
  { valor: 'por-tipo', get etiqueta() { return t('reporte.tipos.por-tipo'); } },
  { valor: 'por-plazo', get etiqueta() { return t('reporte.tipos.por-plazo'); } },
  { valor: 'por-mes', get etiqueta() { return t('reporte.tipos.por-mes'); } },
];

export const REPORTE_POR_DEFECTO = TIPOS_REPORTE[0].valor;

/** Valor de la URL → tipo de reporte; si falta o no existe, el de por defecto. */
export function tipoReporteValido(valor) {
  return TIPOS_REPORTE.some((x) => x.valor === valor) ? valor : REPORTE_POR_DEFECTO;
}

export const etiquetaTipo = (valor) => TIPOS.find((x) => x.valor === valor)?.etiqueta ?? valor;
export const etiquetaPlazo = (valor) => PLAZOS.find((x) => x.valor === valor)?.etiqueta ?? valor;

/** "2026-11" → "nov 2026". */
export function etiquetaMes(mes) {
  const [anio, numero] = mes.split('-').map(Number);
  return `${nombresMeses()[numero - 1]} ${anio}`;
}

/** Filas de la tabla alternativa de cada gráfica ("Ver datos en tabla"). */
export function tablasDelReporte(reporte) {
  return {
    general: {
      columnas: [t('reporte.tabla.estado'), t('reporte.tabla.metas')],
      filas: [
        [t('reporte.tabla.cumplidas'), reporte.metasCumplidas],
        [t('reporte.tabla.activas'), reporte.metasActivas],
        [t('reporte.tabla.total'), reporte.totalMetas],
      ],
    },
    porTipo: {
      columnas: [t('reporte.tabla.tipo'), t('reporte.tabla.metas'), t('reporte.tabla.cumplidas'), t('reporte.tabla.porcentaje')],
      filas: reporte.porTipo.map((x) => [etiquetaTipo(x.tipo), x.total, x.cumplidas, `${x.porcentaje} %`]),
    },
    porPlazo: {
      columnas: [t('reporte.tabla.plazo'), t('reporte.tabla.activas'), t('reporte.tabla.cumplidas')],
      filas: reporte.porPlazo.map((p) => [etiquetaPlazo(p.plazo), p.activas, p.cumplidas]),
    },
    porMes: {
      columnas: [t('reporte.tabla.mes'), t('reporte.tabla.metasCumplidas')],
      filas: reporte.cumplidasPorMes.map((m) => [etiquetaMes(m.mes), m.cumplidas]),
    },
  };
}
