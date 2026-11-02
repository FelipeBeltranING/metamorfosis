import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * HU21: crea (o reutiliza) el enlace público del logro. 201 -> { token, url, nombreMeta, fechaCumplida }.
 * Lanza Error con `mensaje`, `errores` y `estado` (400 meta no cumplida, 403, 404). Sin `estado` = sin conexión.
 */
export async function crearEnlace(codigoMeta) {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas/${codigoMeta}/compartir`, {
      method: 'POST',
      headers: encabezados,
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
  if (!datos?.url) throw new Error('Respuesta inesperada del servidor');
  return datos;
}

/**
 * HU21: datos públicos del logro (no requiere sesión).
 * 200 -> { nombreMeta, tipo, plazo, fechaCumplida, nombreVisible }; 404 = el enlace no existe.
 */
export async function obtenerLogroPublico(token) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/logros/${encodeURIComponent(token)}`);
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}
