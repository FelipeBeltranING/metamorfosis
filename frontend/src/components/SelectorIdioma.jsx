import { Languages } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { IDIOMAS, idiomaActual } from '../i18n';

/** Selector de idioma (español / inglés). El cambio se ve al instante y se recuerda en este navegador. */
export default function SelectorIdioma({ clase = '' }) {
  const { t, i18n } = useTranslation();
  const actual = idiomaActual();

  return (
    <label className={`relative flex items-center ${clase}`}>
      <span className="sr-only">{t('idioma.etiqueta')}</span>
      <Languages className="pointer-events-none absolute left-2.5 size-4 text-gray-500" aria-hidden="true" />
      <select
        value={actual}
        onChange={(e) => i18n.changeLanguage(e.target.value)}
        className="h-10 cursor-pointer appearance-none rounded-lg border border-zinc-200 bg-white pl-8 pr-3 text-sm font-semibold text-gray-700 hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
      >
        {IDIOMAS.map((i) => (
          <option key={i.valor} value={i.valor} lang={i.valor}>{i.corto} · {i.nombre}</option>
        ))}
      </select>
    </label>
  );
}
