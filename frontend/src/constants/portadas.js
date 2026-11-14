// HU19: lienzos de la portada del perfil. Son SVG generados aquí mismo (no imágenes subidas ni
// descargadas): se ven nítidos en cualquier pantalla, pesan poco y son iguales en el tema claro y
// el oscuro. Las estrellas y figuras salen de un generador con semilla fija, así que cada lienzo
// se dibuja siempre igual.
import { t } from '../i18n';

const W = 1200, H = 300;

function rng(semilla) { let s = semilla >>> 0; return () => ((s = (s * 1664525 + 1013904223) >>> 0) / 4294967296); }
const r2 = (n) => Math.round(n * 10) / 10;

function estrellas(r, n, { minR = 0.4, maxR = 1.5, color = '#fff', hasta = H } = {}) {
  let s = '';
  for (let i = 0; i < n; i++) {
    const x = r2(r() * W), y = r2(r() * hasta), rad = r2(minR + r() ** 3 * (maxR - minR)), o = r2(0.35 + r() * 0.65);
    s += `<circle cx="${x}" cy="${y}" r="${rad}" fill="${color}" opacity="${o}"/>`;
  }
  return s;
}
function destello(x, y, t, color = '#fff') {
  return `<g opacity=".95"><circle cx="${x}" cy="${y}" r="${t * 2.2}" fill="${color}" opacity=".18"/>`
    + `<path d="M${x} ${y - t * 4}Q${x} ${y} ${x + t * 4} ${y}Q${x} ${y} ${x} ${y + t * 4}Q${x} ${y} ${x - t * 4} ${y}Q${x} ${y} ${x} ${y - t * 4}Z" fill="${color}"/></g>`;
}
const svg = (defs, cuerpo) => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="xMidYMid slice"><defs>${defs}</defs>${cuerpo}</svg>`;
const blur = (id, d) => `<filter id="${id}" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="${d}"/></filter>`;

function montanas(r, base, alto, picos, color, suave = false) {
  const pasos = picos * 2; let d = `M0 ${H}L0 ${base}`;
  for (let i = 1; i <= pasos; i++) {
    const x = (W / pasos) * i, y = i % 2 ? base - alto * (0.55 + r() * 0.45) : base - alto * (0.1 + r() * 0.25);
    d += suave ? `Q${r2(x - W / pasos / 2)} ${r2(y)} ${r2(x)} ${r2(y)}` : `L${r2(x)} ${r2(y)}`;
  }
  return `<path d="${d}L${W} ${H}Z" fill="${color}"/>`;
}
function ola(y, amp, largo, fase, color, opacidad = 1) {
  let d = `M0 ${H}L0 ${y}`;
  for (let x = 0; x <= W + largo; x += largo / 2) {
    const k = Math.round(x / (largo / 2));
    d += `Q${r2(x + largo / 4)} ${r2(y + (k % 2 ? amp : -amp) + fase)} ${r2(x + largo / 2)} ${y}`;
  }
  return `<path d="${d}L${W} ${H}Z" fill="${color}" opacity="${opacidad}"/>`;
}
function pino(x, base, alto, color) {
  const a = alto, w = a * 0.42;
  return `<path d="M${x} ${base - a}L${x + w * 0.5} ${base - a * 0.55}L${x + w * 0.3} ${base - a * 0.55}L${x + w * 0.75} ${base - a * 0.15}L${x + w * 0.1} ${base - a * 0.15}L${x + w * 0.1} ${base}L${x - w * 0.1} ${base}L${x - w * 0.1} ${base - a * 0.15}L${x - w * 0.75} ${base - a * 0.15}L${x - w * 0.3} ${base - a * 0.55}L${x - w * 0.5} ${base - a * 0.55}Z" fill="${color}"/>`;
}
function mariposa(x, y, t, rot, color, o) {
  return `<g transform="translate(${x} ${y}) rotate(${rot}) scale(${t})" fill="${color}" opacity="${o}">`
    + '<path d="M0 0C-6-22-34-30-40-14-44-2-26 8-8 6-22 12-30 30-16 34-6 36 0 18 0 4Z"/>'
    + '<path d="M0 0C6-22 34-30 40-14 44-2 26 8 8 6 22 12 30 30 16 34 6 36 0 18 0 4Z"/>'
    + '<rect x="-1.2" y="-6" width="2.4" height="30" rx="1.2"/></g>';
}

const DIBUJOS = {
  ESTRELLADA: () => {
    const r = rng(7);
    let destellos = '';
    for (const [x, y, t] of [[180, 70, 3], [470, 40, 2.2], [760, 110, 2.6], [1010, 60, 2], [330, 200, 1.8], [890, 230, 2.2]]) destellos += destello(x, y, t);
    return svg(`<linearGradient id="c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#01030f"/><stop offset=".55" stop-color="#06123a"/><stop offset="1" stop-color="#0f2a6b"/></linearGradient>${blur('b', 30)}${blur('l', 8)}`,
      `<rect width="${W}" height="${H}" fill="url(#c)"/>`
      + `<ellipse cx="620" cy="150" rx="520" ry="46" transform="rotate(-12 620 150)" fill="#8fb4ff" opacity=".13" filter="url(#b)"/>`
      + estrellas(r, 260, { maxR: 1.3 }) + estrellas(r, 18, { minR: 1.4, maxR: 2.2, color: '#dbe7ff' }) + destellos
      + `<circle cx="1080" cy="78" r="46" fill="#fef9c3" opacity=".22" filter="url(#l)"/><circle cx="1080" cy="78" r="30" fill="#fefce8"/><circle cx="1094" cy="70" r="27" fill="#04103a"/>`);
  },
  AURORA: () => {
    const r = rng(11);
    return svg(`<linearGradient id="c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#010409"/><stop offset="1" stop-color="#062a3a"/></linearGradient>${blur('b', 18)}`,
      `<rect width="${W}" height="${H}" fill="url(#c)"/>` + estrellas(r, 150, { maxR: 1.1, hasta: 220 })
      + '<g filter="url(#b)" fill="none" stroke-linecap="round">'
      + '<path d="M-50 150C150 40 300 210 520 110S860 40 1250 140" stroke="#34d399" stroke-width="46" opacity=".75"/>'
      + '<path d="M-50 190C200 110 380 230 600 150S950 90 1250 190" stroke="#22d3ee" stroke-width="30" opacity=".55"/>'
      + '<path d="M-50 100C180 20 360 140 620 60S980 10 1250 80" stroke="#a78bfa" stroke-width="26" opacity=".55"/></g>'
      + montanas(rng(5), 300, 70, 7, '#02101a', true));
  },
  NEBULOSA: () => {
    const r = rng(21);
    return svg(`${blur('b', 40)}${blur('m', 22)}`,
      `<rect width="${W}" height="${H}" fill="#07010f"/>`
      + '<g filter="url(#b)"><circle cx="300" cy="140" r="150" fill="#7c3aed" opacity=".75"/><circle cx="560" cy="200" r="130" fill="#db2777" opacity=".6"/>'
      + '<circle cx="860" cy="110" r="170" fill="#2563eb" opacity=".6"/><circle cx="1080" cy="220" r="110" fill="#c026d3" opacity=".5"/></g>'
      + '<g filter="url(#m)"><circle cx="460" cy="120" r="60" fill="#f9a8d4" opacity=".35"/><circle cx="760" cy="170" r="50" fill="#93c5fd" opacity=".35"/></g>'
      + estrellas(r, 220, { maxR: 1.3 }) + destello(420, 90, 2.4) + destello(940, 200, 2) + destello(140, 230, 1.6));
  },
  ATARDECER: () => svg(`<linearGradient id="c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#4c1d95"/><stop offset=".45" stop-color="#e11d48"/><stop offset=".75" stop-color="#fb923c"/><stop offset="1" stop-color="#fde68a"/></linearGradient>${blur('b', 14)}`,
    `<rect width="${W}" height="${H}" fill="url(#c)"/><circle cx="840" cy="200" r="70" fill="#fef3c7" opacity=".55" filter="url(#b)"/><circle cx="840" cy="200" r="52" fill="#fff7ed"/>`
    + montanas(rng(3), 270, 150, 4, '#9d174d', false) + montanas(rng(8), 290, 110, 6, '#6b21a8') + montanas(rng(13), 305, 70, 9, '#2e1065')),
  OCEANO: () => svg(`<linearGradient id="c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#bae6fd"/><stop offset="1" stop-color="#e0f2fe"/></linearGradient>`,
    `<rect width="${W}" height="${H}" fill="url(#c)"/><circle cx="220" cy="80" r="40" fill="#fff" opacity=".9"/>`
    + ola(150, 14, 260, 0, '#7dd3fc') + ola(185, 16, 300, 4, '#38bdf8') + ola(220, 14, 240, -3, '#0284c7') + ola(255, 12, 320, 2, '#1e3a8a')),
  BOSQUE: () => {
    const r = rng(17); let fondo = '', medio = '', frente = '';
    for (let x = -20; x < W + 40; x += 34 + r() * 20) fondo += pino(r2(x), 250, 90 + r() * 50, '#6ee7b7');
    for (let x = -10; x < W + 40; x += 46 + r() * 26) medio += pino(r2(x), 280, 120 + r() * 60, '#059669');
    for (let x = 0; x < W + 40; x += 70 + r() * 40) frente += pino(r2(x), 310, 160 + r() * 70, '#064e3b');
    return svg(`<linearGradient id="c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fef3c7"/><stop offset=".6" stop-color="#d1fae5"/><stop offset="1" stop-color="#a7f3d0"/></linearGradient>${blur('b', 20)}`,
      `<rect width="${W}" height="${H}" fill="url(#c)"/><circle cx="600" cy="120" r="80" fill="#fffbeb" opacity=".9" filter="url(#b)"/>`
      + fondo + `<rect y="200" width="${W}" height="60" fill="#ecfdf5" opacity=".55" filter="url(#b)"/>` + medio + frente);
  },
  METAMORFOSIS: () => {
    const r = rng(29); let m = '';
    for (let i = 0; i < 16; i++) m += mariposa(r2(40 + r() * 1120), r2(30 + r() * 240), r2(0.35 + r() * 0.75), Math.round(-35 + r() * 70), '#fff', r2(0.12 + r() * 0.3));
    return svg(`<linearGradient id="c" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#5b21b6"/><stop offset=".55" stop-color="#7f22fe"/><stop offset="1" stop-color="#c026d3"/></linearGradient>`,
      `<rect width="${W}" height="${H}" fill="url(#c)"/>`
      + '<path d="M60 250C240 140 380 260 560 170S880 60 1140 120" fill="none" stroke="#fff" stroke-width="2.5" stroke-dasharray="2 10" stroke-linecap="round" opacity=".35"/>'
      + m + mariposa(1140, 110, 1.15, 18, '#fde68a', 0.95));
  },
  LLUVIA: () => {
    const r = rng(41); let c = '';
    for (let i = 0; i < 14; i++) {
      const x = r2(100 + r() * 1100), y = r2(-20 + r() * 200), l = r2(80 + r() * 160);
      c += `<line x1="${x}" y1="${y}" x2="${r2(x - l)}" y2="${r2(y + l * 0.45)}" stroke="url(#e)" stroke-width="${r2(1 + r() * 1.8)}" stroke-linecap="round"/><circle cx="${x}" cy="${y}" r="1.8" fill="#fff"/>`;
    }
    return svg(`<linearGradient id="c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0b0420"/><stop offset="1" stop-color="#3b0764"/></linearGradient><linearGradient id="e" x1="1" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>`,
      `<rect width="${W}" height="${H}" fill="url(#c)"/>` + estrellas(r, 160, { maxR: 1.1 }) + c + montanas(rng(2), 300, 50, 10, '#1e0638', true));
  },
  TOPOGRAFIA: () => {
    let l = '';
    const centros = [[260, 150, '#2dd4bf'], [820, 120, '#a78bfa'], [1120, 260, '#2dd4bf']];
    for (const [cx, cy, col] of centros) for (let k = 1; k <= 11; k++) {
      const rx = k * 26, ry = k * 17; let d = '';
      for (let a = 0; a <= 360; a += 6) {
        const rad = (a * Math.PI) / 180, w = 1 + 0.1 * Math.sin(rad * 3 + k * 0.7) + 0.05 * Math.cos(rad * 5 + k);
        d += `${a ? 'L' : 'M'}${r2(cx + Math.cos(rad) * rx * w)} ${r2(cy + Math.sin(rad) * ry * w)}`;
      }
      l += `<path d="${d}Z" fill="none" stroke="${col}" stroke-width="1.4" stroke-linejoin="round" opacity="${r2(0.75 - k * 0.045)}"/>`;
    }
    return svg('', `<rect width="${W}" height="${H}" fill="#0b1220"/>` + l);
  },
  FIESTA: () => {
    const r = rng(53); const cols = ['#f472b6', '#60a5fa', '#fbbf24', '#a78bfa', '#34d399']; let s = '';
    for (let i = 0; i < 34; i++) {
      const x = r2(r() * W), y = r2(r() * H), c = cols[i % cols.length], t = r2(8 + r() * 16), k = i % 4, rot = Math.round(r() * 360);
      if (k === 0) s += `<circle cx="${x}" cy="${y}" r="${t * 0.6}" fill="${c}"/>`;
      else if (k === 1) s += `<path d="M${x} ${y - t}L${x + t} ${y + t * 0.8}L${x - t} ${y + t * 0.8}Z" fill="none" stroke="${c}" stroke-width="3.5" stroke-linejoin="round" transform="rotate(${rot} ${x} ${y})"/>`;
      else if (k === 2) s += `<path d="M${x - t * 1.5} ${y}q${t * 0.5}-${t * 0.8} ${t} 0t${t} 0t${t} 0" fill="none" stroke="${c}" stroke-width="3.5" stroke-linecap="round" transform="rotate(${rot} ${x} ${y})"/>`;
      else s += `<rect x="${x}" y="${y}" width="${t}" height="${t}" rx="2" fill="${c}" transform="rotate(${rot} ${x} ${y})"/>`;
    }
    return svg('', `<rect width="${W}" height="${H}" fill="#fff7ed"/>` + s);
  },
};

const uri = (dibujo) => `url("data:image/svg+xml;utf8,${encodeURIComponent(dibujo)}")`;
const estilo = (valor) => ({ backgroundImage: uri(DIBUJOS[valor]()), backgroundSize: 'cover', backgroundPosition: 'center' });

// Los valores son los del enum Portada del backend.
export const PORTADAS = [
  { valor: 'NINGUNA', get nombre() { return t('portadas.NINGUNA'); }, estilo: null },
  { valor: 'ESTRELLADA', get nombre() { return t('portadas.ESTRELLADA'); }, estilo: estilo('ESTRELLADA') },
  { valor: 'AURORA', get nombre() { return t('portadas.AURORA'); }, estilo: estilo('AURORA') },
  { valor: 'NEBULOSA', get nombre() { return t('portadas.NEBULOSA'); }, estilo: estilo('NEBULOSA') },
  { valor: 'ATARDECER', get nombre() { return t('portadas.ATARDECER'); }, estilo: estilo('ATARDECER') },
  { valor: 'OCEANO', get nombre() { return t('portadas.OCEANO'); }, estilo: estilo('OCEANO') },
  { valor: 'BOSQUE', get nombre() { return t('portadas.BOSQUE'); }, estilo: estilo('BOSQUE') },
  { valor: 'METAMORFOSIS', get nombre() { return t('portadas.METAMORFOSIS'); }, estilo: estilo('METAMORFOSIS') },
  { valor: 'LLUVIA', get nombre() { return t('portadas.LLUVIA'); }, estilo: estilo('LLUVIA') },
  { valor: 'TOPOGRAFIA', get nombre() { return t('portadas.TOPOGRAFIA'); }, estilo: estilo('TOPOGRAFIA') },
  { valor: 'FIESTA', get nombre() { return t('portadas.FIESTA'); }, estilo: estilo('FIESTA') },
];

export const PORTADA_POR_VALOR = Object.fromEntries(PORTADAS.map((p) => [p.valor, p]));

export const MAXIMO_FRASE = 100;
