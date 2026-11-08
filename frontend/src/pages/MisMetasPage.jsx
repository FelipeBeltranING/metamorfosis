import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  CircleCheck, KeyRound, Plus,
} from 'lucide-react';
import {
  Link, useLocation, useNavigate, useSearchParams,
} from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import MetaTarjeta from '../components/MetaTarjeta';
import MetaTarjetaEsqueleto from '../components/MetaTarjetaEsqueleto';
import EstadoVacio from '../components/EstadoVacio';
import EstadoError from '../components/EstadoError';
import ConfirmarEliminarModal from '../components/ConfirmarEliminarModal';
import ConfirmarCumplidaModal from '../components/ConfirmarCumplidaModal';
import CompartirLogroModal from '../components/CompartirLogroModal';
import InvitarPersonasModal from '../components/InvitarPersonasModal';
import AvisoInvitaciones from '../components/AvisoInvitaciones';
import UnirseConCodigoModal from '../components/UnirseConCodigoModal';
import FiltrosMetas from '../components/FiltrosMetas';
import SinResultadosFiltro from '../components/SinResultadosFiltro';
import {
  cambiarVisibilidad, eliminarMeta, listarMetas, marcarCumplida,
} from '../services/metaService';
import { obtenerResumenes } from '../services/subMetaService';
import { useAvisosInsignia } from '../hooks/useAvisosInsignia';
import {
  FILTROS_POR_DEFECTO, filtrosAUrl, filtrosDesdeUrl, hayFiltros,
} from '../utils/filtrosMetas';

const AVISOS = {
  creada: '¡Meta creada! Ya aparece en tus metas activas.',
  actualizada: '¡Meta actualizada!',
  eliminada: 'Meta eliminada.',
  sesion: '¡Sesión iniciada!',
  cumplida: '¡Meta cumplida! Ya está en tus metas cumplidas.',
  oculta: 'Meta oculta: ya no se mostrará en tu perfil público.', // HU25
  visible: 'La meta vuelve a mostrarse en tu perfil público.', // HU25
};

/** Qué aviso mostrar al llegar a la lista (viene en el estado de navegación). */
function avisoInicial(estadoNavegacion) {
  if (estadoNavegacion?.metaCreada) return 'creada';
  if (estadoNavegacion?.metaActualizada) return 'actualizada';
  if (estadoNavegacion?.sesionIniciada) return 'sesion';
  return null;
}

export default function MisMetasPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const mostrarInsignias = useAvisosInsignia();
  // HU27: los filtros viven en la URL (?estado=&tipo=&plazo=) para mantenerse al recargar.
  const [searchParams, setSearchParams] = useSearchParams();
  const filtros = useMemo(() => filtrosDesdeUrl(searchParams), [searchParams]);
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [aviso, setAviso] = useState(() => avisoInicial(location.state));
  const [metaAEliminar, setMetaAEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);
  const [errorEliminar, setErrorEliminar] = useState('');
  // HU6: marcar como cumplida, con confirmación porque no se puede deshacer.
  const [metaAMarcar, setMetaAMarcar] = useState(null);
  const [marcando, setMarcando] = useState(false);
  const [errorMarcar, setErrorMarcar] = useState('');
  // HU21: meta cumplida que se va a compartir.
  const [metaACompartir, setMetaACompartir] = useState(null);
  const [metaAInvitar, setMetaAInvitar] = useState(null); // HU13
  const [uniendose, setUniendose] = useState(false); // unirse con código
  // Aviso de error fuera del diálogo (por ejemplo, la meta ya no existe).
  const [errorLista, setErrorLista] = useState('');
  // HU25: metas cuya visibilidad se está cambiando (evita el doble clic).
  const [cambiandoVisibilidad, setCambiandoVisibilidad] = useState(() => new Set());
  // Sub-metas: constancia y sub-metas de hoy de cada meta, por código de meta.
  const [resumenes, setResumenes] = useState({});


  // Carga de metas (se repite al reintentar y al cambiar un filtro).
  useEffect(() => {
    let activo = true;
    listarMetas({ estado: filtros.estado, tipo: filtros.tipo, plazo: filtros.plazo })
      .then((metas) => {
        if (!activo) return;
        setEstado({ fase: 'lista', metas });
      })
      .catch((error) => activo && setEstado({
        fase: 'error',
        mensaje: error.message,
        sesionExpirada: error.estado === 401,
      }));
    return () => {
      activo = false;
    };
  }, [intento, filtros.estado, filtros.tipo, filtros.plazo]);

  // Sub-metas: resumen del camino de cada meta (no depende de los filtros).
  useEffect(() => {
    let activo = true;
    // Si el resumen falla, las tarjetas se ven igual pero sin la parte del camino.
    obtenerResumenes()
      .then((lista) => activo && setResumenes(Object.fromEntries(lista.map((r) => [r.codigoMeta, r]))))
      .catch(() => {});
    return () => {
      activo = false;
    };
  }, [intento]);

  // Sesión vencida o revocada: de vuelta al login.
  useEffect(() => {
    if (estado.sesionExpirada) navigate('/login', { replace: true });
  }, [estado, navigate]);

  // Aviso de éxito: se limpia el estado de navegación y se oculta a los 4 s.
  useEffect(() => {
    if (!aviso) return undefined;
    // Conserva ?estado=&tipo=&plazo= (HU27) al limpiar el estado de navegación.
    navigate({ pathname: location.pathname, search: location.search }, { replace: true, state: null });
    const temporizador = setTimeout(() => setAviso(null), 4000);
    return () => clearTimeout(temporizador);
  }, [aviso, navigate, location.pathname, location.search]);

  // El aviso de error de la lista se oculta solo a los 5 s.
  useEffect(() => {
    if (!errorLista) return undefined;
    const temporizador = setTimeout(() => setErrorLista(''), 5000);
    return () => clearTimeout(temporizador);
  }, [errorLista]);

  const reintentar = () => {
    setEstado({ fase: 'cargando' });
    setIntento((n) => n + 1);
  };

  // HU27: cambiar o limpiar filtros. replace evita llenar el historial con cada cambio.
  const cambiarFiltros = (nuevos) => {
    // Si no cambia nada (p. ej. volver a pulsar el filtro elegido), no se recarga.
    if (JSON.stringify(filtrosAUrl(nuevos)) === JSON.stringify(filtrosAUrl(filtros))) return;
    setEstado({ fase: 'cargando' });
    setSearchParams(filtrosAUrl(nuevos), { replace: true });
  };
  const limpiarFiltros = () => cambiarFiltros(FILTROS_POR_DEFECTO);

  // HU2: la pantalla de edición recibe la meta para precargar el formulario.
  const abrirEdicion = (meta) => {
    navigate(`/metas/${meta.codigoMeta}/editar`, { state: { meta } });
  };

  // HU4: eliminar con confirmación.
  const abrirConfirmacion = (meta) => {
    setErrorEliminar('');
    setMetaAEliminar(meta);
  };

  const cancelarEliminar = useCallback(() => setMetaAEliminar(null), []);

  const confirmarEliminar = async () => {
    const { codigoMeta } = metaAEliminar;
    setEliminando(true);
    setErrorEliminar('');
    try {
      await eliminarMeta(codigoMeta);
      setEstado((actual) => (actual.fase === 'lista'
        ? { ...actual, metas: actual.metas.filter((m) => m.codigoMeta !== codigoMeta) }
        : actual));
      setMetaAEliminar(null);
      setAviso('eliminada');
    } catch (error) {
      setErrorEliminar(error.message);
    } finally {
      setEliminando(false);
    }
  };

  /** Reemplaza una meta de la lista (o la quita si `nueva` es null). */
  const actualizarMeta = (codigoMeta, nueva) => {
    setEstado((actual) => (actual.fase === 'lista'
      ? {
        ...actual,
        metas: nueva
          ? actual.metas.map((m) => (m.codigoMeta === codigoMeta ? nueva : m))
          : actual.metas.filter((m) => m.codigoMeta !== codigoMeta),
      }
      : actual));
  };

  // HU6: el círculo abre la confirmación; la meta solo cambia cuando el servidor responde.
  const abrirMarcarCumplida = (meta) => {
    setErrorMarcar('');
    setMetaAMarcar(meta);
  };

  const cancelarMarcarCumplida = useCallback(() => setMetaAMarcar(null), []);

  const confirmarMarcarCumplida = async () => {
    const { codigoMeta } = metaAMarcar;
    setMarcando(true);
    setErrorMarcar('');
    try {
      const { meta: actualizada, insigniasNuevas } = await marcarCumplida(codigoMeta);
      actualizarMeta(codigoMeta, actualizada);
      setMetaAMarcar(null);
      setAviso('cumplida');
      mostrarInsignias(insigniasNuevas); // HU11
    } catch (error) {
      if (error.estado === 401) {
        navigate('/login', { replace: true });
      } else if (error.estado === 404) {
        actualizarMeta(codigoMeta, null);
        setMetaAMarcar(null);
        setErrorLista('Esta meta ya no existe');
      } else {
        setErrorMarcar(error.message);
      }
    } finally {
      setMarcando(false);
    }
  };

  // HU25: ocultar o mostrar en el perfil. La tarjeta cambia cuando el servidor responde.
  const alternarVisibilidad = async (meta) => {
    const { codigoMeta } = meta;
    if (cambiandoVisibilidad.has(codigoMeta)) return;
    setCambiandoVisibilidad((actual) => new Set(actual).add(codigoMeta));
    try {
      const actualizada = await cambiarVisibilidad(codigoMeta, !meta.oculta);
      actualizarMeta(codigoMeta, actualizada);
      setErrorLista('');
      setAviso(actualizada.oculta ? 'oculta' : 'visible');
    } catch (error) {
      setAviso(null);
      if (error.estado === 401) {
        navigate('/login', { replace: true });
      } else if (error.estado === 404) {
        actualizarMeta(codigoMeta, null);
        setErrorLista('Esta meta ya no existe');
      } else {
        setErrorLista(error.message);
      }
    } finally {
      setCambiandoVisibilidad((actual) => {
        const siguiente = new Set(actual);
        siguiente.delete(codigoMeta);
        return siguiente;
      });
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />

      <main className="mx-auto w-full max-w-[960px] px-4 pb-28 pt-6 sm:pb-10 sm:pt-12">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold sm:text-3xl">Mis metas</h1>
            <p className="mt-1 text-sm text-gray-500 sm:text-base">
              Marca el círculo cuando logres una meta.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setUniendose(true)}
              className="flex h-11 items-center gap-2 rounded-xl border border-violet-300 bg-white px-3.5 text-sm font-bold text-violet-700 hover:bg-violet-50 sm:h-14 sm:px-5 sm:text-base"
            >
              <KeyRound className="size-4 sm:size-5" aria-hidden="true" />
              <span>Unirme<span className="hidden sm:inline"> con código</span></span>
            </button>
            <Link
              to="/metas/nueva"
              className="hidden h-14 items-center rounded-xl bg-violet-600 px-6 text-base font-bold text-white hover:bg-violet-700 sm:flex"
            >
              + Nueva meta
            </Link>
          </div>
        </div>

        <AvisoInvitaciones />

        <FiltrosMetas
          filtros={filtros}
          cantidad={estado.fase === 'lista' ? estado.metas.length : 0}
          cargando={estado.fase === 'cargando'}
          onCambiar={cambiarFiltros}
          onLimpiar={limpiarFiltros}
        />

        {estado.fase === 'cargando' && (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2" aria-busy="true">
            {Array.from({ length: 4 }, (_, i) => <MetaTarjetaEsqueleto key={i} />)}
          </ul>
        )}

        {estado.fase === 'error' && (
          <EstadoError mensaje={estado.mensaje} onReintentar={reintentar} />
        )}

        {estado.fase === 'lista' && estado.metas.length === 0 && (
          hayFiltros(filtros) ? <SinResultadosFiltro onLimpiar={limpiarFiltros} /> : <EstadoVacio />
        )}

        {estado.fase === 'lista' && estado.metas.length > 0 && (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {estado.metas.map((meta) => (
              <MetaTarjeta
                key={meta.codigoMeta}
                meta={meta}
                resumen={resumenes[meta.codigoMeta]}
                onMarcarCumplida={abrirMarcarCumplida}
                onEditar={abrirEdicion}
                onCambiarVisibilidad={alternarVisibilidad}
                onEliminar={abrirConfirmacion}
                onInvitar={setMetaAInvitar}
                onCompartir={setMetaACompartir}
              />
            ))}
          </ul>
        )}
      </main>

      {/* Botón flotante solo en móvil */}
      <Link
        to="/metas/nueva"
        aria-label="Nueva meta"
        className="fixed bottom-6 right-4 flex size-14 items-center justify-center rounded-full bg-violet-600 text-white shadow-lg hover:bg-violet-700 sm:hidden"
      >
        <Plus className="size-6" aria-hidden="true" />
      </Link>

      {metaAEliminar && (
        <ConfirmarEliminarModal
          meta={metaAEliminar}
          resumen={resumenes[metaAEliminar.codigoMeta]}
          eliminando={eliminando}
          error={errorEliminar}
          onConfirmar={confirmarEliminar}
          onCancelar={cancelarEliminar}
        />
      )}

      {metaAMarcar && (
        <ConfirmarCumplidaModal
          meta={metaAMarcar}
          guardando={marcando}
          error={errorMarcar}
          onConfirmar={confirmarMarcarCumplida}
          onCancelar={cancelarMarcarCumplida}
        />
      )}

      {metaACompartir && (
        <CompartirLogroModal meta={metaACompartir} onCerrar={() => setMetaACompartir(null)} />
      )}

      {uniendose && <UnirseConCodigoModal onCerrar={() => setUniendose(false)} />}

      {metaAInvitar && (
        <InvitarPersonasModal
          meta={metaAInvitar}
          onInvitado={(participantes) => setEstado((actual) => (actual.fase === 'lista'
            ? { ...actual, metas: actual.metas.map((m) => (m.codigoMeta === metaAInvitar.codigoMeta ? { ...m, participantes } : m)) }
            : actual))}
          onCerrar={() => setMetaAInvitar(null)}
        />
      )}

      {aviso && (
        <div
          role="status"
          className="fixed bottom-24 left-1/2 flex w-[calc(100%-2rem)] max-w-[640px] -translate-x-1/2 items-center gap-3 rounded-xl bg-emerald-400 p-4 text-sm font-semibold leading-5 text-gray-900 shadow-[0_4px_16px_rgba(30,27,46,0.07)] sm:bottom-8"
        >
          <CircleCheck className="size-6 shrink-0" aria-hidden="true" />
          <span className="flex-1">{AVISOS[aviso]}</span>
          {/* Con el filtro Activas la meta cumplida sale de la lista: el aviso dice dónde quedó. */}
          {aviso === 'cumplida' && filtros.estado === 'ACTIVA' && (
            <button
              type="button"
              onClick={() => { setAviso(null); cambiarFiltros({ ...filtros, estado: 'CUMPLIDA' }); }}
              className="shrink-0 rounded-lg bg-white/70 px-3 py-1.5 font-bold text-gray-900 hover:bg-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gray-900"
            >
              Ver cumplidas
            </button>
          )}
        </div>
      )}

      {errorLista && (
        <div
          role="alert"
          className="fixed bottom-24 left-1/2 flex w-[calc(100%-2rem)] max-w-[640px] -translate-x-1/2 items-center gap-3 rounded-xl bg-red-50 p-4 text-sm font-semibold leading-5 text-red-700 shadow-[0_4px_16px_rgba(30,27,46,0.07)] sm:bottom-8"
        >
          {errorLista}
        </div>
      )}
    </div>
  );
}
