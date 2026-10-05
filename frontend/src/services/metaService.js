const API_URL = import.meta.env.VITE_API_URL;

// TODO (Sprint de login): el código de usuario saldrá del token JWT.
const CODIGO_USUARIO_TEMPORAL = 1;

/** Crea una meta. Lanza Error con `mensaje` y `errores` si el servidor la rechaza. */
export async function crearMeta(solicitud) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/metas?codigoUsuario=${CODIGO_USUARIO_TEMPORAL}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
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