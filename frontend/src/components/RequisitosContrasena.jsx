import { Check, Circle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { REQUISITOS_CONTRASENA } from '../utils/contrasena';

/** Lista de requisitos de la contraseña nueva que se marcan en verde mientras se escribe. */
export default function RequisitosContrasena({ valor }) {
  const { t } = useTranslation();
  return (
    <div className="rounded-xl bg-slate-50 p-3.5">
      <p id="requisitos-contrasena" className="text-[13px] font-bold text-gray-700">{t('contrasena.requisitos')}</p>
      <ul aria-labelledby="requisitos-contrasena" className="mt-2 grid gap-1.5 sm:grid-cols-2">
        {REQUISITOS_CONTRASENA.map(({ clave, cumple }) => {
          const ok = cumple(valor);
          return (
            <li key={clave} className={`flex items-center gap-2 text-[13px] font-semibold ${ok ? 'text-emerald-700' : 'text-gray-500'}`}>
              {ok
                ? <Check className="size-4 shrink-0" aria-hidden="true" />
                : <Circle className="size-4 shrink-0" aria-hidden="true" />}
              {t(`contrasena.${clave}`)}
              <span className="sr-only">{ok ? t('contrasena.cumplido') : t('contrasena.pendiente')}</span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
