import { useState } from 'react';
import { Check, Shuffle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import AvatarIlustrado from './AvatarIlustrado';
import { PIEZAS_AVATAR, avatarAlAzar, avatarCompleto } from '../constants/avatar';

const clave = (valor) => valor ?? 'ninguno';

/**
 * HU20: sección "Tu avatar" de Editar perfil. Se arma pieza por pieza (piel, peinado, color de pelo,
 * ojos, boca y accesorio); cada opción muestra cómo quedaría. Todo es gratis y se guarda con el
 * botón del formulario, igual que la apariencia (HU19).
 */
export default function SeccionAvatar({ avatar, onCambiar, error }) {
  const { t } = useTranslation();
  const [categoria, setCategoria] = useState(PIEZAS_AVATAR[1].campo);
  const actual = avatarCompleto(avatar);
  const pieza = PIEZAS_AVATAR.find((p) => p.campo === categoria);

  return (
    <section aria-labelledby="titulo-avatar" className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
      <div>
        <h2 id="titulo-avatar" className="text-lg font-extrabold">{t('avatar.titulo')}</h2>
        <p className="text-sm text-gray-600">{t('avatar.texto')}</p>
      </div>

      <div className="flex flex-col gap-5 md:flex-row md:items-start">
        <div className="flex flex-col items-center gap-3 md:w-56 md:shrink-0">
          <div className="flex size-44 items-center justify-center rounded-full bg-violet-100">
            <AvatarIlustrado avatar={actual} className="size-40" alt={t('avatar.vistaPrevia')} />
          </div>
          <button
            type="button"
            onClick={() => onCambiar(avatarAlAzar())}
            className="flex h-10 items-center gap-2 rounded-xl border border-zinc-200 px-4 text-sm font-bold text-gray-700 hover:bg-gray-50"
          >
            <Shuffle className="size-4" aria-hidden="true" />
            {t('avatar.sorprendeme')}
          </button>
        </div>

        <div className="flex min-w-0 flex-1 flex-col gap-4">
          <div role="group" aria-label={t('avatar.categorias')} className="flex flex-wrap gap-2">
            {PIEZAS_AVATAR.map(({ campo }) => (
              <button
                key={campo}
                type="button"
                aria-pressed={categoria === campo}
                onClick={() => setCategoria(campo)}
                className={`h-9 rounded-full border px-3.5 text-sm font-bold ${
                  categoria === campo
                    ? 'border-violet-600 bg-violet-600 text-white'
                    : 'border-zinc-200 bg-white text-gray-700 hover:border-violet-300'
                }`}
              >
                {t(`avatar.categoria.${campo}`)}
              </button>
            ))}
          </div>

          <div
            role="group"
            aria-label={t(`avatar.categoria.${categoria}`)}
            className={pieza.tipo === 'color' ? 'flex flex-wrap gap-3' : 'grid grid-cols-3 gap-2.5 sm:grid-cols-4'}
          >
            {pieza.valores.map((valor) => {
              const activa = actual[categoria] === valor;
              const nombre = t(`avatar.${categoria}.${clave(valor)}`);
              const elegir = () => onCambiar({ ...actual, [categoria]: valor });
              if (pieza.tipo === 'color') {
                return (
                  <button
                    key={valor}
                    type="button"
                    aria-pressed={activa}
                    aria-label={nombre}
                    title={nombre}
                    onClick={elegir}
                    className={`flex size-11 items-center justify-center rounded-full border border-black/10 ${
                      activa ? 'ring-4 ring-violet-500 ring-offset-2' : ''
                    }`}
                    style={{ backgroundColor: `#${valor}` }}
                  >
                    {activa && <Check className="size-5 text-white drop-shadow" strokeWidth={3} aria-hidden="true" />}
                  </button>
                );
              }
              return (
                <button
                  key={clave(valor)}
                  type="button"
                  aria-pressed={activa}
                  onClick={elegir}
                  className={`relative flex flex-col items-center gap-1 rounded-2xl border-2 p-2 text-xs font-bold ${
                    activa ? 'border-violet-600 bg-violet-50 text-violet-900' : 'border-zinc-200 bg-white text-gray-700 hover:border-violet-300'
                  }`}
                >
                  <AvatarIlustrado avatar={{ ...actual, [categoria]: valor }} className="size-20" />
                  <span className="text-center leading-tight">{nombre}</span>
                  {activa && (
                    <span className="absolute right-1.5 top-1.5 flex size-5 items-center justify-center rounded-full bg-violet-600 text-white">
                      <Check className="size-3.5" strokeWidth={3} aria-hidden="true" />
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {error && <p role="alert" className="text-sm font-bold text-red-700">{error}</p>}
      <p className="text-xs text-gray-500">{t('avatar.credito')}</p>
    </section>
  );
}
