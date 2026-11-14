import { t } from '../i18n';
import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * HU-23: petición a /api/usuarios/me con el token de la sesión. Devuelve la cuenta
 * { codigoUsuario, nombre, apellido, email, foto, apariencia }. Lanza Error con `estado` y `errores`
 * si el servidor la rechaza; sin `estado` = sin conexión.
 */
async function pedir(ruta, { method = 'GET', body } = {}) {
  const encabezados = cabeceras(body ? { 'Content-Type': 'application/json' } : {});
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/usuarios/me${ruta}`, {
      method,
      headers: encabezados,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error(t('errores.sinConexion'));
  }
  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? t('errores.inesperado'));
    error.estado = respuesta.status;
    error.errores = datos?.errores ?? [];
    throw error;
  }
  if (!datos || typeof datos.nombre !== 'string') throw new Error(t('errores.respuestaInesperada'));
  return datos;
}

export const obtenerCuenta = () => pedir('');

export const editarDatos = ({ nombre, apellido }) => pedir('', { method: 'PUT', body: { nombre, apellido } });

/** `foto`: data URL ya recortada ("data:image/jpeg;base64,..."). */
export const cambiarFoto = (foto) => pedir('/foto', { method: 'PUT', body: { foto } });

export const quitarFoto = () => pedir('/foto', { method: 'DELETE' });

/** HU19: `apariencia` como la manda el servidor: { modo: 'OSCURO', acento: 'AZUL' }. */
export const cambiarApariencia = (apariencia) => pedir('/apariencia', { method: 'PUT', body: apariencia });

/** HU20: elige la mascota (gratis): 'MARIPOSA', 'MARIQUITA', 'ABEJA', 'LUCIERNAGA', 'LIBELULA' o 'CARACOL'. */
export const cambiarMascota = (mascota) => pedir('/mascota', { method: 'PUT', body: { mascota } });
