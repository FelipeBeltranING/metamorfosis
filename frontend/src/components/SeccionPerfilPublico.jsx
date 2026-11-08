import { Check, Eye } from 'lucide-react';
import CabeceraPerfil from './CabeceraPerfil';
import IconoInsignia from './IconoInsignia';
import InsigniaDestacada from './InsigniaDestacada';
import MensajeError from './MensajeError';
import { MAXIMO_FRASE, PORTADAS } from '../constants/portadas';

/**
 * HU19: lo que los demás ven del perfil, con una vista previa ("Así te ven los demás") que
 * cambia mientras se elige. Se guarda con "Guardar cambios", junto con el resto de Editar perfil.
 */
export default function SeccionPerfilPublico({
  valor, onCambiar, vistaPrevia, insignias = [], errores = {},
}) {
  const insigniaElegida = insignias.find((i) => i.codigoInsignia === valor.codigoInsigniaDestacada) ?? null;
  const cambiar = (campo) => (nuevo) => onCambiar({ ...valor, [campo]: nuevo });
  const largo = valor.frase.length;

  return (
    <section aria-labelledby="titulo-perfil-publico" className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
      <div>
        <h2 id="titulo-perfil-publico" className="text-lg font-extrabold">Tu perfil público</h2>
        <p className="mt-1 text-sm text-gray-600">Lo que ven las demás personas cuando abren tu perfil. Se guarda con Guardar cambios.</p>
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-baseline justify-between gap-2">
          <label htmlFor="frase-perfil" className="text-sm font-bold text-gray-700">Frase o lema</label>
          <span className={`text-xs ${largo > MAXIMO_FRASE ? 'font-bold text-red-600' : 'text-gray-500'}`} aria-live="polite">
            {largo} / {MAXIMO_FRASE}
          </span>
        </div>
        <input
          id="frase-perfil"
          value={valor.frase}
          onChange={(e) => cambiar('frase')(e.target.value)}
          placeholder="Por ejemplo: Un libro a la vez"
          maxLength={MAXIMO_FRASE + 20}
          aria-invalid={Boolean(errores.frase)}
          aria-describedby={errores.frase ? 'error-frase-perfil' : undefined}
          className={`h-12 w-full rounded-xl border bg-white px-3.5 text-base text-gray-900 outline-none placeholder:text-gray-400 focus:ring-2 focus:ring-violet-300 ${errores.frase ? 'border-2 border-red-500' : 'border-zinc-300'}`}
        />
        <MensajeError id="error-frase-perfil" texto={errores.frase} />
      </div>

      <fieldset className="flex flex-col gap-2.5">
        <legend className="mb-2.5 text-sm font-bold text-gray-700">Portada</legend>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {PORTADAS.map((p) => {
            const activa = valor.portada === p.valor;
            return (
              <button
                key={p.valor}
                type="button"
                aria-pressed={activa}
                onClick={() => cambiar('portada')(p.valor)}
                className={`relative flex flex-col overflow-hidden rounded-xl border-2 text-left ${activa ? 'border-violet-600' : 'border-zinc-200 hover:border-zinc-300'}`}
              >
                <span
                  className={`h-14 ${p.estilo ? '' : 'flex items-center justify-center bg-slate-100 text-xs font-semibold text-gray-500'}`}
                  style={p.estilo ?? undefined}
                  aria-hidden="true"
                >
                  {!p.estilo && 'Sin portada'}
                </span>
                <span className="px-3 py-2 text-sm font-bold">{p.nombre}</span>
                {activa && (
                  <span className="absolute right-1.5 top-1.5 flex size-6 items-center justify-center rounded-full bg-violet-600 text-white">
                    <Check className="size-4" aria-hidden="true" />
                  </span>
                )}
              </button>
            );
          })}
        </div>
        <MensajeError id="error-portada-perfil" texto={errores.portada} />
      </fieldset>

      <fieldset className="flex flex-col gap-2.5">
        <legend className="mb-1 text-sm font-bold text-gray-700">Insignia destacada</legend>
        <p className="text-[13px] text-gray-500">Se muestra junto a tu nombre.</p>
        {insignias.length === 0 ? (
          <p className="rounded-xl bg-slate-50 p-3 text-sm text-gray-600">Cuando ganes tu primera insignia podrás destacarla aquí.</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {[{ codigoInsignia: null, nombre: 'Ninguna' }, ...insignias].map((i) => {
              const activa = valor.codigoInsigniaDestacada === i.codigoInsignia;
              return (
                <button
                  key={i.codigoInsignia ?? 'ninguna'}
                  type="button"
                  aria-pressed={activa}
                  onClick={() => cambiar('codigoInsigniaDestacada')(i.codigoInsignia)}
                  className={`flex h-10 items-center gap-2 rounded-full border-2 px-3.5 text-sm font-bold ${activa ? 'border-violet-600 bg-violet-50 text-violet-800' : 'border-zinc-200 bg-white text-gray-700 hover:border-zinc-300'}`}
                >
                  {i.codigoInsignia !== null && <IconoInsignia nombre={i.icono} className="size-4 text-violet-600" aria-hidden="true" />}
                  {i.nombre}
                </button>
              );
            })}
          </div>
        )}
        <MensajeError id="error-insignia-perfil" texto={errores.insigniaDestacada} />
      </fieldset>

      {/* Vista previa con lo elegido, antes de guardar */}
      <div className="flex flex-col gap-2.5 rounded-2xl bg-slate-50 p-4">
        <p className="flex items-center gap-2 text-sm font-bold text-gray-700">
          <Eye className="size-4" aria-hidden="true" />
          Así te ven los demás
        </p>
        <CabeceraPerfil
          compacta
          nombre={vistaPrevia.nombre}
          apellido={vistaPrevia.apellido}
          foto={vistaPrevia.foto}
          fechaRegistro={vistaPrevia.fechaRegistro}
          frase={valor.frase.trim().replace(/\s+/g, ' ')}
          portada={valor.portada}
          extraNombre={<InsigniaDestacada insignia={insigniaElegida} />}
        />
        {vistaPrevia.debajo}
      </div>
      <MensajeError id="error-perfil-publico" texto={errores.general} />
    </section>
  );
}
