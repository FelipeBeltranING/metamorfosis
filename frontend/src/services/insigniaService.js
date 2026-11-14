import { t } from '../i18n';
import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * HU12: galería de insignias del usuario autenticado. Lanza Error con `estado` (código HTTP);
 * sin `estado` = sin conexión.
 */
export async function obtenerGaleria() {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/usuarios/me/insignias`, { headers: encabezados });
  } catch {
    throw new Error(t('errores.sinConexion'));
  }
  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? t('errores.inesperado'));
    error.estado = respuesta.status;
    throw error;
  }
  if (!datos || !Array.isArray(datos.insignias)) {
    throw new Error(t('errores.respuestaInesperada'));
  }
  return datos;
}
