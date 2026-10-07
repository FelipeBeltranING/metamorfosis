import { useEffect, useState } from 'react';
import { CircleCheck, Plus } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import MetaTarjeta from '../components/MetaTarjeta';
import MetaTarjetaEsqueleto from '../components/MetaTarjetaEsqueleto';
import EstadoVacio from '../components/EstadoVacio';
import EstadoError from '../components/EstadoError';
import { listarMetas } from '../services/metaService';

export default function MisMetasPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [metaCreada, setMetaCreada] = useState(() => Boolean(location.state?.metaCreada));

  // Carga de metas (se repite al reintentar).
  useEffect(() => {
    let activo = true;
    listarMetas()
      .then((metas) => activo && setEstado({ fase: 'lista', metas }))
      .catch((error) => activo && setEstado({ fase: 'error', mensaje: error.message }));
    return () => {
      activo = false;
    };
  }, [intento]);

  // Toast de éxito: se limpia el estado de navegación y se oculta a los 4 s.
  useEffect(() => {
    if (!metaCreada) return undefined;
    navigate(location.pathname, { replace: true, state: null });
    const temporizador = setTimeout(() => setMetaCreada(false), 4000);
    return () => clearTimeout(temporizador);
  }, [metaCreada, navigate, location.pathname]);

  const reintentar = () => {
    setEstado({ fase: 'cargando' });
    setIntento((n) => n + 1);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />

      <main className="mx-auto w-full max-w-[960px] px-4 pb-28 pt-6 sm:pb-10 sm:pt-12">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-bold sm:text-3xl">Mis metas activas</h1>
          <Link
            to="/metas/nueva"
            className="hidden h-14 items-center rounded-xl bg-violet-600 px-6 text-base font-bold text-white hover:bg-violet-700 sm:flex"
          >
            + Nueva meta
          </Link>
        </div>

        {estado.fase === 'cargando' && (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2" aria-busy="true">
            {Array.from({ length: 4 }, (_, i) => <MetaTarjetaEsqueleto key={i} />)}
          </ul>
        )}

        {estado.fase === 'error' && (
          <EstadoError mensaje={estado.mensaje} onReintentar={reintentar} />
        )}

        {estado.fase === 'lista' && estado.metas.length === 0 && <EstadoVacio />}

        {estado.fase === 'lista' && estado.metas.length > 0 && (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {estado.metas.map((meta) => <MetaTarjeta key={meta.codigoMeta} meta={meta} />)}
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

      {metaCreada && (
        <div
          role="status"
          className="fixed bottom-24 left-1/2 flex w-[calc(100%-2rem)] max-w-md -translate-x-1/2 items-center gap-3 rounded-xl bg-emerald-400 p-4 text-sm font-semibold text-gray-900 shadow-lg sm:bottom-6"
        >
          <CircleCheck className="size-6 shrink-0" aria-hidden="true" />
          ¡Meta creada! Ya aparece en tus metas activas.
        </div>
      )}
    </div>
  );
}