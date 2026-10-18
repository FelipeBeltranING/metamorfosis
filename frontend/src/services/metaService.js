import { obtenerSesion } from './sesionService';

const API_URL = import.meta.env.VITE_API_URL;

/** Sesión vigente, o Error legible si no hay (p. ej. el token expiró con la pantalla abierta). */
function sesionActiva() {
  const sesion = obtenerSesion();
  if (!sesion) throw new Error('Tu sesión expiró. Inicia sesión de nuevo.');
  return sesion;
}

const autorizacion = (token) => ({ Authorization: `Bearer ${token}` });

// TODO: cuando el backend valide el JWT, quitar el parámetro `codigoUsuario` de las
// peticiones (el servidor lo tomará del token).

/** Crea una meta. Lanza Error con `mensaje` y `errores` si el servidor la rechaza. */
export async function crearMeta(solicitud) {
  const { codigoUsuario, token } = sesionActiva();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas?codigoUsuario=${codigoUsuario}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...autorizacion(token) },
      body: JSON.stringify(solicitud),
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

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
  const { codigoUsuario, token } = sesionActiva();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas?codigoUsuario=${codigoUsuario}`, {
      headers: autorizacion(token),
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) throw new Error(datos?.mensaje ?? 'Error inesperado');
  return datos;
}

/** Elimina una meta (solo su creador puede). Lanza Error con mensaje legible si falla. */
export async function eliminarMeta(codigoMeta) {
  const { codigoUsuario, token } = sesionActiva();
  let respuesta;
  try {
    respuesta = await fetch(
      `${API_URL}/api/metas/${codigoMeta}?codigoUsuario=${codigoUsuario}`,
      { method: 'DELETE', headers: autorizacion(token) },
    );
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

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
  const { codigoUsuario, token } = sesionActiva();
  let respuesta;
  try {
    respuesta = await fetch(
      `${API_URL}/api/metas/${codigoMeta}?codigoUsuario=${codigoUsuario}`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...autorizacion(token) },
        body: JSON.stringify(solicitud),
      },
    );
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }

  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.errores = datos?.errores ?? [];
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}
