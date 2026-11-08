// HU19: portadas del perfil. Los valores son los del enum Portada del backend; cada una se
// dibuja con CSS (no son imágenes subidas) y se ve igual en el tema claro y en el oscuro.
export const PORTADAS = [
  { valor: 'NINGUNA', nombre: 'Sin portada', estilo: null },
  { valor: 'CRISALIDA', nombre: 'Crisálida', estilo: { background: 'linear-gradient(135deg, #7f22fe 0%, #c084fc 100%)' } },
  { valor: 'AURORA', nombre: 'Aurora', estilo: { background: 'linear-gradient(120deg, #22d3ee 0%, #a78bfa 50%, #f472b6 100%)' } },
  { valor: 'OCEANO', nombre: 'Océano', estilo: { background: 'linear-gradient(135deg, #0ea5e9 0%, #1e3a8a 100%)' } },
  { valor: 'BOSQUE', nombre: 'Bosque', estilo: { background: 'linear-gradient(135deg, #34d399 0%, #065f46 100%)' } },
  { valor: 'ATARDECER', nombre: 'Atardecer', estilo: { background: 'linear-gradient(135deg, #fb923c 0%, #f43f5e 55%, #7c3aed 100%)' } },
  {
    valor: 'NOCHE',
    nombre: 'Noche',
    estilo: {
      backgroundColor: '#0b1026',
      backgroundImage: 'radial-gradient(circle, rgba(255,255,255,.9) 1px, transparent 1.6px), radial-gradient(circle, rgba(255,255,255,.55) 1px, transparent 1.6px), linear-gradient(135deg, #0b1026 0%, #312e81 100%)',
      backgroundSize: '46px 46px, 70px 70px, 100% 100%',
      backgroundPosition: '0 0, 23px 31px, 0 0',
    },
  },
  {
    valor: 'CONFETI',
    nombre: 'Confeti',
    estilo: {
      backgroundColor: '#faf5ff',
      backgroundImage: 'radial-gradient(circle, #f472b6 2.5px, transparent 3px), radial-gradient(circle, #60a5fa 2.5px, transparent 3px), radial-gradient(circle, #fbbf24 2.5px, transparent 3px), radial-gradient(circle, #a78bfa 2.5px, transparent 3px)',
      backgroundSize: '38px 38px, 46px 46px, 54px 54px, 30px 30px',
      backgroundPosition: '0 0, 19px 14px, 8px 30px, 24px 5px',
    },
  },
];

export const PORTADA_POR_VALOR = Object.fromEntries(PORTADAS.map((p) => [p.valor, p]));

export const MAXIMO_FRASE = 100;
