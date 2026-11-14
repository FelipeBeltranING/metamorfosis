import { useTranslation } from 'react-i18next';

/**
 * Sub-metas: anillo con el % de constancia (días cumplidos de los que tocaban).
 * Sin porcentaje (aún no hay días que contar) muestra "—".
 */
export default function AnilloConstancia({ porcentaje, tamano = 64, grosor = 8, etiqueta }) {
  const { t } = useTranslation();
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
          ? t('anillo.sinDias')
          : t('anillo.constancia', { porcentaje }))}
      >
        <circle cx={tamano / 2} cy={tamano / 2} r={radio} fill="none" style={{ stroke: 'var(--color-violet-100)' }} strokeWidth={grosor} />
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={radio}
          fill="none"
          style={{ stroke: 'var(--color-violet-600)' }}
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
