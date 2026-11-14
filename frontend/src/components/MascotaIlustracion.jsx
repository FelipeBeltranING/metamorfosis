import { useTranslation } from 'react-i18next';

/**
 * HU20: dibujos de las mascotas (SVG propio, viewBox 64×64). Todas son insectos o bichos pequeños
 * del mundo de la metamorfosis. Los colores son fijos para que se reconozcan en cualquier tema.
 */
const OJOS = (x1, x2, y) => (
  <>
    <circle cx={x1} cy={y} r="2.6" fill="#1e1b2e" />
    <circle cx={x2} cy={y} r="2.6" fill="#1e1b2e" />
    <circle cx={x1 + 0.9} cy={y - 0.9} r="0.9" fill="#fff" />
    <circle cx={x2 + 0.9} cy={y - 0.9} r="0.9" fill="#fff" />
  </>
);
const SONRISA = (x, y) => <path d={`M${x - 4} ${y} q4 4 8 0`} stroke="#1e1b2e" strokeWidth="1.8" fill="none" strokeLinecap="round" />;
const ANTENAS = (y, color = '#1e1b2e') => (
  <g stroke={color} strokeWidth="1.8" fill="none" strokeLinecap="round">
    <path d={`M28 ${y} q-3 -9 -9 -11`} />
    <path d={`M36 ${y} q3 -9 9 -11`} />
    <circle cx="19" cy={y - 11} r="1.8" fill={color} />
    <circle cx="45" cy={y - 11} r="1.8" fill={color} />
  </g>
);

const DIBUJOS = {
  MARIPOSA: (
    <>
      <ellipse cx="18" cy="26" rx="14" ry="13" fill="#a78bfa" />
      <ellipse cx="46" cy="26" rx="14" ry="13" fill="#a78bfa" />
      <ellipse cx="20" cy="45" rx="10" ry="9" fill="#f0abfc" />
      <ellipse cx="44" cy="45" rx="10" ry="9" fill="#f0abfc" />
      <circle cx="17" cy="25" r="4.5" fill="#ede9fe" />
      <circle cx="47" cy="25" r="4.5" fill="#ede9fe" />
      <rect x="27" y="22" width="10" height="32" rx="5" fill="#5b21b6" />
      {ANTENAS(22, '#5b21b6')}
      <circle cx="32" cy="24" r="7.5" fill="#7c3aed" />
      {OJOS(29.4, 34.6, 23.5)}
    </>
  ),
  MARIQUITA: (
    <>
      <circle cx="32" cy="38" r="20" fill="#ef4444" />
      <path d="M32 18 v40" stroke="#1e1b2e" strokeWidth="2" />
      <circle cx="23" cy="33" r="3.6" fill="#1e1b2e" />
      <circle cx="41" cy="33" r="3.6" fill="#1e1b2e" />
      <circle cx="22" cy="45" r="3" fill="#1e1b2e" />
      <circle cx="42" cy="45" r="3" fill="#1e1b2e" />
      {ANTENAS(16)}
      <circle cx="32" cy="18" r="10" fill="#1e1b2e" />
      <circle cx="28.5" cy="17" r="2.6" fill="#fff" />
      <circle cx="35.5" cy="17" r="2.6" fill="#fff" />
      <circle cx="29" cy="17.4" r="1.3" fill="#1e1b2e" />
      <circle cx="36" cy="17.4" r="1.3" fill="#1e1b2e" />
    </>
  ),
  ABEJA: (
    <>
      <ellipse cx="22" cy="20" rx="9" ry="12" fill="#bae6fd" opacity="0.9" transform="rotate(-25 22 20)" />
      <ellipse cx="42" cy="20" rx="9" ry="12" fill="#bae6fd" opacity="0.9" transform="rotate(25 42 20)" />
      <ellipse cx="32" cy="38" rx="20" ry="17" fill="#facc15" />
      <path d="M22 25 q10 -3 20 0 v4 q-10 -3 -20 0z" fill="#1e1b2e" />
      <rect x="13" y="37" width="38" height="5" fill="#1e1b2e" />
      <rect x="15" y="46" width="34" height="4.5" rx="2" fill="#1e1b2e" />
      {OJOS(26, 38, 32)}
      {SONRISA(32, 34.5)}
      <path d="M52 40 l7 -2 -6 5z" fill="#1e1b2e" />
    </>
  ),
  LUCIERNAGA: (
    <>
      <circle cx="32" cy="48" r="14" fill="#fde68a" opacity="0.55" />
      <ellipse cx="32" cy="47" rx="10" ry="10" fill="#facc15" />
      <ellipse cx="21" cy="30" rx="8" ry="11" fill="#c7d2fe" opacity="0.9" transform="rotate(-20 21 30)" />
      <ellipse cx="43" cy="30" rx="8" ry="11" fill="#c7d2fe" opacity="0.9" transform="rotate(20 43 30)" />
      <ellipse cx="32" cy="33" rx="9" ry="12" fill="#334155" />
      {ANTENAS(14)}
      <circle cx="32" cy="19" r="8.5" fill="#475569" />
      <circle cx="29" cy="18.5" r="2.4" fill="#fff" />
      <circle cx="35" cy="18.5" r="2.4" fill="#fff" />
      <circle cx="29.4" cy="19" r="1.2" fill="#1e1b2e" />
      <circle cx="35.4" cy="19" r="1.2" fill="#1e1b2e" />
    </>
  ),
  LIBELULA: (
    <>
      <ellipse cx="18" cy="24" rx="15" ry="5" fill="#99f6e4" opacity="0.9" transform="rotate(-12 18 24)" />
      <ellipse cx="46" cy="24" rx="15" ry="5" fill="#99f6e4" opacity="0.9" transform="rotate(12 46 24)" />
      <ellipse cx="19" cy="33" rx="13" ry="4.5" fill="#5eead4" opacity="0.9" transform="rotate(10 19 33)" />
      <ellipse cx="45" cy="33" rx="13" ry="4.5" fill="#5eead4" opacity="0.9" transform="rotate(-10 45 33)" />
      <rect x="29.5" y="22" width="5" height="38" rx="2.5" fill="#0d9488" />
      <circle cx="32" cy="17" r="8" fill="#14b8a6" />
      <circle cx="27.5" cy="15.5" r="4" fill="#ccfbf1" />
      <circle cx="36.5" cy="15.5" r="4" fill="#ccfbf1" />
      <circle cx="28" cy="16" r="2" fill="#1e1b2e" />
      <circle cx="37" cy="16" r="2" fill="#1e1b2e" />
      {SONRISA(32, 20)}
    </>
  ),
  CARACOL: (
    <>
      <path d="M6 54 q4 -10 16 -10 h30 q6 0 6 6 v4z" fill="#fdba74" />
      <circle cx="30" cy="34" r="17" fill="#c084fc" />
      <path d="M30 34 m0 -10 a10 10 0 1 1 -9.5 13 a6 6 0 1 1 9.5 -7 a2.5 2.5 0 1 1 -2.5 3" stroke="#7e22ce" strokeWidth="2.4" fill="none" strokeLinecap="round" />
      <g stroke="#fb923c" strokeWidth="2" strokeLinecap="round">
        <path d="M51 46 v-14" />
        <path d="M56 46 v-12" />
      </g>
      <circle cx="51" cy="31" r="3" fill="#1e1b2e" />
      <circle cx="56" cy="33" r="3" fill="#1e1b2e" />
      <circle cx="51.8" cy="30.2" r="1" fill="#fff" />
      <circle cx="56.8" cy="32.2" r="1" fill="#fff" />
    </>
  ),
};

export default function MascotaIlustracion({ mascota = 'MARIPOSA', tamano = 64, decorativa = false, className = '' }) {
  const { t } = useTranslation();
  const valor = DIBUJOS[mascota] ? mascota : 'MARIPOSA';
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 64 64"
      className={className}
      role={decorativa ? undefined : 'img'}
      aria-hidden={decorativa ? 'true' : undefined}
      aria-label={decorativa ? undefined : t(`mascota.nombres.${valor}`)}
    >
      {DIBUJOS[valor]}
    </svg>
  );
}
