import { t } from '../i18n';
import { cabeceras, errorSesionExpirada } from './metaService';

const API_URL = import.meta.env.VITE_API_URL;

/**
 * HU7: estadísticas del reporte general. Lanza Error con `estado` (código HTTP);
 * sin `estado` = sin conexión.
 */
export async function obtenerReporte() {
  const encabezados = cabeceras();
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/reportes/resumen`, { headers: encabezados });
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
  if (!datos || !Array.isArray(datos.porTipo)) throw new Error(t('errores.respuestaInesperada'));
  return datos;
}
