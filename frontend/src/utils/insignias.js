import { Award, Compass, Flame, Footprints, Mountain, Rocket, Zap } from 'lucide-react';

/** HU11: ícono de cada insignia. El backend manda el nombre del ícono de lucide-react. */
const ICONOS = { Footprints, Flame, Rocket, Zap, Mountain, Compass };

export function iconoInsignia(nombre) {
  return ICONOS[nombre] ?? Award;
}

/** Tiempo que el aviso de una insignia queda visible antes de cerrarse solo. */
export const DURACION_AVISO_INSIGNIA_MS = 6000;
