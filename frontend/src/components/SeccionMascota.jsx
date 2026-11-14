import { Check } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import MascotaIlustracion from './MascotaIlustracion';
import { MASCOTAS } from '../constants/mascotas';

/**
 * HU20: sección "Tu mascota" de Editar perfil. Se elige una entre varias opciones gratis; se guarda
 * con el botón del formulario, igual que la apariencia (HU19).
 */
export default function SeccionMascota({ mascota, onCambiar, error }) {
  const { t } = useTranslation();
  return (
    <section aria-labelledby="titulo-mascota" className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
      <div>
        <h2 id="titulo-mascota" className="text-lg font-extrabold">{t('mascota.titulo')}</h2>
        <p className="text-sm text-gray-600">{t('mascota.texto')}</p>
      </div>
      <div role="group" aria-labelledby="titulo-mascota" className="grid grid-cols-3 gap-3 sm:grid-cols-6">
        {MASCOTAS.map((valor) => {
          const activa = mascota === valor;
          return (
            <button
              key={valor}
              type="button"
              aria-pressed={activa}
              onClick={() => onCambiar(valor)}
              className={`relative flex flex-col items-center gap-1.5 rounded-2xl border-2 p-2.5 text-sm font-bold ${
                activa ? 'border-violet-600 bg-violet-50 text-violet-900' : 'border-zinc-200 bg-white text-gray-700 hover:border-violet-300'
              }`}
            >
              <MascotaIlustracion mascota={valor} tamano={56} decorativa />
              {t(`mascota.nombres.${valor}`)}
              {activa && (
                <span className="absolute right-1.5 top-1.5 flex size-5 items-center justify-center rounded-full bg-violet-600 text-white">
                  <Check className="size-3.5" strokeWidth={3} aria-hidden="true" />
                </span>
              )}
            </button>
          );
        })}
      </div>
      {error && <p role="alert" className="text-sm font-bold text-red-700">{error}</p>}
    </section>
  );
}
