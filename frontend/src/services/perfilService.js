import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * HU22: Mi perfil del usuario autenticado. Lanza Error con `estado` (código HTTP);
 * sin `estado` = sin conexión.
 */
export async function obtenerPerfil() {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/usuarios/me/perfil`, { headers: encabezados });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }
  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    throw error;
  }
  if (!datos || !datos.resumen || !Array.isArray(datos.insignias)) {
    throw new Error('Respuesta inesperada del servidor');
  }
  return datos;
}
