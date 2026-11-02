const API_URL = import.meta.env.VITE_API_URL;

/**
 * POST JSON a la API. Lanza Error con `mensaje`, `errores` y `estado` (código HTTP)
 * para que la pantalla distinga 400, 401, 409... Sin `estado` = sin conexión.
 */
async function post(ruta, solicitud, token) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
      },
      body: solicitud && JSON.stringify(solicitud),
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

/**
 * HU8 paso 1: pide el correo de autorización. 202 -> { email, fechaExpiracion, sesion }.
 * `sesion` sirve para preguntar con consultarEstado() si el dueño ya autorizó.
 * 409 = correo ya registrado, 429 = reenvío muy seguido o el dueño rechazó el registro.
 */
export function solicitarRegistro(email) {
  return post('/api/auth/registro/solicitar', { email });
}

/** HU8 paso 2: comprueba el enlace del correo. 200 -> { email, fechaExpiracion }; 410 = enlace no válido. */
export async function consultarSolicitud(token) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/auth/registro/solicitud/${encodeURIComponent(token)}`);
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}

/** HU8: botón "Autorizar y continuar" del correo. 200 -> { email, fechaExpiracion }; 410 = enlace no válido. */
export function autorizarRegistro(token) {
  return post('/api/auth/registro/autorizar', { token });
}

/**
 * HU8: la página donde se pidió el registro pregunta si ya se autorizó.
 * 200 -> { estado: 'PENDIENTE' | 'AUTORIZADA' | 'RECHAZADA' | 'VENCIDA' }; 410 = la solicitud ya no existe.
 */
export async function consultarEstado(sesion) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}/api/auth/registro/estado/${encodeURIComponent(sesion)}`);
  } catch {
    throw new Error('No se pudo conectar con el servidor');
  }
  const datos = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje ?? 'Error inesperado');
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}

/** HU8: "No fui yo". 204 sin cuerpo; 410 = enlace no válido. */
export function rechazarRegistro(token) {
  return post('/api/auth/registro/rechazar', { token });
}

/**
 * HU8 paso 3: crea la cuenta con el token del correo (el email sale del token).
 * 201 -> { codigoUsuario, nombre, apellido, email }. 410 = enlace no válido, 409 = correo ya registrado.
 */
export function registrar({ token, nombre, apellido, password }) {
  return post('/api/auth/registro', { token, nombre, apellido, password });
}

/**
 * Inicia sesión. 200 -> { token, codigoUsuario }. 401 = credenciales incorrectas,
 * 400 = datos inválidos (el detalle viene en `errores`), 429 = demasiados intentos
 * fallidos (bloqueo temporal; el mensaje dice cuántos minutos esperar).
 */
export async function iniciarSesion({ email, password }) {
  const datos = await post('/api/auth/login', { email, password });
  if (!datos?.token || !datos?.codigoUsuario) throw new Error('Respuesta inesperada del servidor');
  return datos;
}

/**
 * Cierra la sesión en el servidor (revoca el token). 204 sin cuerpo. 401 si el token ya no sirve
 * (falso, expirado o ya cerrado).
 */
export function cerrarSesionRemota(token) {
  return post('/api/auth/logout', null, token);
}
