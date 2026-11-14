import { useEffect, useRef, useState } from 'react';
import {
  Check, CircleAlert, CircleCheck, Clock, LoaderCircle, Lock, Search, UserPlus, X,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import AvatarUsuario from './AvatarUsuario';
import CodigoInvitacion from './CodigoInvitacion';
import { buscarUsuarios, MINIMO_LETRAS } from '../services/usuarioService';
import { invitarAMeta } from '../services/metaService';
import { inicialesDe, obtenerSesion } from '../services/sesionService';

function Etiqueta({ tono, Icono, children }) {
  const tonos = {
    creador: 'bg-violet-50 text-violet-700',
    participa: 'bg-emerald-50 text-emerald-700',
    pendiente: 'bg-amber-50 text-amber-700',
  };
  return (
    <span className={`flex shrink-0 items-center gap-1 rounded-lg px-2.5 py-1 text-[13px] font-bold ${tonos[tono]}`}>
      {Icono && <Icono className="size-3.5" aria-hidden="true" />}
      {children}
    </span>
  );
}

function Persona({ persona, detalle, children }) {
  return (
    <li className="flex items-center gap-3 py-2.5">
      <AvatarUsuario foto={persona.foto} iniciales={inicialesDe(persona.nombre, persona.apellido)} clase="size-10 text-sm" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-[15px] font-bold text-gray-900">{persona.nombre} {persona.apellido}</span>
        {detalle && <span className="truncate text-[13px] text-gray-500">{detalle}</span>}
      </span>
      {children}
    </li>
  );
}

/**
 * HU13: invitar personas a una meta. Muestra quiénes están (o están invitados) y un buscador
 * (el de HU24). La persona invitada queda pendiente hasta que acepte (HU14).
 */
export default function InvitarPersonasModal({ meta, onInvitado, onCerrar }) {
  const { t } = useTranslation();
  const yo = obtenerSesion()?.codigoUsuario;
  const [participantes, setParticipantes] = useState(meta.participantes ?? []);
  const [texto, setTexto] = useState('');
  const [busqueda, setBusqueda] = useState({ fase: 'inicial' });
  const [invitando, setInvitando] = useState(null);
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');
  const campo = useRef(null);

  useEffect(() => { campo.current?.focus(); }, []);

  useEffect(() => {
    const limpio = texto.trim();
    if (limpio.length < MINIMO_LETRAS) return undefined;
    const control = new AbortController();
    const espera = setTimeout(() => {
      setBusqueda({ fase: 'buscando' });
      buscarUsuarios(limpio, { signal: control.signal })
        .then((personas) => setBusqueda({ fase: 'lista', personas, texto: limpio }))
        .catch((e) => e.name !== 'AbortError' && setBusqueda({ fase: 'error', mensaje: e.message }));
    }, 250);
    return () => { clearTimeout(espera); control.abort(); };
  }, [texto]);

  useEffect(() => {
    const alPresionar = (e) => e.key === 'Escape' && onCerrar();
    document.addEventListener('keydown', alPresionar);
    return () => document.removeEventListener('keydown', alPresionar);
  }, [onCerrar]);

  const estadoDe = (codigoUsuario) => participantes.find((p) => p.codigoUsuario === codigoUsuario)?.estado;

  const invitar = async (persona) => {
    setInvitando(persona.codigoUsuario);
    setError('');
    setAviso('');
    try {
      const { participante, correoEnviado } = await invitarAMeta(meta.codigoMeta, persona.codigoUsuario);
      const lista = [...participantes.filter((p) => p.codigoUsuario !== participante.codigoUsuario), participante];
      setParticipantes(lista);
      onInvitado?.(lista);
      const nombre = `${persona.nombre} ${persona.apellido}`;
      setAviso(correoEnviado
        ? t('invitar.correoEnviado', { nombre })
        : t('invitar.correoNoEnviado', { nombre }));
    } catch (e) {
      setError(e.message);
    } finally {
      setInvitando(null);
    }
  };

  const ordenados = [...participantes].sort((a, b) => Number(b.creador) - Number(a.creador));

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center sm:p-4"
      onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-invitar"
        className="flex max-h-[92vh] w-full max-w-lg flex-col rounded-t-2xl bg-white shadow-2xl sm:rounded-2xl"
      >
        <div className="flex items-start gap-4 p-6 pb-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-violet-50 text-violet-600">
            <UserPlus className="size-6" aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <h2 id="titulo-invitar" className="text-xl font-bold text-gray-900">{t('invitar.titulo')}</h2>
            <p className="mt-1 truncate text-[15px] font-semibold text-violet-700">{meta.nombre}</p>
          </div>
          <button type="button" onClick={onCerrar} aria-label={t('invitar.cerrarVentana')} className="rounded-lg p-2 text-gray-500 hover:bg-gray-100">
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto px-6 pb-2">
          <p className="text-sm leading-6 text-gray-600">
            {t('invitar.explicacion')}
          </p>

          <div className="flex flex-col gap-2">
            <label htmlFor="buscar-invitar" className="text-sm font-bold text-gray-700">{t('invitar.buscar')}</label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3.5 top-1/2 size-5 -translate-y-1/2 text-gray-400" aria-hidden="true" />
              <input
                ref={campo}
                id="buscar-invitar"
                type="search"
                value={texto}
                onChange={(e) => {
                  setTexto(e.target.value);
                  if (e.target.value.trim().length < MINIMO_LETRAS) setBusqueda({ fase: 'inicial' });
                }}
                placeholder={t('invitar.placeholder')}
                autoComplete="off"
                className="h-12 w-full rounded-xl border border-zinc-300 bg-white pl-11 pr-3.5 text-base text-gray-900 outline-none focus:ring-2 focus:ring-violet-300"
              />
            </div>
            {busqueda.fase === 'inicial' && (
              <p className="text-[13px] text-gray-500">{t('invitar.minimo', { count: MINIMO_LETRAS })}</p>
            )}
          </div>

          {aviso && (
            <p role="status" className="flex items-start gap-2 rounded-xl bg-emerald-50 p-3 text-sm font-semibold leading-5 text-emerald-800">
              <CircleCheck className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              {aviso}
            </p>
          )}
          {error && (
            <p role="alert" className="flex items-start gap-2 rounded-xl bg-red-50 p-3 text-sm font-semibold leading-5 text-red-700">
              <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              {error}
            </p>
          )}

          {busqueda.fase === 'buscando' && (
            <p className="flex items-center gap-2 text-sm text-gray-600">
              <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              {t('invitar.buscando')}
            </p>
          )}
          {busqueda.fase === 'error' && <p role="alert" className="text-sm font-semibold text-red-600">{busqueda.mensaje}</p>}
          {busqueda.fase === 'lista' && busqueda.personas.length === 0 && (
            <p className="rounded-xl bg-slate-50 p-4 text-center text-sm text-gray-600">
              {t('invitar.sinResultados', { texto: busqueda.texto })}
            </p>
          )}
          {busqueda.fase === 'lista' && busqueda.personas.length > 0 && (
            <ul aria-label={t('invitar.resultados')} className="-my-2.5 flex flex-col divide-y divide-zinc-100">
              {busqueda.personas.map((p) => {
                const estado = estadoDe(p.codigoUsuario);
                return (
                  <Persona
                    key={p.codigoUsuario}
                    persona={p}
                    detalle={p.privado ? <span className="inline-flex items-center gap-1"><Lock className="size-3" aria-hidden="true" />{t('invitar.perfilPrivado')}</span> : null}
                  >
                    {estado === 'ACEPTADA' && <Etiqueta tono="participa" Icono={Check}>{t('invitar.participa')}</Etiqueta>}
                    {estado === 'PENDIENTE' && <Etiqueta tono="pendiente" Icono={Clock}>{t('invitar.invitado')}</Etiqueta>}
                    {!estado && (
                      <button
                        type="button"
                        onClick={() => invitar(p)}
                        disabled={invitando !== null}
                        aria-label={t('invitar.invitarA', { nombre: `${p.nombre} ${p.apellido}` })}
                        className="flex h-10 shrink-0 items-center gap-1.5 rounded-xl border border-violet-300 bg-white px-3.5 text-sm font-bold text-violet-700 hover:bg-violet-50 disabled:opacity-60"
                      >
                        {invitando === p.codigoUsuario
                          ? <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                          : <UserPlus className="size-4" aria-hidden="true" />}
                        {invitando === p.codigoUsuario ? t('invitar.invitando') : t('invitar.invitarBoton')}
                      </button>
                    )}
                  </Persona>
                );
              })}
            </ul>
          )}

          <CodigoInvitacion codigoMeta={meta.codigoMeta} />

          <div className="flex flex-col gap-1 border-t border-zinc-100 pt-4">
            <h3 className="text-sm font-bold text-gray-700">{t('invitar.enEstaMeta', { count: participantes.filter((p) => p.estado === 'ACEPTADA').length })}</h3>
            <ul className="flex flex-col divide-y divide-zinc-100">
              {ordenados.map((p) => (
                <Persona key={p.codigoUsuario} persona={p} detalle={p.codigoUsuario === yo ? t('invitar.tu') : null}>
                  {p.creador && <Etiqueta tono="creador">{t('invitar.creador')}</Etiqueta>}
                  {!p.creador && p.estado === 'ACEPTADA' && <Etiqueta tono="participa" Icono={Check}>{t('invitar.participa')}</Etiqueta>}
                  {p.estado === 'PENDIENTE' && <Etiqueta tono="pendiente" Icono={Clock}>{t('invitar.pendiente')}</Etiqueta>}
                </Persona>
              ))}
            </ul>
          </div>
        </div>

        <div className="border-t border-zinc-100 p-4 sm:px-6">
          <button
            type="button"
            onClick={onCerrar}
            className="flex h-12 w-full items-center justify-center rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700"
          >
            {t('invitar.listo')}
          </button>
        </div>
      </section>
    </div>
  );
}
