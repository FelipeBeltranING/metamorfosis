import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * HU13 y HU14: invitaciones a metas compartidas y código para unirse. Lanza Error con `estado`
 * (código HTTP) y `errores`; sin `estado` = sin conexión. `conSesion: false` = sin token.
 */
async function pedir(ruta, { method = 'GET', body, conSesion = true } = {}) {
  const extra = body !== undefined ? { 'Content-Type': 'application/json' } : {};
  const encabezados = conSesion ? cabeceras(extra) : extra;
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method,
      headers: encabezados,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error('No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.');
  }
  if (respuesta.status === 401 && conSesion) throw errorSesionExpirada();
  if (respuesta.status === 204) return null;
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.errores?.[0] ?? datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    error.errores = datos?.errores ?? [];
    throw error;
  }
  return datos;
}

/** HU14: invitaciones pendientes [{ codigoMeta, nombreMeta, tipo, plazo, invitador, participantes, fechaInvitacion }]. */
export const listarInvitaciones = () => pedir('/api/invitaciones');

/** HU14: acepta (true) o rechaza (false). Devuelve { codigoMeta, nombreMeta, aceptada }. */
export const responderInvitacion = (codigoMeta, aceptar) => pedir(`/api/invitaciones/${codigoMeta}`, { method: 'PATCH', body: { aceptar } });

/** HU14: datos del enlace del correo { codigoMeta, nombreMeta, nombreInvitador, paraEmail } (sin sesión). */
export const consultarInvitacionCorreo = (token) => pedir(`/api/invitaciones/correo?${new URLSearchParams({ token })}`, { conSesion: false });

/** HU14: aceptar desde el correo (con la sesión de la cuenta invitada). */
export const aceptarInvitacionCorreo = (token) => pedir('/api/invitaciones/correo/aceptar', { method: 'POST', body: { token } });

/** HU14: rechazar desde el correo (sin sesión). */
export const rechazarInvitacionCorreo = (token) => pedir('/api/invitaciones/correo/rechazar', { method: 'POST', body: { token }, conSesion: false });

/** Código activo para unirse a la meta { codigo, expira } (codigo null = no hay). Solo el creador. */
export const verCodigoMeta = (codigoMeta) => pedir(`/api/metas/${codigoMeta}/codigo`);

/** Genera un código nuevo (el anterior deja de servir). */
export const generarCodigoMeta = (codigoMeta) => pedir(`/api/metas/${codigoMeta}/codigo`, { method: 'POST' });

export const desactivarCodigoMeta = (codigoMeta) => pedir(`/api/metas/${codigoMeta}/codigo`, { method: 'DELETE' });

/** Se une a la meta del código. Devuelve la meta. */
export const unirseConCodigo = (codigo) => pedir('/api/metas/unirse', { method: 'POST', body: { codigo } });

/** "ABCDEFGH" → "ABCD-EFGH". */
export const mostrarCodigo = (codigo) => (codigo ? `${codigo.slice(0, 4)}-${codigo.slice(4)}` : '');
