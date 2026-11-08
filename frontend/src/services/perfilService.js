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

/**
 * HU24: perfil público de otra persona (sin correo; solo sus metas visibles). Lanza Error con
 * `estado` (404 si no existe); sin `estado` = sin conexión.
 */
export async function obtenerPerfilPublico(codigoUsuario) {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/usuarios/${codigoUsuario}/perfil`, { headers: encabezados });
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
  // HU26: de un perfil privado el resumen llega en null (solo viene la cabecera).
  if (!datos || (!datos.resumen && !datos.privado) || !Array.isArray(datos.insignias)) {
    throw new Error('Respuesta inesperada del servidor');
  }
  return datos;
}

/**
 * HU26: hace privado (true) o público (false) el perfil. Devuelve { privado }. Lanza Error con
 * `estado` (código HTTP); sin `estado` = sin conexión.
 */
export async function cambiarPrivacidad(privado) {
  const encabezados = cabeceras({ 'Content-Type': 'application/json' });
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/usuarios/me/privacidad`, {
      method: 'PATCH',
      headers: encabezados,
      body: JSON.stringify({ privado }),
    });
  } catch {
    throw new Error('No pudimos cambiar la privacidad. Inténtalo de nuevo.');
  }
  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.errores?.[0] ?? datos?.mensaje ?? 'No pudimos cambiar la privacidad. Inténtalo de nuevo.');
    error.estado = respuesta.status;
    throw error;
  }
  if (typeof datos?.privado !== 'boolean') throw new Error('Respuesta inesperada del servidor');
  return datos;
}
