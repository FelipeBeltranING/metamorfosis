// HU19 (prototipo): apariencia de la app. Un tema es una combinación de modo y color de acento.
const CLAVE = 'metamorfosis.apariencia';

export const ACENTOS = [
  { valor: 'violeta', nombre: 'Violeta', color: '#7f22fe' },
  { valor: 'azul', nombre: 'Azul', color: '#2f6bff' },
  { valor: 'esmeralda', nombre: 'Esmeralda', color: '#10b981' },
  { valor: 'coral', nombre: 'Coral', color: '#f0603c' },
];

export const TEMAS = [
  { valor: 'crisalida', nombre: 'Crisálida', descripcion: 'Claro y violeta, el de siempre', modo: 'claro', acento: 'violeta', fondo: '#f8fafc', tarjeta: '#ffffff', texto: '#111827' },
  { valor: 'noche', nombre: 'Noche', descripcion: 'Negro y azul, descansa la vista', modo: 'oscuro', acento: 'azul', fondo: '#070b14', tarjeta: '#0e1524', texto: '#eef2f7' },
  { valor: 'bosque', nombre: 'Bosque', descripcion: 'Oscuro con verde esmeralda', modo: 'oscuro', acento: 'esmeralda', fondo: '#070b14', tarjeta: '#0e1524', texto: '#eef2f7' },
  { valor: 'amanecer', nombre: 'Amanecer', descripcion: 'Claro y cálido, en coral', modo: 'claro', acento: 'coral', fondo: '#fffaf7', tarjeta: '#ffffff', texto: '#111827' },
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

export function temaDe({ modo, acento }) {
  return TEMAS.find((t) => t.modo === modo && t.acento === acento)?.valor ?? null;
}

