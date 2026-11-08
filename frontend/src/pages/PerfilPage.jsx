import { useEffect, useState } from 'react';
import {
  Award, ChartColumn, CircleCheck, Lock, PencilLine, Target, TrendingUp,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import CabeceraPerfil from '../components/CabeceraPerfil';
import EstadoError from '../components/EstadoError';
import IconoInsignia from '../components/IconoInsignia';
import { EtiquetaOculta } from '../components/MetaTarjeta';
import TarjetaPrivacidad from '../components/TarjetaPrivacidad';
import { TIPOS } from '../constants/opcionesMeta';
import { obtenerPerfil } from '../services/perfilService';
import { formatearFecha } from '../utils/fechas';
import { EVENTO_CUENTA, datosPerfil, obtenerSesion } from '../services/sesionService';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t]));
const TARJETA = 'rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6';
const ENLACE = 'rounded-md text-sm font-bold text-violet-700 hover:text-violet-800 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300';

/** Título de un bloque del perfil, con un enlace opcional a la derecha. */
function TituloBloque({
  id, titulo, detalle, enlace,
}) {
  return (
    <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2">
      <div className="flex items-baseline gap-2">
        <h2 id={id} className="text-lg font-bold text-gray-900">{titulo}</h2>
        {detalle && <span className="text-sm text-gray-500">{detalle}</span>}
      </div>
      {enlace}
    </div>
  );
}

function Cifra({
  Icono, valor, etiqueta,
}) {
  return (
    <div className="flex items-center gap-4 rounded-xl bg-slate-50 p-4">
      <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-violet-100">
        <Icono className="size-6 text-violet-600" aria-hidden="true" />
      </div>
      <div>
        <p className="text-2xl font-bold leading-tight text-gray-900">{valor}</p>
        <p className="text-sm text-gray-500">{etiqueta}</p>
      </div>
    </div>
  );
}

export function EsqueletoPerfil() {
  return (
    <div className="flex animate-pulse flex-col gap-4" aria-hidden="true">
      <div className={`${TARJETA} flex items-center gap-4`}>
        <div className="size-16 shrink-0 rounded-full bg-slate-200 sm:size-20" />
        <div className="flex flex-1 flex-col gap-2">
          <div className="h-6 w-48 rounded bg-slate-200" />
          <div className="h-4 w-64 max-w-full rounded bg-slate-100" />
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        {[1, 2, 3].map((n) => <div key={n} className="h-20 rounded-xl bg-slate-200" />)}
      </div>
      <div className={`${TARJETA} h-40`} />
    </div>
  );
}

/**
 * HU22: Mi perfil. Datos personales, resumen de metas (el mismo porcentaje del reporte, HU7),
 * insignias ganadas (HU11) y las tres últimas metas cumplidas. Es de solo lectura.
 */
export default function PerfilPage() {
  const navigate = useNavigate();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let activo = true;
    obtenerPerfil()
      .then((perfil) => activo && setEstado({ fase: 'listo', perfil }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else setEstado({ fase: 'error', mensaje: error.message });
      });
    return () => {
      activo = false;
    };
  }, [intento, navigate]);

  const perfil = estado.fase === 'listo' ? estado.perfil : null;

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto w-full max-w-[860px] px-4 pb-16 pt-6 sm:pt-12" aria-busy={estado.fase === 'cargando'}>
        <h1 className="mb-6 text-2xl font-bold sm:text-3xl">Mi perfil</h1>

        {estado.fase === 'cargando' && <EsqueletoPerfil />}

        {estado.fase === 'error' && (
          <EstadoError
            titulo="No pudimos cargar tu perfil"
            mensaje={estado.mensaje}
            onReintentar={() => {
              setEstado({ fase: 'cargando' });
              setIntento((n) => n + 1);
            }}
          />
        )}

        {perfil && <ContenidoPerfil perfil={perfil} />}
      </main>
    </div>
  );
}

/**
 * Bloques del perfil. HU24: con `publico` es el perfil de otra persona: sin correo, sin Editar
 * perfil, sin enlaces a pantallas propias y con los textos en tercera persona.
 */
export function ContenidoPerfil({ perfil, publico = false }) {
  const {
    resumen, insignias, ultimasCumplidas, personalizacion,
  } = perfil;
  // HU-23: la foto propia viene de la cuenta (la sincroniza el encabezado) y se actualiza al editarla.
  const [fotoCuenta, setFotoCuenta] = useState(() => datosPerfil(obtenerSesion())?.foto ?? null);
  useEffect(() => {
    if (publico) return undefined;
    const alCambiarCuenta = () => setFotoCuenta(datosPerfil(obtenerSesion())?.foto ?? null);
    window.addEventListener(EVENTO_CUENTA, alCambiarCuenta);
    return () => window.removeEventListener(EVENTO_CUENTA, alCambiarCuenta);
  }, [publico]);
  const foto = publico ? perfil.foto : fotoCuenta;
  // HU26: de un perfil privado ajeno solo llega la cabecera.
  const privadoAjeno = publico && perfil.privado;

  return (
    <div className="flex flex-col gap-4">
      {/* 1. Datos personales (HU19: con la portada y la frase que eligió) */}
      <CabeceraPerfil
        nombre={perfil.nombre}
        apellido={perfil.apellido}
        foto={foto}
        email={publico ? null : perfil.email}
        fechaRegistro={perfil.fechaRegistro}
        frase={personalizacion?.frase}
        portada={personalizacion?.portada}
        acciones={publico ? null : (
          <Link
            to="/perfil/editar"
            className="flex h-11 shrink-0 items-center justify-center gap-2 rounded-xl border border-violet-300 bg-violet-50 px-4 text-[15px] font-bold text-violet-700 hover:bg-violet-100"
          >
            <PencilLine className="size-[18px]" aria-hidden="true" />
            Editar perfil
          </Link>
        )}
      />

      {/* HU26: interruptor Perfil privado (solo en Mi perfil) */}
      {!publico && <TarjetaPrivacidad inicial={perfil.perfilPrivado} />}

      {privadoAjeno && (
        <section aria-label="Perfil privado" className={`${TARJETA} flex flex-col items-center gap-3 text-center`}>
          <div className="flex size-14 items-center justify-center rounded-2xl bg-violet-50">
            <Lock className="size-7 text-violet-600" aria-hidden="true" />
          </div>
          <p className="text-lg font-bold text-gray-900">Este perfil es privado</p>
          <p className="text-sm text-gray-600">{`${perfil.nombre} decidió no mostrar sus metas ni sus insignias.`}</p>
        </section>
      )}

      {!privadoAjeno && (
      <>
      {/* 2. Resumen de metas */}
      <section aria-labelledby="perfil-resumen" className={TARJETA}>
        <TituloBloque
          id="perfil-resumen"
          titulo="Resumen de metas"
          enlace={publico ? null : <Link to="/reporte" className={ENLACE}>Ver mi reporte</Link>}
        />
        <div className="grid gap-3 sm:grid-cols-3">
          <Cifra Icono={Target} valor={resumen.metasActivas} etiqueta="Metas activas" />
          <Cifra Icono={CircleCheck} valor={resumen.metasCumplidas} etiqueta="Metas cumplidas" />
          <Cifra Icono={TrendingUp} valor={`${resumen.porcentajeCumplimiento} %`} etiqueta="Cumplimiento" />
        </div>
      </section>

      {/* 3. Insignias ganadas (HU11). La galería completa, con las pendientes, es la HU12. */}
      <section aria-labelledby="perfil-insignias" className={TARJETA}>
        <TituloBloque
          id="perfil-insignias"
          titulo={publico ? 'Insignias' : 'Mis insignias'}
          detalle={`${insignias.length} de ${perfil.totalInsignias} insignias`}
          enlace={publico ? null : <Link to="/insignias" className={ENLACE}>Ver galería</Link>}
        />
        {insignias.length === 0 ? (
          <div className="flex items-center gap-4 rounded-xl bg-slate-50 p-4">
            <Award className="size-8 shrink-0 text-gray-300" aria-hidden="true" />
            {publico ? (
              <p className="text-sm text-gray-600">Aún no tiene insignias.</p>
            ) : (
              <p className="text-sm text-gray-600">
                Aún no tienes insignias. Cumple tu primera meta para ganar
                {' '}
                <strong className="font-semibold text-gray-800">Primer paso</strong>
                .
              </p>
            )}
          </div>
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {insignias.map((insignia) => (
              <li
                key={insignia.codigoInsignia}
                className="flex flex-col items-center gap-2 rounded-xl border border-violet-100 bg-violet-50/60 p-4 text-center"
                title={insignia.descripcion}
              >
                <div className="flex size-12 items-center justify-center rounded-full bg-violet-600">
                  <IconoInsignia nombre={insignia.icono} className="size-6 text-white" aria-hidden="true" />
                </div>
                <p className="text-sm font-bold leading-tight text-gray-900">{insignia.nombre}</p>
                <p className="text-xs text-gray-500">{formatearFecha(insignia.fechaObtenida)}</p>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 4. Últimas metas cumplidas (máximo tres) */}
      <section aria-labelledby="perfil-cumplidas" className={TARJETA}>
        <TituloBloque
          id="perfil-cumplidas"
          titulo="Últimas metas cumplidas"
          enlace={!publico && resumen.metasCumplidas > 0
            ? <Link to="/metas?estado=CUMPLIDA" className={ENLACE}>Ver todas</Link>
            : null}
        />
        {ultimasCumplidas.length > 0 && (
          <ul className="flex flex-col divide-y divide-zinc-100">
            {ultimasCumplidas.map((meta) => {
              const tipo = TIPO_POR_VALOR[meta.tipo] ?? TIPO_POR_VALOR.OTRO;
              return (
                <li key={meta.codigoMeta} className="flex items-center gap-3 py-3 first:pt-0 last:pb-0">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-emerald-50">
                    <tipo.Icono className="size-5 text-emerald-600" aria-hidden="true" />
                  </div>
                  <div className="min-w-0 flex-1">
                    {/* HU24: el camino de una meta ajena no se puede abrir, así que no es enlace. */}
                    {publico ? (
                      <p className="truncate text-[15px] font-semibold text-gray-900">{meta.nombre}</p>
                    ) : (
                      <Link
                        to={`/metas/${meta.codigoMeta}`}
                        className="block truncate rounded-md text-[15px] font-semibold text-gray-900 hover:text-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
                      >
                        {meta.nombre}
                      </Link>
                    )}
                    <p className="text-sm text-gray-500">
                      {`${tipo.etiqueta} · Cumplida el ${formatearFecha(meta.fechaCumplida)}`}
                    </p>
                  </div>
                  {/* HU25: sigue en su perfil, pero no se muestra a otros usuarios. */}
                  {meta.oculta && <EtiquetaOculta className="shrink-0 py-1" />}
                  <CircleCheck className="size-5 shrink-0 text-emerald-600" aria-hidden="true" />
                </li>
              );
            })}
          </ul>
        )}
        {publico && ultimasCumplidas.length === 0 && (
          <p className="text-sm text-gray-600">Todavía no tiene metas cumplidas para mostrar.</p>
        )}
        {!publico && ultimasCumplidas.length === 0 && resumen.totalMetas > 0 && (
          <p className="text-sm text-gray-600">Aún no has cumplido metas. ¡La primera está cerca!</p>
        )}
        {!publico && resumen.totalMetas === 0 && (
          <div className="flex flex-col items-center gap-4 py-4 text-center">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-violet-50">
              <ChartColumn className="size-7 text-violet-600" aria-hidden="true" />
            </div>
            <p className="text-sm text-gray-600">Todavía no tienes metas. Crea la primera y aquí verás tus logros.</p>
            <Link
              to="/metas/nueva"
              className="flex h-12 items-center rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
            >
              Crear mi primera meta
            </Link>
          </div>
        )}
      </section>
      </>
      )}
    </div>
  );
}
