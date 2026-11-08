/** HU13: "Con Laura y Andrés · 1 invitación pendiente" (sin contar al usuario). */
export function textoParticipantes(participantes, codigoUsuario) {
  const otros = participantes.filter((p) => p.codigoUsuario !== codigoUsuario);
  const aceptados = otros.filter((p) => p.estado === 'ACEPTADA').map((p) => p.nombre);
  const pendientes = otros.filter((p) => p.estado === 'PENDIENTE').length;
  const partes = [];
  if (aceptados.length === 1) partes.push(`Con ${aceptados[0]}`);
  else if (aceptados.length === 2) partes.push(`Con ${aceptados[0]} y ${aceptados[1]}`);
  else if (aceptados.length > 2) partes.push(`Con ${aceptados[0]} y ${aceptados.length - 1} más`);
  if (pendientes) partes.push(`${pendientes} ${pendientes === 1 ? 'invitación pendiente' : 'invitaciones pendientes'}`);
  return partes.join(' · ');
}
