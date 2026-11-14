import { t } from '../i18n';
import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * El camino de una meta (HU28 hitos y hábitos, HU5 registrar el avance, HU29 avance y constancia):
 * petición a la API con la sesión. Lanza Error con `estado` (código HTTP) y
 * `errores` (lista de validación). Sin `estado` = sin conexión. 204 devuelve null.
 */
async function pedir(ruta, { metodo = 'GET', cuerpo } = {}) {
  const encabezados = cabeceras(cuerpo !== undefined ? { 'Content-Type': 'application/json' } : {});
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method: metodo,
      headers: encabezados,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new Error(t('errores.sinConexion'));
  }
  if (respuesta.status === 401) throw errorSesionExpirada();
  if (respuesta.status === 204) return null;
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.errores?.[0] ?? datos?.mensaje ?? t('errores.inesperado'));
    error.errores = datos?.errores ?? [];
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}

/** HU3: avance (HU29), constancia y hábitos de hoy de cada meta activa. */
export function obtenerResumenes() {
  return pedir('/api/metas/camino-hoy');
}

/** "Tu camino" de una meta: hitos con el avance (HU29) y hábitos con sus últimos 7 días (HU5). */
export function obtenerCamino(codigoMeta) {
  return pedir(`/api/metas/${codigoMeta}/camino`);
}

/** HU28: un paso del camino (para editarlo), con el nombre de su meta y si se puede cambiar. */
export function obtenerPaso(codigoSubMeta) {
  return pedir(`/api/submetas/${codigoSubMeta}`);
}

/** HU28: agrega un hito o un hábito al camino. */
export function crearSubMeta(codigoMeta, datos) {
  return pedir(`/api/metas/${codigoMeta}/submetas`, { metodo: 'POST', cuerpo: datos });
}

export function editarSubMeta(codigoSubMeta, datos) {
  return pedir(`/api/submetas/${codigoSubMeta}`, { metodo: 'PUT', cuerpo: datos });
}

export function eliminarSubMeta(codigoSubMeta) {
  return pedir(`/api/submetas/${codigoSubMeta}`, { metodo: 'DELETE' });
}

/** Registra o corrige un día (fecha AAAA-MM-DD). Sí/No: 1 hecho, 0 no hecho. */
export function registrarDia(codigoSubMeta, fecha, valor) {
  return pedir(`/api/submetas/${codigoSubMeta}/registros/${fecha}`, { metodo: 'PUT', cuerpo: { valor } });
}

export function quitarRegistro(codigoSubMeta, fecha) {
  return pedir(`/api/submetas/${codigoSubMeta}/registros/${fecha}`, { metodo: 'DELETE' });
}

/** HU5: marca un hito como logrado. Responde { hito, avance }. */
export function lograrHito(codigoSubMeta) {
  return pedir(`/api/submetas/${codigoSubMeta}/logro`, { metodo: 'PUT' });
}

/** HU5: devuelve un hito a pendiente. Responde { hito, avance }. */
export function deshacerHito(codigoSubMeta) {
  return pedir(`/api/submetas/${codigoSubMeta}/logro`, { metodo: 'DELETE' });
}

/** HU29: constancia, rachas e historial de un hábito. */
export function obtenerEstadisticas(codigoSubMeta) {
  return pedir(`/api/submetas/${codigoSubMeta}/estadisticas`);
}
