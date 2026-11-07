import { Check, Monitor, Moon, Sun } from 'lucide-react';
import { ACENTOS, TEMAS, temaDe } from '../tema/apariencia';
import MensajeError from './MensajeError';

const MODOS = [
  { valor: 'claro', nombre: 'Claro', Icono: Sun },
  { valor: 'oscuro', nombre: 'Oscuro', Icono: Moon },
  { valor: 'automatico', nombre: 'Automático', Icono: Monitor },
];

/** HU19: tema, modo y color de acento. Se ve al instante; se guarda con "Guardar cambios". */
export default function SeccionApariencia({ apariencia, onCambiar, error }) {
  const temaActivo = temaDe(apariencia);

  return (
    <section aria-labelledby="titulo-apariencia" className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
      <div>
        <h2 id="titulo-apariencia" className="text-lg font-extrabold">Apariencia</h2>
        <p className="mt-1 text-sm text-gray-600">Elige cómo se ve Metamorfosis. Lo ves al instante y se guarda en tu cuenta al tocar Guardar cambios.</p>
      </div>

      <fieldset className="flex flex-col gap-2.5">
        <legend className="mb-2.5 text-sm font-bold text-gray-700">Temas</legend>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {TEMAS.map((t) => {
            const activo = temaActivo === t.valor;
            const acento = ACENTOS.find((a) => a.valor === t.acento).color;
            return (
              <button
                key={t.valor}
                type="button"
                aria-pressed={activo}
                onClick={() => onCambiar({ modo: t.modo, acento: t.acento })}
                className={`relative flex flex-col overflow-hidden rounded-xl border-2 text-left transition ${activo ? 'border-violet-600' : 'border-zinc-200 hover:border-zinc-300'}`}
              >
                <span className="flex h-20 flex-col gap-1.5 p-2.5" style={{ background: t.fondo }} aria-hidden="true">
                  <span className="flex items-center gap-1.5 rounded-md p-1.5" style={{ background: t.tarjeta }}>
                    <span className="size-3 rounded-full" style={{ background: acento }} />
                    <span className="h-1.5 flex-1 rounded-full" style={{ background: t.texto, opacity: 0.7 }} />
                  </span>
                  <span className="flex gap-1.5">
                    <span className="h-5 flex-1 rounded-md" style={{ background: t.tarjeta }} />
                    <span className="h-5 w-8 rounded-md" style={{ background: acento }} />
                  </span>
                </span>
                <span className="flex flex-col gap-0.5 px-3 py-2.5">
                  <span className="text-[15px] font-extrabold">{t.nombre}</span>
                  <span className="text-[13px] leading-snug text-gray-600">{t.descripcion}</span>
                </span>
                {activo && (
                  <span className="absolute right-2 top-2 flex size-6 items-center justify-center rounded-full bg-violet-600 text-white">
                    <Check className="size-4" aria-hidden="true" />
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </fieldset>

      <div className="flex flex-col gap-5">
        <fieldset>
          <legend className="mb-2.5 text-sm font-bold text-gray-700">Modo</legend>
          <div className="grid grid-cols-3 gap-1 rounded-xl bg-slate-100 p-1 sm:max-w-md">
            {MODOS.map(({ valor, nombre, Icono }) => {
              const activo = apariencia.modo === valor;
              return (
                <button
                  key={valor}
                  type="button"
                  aria-pressed={activo}
                  onClick={() => onCambiar({ ...apariencia, modo: valor })}
                  className={`flex h-10 items-center justify-center gap-1.5 rounded-lg text-sm font-bold ${activo ? 'bg-white text-violet-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'}`}
                >
                  <Icono className="size-4" aria-hidden="true" />
                  {nombre}
                </button>
              );
            })}
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-2.5 text-sm font-bold text-gray-700">Color de acento</legend>
          <div className="flex gap-3">
            {ACENTOS.map((a) => {
              const activo = apariencia.acento === a.valor;
              return (
                <button
                  key={a.valor}
                  type="button"
                  aria-pressed={activo}
                  aria-label={`Acento ${a.nombre}`}
                  title={a.nombre}
                  onClick={() => onCambiar({ ...apariencia, acento: a.valor })}
                  className={`flex size-10 items-center justify-center rounded-full ring-offset-2 ${activo ? 'ring-2 ring-gray-900' : ''}`}
                  style={{ background: a.color, '--tw-ring-offset-color': 'var(--tarjeta, #fff)' }}
                >
                  {activo && <Check className="size-5 text-white" aria-hidden="true" />}
                </button>
              );
            })}
          </div>
        </fieldset>
      </div>
      <MensajeError id="error-apariencia" texto={error} />
    </section>
  );
}
