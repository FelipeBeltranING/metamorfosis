import { t } from '../i18n';
import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/** HU24: mínimo de letras para buscar (el servidor aplica la misma regla). */
export const MINIMO_LETRAS = 2;

/**
 * HU24: busca personas por nombre o apellido. Devuelve [{ codigoUsuario, nombre, apellido, foto }]
 * (máximo 20). Lanza Error con `estado` (código HTTP); sin `estado` = sin conexión.
 */
export async function buscarUsuarios(nombre, { signal } = {}) {
  const encabezados = cabeceras();
  const params = new URLSearchParams({ nombre }).toString();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/usuarios?${params}`, { headers: encabezados, signal });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error(t('errores.sinConexion'));
  }
  if (respuesta.status === 401) throw errorSesionExpirada();
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.errores?.[0] ?? datos?.mensaje ?? t('errores.inesperado'));
    error.estado = respuesta.status;
    throw error;
  }
  if (!Array.isArray(datos)) throw new Error(t('errores.respuestaInesperada'));
  return datos;
}
