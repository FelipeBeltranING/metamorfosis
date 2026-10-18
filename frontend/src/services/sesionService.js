const CLAVE_SESION = 'metamorfosis.sesion';

/** Fecha de expiración (ms) leída del claim `exp` del JWT, o null si no se puede leer. */
function expiracionDelToken(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const carga = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')));
    return typeof carga.exp === 'number' ? carga.exp * 1000 : null;
  } catch {
    return null;
  }
}

/** Guarda la sesión que devuelve el login: { token, codigoUsuario }. */
export function guardarSesion({ token, codigoUsuario }) {
  localStorage.setItem(CLAVE_SESION, JSON.stringify({ token, codigoUsuario }));
}

/** Cierra la sesión local (la usará también el botón de cerrar sesión cuando exista). */
export function cerrarSesion() {
  try {
    localStorage.removeItem(CLAVE_SESION);
  } catch {
    // Sin acceso al almacenamiento: no hay nada que borrar.
  }
}

/** Sesión vigente { token, codigoUsuario } o null. Si el token ya expiró, la borra. */
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
