import { Check, Eye, Pin } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import CabeceraPerfil from './CabeceraPerfil';
import IconoInsignia from './IconoInsignia';
import InsigniaDestacada from './InsigniaDestacada';
import MetasDestacadas from './MetasDestacadas';
import MensajeError from './MensajeError';
import { MAXIMO_FRASE, PORTADAS } from '../constants/portadas';

/**
 * HU19: lo que los demás ven del perfil, con una vista previa ("Así te ven los demás") que
 * cambia mientras se elige. Se guarda con "Guardar cambios", junto con el resto de Editar perfil.
 */
export default function SeccionPerfilPublico({
  valor, onCambiar, vistaPrevia, insignias = [], metas = [], errores = {},
}) {
  const { t } = useTranslation();
  const MAXIMO = 3;
  const elegidas = valor.metasDestacadas;
  const alternar = (codigo) => cambiar('metasDestacadas')(
    elegidas.includes(codigo) ? elegidas.filter((c) => c !== codigo) : [...elegidas, codigo],
  );
  const metasElegidas = metas.filter((m) => elegidas.includes(m.codigoMeta));
  const insigniaElegida = insignias.find((i) => i.codigoInsignia === valor.codigoInsigniaDestacada) ?? null;
  const cambiar = (campo) => (nuevo) => onCambiar({ ...valor, [campo]: nuevo });
  const largo = valor.frase.length;

  return (
    <section aria-labelledby="titulo-perfil-publico" className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
      <div>
        <h2 id="titulo-perfil-publico" className="text-lg font-extrabold">{t('perfilPublicoForm.titulo')}</h2>
        <p className="mt-1 text-sm text-gray-600">{t('perfilPublicoForm.subtitulo')}</p>
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-baseline justify-between gap-2">
          <label htmlFor="frase-perfil" className="text-sm font-bold text-gray-700">{t('perfilPublicoForm.frase')}</label>
          <span className={`text-xs ${largo > MAXIMO_FRASE ? 'font-bold text-red-600' : 'text-gray-500'}`} aria-live="polite">
            {largo} / {MAXIMO_FRASE}
          </span>
        </div>
        <input
          id="frase-perfil"
          value={valor.frase}
          onChange={(e) => cambiar('frase')(e.target.value)}
          placeholder={t('perfilPublicoForm.frasePlaceholder')}
          maxLength={MAXIMO_FRASE + 20}
          aria-invalid={Boolean(errores.frase)}
          aria-describedby={errores.frase ? 'error-frase-perfil' : undefined}
          className={`h-12 w-full rounded-xl border bg-white px-3.5 text-base text-gray-900 outline-none placeholder:text-gray-400 focus:ring-2 focus:ring-violet-300 ${errores.frase ? 'border-2 border-red-500' : 'border-zinc-300'}`}
        />
        <MensajeError id="error-frase-perfil" texto={errores.frase} />
      </div>

      <fieldset className="flex flex-col gap-2.5">
        <legend className="mb-2.5 text-sm font-bold text-gray-700">{t('perfilPublicoForm.portada')}</legend>
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
                  {!p.estilo && t('perfilPublicoForm.sinPortada')}
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
        <legend className="mb-1 text-sm font-bold text-gray-700">{t('perfilPublicoForm.insignia')}</legend>
        <p className="text-[13px] text-gray-500">{t('perfilPublicoForm.insigniaAyuda')}</p>
        {insignias.length === 0 ? (
          <p className="rounded-xl bg-slate-50 p-3 text-sm text-gray-600">{t('perfilPublicoForm.sinInsignias')}</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {[{ codigoInsignia: null, nombre: t('perfilPublicoForm.ninguna') }, ...insignias].map((i) => {
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

      <fieldset className="flex flex-col gap-2.5">
        <legend className="mb-1 text-sm font-bold text-gray-700">{t('perfilPublicoForm.metas')}</legend>
        <p className="text-[13px] text-gray-500">
          {t('perfilPublicoForm.metasAyuda', { max: MAXIMO, elegidas: elegidas.length })}
        </p>
        {metas.length === 0 ? (
          <p className="rounded-xl bg-slate-50 p-3 text-sm text-gray-600">{t('perfilPublicoForm.sinMetas')}</p>
        ) : (
          <ul className="flex max-h-72 flex-col divide-y divide-zinc-100 overflow-y-auto rounded-xl border border-zinc-200">
            {metas.map((m) => {
              const marcada = elegidas.includes(m.codigoMeta);
              const bloqueada = !marcada && elegidas.length >= MAXIMO;
              return (
                <li key={m.codigoMeta}>
                  <label className={`flex items-center gap-3 px-4 py-3 ${bloqueada ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-slate-50'}`}>
                    <input
                      type="checkbox"
                      checked={marcada}
                      disabled={bloqueada}
                      onChange={() => alternar(m.codigoMeta)}
                      className="size-5 shrink-0 accent-violet-600"
                    />
                    <span className="min-w-0 flex-1 truncate text-[15px] font-semibold text-gray-900">{m.nombre}</span>
                    <span className={`shrink-0 rounded-lg px-2 py-0.5 text-xs font-bold ${m.estado === 'CUMPLIDA' ? 'bg-emerald-50 text-emerald-700' : 'bg-violet-50 text-violet-700'}`}>
                      {m.estado === 'CUMPLIDA' ? t('perfilPublicoForm.cumplida') : t('perfilPublicoForm.activa')}
                    </span>
                    {marcada && <Pin className="size-4 shrink-0 text-violet-600" aria-label={t('perfilPublicoForm.destacada')} />}
                  </label>
                </li>
              );
            })}
          </ul>
        )}
        <MensajeError id="error-metas-perfil" texto={errores.metasDestacadas} />
      </fieldset>

      {/* Vista previa con lo elegido, antes de guardar */}
      <div className="flex flex-col gap-2.5 rounded-2xl bg-slate-50 p-4">
        <p className="flex items-center gap-2 text-sm font-bold text-gray-700">
          <Eye className="size-4" aria-hidden="true" />
          {t('perfilPublicoForm.asiTeVen')}
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
        <MetasDestacadas compacta metas={metasElegidas} />
      </div>
      <MensajeError id="error-perfil-publico" texto={errores.general} />
    </section>
  );
}
