const API_URL = import.meta.env.VITE_API_URL;

/**
 * POST JSON a la API. Lanza Error con `mensaje`, `errores` y `estado` (código HTTP)
 * para que la pantalla distinga 400, 401, 409... Sin `estado` = sin conexión.
 */
async function post(ruta, solicitud) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
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
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}

/** Registra un usuario nuevo. 201 -> { codigoUsuario, nombre, apellido, email }. 409 = correo ya registrado. */
export function registrar({ nombre, apellido, email, password }) {
  return post('/api/auth/registro', { nombre, apellido, email, password });
}

/**
 * Inicia sesión. 200 -> { token, codigoUsuario }. 401 = credenciales incorrectas,
 * 400 = datos inválidos (el detalle viene en `errores`).
 */
export async function iniciarSesion({ email, password }) {
  const datos = await post('/api/auth/login', { email, password });
  if (!datos?.token || !datos?.codigoUsuario) throw new Error('Respuesta inesperada del servidor');
  return datos;
}
