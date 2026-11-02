import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * Sub-metas: petición a la API con la sesión. Lanza Error con `estado` (código HTTP) y
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
    throw new Error('No se pudo conectar con el servidor');
  }
  if (respuesta.status === 401) throw errorSesionExpirada();
  if (respuesta.status === 204) return null;
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.errores?.[0] ?? datos?.mensaje ?? 'Error inesperado');
    error.errores = datos?.errores ?? [];
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}

/** HU3: constancia y sub-metas de hoy de cada meta activa. */
export function obtenerResumenes() {
  return pedir('/api/metas/camino-hoy');
}

/** "Tu camino" de una meta: sub-metas con sus últimos 7 días. */
export function obtenerCamino(codigoMeta) {
  return pedir(`/api/metas/${codigoMeta}/camino`);
}

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

export function obtenerEstadisticas(codigoSubMeta) {
  return pedir(`/api/submetas/${codigoSubMeta}/estadisticas`);
}
