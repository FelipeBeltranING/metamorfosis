/** Requisitos de una contraseña nueva: los mismos del servidor (ReglaContrasena) y del registro. */
export const REQUISITOS_CONTRASENA = [
  { clave: 'req8', cumple: (v) => v.length >= 8 },
  { clave: 'reqMayuscula', cumple: (v) => /[A-Z]/.test(v) },
  { clave: 'reqMinuscula', cumple: (v) => /[a-z]/.test(v) },
  { clave: 'reqNumero', cumple: (v) => /\d/.test(v) },
];

/** HU32 y HU33: revisa la contraseña nueva y su confirmación; devuelve { nueva, confirmacion } con los errores. */
export function validarContrasenaNueva({ nueva, confirmacion }, t) {
  const errores = {};
  if (!nueva) errores.nueva = t('contrasena.faltaNueva');
  else if (!REQUISITOS_CONTRASENA.every((r) => r.cumple(nueva))) errores.nueva = t('contrasena.noCumple');
  if (nueva && confirmacion !== nueva) errores.confirmacion = t('contrasena.noCoinciden');
  return errores;
}
