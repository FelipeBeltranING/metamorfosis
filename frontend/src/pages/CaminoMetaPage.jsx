import { useCallback, useEffect, useState } from 'react';
import { Check, ChevronLeft, CircleAlert, CircleCheck, Lock, Route, Trophy } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import AnilloConstancia from '../components/AnilloConstancia';
import MenuSubMeta from '../components/MenuSubMeta';
import RegistrarValorModal from '../components/RegistrarValorModal';
import EliminarSubMetaModal from '../components/EliminarSubMetaModal';
import ConfirmarCumplidaModal from '../components/ConfirmarCumplidaModal';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';
import { marcarCumplida } from '../services/metaService';
import {
  eliminarSubMeta, obtenerCamino, quitarRegistro, registrarDia,
} from '../services/subMetaService';
import { diaCorto, formatearFecha } from '../utils/fechas';
import { detalleSubMeta, formatearNumero } from '../utils/subMetas';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t.etiqueta]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p.etiqueta]));
// 5 días en el celular y 7 desde sm: la 6.ª y 7.ª columna se ocultan en pantallas angostas.
const COLUMNAS = 'grid grid-cols-[minmax(0,1fr)_repeat(5,44px)] gap-1 sm:grid-cols-[minmax(200px,1fr)_repeat(7,64px)] sm:gap-2';
const OCULTA_EN_MOVIL = (i) => (i >= 5 ? 'hidden sm:flex' : 'flex');

/**
 * Sub-metas · "Tu camino" de una meta: resumen de constancia y cuadrícula de los últimos días
 * para marcar cada sub-meta (HU-B). Una meta cumplida se muestra en solo lectura (HU6).
 */
export default function CaminoMetaPage() {
  const { codigoMeta } = useParams();
  const navigate = useNavigate();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [aviso, setAviso] = useState('');
  const [errorAccion, setErrorAccion] = useState('');
  const [registrando, setRegistrando] = useState(null); // { subMeta, dia }
  const [ocupado, setOcupado] = useState(''); // celda que se está guardando
  const [aEliminar, setAEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const [errorEliminar, setErrorEliminar] = useState('');
  const [confirmarCumplida, setConfirmarCumplida] = useState(false);
  const [marcando, setMarcando] = useState(false);
  const [errorCumplida, setErrorCumplida] = useState('');

  const manejarError = useCallback((error, poner) => {
    if (error.estado === 401) navigate('/login', { replace: true });
    else poner(error.estado ? error.message : 'No se guardó. No pudimos conectar con el servidor: revisa tu conexión.');
  }, [navigate]);

  const recargar = useCallback(() => obtenerCamino(codigoMeta)
    .then((camino) => setEstado({ fase: 'lista', camino })), [codigoMeta]);

  useEffect(() => {
    let activo = true;
    obtenerCamino(codigoMeta)
      .then((camino) => activo && setEstado({ fase: 'lista', camino }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else setEstado({ fase: 'error', mensaje: error.estado === 404 ? 'Esta meta no existe.' : error.message });
      });
    return () => {
      activo = false;
    };
  }, [codigoMeta, intento, navigate]);

  useEffect(() => {
    if (!aviso && !errorAccion) return undefined;
    const t = setTimeout(() => {
      setAviso('');
      setErrorAccion('');
    }, 4500);
    return () => clearTimeout(t);
  }, [aviso, errorAccion]);

  if (estado.fase !== 'lista') {
    return (
      <div className="min-h-screen bg-slate-50 text-gray-900">
        <Encabezado />
        <main className="mx-auto w-full max-w-[1040px] px-4 pt-8">
          {estado.fase === 'cargando' && (
            <div aria-busy="true" className="flex flex-col gap-5">
              <div className="h-36 animate-pulse rounded-2xl bg-white" />
              <div className="h-80 animate-pulse rounded-2xl bg-white" />
            </div>
          )}
          {estado.fase === 'error' && (
            <EstadoError mensaje={estado.mensaje} onReintentar={() => { setEstado({ fase: 'cargando' }); setIntento((n) => n + 1); }} />
          )}
        </main>
      </div>
    );
  }

  const { camino } = estado;
  const fechas = camino.subMetas[0]?.dias.map((d) => d.fecha) ?? [];

  // Sí/No: vacío → hecho → no hecho → vacío.
  const alternarSiNo = async (subMeta, dia) => {
    const clave = `${subMeta.codigoSubMeta}-${dia.fecha}`;
    setOcupado(clave);
    try {
      if (dia.valor === null) await registrarDia(subMeta.codigoSubMeta, dia.fecha, 1);
      else if (Number(dia.valor) >= 1) await registrarDia(subMeta.codigoSubMeta, dia.fecha, 0);
      else await quitarRegistro(subMeta.codigoSubMeta, dia.fecha);
      await recargar();
    } catch (error) {
      manejarError(error, setErrorAccion);
    } finally {
      setOcupado('');
    }
  };

  const guardarValor = async (valor) => {
    await registrarDia(registrando.subMeta.codigoSubMeta, registrando.dia.fecha, valor);
    await recargar();
    setRegistrando(null);
    setAviso('Registro guardado.');
  };

  const quitarValor = async () => {
    await quitarRegistro(registrando.subMeta.codigoSubMeta, registrando.dia.fecha);
    await recargar();
    setRegistrando(null);
    setAviso('Registro quitado.');
  };

  const confirmarEliminar = async () => {
    setEliminando(true);
    setErrorEliminar('');
    try {
      await eliminarSubMeta(aEliminar.codigoSubMeta);
      await recargar();
      setAEliminar(null);
      setAviso('Sub-meta eliminada.');
    } catch (error) {
      manejarError(error, setErrorEliminar);
    } finally {
      setEliminando(false);
    }
  };

  const confirmarMarcarCumplida = async () => {
    setMarcando(true);
    setErrorCumplida('');
    try {
      await marcarCumplida(camino.codigoMeta);
      await recargar();
      setConfirmarCumplida(false);
      setAviso('¡Meta cumplida! Ya está en tus logros.');
    } catch (error) {
      manejarError(error, setErrorCumplida);
    } finally {
      setMarcando(false);
    }
  };

  const celda = (subMeta, dia, i) => {
    const etiquetaDia = dia.fecha === camino.hoy ? 'hoy' : `${diaCorto(dia.fecha)} ${dia.fecha.slice(8)}`;
    const visible = OCULTA_EN_MOVIL(i);
    if (!dia.toca) {
      return (
        <span key={dia.fecha} role="cell" aria-label={`${subMeta.nombre}, ${etiquetaDia}: no toca`} className={`${visible} items-center justify-center text-lg font-bold text-zinc-400`}>–</span>
      );
    }
    const hecho = dia.cumplido;
    const registrado = dia.valor !== null;
    const bloqueado = camino.soloLectura || ocupado === `${subMeta.codigoSubMeta}-${dia.fecha}`;
    if (subMeta.tipo === 'SI_NO') {
      const estadoTexto = !registrado ? 'sin marcar' : hecho ? 'hecho' : 'no hecho';
      return (
        <span key={dia.fecha} role="cell" className={`${visible} items-center justify-center`}>
          <button
            type="button"
            disabled={bloqueado}
            onClick={() => alternarSiNo(subMeta, dia)}
            aria-label={`${subMeta.nombre}, ${etiquetaDia}: ${estadoTexto}${camino.soloLectura ? '' : '. Tocar para cambiar'}`}
            className={`flex size-10 items-center justify-center rounded-full text-lg font-extrabold sm:size-11 ${
              hecho ? 'bg-emerald-500 text-white'
                : registrado ? 'border border-orange-300 bg-orange-50 text-orange-700'
                  : 'border-2 border-zinc-300 bg-white'
            } ${bloqueado ? 'cursor-default' : 'hover:ring-2 hover:ring-violet-200'} disabled:opacity-100`}
          >
            {hecho ? <Check className="size-5" strokeWidth={3} aria-hidden="true" /> : registrado ? '×' : ''}
          </button>
        </span>
      );
    }
    return (
      <span key={dia.fecha} role="cell" className={`${visible} items-center justify-center`}>
        <button
          type="button"
          disabled={bloqueado}
          onClick={() => setRegistrando({ subMeta, dia })}
          aria-label={`${subMeta.nombre}, ${etiquetaDia}: ${registrado ? `${formatearNumero(dia.valor)} ${subMeta.unidad}, ${hecho ? 'cumplido' : 'no cumplido'}` : 'sin registrar'}`}
          className={`flex h-11 w-[42px] flex-col items-center justify-center rounded-xl border sm:w-[60px] ${
            hecho ? 'border-emerald-300 bg-emerald-50 text-emerald-700'
              : registrado ? 'border-orange-300 bg-orange-50 text-orange-700'
                : 'border-zinc-200 bg-white text-zinc-500'
          } ${bloqueado ? 'cursor-default' : 'hover:ring-2 hover:ring-violet-200'}`}
        >
          <span className="text-[15px] font-extrabold leading-none sm:text-base">{registrado ? formatearNumero(dia.valor) : '—'}</span>
          <span className="max-w-full truncate px-0.5 text-[10px] font-bold sm:text-[11px]">{subMeta.unidad}</span>
        </button>
      </span>
    );
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto flex w-full max-w-[1040px] flex-col gap-5 px-4 pb-16 pt-6 sm:pt-8">
        <Link to={camino.soloLectura ? '/mis-logros' : '/metas'} className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
          <ChevronLeft className="size-[18px]" aria-hidden="true" />
          {camino.soloLectura ? 'Mis logros' : 'Mis metas'}
        </Link>

        <section className={`flex flex-wrap items-center gap-5 rounded-2xl border bg-white p-5 sm:p-7 ${camino.soloLectura ? 'border-violet-300' : 'border-zinc-200'}`}>
          <AnilloConstancia porcentaje={camino.constancia7} tamano={96} grosor={10} />
          <div className="flex min-w-0 flex-[1_1_260px] flex-col gap-2">
            {camino.soloLectura && camino.fechaCumplida && (
              <span className="inline-flex items-center gap-1.5 self-start rounded-lg bg-violet-600 px-2.5 py-1 text-sm font-extrabold text-white">
                <Trophy className="size-4" aria-hidden="true" />
                Cumplida el {formatearFecha(camino.fechaCumplida)}
              </span>
            )}
            <h1 className="break-words text-2xl font-extrabold sm:text-3xl">{camino.nombre}</h1>
            <div className="flex flex-wrap gap-1.5 text-[13px] font-semibold text-gray-600">
              {TIPO_POR_VALOR[camino.tipo] && <span className="rounded-lg bg-slate-100 px-2.5 py-1.5">{TIPO_POR_VALOR[camino.tipo]}</span>}
              {PLAZO_POR_VALOR[camino.plazo] && <span className="rounded-lg bg-slate-100 px-2.5 py-1.5">{PLAZO_POR_VALOR[camino.plazo]}</span>}
              <span className="rounded-lg bg-slate-100 px-2.5 py-1.5">Creada el {formatearFecha(camino.fechaCreacion)}</span>
            </div>
          </div>
          {camino.subMetas.length > 0 && (
            <dl className="grid flex-[1_1_320px] grid-cols-3 gap-2.5">
              <div className="rounded-xl bg-slate-50 px-3.5 py-3">
                <dt className="text-[13px] font-semibold text-gray-600">Hoy</dt>
                <dd className="mt-0.5 text-xl font-extrabold">{camino.tocanHoy ? `${camino.cumplidasHoy} de ${camino.tocanHoy}` : '—'}</dd>
              </div>
              <div className="rounded-xl bg-slate-50 px-3.5 py-3">
                <dt className="text-[13px] font-semibold text-gray-600">Últimos 30 días</dt>
                <dd className="mt-0.5 text-xl font-extrabold">{camino.constancia30 ?? '—'}{camino.constancia30 !== null && '%'}</dd>
              </div>
              <div className="rounded-xl bg-slate-50 px-3.5 py-3">
                <dt className="text-[13px] font-semibold text-gray-600">Días seguidos al 100%</dt>
                <dd className="mt-0.5 text-xl font-extrabold">{camino.diasSeguidosCompletos}</dd>
              </div>
            </dl>
          )}
        </section>

        {camino.subMetas.length === 0 ? (
          <section className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-violet-300 bg-white px-6 py-12 text-center">
            <div className="flex size-16 items-center justify-center rounded-2xl bg-violet-50 text-violet-600">
              <Route className="size-8" aria-hidden="true" />
            </div>
            <h2 className="text-xl font-extrabold">Traza el camino hacia esta meta</h2>
            <p className="max-w-lg text-[15px] leading-6 text-gray-600">
              Divide la meta en pequeñas sub-metas diarias. Cada día marcas lo que hiciste y ves qué tan constante vas.
            </p>
            {camino.puedeEditar && (
              <Link to={`/metas/${camino.codigoMeta}/submetas/nueva`} className="mt-2 flex h-12 items-center rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700">
                + Agregar la primera sub-meta
              </Link>
            )}
          </section>
        ) : (
          <section aria-labelledby="titulo-camino" className="flex flex-col gap-3 rounded-2xl border border-zinc-200 bg-white p-4 sm:p-7">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 id="titulo-camino" className="text-xl font-extrabold sm:text-2xl">
                  {camino.soloLectura ? 'Así recorriste tu camino' : 'Tu camino'}
                </h2>
                <p className="text-sm text-gray-600 sm:text-[15px]">
                  {camino.soloLectura
                    ? 'Solo lectura: la meta está cumplida y sus sub-metas ya no se registran.'
                    : 'Marca cada día lo que hiciste. Toca el nombre para ver sus estadísticas.'}
                </p>
              </div>
              {camino.soloLectura ? (
                <span className="flex items-center gap-1.5 rounded-lg bg-slate-100 px-3 py-2 text-[13px] font-bold text-gray-600">
                  <Lock className="size-4" aria-hidden="true" /> Cerrada
                </span>
              ) : camino.puedeEditar && (
                <Link to={`/metas/${camino.codigoMeta}/submetas/nueva`} className="flex h-11 items-center rounded-xl bg-violet-600 px-4 text-[15px] font-bold text-white hover:bg-violet-700">
                  + Agregar sub-meta
                </Link>
              )}
            </div>

            <div role="table" aria-label="Sub-metas de los últimos días" className="flex flex-col gap-2">
              <div role="row" className={`${COLUMNAS} items-end px-1`}>
                <span role="columnheader" className="px-2 text-[13px] font-bold text-gray-600">Sub-meta</span>
                {fechas.map((f, i) => (
                  <span key={f} role="columnheader" className={`${OCULTA_EN_MOVIL(i)} flex-col items-center text-[11px] font-extrabold leading-tight sm:text-xs ${f === camino.hoy ? 'text-violet-700' : 'text-gray-600'}`}>
                    <span>{f === camino.hoy ? 'Hoy' : diaCorto(f)}</span>
                    <span className="text-sm sm:text-[15px]">{Number(f.slice(8))}</span>
                  </span>
                ))}
              </div>
              {camino.subMetas.map(({ subMeta, dias }) => (
                <div key={subMeta.codigoSubMeta} role="row" className={`${COLUMNAS} items-center rounded-xl bg-slate-50 py-1.5 pl-1 pr-1`}>
                  <div role="rowheader" className="flex min-w-0 items-center gap-0.5">
                    <MenuSubMeta subMeta={subMeta} puedeEditar={camino.puedeEditar} onEliminar={(s) => { setErrorEliminar(''); setAEliminar(s); }} />
                    <Link to={`/submetas/${subMeta.codigoSubMeta}`} className="min-w-0 py-1">
                      <span className="block truncate text-[15px] font-bold text-gray-900 sm:text-base">{subMeta.nombre}</span>
                      <span className="block truncate text-[11px] font-semibold text-gray-600 sm:text-[13px]">{detalleSubMeta(subMeta)}</span>
                    </Link>
                  </div>
                  {dias.map((dia, i) => celda(subMeta, dia, i))}
                </div>
              ))}
            </div>

            <div className="flex flex-wrap gap-x-4 gap-y-1.5 pt-1 text-[13px] font-semibold text-gray-600">
              <span className="flex items-center gap-1.5"><span aria-hidden="true" className="size-3.5 rounded-full bg-emerald-500" />Cumplido</span>
              <span className="flex items-center gap-1.5"><span aria-hidden="true" className="size-3.5 rounded-full border border-orange-300 bg-orange-50" />No cumplido</span>
              <span className="flex items-center gap-1.5"><span aria-hidden="true" className="size-3.5 rounded-full border-2 border-zinc-300" />Sin marcar</span>
              <span>– No toca ese día</span>
            </div>
          </section>
        )}

        {!camino.soloLectura && camino.puedeEditar && (
          <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-dashed border-violet-300 bg-violet-50/60 p-5">
            <p className="flex-[1_1_320px] text-[15px] leading-6 text-gray-600">
              ¿Ya llegaste al destino? Márcala como cumplida: pasará a tus logros y podrás compartirla.
            </p>
            <button
              type="button"
              onClick={() => { setErrorCumplida(''); setConfirmarCumplida(true); }}
              className="h-11 rounded-xl border border-violet-300 bg-white px-4 font-bold text-violet-800 hover:bg-violet-50"
            >
              Marcar meta como cumplida
            </button>
          </section>
        )}
      </main>

      {registrando && (
        <RegistrarValorModal
          subMeta={registrando.subMeta}
          fecha={registrando.dia.fecha}
          esHoy={registrando.dia.fecha === camino.hoy}
          valorActual={registrando.dia.valor}
          onGuardar={guardarValor}
          onQuitar={quitarValor}
          onCerrar={() => setRegistrando(null)}
        />
      )}
      {aEliminar && (
        <EliminarSubMetaModal
          subMeta={aEliminar}
          nombreMeta={camino.nombre}
          eliminando={eliminando}
          error={errorEliminar}
          onConfirmar={confirmarEliminar}
          onCancelar={() => setAEliminar(null)}
        />
      )}
      {confirmarCumplida && (
        <ConfirmarCumplidaModal
          meta={{ nombre: camino.nombre }}
          guardando={marcando}
          error={errorCumplida}
          onConfirmar={confirmarMarcarCumplida}
          onCancelar={() => setConfirmarCumplida(false)}
        />
      )}
      {(aviso || errorAccion) && (
        <div
          role={errorAccion ? 'alert' : 'status'}
          className={`fixed bottom-6 left-1/2 flex w-[calc(100%-2rem)] max-w-[640px] -translate-x-1/2 items-center gap-3 rounded-xl p-4 text-sm font-semibold shadow-lg ${
            errorAccion ? 'bg-red-50 text-red-700' : 'bg-emerald-400 text-gray-900'
          }`}
        >
          {errorAccion ? <CircleAlert className="size-6 shrink-0" aria-hidden="true" /> : <CircleCheck className="size-6 shrink-0" aria-hidden="true" />}
          {errorAccion || aviso}
        </div>
      )}
    </div>
  );
}
