import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Trophy } from 'lucide-react';
import Encabezado from '../components/Encabezado';
import MetaTarjeta from '../components/MetaTarjeta';
import MetaTarjetaEsqueleto from '../components/MetaTarjetaEsqueleto';
import EstadoError from '../components/EstadoError';
import CompartirLogroModal from '../components/CompartirLogroModal';
import { listarCumplidas } from '../services/metaService';

/** HU21 (Mis logros): metas cumplidas del usuario, cada una con su botón para compartirla. */
export default function MisLogrosPage() {
  const navigate = useNavigate();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [metaACompartir, setMetaACompartir] = useState(null);

  useEffect(() => {
    let activo = true;
    listarCumplidas()
      .then((metas) => activo && setEstado({ fase: 'lista', metas }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) {
          navigate('/login', { replace: true });
          return;
        }
        setEstado({ fase: 'error', mensaje: error.message });
      });
    return () => {
      activo = false;
    };
  }, [intento, navigate]);

  const reintentar = () => {
    setEstado({ fase: 'cargando' });
    setIntento((n) => n + 1);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />

      <main className="mx-auto w-full max-w-[960px] px-4 pb-10 pt-6 sm:pt-12">
        <div className="mb-6">
          <h1 className="text-2xl font-bold sm:text-3xl">Mis logros</h1>
          <p className="mt-1 text-sm text-gray-500 sm:text-base">
            Las metas que ya cumpliste. Compártelas con quien quieras.
          </p>
        </div>

        {estado.fase === 'cargando' && (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2" aria-busy="true">
            {Array.from({ length: 2 }, (_, i) => <MetaTarjetaEsqueleto key={i} />)}
          </ul>
        )}

        {estado.fase === 'error' && (
          <EstadoError mensaje={estado.mensaje} onReintentar={reintentar} />
        )}

        {estado.fase === 'lista' && estado.metas.length === 0 && (
          <section className="flex flex-col items-center rounded-2xl border border-dashed border-zinc-300 bg-white px-6 py-14 text-center">
            <div className="flex size-16 items-center justify-center rounded-2xl bg-violet-50 text-violet-600">
              <Trophy className="size-8" aria-hidden="true" />
            </div>
            <h2 className="mt-4 text-lg font-bold">Aún no tienes logros</h2>
            <p className="mt-1 max-w-sm text-sm text-gray-500">
              Cuando marques una meta como cumplida, aparecerá aquí.
            </p>
            <Link
              to="/metas"
              className="mt-6 inline-flex h-11 items-center rounded-xl bg-violet-600 px-5 font-semibold text-white hover:bg-violet-700"
            >
              Ir a mis metas
            </Link>
          </section>
        )}

        {estado.fase === 'lista' && estado.metas.length > 0 && (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {estado.metas.map((meta) => (
              <MetaTarjeta key={meta.codigoMeta} meta={meta} onCompartir={setMetaACompartir} />
            ))}
          </ul>
        )}
      </main>

      {metaACompartir && (
        <CompartirLogroModal meta={metaACompartir} onCerrar={() => setMetaACompartir(null)} />
      )}
    </div>
  );
}
