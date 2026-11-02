/**
 * HU21: arma la imagen (PNG 1200×630) que se comparte al cumplir una meta.
 * Se dibuja en un canvas en el navegador; no necesita servidor.
 */

const ANCHO = 1200;
const ALTO = 630;
const COLORES = {
  fondo: '#7F22FE',      // violet-600
  circulo: '#8E51FF',    // violet-500
  circuloOscuro: '#7008E7', // violet-700
  blanco: '#FFFFFF',
};
const FUENTE = 'Nunito, ui-sans-serif, system-ui, sans-serif';

// Trazos de los íconos de lucide (viewBox 24×24), los mismos que usa la app.
const ICONO_TROFEO = [
  'M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2',
  'M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2',
  'M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3',
  'M4 22h16',
  'M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z',
  'M6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3',
];
const ICONO_BROTE = [
  'M14 9.536V7a4 4 0 0 1 4-4h1.5a.5.5 0 0 1 .5.5V5a4 4 0 0 1-4 4 4 4 0 0 0-4 4c0 2 1 3 1 5a5 5 0 0 1-1 3',
  'M4 9a5 5 0 0 1 8 4 5 5 0 0 1-8-4',
  'M5 21h14',
];

const FORMATO_FECHA_LARGA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });

/** "2026-10-31" → "31 de octubre de 2026" (con la fecha local, sin correr el día). */
export function formatearFechaLarga(iso) {
  if (!iso) return '';
  const [anio, mes, dia] = iso.split('-').map(Number);
  return FORMATO_FECHA_LARGA.format(new Date(anio, mes - 1, dia));
}

function dibujarIcono(ctx, trazos, x, y, tamano, color) {
  ctx.save();
  ctx.translate(x, y);
  ctx.scale(tamano / 24, tamano / 24);
  ctx.strokeStyle = color;
  ctx.lineWidth = 2;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  trazos.forEach((d) => ctx.stroke(new Path2D(d)));
  ctx.restore();
}

function circulo(ctx, x, y, radio, color) {
  ctx.beginPath();
  ctx.arc(x, y, radio, 0, Math.PI * 2);
  ctx.fillStyle = color;
  ctx.fill();
}

/** Parte el texto en líneas que quepan en `anchoMax`; corta con "…" si no cabe en `maxLineas`. */
function partirEnLineas(ctx, texto, anchoMax, maxLineas) {
  const palabras = texto.split(/\s+/);
  const lineas = [];
  let actual = '';
  palabras.forEach((palabra) => {
    const prueba = actual ? `${actual} ${palabra}` : palabra;
    if (ctx.measureText(prueba).width <= anchoMax || !actual) {
      actual = prueba;
    } else {
      lineas.push(actual);
      actual = palabra;
    }
  });
  if (actual) lineas.push(actual);

  if (lineas.length <= maxLineas) return lineas;
  const visibles = lineas.slice(0, maxLineas);
  let ultima = visibles[maxLineas - 1];
  while (ultima.length > 1 && ctx.measureText(`${ultima}…`).width > anchoMax) {
    ultima = ultima.slice(0, -1);
  }
  visibles[maxLineas - 1] = `${ultima.trimEnd()}…`;
  return visibles;
}

/**
 * Genera la tarjeta del logro y devuelve un Blob PNG.
 * @param {{ nombreMeta: string, nombreVisible: string, fechaCumplida: string }} datos
 */
export async function generarTarjetaLogro({ nombreMeta, nombreVisible, fechaCumplida }) {
  // Que el canvas use Nunito y no la fuente de respaldo.
  if (document.fonts?.load) {
    await Promise.all([
      document.fonts.load(`700 64px ${FUENTE}`),
      document.fonts.load(`600 28px ${FUENTE}`),
    ]).catch(() => {});
  }

  const canvas = document.createElement('canvas');
  canvas.width = ANCHO;
  canvas.height = ALTO;
  const ctx = canvas.getContext('2d');

  // Fondo y círculos decorativos.
  ctx.fillStyle = COLORES.fondo;
  ctx.fillRect(0, 0, ANCHO, ALTO);
  circulo(ctx, 845, 690, 190, COLORES.circuloOscuro);
  circulo(ctx, 1035, 170, 330, COLORES.circulo);

  // Trofeo en el círculo blanco.
  circulo(ctx, 905, 315, 150, COLORES.blanco);
  dibujarIcono(ctx, ICONO_TROFEO, 905 - 70, 315 - 70, 140, COLORES.fondo);

  // Logo y nombre de la app.
  ctx.fillStyle = COLORES.blanco;
  ctx.beginPath();
  ctx.roundRect(80, 78, 64, 64, 14);
  ctx.fill();
  dibujarIcono(ctx, ICONO_BROTE, 80 + 16, 78 + 16, 32, COLORES.fondo);
  ctx.textBaseline = 'middle';
  ctx.font = `700 34px ${FUENTE}`;
  ctx.fillText('Metamorfosis', 162, 112);

  // Etiqueta.
  ctx.textBaseline = 'alphabetic';
  ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
  ctx.font = `700 23px ${FUENTE}`;
  if ('letterSpacing' in ctx) ctx.letterSpacing = '3px';
  ctx.fillText('LOGRO CUMPLIDO', 80, 246);
  if ('letterSpacing' in ctx) ctx.letterSpacing = '0px';

  // Nombre de la meta: hasta 2 líneas; si es largo, la letra se reduce.
  const anchoTexto = 640;
  let tamano = 68;
  ctx.font = `700 ${tamano}px ${FUENTE}`;
  let lineas = partirEnLineas(ctx, nombreMeta, anchoTexto, 2);
  if (lineas.length > 1) {
    tamano = 54;
    ctx.font = `700 ${tamano}px ${FUENTE}`;
    lineas = partirEnLineas(ctx, nombreMeta, anchoTexto, 2);
  }
  ctx.fillStyle = COLORES.blanco;
  const altoLinea = tamano * 1.15;
  let y = 248 + altoLinea;
  lineas.forEach((linea) => {
    ctx.fillText(linea, 80, y);
    y += altoLinea;
  });

  // Quién y cuándo.
  ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
  ctx.font = `600 28px ${FUENTE}`;
  const detalle = [nombreVisible, formatearFechaLarga(fechaCumplida)].filter(Boolean).join(' · ');
  ctx.fillText(detalle, 80, y - altoLinea + 62);

  // Frase final.
  ctx.fillStyle = COLORES.blanco;
  ctx.font = `600 23px ${FUENTE}`;
  ctx.fillText('La constancia se construye un día a la vez.', 80, 560);

  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('No se pudo crear la imagen'))), 'image/png');
  });
}
