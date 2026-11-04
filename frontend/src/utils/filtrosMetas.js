import { PLAZOS, TIPOS } from '../constants/opcionesMeta';

/** HU27: opciones del filtro de estado de Mis metas. */
export const ESTADOS_FILTRO = [
  { valor: 'ACTIVA', etiqueta: 'Activas' },
  { valor: 'CUMPLIDA', etiqueta: 'Cumplidas' },
  { valor: 'TODAS', etiqueta: 'Todas' },
];

export const FILTROS_POR_DEFECTO = { estado: 'ACTIVA', tipo: '', plazo: '' };

const ESTADOS_VALIDOS = new Set(ESTADOS_FILTRO.map((e) => e.valor));
const TIPOS_VALIDOS = new Set(TIPOS.map((t) => t.valor));
const PLAZOS_VALIDOS = new Set(PLAZOS.map((p) => p.valor));

/**
 * Lee los filtros de la URL (?estado=&tipo=&plazo=). Un valor que no existe se ignora
 * y queda el de por defecto, así un enlace mal escrito no rompe la página.
 */
export function filtrosDesdeUrl(searchParams) {
  const estado = searchParams.get('estado');
  const tipo = searchParams.get('tipo');
  const plazo = searchParams.get('plazo');
  return {
    estado: ESTADOS_VALIDOS.has(estado) ? estado : FILTROS_POR_DEFECTO.estado,
    tipo: TIPOS_VALIDOS.has(tipo) ? tipo : '',
    plazo: PLAZOS_VALIDOS.has(plazo) ? plazo : '',
  };
}

/** Parámetros de URL para unos filtros; omite los que tienen el valor por defecto. */
export function filtrosAUrl({ estado, tipo, plazo }) {
  const params = {};
  if (estado && estado !== FILTROS_POR_DEFECTO.estado) params.estado = estado;
  if (tipo) params.tipo = tipo;
  if (plazo) params.plazo = plazo;
  return params;
}

/** true si algún filtro es distinto del de por defecto. */
export function hayFiltros(filtros) {
  return Object.keys(filtrosAUrl(filtros)).length > 0;
}
