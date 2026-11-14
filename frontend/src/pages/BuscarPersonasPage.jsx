import { useEffect, useState } from 'react';
import {
  ChevronRight, Lock, Search, UsersRound,
} from 'lucide-react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import AvatarUsuario from '../components/AvatarUsuario';
import { buscarUsuarios, MINIMO_LETRAS } from '../services/usuarioService';
import { iniciales } from '../utils/perfil';

const ESPERA_MS = 300;

/** Mismo criterio del servidor: sin comodines ni espacios de más. */
const letrasUtiles = (texto) => texto.replace(/[%_\\]/g, '').trim();

function Esqueleto() {
  return (
    <ul className="flex animate-pulse flex-col gap-2" aria-hidden="true">
      {[1, 2, 3].map((n) => (
        <li key={n} className="flex items-center gap-4 rounded-2xl border border-zinc-200 bg-white p-4">
          <div className="size-12 shrink-0 rounded-full bg-slate-200" />
          <div className="h-5 w-48 rounded bg-slate-200" />
        </li>
      ))}
    </ul>
  );
}

/**
 * HU24: Personas. Busca por nombre o apellido (al menos dos letras) y abre el perfil público de
 * cada resultado. El texto vive en la URL (?nombre=) para mantenerse al recargar o al volver.
 */
export default function BuscarPersonasPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const nombre = searchParams.get('nombre') ?? '';
  const [texto, setTexto] = useState(nombre);
  const [intento, setIntento] = useState(0);
  // Respuesta de la última búsqueda, marcada con la clave (texto + intento) que la pidió.
  const [respuesta, setRespuesta] = useState({ clave: null });

  // El texto escrito pasa a la URL un momento después de dejar de escribir.
  useEffect(() => {
    if (texto === nombre) return undefined;
    const temporizador = setTimeout(() => {
      setSearchParams(texto ? { nombre: texto } : {}, { replace: true });
    }, ESPERA_MS);
    return () => clearTimeout(temporizador);
  }, [texto, nombre, setSearchParams]);

  const util = letrasUtiles(nombre);
  const buscable = util.length >= MINIMO_LETRAS;
  const clave = `${nombre}|${intento}`;

  // La búsqueda sale de la URL; el estado solo cambia cuando responde el servidor.
  useEffect(() => {
    if (!buscable) return undefined;
    const controlador = new AbortController();
    buscarUsuarios(nombre, { signal: controlador.signal })
      .then((personas) => setRespuesta({ clave, fase: 'lista', personas }))
      .catch((error) => {
        if (error.name === 'AbortError') return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else if (error.estado === 400) setRespuesta({ clave, fase: 'corto' });
        else setRespuesta({ clave, fase: 'error', mensaje: error.message });
      });
    return () => controlador.abort();
  }, [nombre, buscable, clave, navigate]);

  let estado;
  if (!util) estado = { fase: 'inicial' };
  else if (!buscable) estado = { fase: 'corto' };
  else if (respuesta.clave !== clave) estado = { fase: 'cargando' };
  else estado = respuesta;

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto w-full max-w-[860px] px-4 pb-16 pt-6 sm:pt-12" aria-busy={estado.fase === 'cargando'}>
        <h1 className="text-2xl font-bold sm:text-3xl">{t('personas.titulo')}</h1>
        <p className="mb-6 mt-1 text-gray-600">{t('personas.subtitulo')}</p>

        <form role="search" className="mb-5" onSubmit={(evento) => evento.preventDefault()}>
          <label htmlFor="buscar-personas" className="mb-2 block text-sm font-bold text-gray-700">
            {t('personas.etiqueta')}
          </label>
          <div className="relative">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-gray-400" aria-hidden="true" />
            <input
              id="buscar-personas"
              type="search"
              value={texto}
              onChange={(evento) => setTexto(evento.target.value)}
              placeholder={t('personas.placeholder')}
              autoComplete="off"
              aria-describedby="ayuda-busqueda"
              className="h-12 w-full rounded-xl border border-zinc-300 bg-white pl-12 pr-4 text-base text-gray-900 placeholder:text-gray-400 focus:border-violet-500 focus:outline-none focus:ring-2 focus:ring-violet-200"
            />
          </div>
          <p
            id="ayuda-busqueda"
            className={`mt-2 text-sm ${estado.fase === 'corto' ? 'font-bold text-amber-700' : 'text-gray-500'}`}
          >
            {estado.fase === 'corto' ? t('personas.corto') : t('personas.ayuda')}
          </p>
        </form>

        {estado.fase === 'inicial' && (
          <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-zinc-300 bg-white p-8 text-center">
            <UsersRound className="size-10 text-violet-300" aria-hidden="true" />
            <p className="text-gray-600">{t('personas.empezar')}</p>
          </div>
        )}

        {estado.fase === 'cargando' && <Esqueleto />}

        {estado.fase === 'error' && (
          <EstadoError titulo={t('personas.errorBuscar')} mensaje={estado.mensaje} onReintentar={() => setIntento((n) => n + 1)} />
        )}

        {estado.fase === 'lista' && (
          <>
            <p role="status" className="mb-3 text-sm font-semibold text-gray-600">
              {t('personas.cantidad', { count: estado.personas.length })}
            </p>
            {estado.personas.length === 0 ? (
              <div className="flex flex-col items-center gap-2 rounded-2xl border border-zinc-200 bg-white p-8 text-center">
                <p className="font-bold text-gray-900">{t('personas.sinResultados', { texto: letrasUtiles(nombre) })}</p>
                <p className="text-sm text-gray-600">{t('personas.sinResultadosAyuda')}</p>
              </div>
            ) : (
              <ul className="flex flex-col gap-2">
                {estado.personas.map((persona) => {
                  const nombreCompleto = [persona.nombre, persona.apellido].filter(Boolean).join(' ');
                  return (
                    <li key={persona.codigoUsuario}>
                      <Link
                        to={`/usuarios/${persona.codigoUsuario}`}
                        className="flex items-center gap-4 rounded-2xl border border-zinc-200 bg-white p-4 hover:border-violet-300 hover:bg-violet-50/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
                      >
                        <AvatarUsuario foto={persona.foto} iniciales={iniciales(persona.nombre, persona.apellido)} clase="size-12 text-base" />
                        <span className="min-w-0 flex-1 truncate text-base font-semibold text-gray-900">{nombreCompleto}</span>
                        {/* HU26 */}
                        {persona.privado && (
                          <span className="flex shrink-0 items-center gap-1.5 rounded-lg bg-slate-100 px-2.5 py-1 text-sm font-semibold text-gray-700">
                            <Lock className="size-4" aria-hidden="true" />
                            {t('personas.privado')}
                          </span>
                        )}
                        <span className="sr-only">{t('personas.verPerfil')}</span>
                        <ChevronRight className="size-5 shrink-0 text-gray-400" aria-hidden="true" />
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
          </>
        )}
      </main>
    </div>
  );
}
