/** HU11: tiempo que el aviso de una insignia queda visible antes de cerrarse solo. */
export const DURACION_AVISO_INSIGNIA_MS = 6000;

const PLAZO_EN_TEXTO = { CORTO: 'corto', MEDIANO: 'mediano', LARGO: 'largo' };

/**
 * HU12: cómo se gana una insignia pendiente, según su criterio.
 * Ej.: "Cumple tu primera meta", "Cumple 5 metas", "Cumple 3 metas de corto plazo".
 */
export function comoGanarla({ criterio, plazoRequerido, cantidadRequerida: n }) {
  switch (criterio) {
    case 'METAS_CUMPLIDAS':
      return n === 1 ? 'Cumple tu primera meta' : `Cumple ${n} metas`;
    case 'METAS_CUMPLIDAS_PLAZO': {
      const plazo = PLAZO_EN_TEXTO[plazoRequerido] ?? '';
      return n === 1 ? `Cumple una meta de ${plazo} plazo` : `Cumple ${n} metas de ${plazo} plazo`;
    }
    case 'TIPOS_DISTINTOS':
      return `Cumple metas de ${n} tipos distintos`;
    default:
      return 'Sigue cumpliendo metas';
  }
}

/** HU12: porcentaje entero (0 a 100) para las barras de avance. */
export function porcentajeAvance(avance, total) {
  if (!total) return 0;
  return Math.min(100, Math.round((avance * 100) / total));
}
