import { t } from '../i18n';

/** HU11: tiempo que el aviso de una insignia queda visible antes de cerrarse solo. */
export const DURACION_AVISO_INSIGNIA_MS = 6000;

/**
 * HU12: cómo se gana una insignia pendiente, según su criterio.
 * Ej.: "Cumple tu primera meta", "Cumple 5 metas", "Cumple 3 metas de corto plazo".
 */
export function comoGanarla({ criterio, plazoRequerido, cantidadRequerida: n }) {
  switch (criterio) {
    case 'METAS_CUMPLIDAS':
      return t('insignias.como.metas', { count: n });
    case 'METAS_CUMPLIDAS_PLAZO': {
      const plazo = plazoRequerido ? t(`insignias.plazo.${plazoRequerido}`, { defaultValue: '' }) : '';
      return t('insignias.como.metasPlazo', { count: n, plazo });
    }
    case 'TIPOS_DISTINTOS':
      return t('insignias.como.tipos', { count: n });
    default:
      return t('insignias.como.seguir');
  }
}

/**
 * Nombre y descripción de una insignia en el idioma activo. El catálogo del servidor está en
 * español; se traduce por el ícono (único en cada insignia) y, si no hay traducción, se deja el del servidor.
 */
export function nombreInsignia(insignia) {
  if (!insignia) return '';
  return t(`insignias.catalogo.${insignia.icono}.nombre`, { defaultValue: insignia.nombre });
}

export function descripcionInsignia(insignia) {
  if (!insignia) return '';
  return t(`insignias.catalogo.${insignia.icono}.descripcion`, { defaultValue: insignia.descripcion });
}

/** HU12: porcentaje entero (0 a 100) para las barras de avance. */
export function porcentajeAvance(avance, total) {
  if (!total) return 0;
  return Math.min(100, Math.round((avance * 100) / total));
}
