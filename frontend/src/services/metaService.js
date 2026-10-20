import { cerrarSesion, obtenerSesion } from './sesionService';

const API_URL = import.meta.env.VITE_API_URL;

const MENSAJE_SESION_EXPIRADA = 'Tu sesión expiró. Inicia sesión de nuevo.';

/** Error 401 de sesión: borra la sesión local para que las rutas protegidas manden al login. */
function errorSesionExpirada() {
  cerrarSesion();
  const error = new Error(MENSAJE_SESION_EXPIRADA);
  error.estado = 401;
  return error;
}

/** Cabeceras con el token de la sesión vigente (el servidor saca de ahí al usuario). */
function cabeceras(extra = {}) {
  const sesion = obtenerSesion();
  if (!sesion) throw errorSesionExpirada();
  return { ...extra, Authorization: `Bearer ${sesion.token}` };
}

/** Crea una meta. Lanza Error con `mensaje` y `errores` si el servidor la rechaza. */
export async function crearMeta(solicitud) {
  const encabezados = cabeceras({ 'Content-Type': 'application/json' });
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas`, {
      method: 'POST',
      headers: encabezados,
      body: JSON.stringify(solicitud),
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.errores = datos?.errores ?? [];
    throw error;
  }
  return datos;
}

/** Lista las metas activas del usuario. Lanza Error con mensaje legible si falla. */
export async function listarMetas() {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas`, { headers: encabezados });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) throw new Error(datos?.mensaje ?? 'Error inesperado');
  // Si no llega una lista (p. ej. la petición no llegó a la API), mejor un error que una pantalla rota.
  if (!Array.isArray(datos)) throw new Error('Respuesta inesperada del servidor');
  return datos;
}

/** Elimina una meta (solo su creador puede). Lanza Error con mensaje legible si falla. */
export async function eliminarMeta(codigoMeta) {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas/${codigoMeta}`, {
      method: 'DELETE',
      headers: encabezados,
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  if (!respuesta.ok) {
    const datos = await respuesta.json().catch(() => null);
    throw new Error(datos?.mensaje ?? 'Error inesperado');
  }
  // 204 No Content: no hay cuerpo que leer.
}

/**
 * Edita una meta (solo su creador puede). Lanza Error con `mensaje`, `errores` y `estado`
 * (código HTTP) para que la pantalla distinga 404, 403 y 400. Sin `estado` = sin conexión.
 */
export async function editarMeta(codigoMeta, solicitud) {
  const encabezados = cabeceras({ 'Content-Type': 'application/json' });
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas/${codigoMeta}`, {
      method: 'PUT',
      headers: encabezados,
      body: JSON.stringify(solicitud),
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.errores = datos?.errores ?? [];
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}

/**
 * HU5: registra (o actualiza) el cumplimiento de una meta en una fecha determinada.
 * Lanza Error con `mensaje`, `errores` y `estado` igual que editarMeta.
 */
export async function registrarCumplimiento(codigoMeta, solicitud) {
  const encabezados = cabeceras({ 'Content-Type': 'application/json' });
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas/${codigoMeta}/registros`, {
      method: 'POST',
      headers: encabezados,
      body: JSON.stringify(solicitud),
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.errores = datos?.errores ?? [];
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}
