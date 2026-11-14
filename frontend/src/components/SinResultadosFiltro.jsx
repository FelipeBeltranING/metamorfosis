import { SearchX } from 'lucide-react';
import { useTranslation } from 'react-i18next';

/** HU27: ninguna meta coincide con los filtros elegidos. */
export default function SinResultadosFiltro({ onLimpiar }) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col items-center gap-5 px-4 py-14 text-center sm:py-20">
      <div className="flex size-20 items-center justify-center rounded-3xl bg-violet-50">
        <SearchX className="size-10 text-violet-600" aria-hidden="true" />
      </div>
      <div className="flex flex-col gap-2">
        <h2 className="text-xl font-bold text-gray-900">{t('sinResultados.titulo')}</h2>
        <p className="text-base text-gray-500">{t('sinResultados.texto')}</p>
      </div>
      <button
        type="button"
        onClick={onLimpiar}
        className="flex h-12 items-center justify-center rounded-xl border border-violet-300 bg-violet-50 px-6 text-base font-bold text-violet-800 hover:bg-violet-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
      >
        {t('sinResultados.limpiar')}
      </button>
    </div>
  );
}
