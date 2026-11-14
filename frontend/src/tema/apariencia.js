// HU19 (prototipo): apariencia de la app. Un tema es una combinación de modo y color de acento.
import { t } from '../i18n';

const CLAVE = 'metamorfosis.apariencia';

export const ACENTOS = [
  { valor: 'violeta', get nombre() { return t('apariencia.acentos.violeta'); }, color: '#7f22fe' },
  { valor: 'azul', get nombre() { return t('apariencia.acentos.azul'); }, color: '#2f6bff' },
  { valor: 'esmeralda', get nombre() { return t('apariencia.acentos.esmeralda'); }, color: '#10b981' },
  { valor: 'coral', get nombre() { return t('apariencia.acentos.coral'); }, color: '#f0603c' },
];

export const TEMAS = [
  { valor: 'crisalida', get nombre() { return t('apariencia.temas.crisalida.nombre'); }, get descripcion() { return t('apariencia.temas.crisalida.descripcion'); }, modo: 'claro', acento: 'violeta', fondo: '#f8fafc', tarjeta: '#ffffff', texto: '#111827' },
  { valor: 'noche', get nombre() { return t('apariencia.temas.noche.nombre'); }, get descripcion() { return t('apariencia.temas.noche.descripcion'); }, modo: 'oscuro', acento: 'azul', fondo: '#070b14', tarjeta: '#0e1524', texto: '#eef2f7' },
  { valor: 'bosque', get nombre() { return t('apariencia.temas.bosque.nombre'); }, get descripcion() { return t('apariencia.temas.bosque.descripcion'); }, modo: 'oscuro', acento: 'esmeralda', fondo: '#070b14', tarjeta: '#0e1524', texto: '#eef2f7' },
  { valor: 'amanecer', get nombre() { return t('apariencia.temas.amanecer.nombre'); }, get descripcion() { return t('apariencia.temas.amanecer.descripcion'); }, modo: 'claro', acento: 'coral', fondo: '#fffaf7', tarjeta: '#ffffff', texto: '#111827' },
];

export const POR_DEFECTO = { modo: 'claro', acento: 'violeta' };

export function leerApariencia() {
  try {
    return { ...POR_DEFECTO, ...JSON.parse(localStorage.getItem(CLAVE) || '{}') };
  } catch {
    return POR_DEFECTO;
  }
}

export function aplicarApariencia({ modo, acento }, guardar = false) {
  const oscuro = modo === 'oscuro' || (modo === 'automatico' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  document.documentElement.dataset.modo = oscuro ? 'oscuro' : 'claro';
  document.documentElement.dataset.acento = acento;
  if (guardar) {
    try { localStorage.setItem(CLAVE, JSON.stringify({ modo, acento })); } catch { /* sin almacenamiento */ }
  }
  window.dispatchEvent(new Event('metamorfosis:apariencia'));
}

/** Apariencia que llega del servidor ({ modo: 'OSCURO', acento: 'AZUL' }), o la de siempre. */
export function desdeServidor(apariencia) {
  if (!apariencia?.modo || !apariencia?.acento) return POR_DEFECTO;
  return { modo: apariencia.modo.toLowerCase(), acento: apariencia.acento.toLowerCase() };
}

export function haciaServidor({ modo, acento }) {
  return { modo: modo.toUpperCase(), acento: acento.toUpperCase() };
}

export function mismaApariencia(a, b) {
  return a.modo === b.modo && a.acento === b.acento;
}

/** Al cerrar sesión: quien entre después en este navegador empieza con la apariencia de siempre. */
export function reiniciarApariencia() {
  try { localStorage.removeItem(CLAVE); } catch { /* sin almacenamiento */ }
  aplicarApariencia(POR_DEFECTO);
}

/** En modo automático, sigue al dispositivo si cambia de claro a oscuro mientras la app está abierta. */
export function seguirAlDispositivo() {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    const actual = leerApariencia();
    if (actual.modo === 'automatico') aplicarApariencia(actual);
  });
}

export function temaDe({ modo, acento }) {
  return TEMAS.find((t) => t.modo === modo && t.acento === acento)?.valor ?? null;
}

