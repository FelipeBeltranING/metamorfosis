const CLAVE_SESION = 'metamorfosis.sesion';

/** Contenido (claims) del JWT, o null si no se puede leer. No verifica la firma: eso lo hace el servidor. */
function leerCarga(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const binario = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
    return JSON.parse(new TextDecoder().decode(Uint8Array.from(binario, (c) => c.charCodeAt(0))));
  } catch {
    return null;
  }
}

/** Fecha de expiración (ms) leída del claim `exp` del JWT, o null si no se puede leer. */
function expiracionDelToken(token) {
  const exp = leerCarga(token)?.exp;
  return typeof exp === 'number' ? exp * 1000 : null;
}

/** Guarda la sesión que devuelve el login: { token, codigoUsuario } y, si vienen, nombre, apellido y email. */
export function guardarSesion({ token, codigoUsuario, nombre, apellido, email }) {
  localStorage.setItem(
    CLAVE_SESION,
    JSON.stringify({ token, codigoUsuario, nombre, apellido, email }),
  );
}

/** Borra la sesión de este navegador. No avisa al servidor (para eso, ver `cerrarSesionRemota`). */
export function cerrarSesion() {
  try {
    localStorage.removeItem(CLAVE_SESION);
  } catch {
    // Sin acceso al almacenamiento: no hay nada que borrar.
  }
}

/** Sesión vigente o null. Si el token ya expiró, la borra. */
export function obtenerSesion() {
  try {
    const guardada = localStorage.getItem(CLAVE_SESION);
    if (!guardada) return null;

    const sesion = JSON.parse(guardada);
    const expira = expiracionDelToken(sesion.token);
    const vencida = expira !== null && expira <= Date.now();
    if (!sesion.token || !sesion.codigoUsuario || vencida) {
      cerrarSesion();
      return null;
    }
    return sesion;
  } catch {
    cerrarSesion();
    return null;
  }
}

/**
 * Datos para mostrar en el menú de perfil: { nombre, email }. El nombre viene de la respuesta
 * del login (puede faltar); el correo, de la respuesta o del claim `email` del token.
 */
export function datosPerfil(sesion) {
  if (!sesion) return null;
  const nombre = [sesion.nombre, sesion.apellido].filter(Boolean).join(' ');
  const email = sesion.email || leerCarga(sesion.token)?.email || '';
  return { nombre, email };
}
