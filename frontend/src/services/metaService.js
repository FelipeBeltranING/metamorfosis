import { cerrarSesion, obtenerSesion } from './sesionService';

const API_URL = import.meta.env.VITE_API_URL;

const MENSAJE_SESION_EXPIRADA = 'Tu sesión expiró. Inicia sesión de nuevo.';

/** Error 401 de sesión: borra la sesión local para que las rutas protegidas manden al login. */
export function errorSesionExpirada() {
  cerrarSesion();
  const error = new Error(MENSAJE_SESION_EXPIRADA);
  error.estado = 401;
  return error;
}

/** Cabeceras con el token de la sesión vigente (el servidor saca de ahí al usuario). */
export function cabeceras(extra = {}) {
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

/**
 * Lista las metas del usuario (HU3). HU27: `filtros` opcionales { estado, tipo, plazo };
 * sin estado el servidor devuelve las activas. Lanza Error con mensaje legible si falla.
 */
export async function listarMetas(filtros = {}) {
  const encabezados = cabeceras();
  const params = new URLSearchParams(
    Object.entries(filtros).filter(([, valor]) => valor),
  ).toString();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas${params ? `?${params}` : ''}`, { headers: encabezados });
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
 * HU6: marca la meta como cumplida (no se puede deshacer). Devuelve { meta, insigniasNuevas }:
 * la meta actualizada y las insignias que el usuario ganó al cumplirla (HU11; puede venir vacía).
 * Lanza Error con `mensaje` y `estado` (código HTTP); sin `estado` = sin conexión.
 */
export async function marcarCumplida(codigoMeta) {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas/${codigoMeta}/cumplida`, {
      method: 'PATCH',
      headers: encabezados,
    });
  } catch {
    throw new Error('No se pudo marcar la meta. Revisa tu conexión e inténtalo de nuevo');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    throw error;
  }
  if (!datos?.meta) throw new Error('Respuesta inesperada del servidor');
  return { meta: datos.meta, insigniasNuevas: datos.insigniasNuevas ?? [] };
}

/**
 * HU25: oculta (true) o vuelve a mostrar (false) la meta en el perfil del usuario. Devuelve la
 * meta actualizada. Lanza Error con `mensaje` y `estado` (código HTTP); sin `estado` = sin conexión.
 */
export async function cambiarVisibilidad(codigoMeta, oculta) {
  const encabezados = cabeceras({ 'Content-Type': 'application/json' });
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas/${codigoMeta}/visibilidad`, {
      method: 'PATCH',
      headers: encabezados,
      body: JSON.stringify({ oculta }),
    });
  } catch {
    throw new Error('No pudimos cambiar la visibilidad de la meta. Inténtalo de nuevo.');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.errores?.[0] ?? datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    throw error;
  }
  if (!datos?.codigoMeta) throw new Error('Respuesta inesperada del servidor');
  return datos;
}

/**
 * HU13: invita a otra persona a la meta. Devuelve { participante, correoEnviado }: la persona
 * invitada (estado 'PENDIENTE') y si salió el correo con Aceptar y Rechazar.
 * Lanza Error con `mensaje` y `estado` (código HTTP); sin `estado` = sin conexión.
 */
export async function invitarAMeta(codigoMeta, codigoUsuario) {
  const encabezados = cabeceras({ 'Content-Type': 'application/json' });
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas/${codigoMeta}/invitaciones`, {
      method: 'POST',
      headers: encabezados,
      body: JSON.stringify({ codigoUsuario }),
    });
  } catch {
    throw new Error('No se pudo enviar la invitación. Revisa tu conexión e inténtalo de nuevo.');
  }

  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.errores?.[0] ?? datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    throw error;
  }
  if (!datos?.participante?.codigoUsuario) throw new Error('Respuesta inesperada del servidor');
  return datos;
}
