/**
 * Sub-metas: anillo con el % de constancia (días cumplidos de los que tocaban).
 * Sin porcentaje (aún no hay días que contar) muestra "—".
 */
export default function AnilloConstancia({ porcentaje, tamano = 64, grosor = 8, etiqueta }) {
  const radio = (tamano - grosor) / 2;
  const circunferencia = 2 * Math.PI * radio;
  const valor = porcentaje ?? 0;
  const texto = porcentaje === null || porcentaje === undefined ? '—' : `${porcentaje}%`;
  return (
    <div className="relative shrink-0" style={{ width: tamano, height: tamano }}>
      <svg
        width={tamano}
        height={tamano}
        viewBox={`0 0 ${tamano} ${tamano}`}
        role="img"
        aria-label={etiqueta ?? (porcentaje === null || porcentaje === undefined
          ? 'Aún no hay días para calcular la constancia'
          : `${porcentaje}% de constancia en los últimos 7 días`)}
      >
        <circle cx={tamano / 2} cy={tamano / 2} r={radio} fill="none" stroke="#ede9fe" strokeWidth={grosor} />
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          stroke="#7f22fe"
          strokeWidth={grosor}
          strokeLinecap="round"
          strokeDasharray={`${(circunferencia * valor) / 100} ${circunferencia}`}
          transform={`rotate(-90 ${tamano / 2} ${tamano / 2})`}
        />
      </svg>
      <span
        aria-hidden="true"
        className="absolute inset-0 flex items-center justify-center font-extrabold text-gray-900"
        style={{ fontSize: Math.round(tamano / 4.3) }}
      >
        {texto}
      </span>
    </div>
  );
}
