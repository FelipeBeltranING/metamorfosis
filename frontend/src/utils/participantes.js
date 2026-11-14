import { t } from '../i18n';

/** HU13: "Con Laura y Andrés · 1 invitación pendiente" (sin contar al usuario). */
export function textoParticipantes(participantes, codigoUsuario) {
  const otros = participantes.filter((p) => p.codigoUsuario !== codigoUsuario);
  const aceptados = otros.filter((p) => p.estado === 'ACEPTADA').map((p) => p.nombre);
  const pendientes = otros.filter((p) => p.estado === 'PENDIENTE').length;
  const partes = [];
  if (aceptados.length === 1) partes.push(t('participantes.conUno', { a: aceptados[0] }));
  else if (aceptados.length === 2) partes.push(t('participantes.conDos', { a: aceptados[0], b: aceptados[1] }));
  else if (aceptados.length > 2) partes.push(t('participantes.conVarios', { a: aceptados[0], count: aceptados.length - 1 }));
  if (pendientes) partes.push(t('participantes.pendientes', { count: pendientes }));
  return partes.join(' · ');
}
