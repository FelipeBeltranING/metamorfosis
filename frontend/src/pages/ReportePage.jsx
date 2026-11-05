import { useEffect, useId, useMemo, useState } from 'react';
import { ChartColumn } from 'lucide-react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArcElement, BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale,
  LineElement, PointElement, Tooltip,
} from 'chart.js';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import { obtenerReporte } from '../services/reporteService';
import {
  COLORES, etiquetaMes, etiquetaPlazo, etiquetaTipo, REPORTE_POR_DEFECTO, tablasDelReporte,
  TIPOS_REPORTE, tipoReporteValido,
} from '../utils/reporte';

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, LineElement, PointElement, Tooltip, Legend);

const FUENTE = { size: 13 };
const ENTEROS = { precision: 0, font: FUENTE };

/** Tarjeta de la gráfica elegida con su tabla de datos (alternativa para lectores de pantalla). */
function TarjetaGrafica({
  titulo, descripcion, tabla, children,
}) {
  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6">
      <div>
        <h2 className="text-lg font-bold text-gray-900">{titulo}</h2>
        <p className="text-sm text-gray-500">{descripcion}</p>
      </div>
      {children}
      <details className="text-sm text-gray-600">
        <summary className="cursor-pointer font-semibold text-violet-700">Ver datos en tabla</summary>
        <table className="mt-2 w-full text-left">
          <caption className="sr-only">{titulo}</caption>
          <thead>
            <tr>{tabla.columnas.map((c) => <th key={c} scope="col" className="border-b border-zinc-200 py-1.5 pr-2">{c}</th>)}</tr>
          </thead>
          <tbody>
            {tabla.filas.map((fila) => (
              <tr key={fila[0]}>
                {fila.map((celda, i) => (
                  i === 0
                    ? <th key={`${fila[0]}-c`} scope="row" className="border-b border-zinc-100 py-1.5 pr-2 font-medium">{celda}</th>
                    : <td key={`${fila[0]}-${tabla.columnas[i]}`} className="border-b border-zinc-100 py-1.5 pr-2">{celda}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </section>
  );
}

function Esqueleto() {
  return (
    <div className="flex animate-pulse flex-col gap-4" aria-hidden="true">
      <div className="h-11 w-full rounded-xl bg-slate-200 sm:w-80" />
      <div className="rounded-2xl border border-zinc-200 bg-white p-6">
        <div className="h-5 w-48 rounded bg-slate-200" />
        <div className="mt-6 h-72 rounded-xl bg-slate-100" />
      </div>
    </div>
  );
}

/**
 * HU7: reporte general. Un selector "Tipo de reporte" muestra una gráfica a la vez;
 * por defecto, el cumplimiento general. La opción elegida queda en la URL (?reporte=...).
 */
export default function ReportePage() {
  const navigate = useNavigate();
  const idSelector = useId();
  const [parametros, setParametros] = useSearchParams();
  const tipoReporte = tipoReporteValido(parametros.get('reporte'));
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let activo = true;
    obtenerReporte()
      .then((reporte) => activo && setEstado({ fase: 'listo', reporte }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else setEstado({ fase: 'error', mensaje: error.message });
      });
    return () => {
      activo = false;
    };
  }, [intento, navigate]);

  const reporte = estado.fase === 'listo' ? estado.reporte : null;
  const tablas = useMemo(() => (reporte ? tablasDelReporte(reporte) : null), [reporte]);

  const datos = useMemo(() => {
    if (!reporte) return null;
    return {
      general: {
        labels: ['Cumplidas', 'Activas'],
        datasets: [{
          data: [reporte.metasCumplidas, reporte.metasActivas],
          backgroundColor: [COLORES.cumplidas, COLORES.activas],
          borderWidth: 0,
        }],
      },
      porTipo: {
        labels: reporte.porTipo.map((t) => etiquetaTipo(t.tipo)),
        datasets: [{
          label: '% cumplido', data: reporte.porTipo.map((t) => t.porcentaje), backgroundColor: COLORES.cumplidas, borderRadius: 6,
        }],
      },
      porPlazo: {
        labels: reporte.porPlazo.map((p) => etiquetaPlazo(p.plazo)),
        datasets: [
          { label: 'Activas', data: reporte.porPlazo.map((p) => p.activas), backgroundColor: COLORES.activas, borderRadius: 4 },
          { label: 'Cumplidas', data: reporte.porPlazo.map((p) => p.cumplidas), backgroundColor: COLORES.cumplidas, borderRadius: 4 },
        ],
      },
      porMes: {
        labels: reporte.cumplidasPorMes.map((m) => etiquetaMes(m.mes)),
        datasets: [{
          label: 'Metas cumplidas',
          data: reporte.cumplidasPorMes.map((m) => m.cumplidas),
          borderColor: COLORES.linea,
          backgroundColor: COLORES.linea,
          pointRadius: 4,
          tension: 0.3,
        }],
      },
    };
  }, [reporte]);

  const cambiarTipoReporte = (valor) => {
    const siguientes = new URLSearchParams(parametros);
    if (valor === REPORTE_POR_DEFECTO) siguientes.delete('reporte');
    else siguientes.set('reporte', valor);
    setParametros(siguientes, { replace: true });
  };

  const grafica = () => {
    switch (tipoReporte) {
      case 'por-tipo':
        return (
          <TarjetaGrafica
            titulo="Cumplimiento por tipo de meta"
            descripcion="Porcentaje de metas cumplidas de cada tipo"
            tabla={tablas.porTipo}
          >
            <div className="h-72">
              <Bar
                data={datos.porTipo}
                options={{
                  indexAxis: 'y',
                  maintainAspectRatio: false,
                  plugins: { legend: { display: false } },
                  scales: {
                    x: { min: 0, max: 100, ticks: { callback: (v) => `${v} %`, font: FUENTE } },
                    y: { ticks: { font: FUENTE } },
                  },
                }}
                role="img"
                aria-label="Cumplimiento por tipo de meta"
              />
            </div>
          </TarjetaGrafica>
        );
      case 'por-plazo':
        return (
          <TarjetaGrafica
            titulo="Metas por plazo y estado"
            descripcion="Activas y cumplidas en cada plazo"
            tabla={tablas.porPlazo}
          >
            <div className="h-72">
              <Bar
                data={datos.porPlazo}
                options={{
                  maintainAspectRatio: false,
                  plugins: { legend: { position: 'bottom', labels: { font: FUENTE } } },
                  scales: {
                    x: { stacked: true, ticks: { font: FUENTE } },
                    y: { stacked: true, beginAtZero: true, ticks: ENTEROS },
                  },
                }}
                role="img"
                aria-label="Metas por plazo y estado"
              />
            </div>
          </TarjetaGrafica>
        );
      case 'por-mes':
        return (
          <TarjetaGrafica
            titulo="Metas cumplidas por mes"
            descripcion="Últimos 6 meses, según la fecha de cumplimiento"
            tabla={tablas.porMes}
          >
            <div className="h-72">
              <Line
                data={datos.porMes}
                options={{
                  maintainAspectRatio: false,
                  plugins: { legend: { display: false } },
                  scales: { y: { beginAtZero: true, ticks: ENTEROS }, x: { ticks: { font: FUENTE } } },
                }}
                role="img"
                aria-label="Metas cumplidas por mes en los últimos 6 meses"
              />
            </div>
          </TarjetaGrafica>
        );
      default:
        return (
          <TarjetaGrafica
            titulo="Cumplimiento general"
            descripcion={`${reporte.metasCumplidas} de ${reporte.totalMetas} metas cumplidas`}
            tabla={tablas.general}
          >
            <div className="relative mx-auto h-64 w-64">
              <Doughnut
                data={datos.general}
                options={{ cutout: '72%', maintainAspectRatio: false, plugins: { legend: { display: false } } }}
                role="img"
                aria-label={`Cumplimiento general: ${reporte.porcentajeCumplimiento} %`}
              />
              <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                <span className="text-4xl font-bold text-violet-700">{reporte.porcentajeCumplimiento} %</span>
                <span className="text-sm text-gray-500">cumplido</span>
              </div>
            </div>
            <div className="flex justify-center gap-6 text-sm text-gray-600">
              <span className="flex items-center gap-2">
                <span className="size-3 rounded-full" style={{ backgroundColor: COLORES.cumplidas }} aria-hidden="true" />
                {`Cumplidas: ${reporte.metasCumplidas}`}
              </span>
              <span className="flex items-center gap-2">
                <span className="size-3 rounded-full" style={{ backgroundColor: COLORES.activas }} aria-hidden="true" />
                {`Activas: ${reporte.metasActivas}`}
              </span>
            </div>
          </TarjetaGrafica>
        );
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto w-full max-w-[860px] px-4 pb-16 pt-6 sm:pt-12">
        <div className="mb-6">
          <h1 className="text-2xl font-bold sm:text-3xl">Mi reporte</h1>
          <p className="mt-1 text-sm text-gray-500 sm:text-base">Así vas con tus metas.</p>
        </div>

        {estado.fase === 'cargando' && <Esqueleto />}

        {estado.fase === 'error' && (
          <EstadoError
            mensaje={estado.mensaje}
            onReintentar={() => {
              setEstado({ fase: 'cargando' });
              setIntento((n) => n + 1);
            }}
          />
        )}

        {reporte && reporte.totalMetas === 0 && (
          <div className="flex flex-col items-center gap-5 px-4 py-16 text-center">
            <div className="flex size-20 items-center justify-center rounded-3xl bg-violet-50">
              <ChartColumn className="size-10 text-violet-600" aria-hidden="true" />
            </div>
            <div>
              <h2 className="text-xl font-bold">Aún no hay datos para tu reporte</h2>
              <p className="mt-2 text-gray-500">Crea tu primera meta y aquí verás cómo avanzas.</p>
            </div>
            <Link to="/metas/nueva" className="flex h-12 items-center rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700">
              Crear mi primera meta
            </Link>
          </div>
        )}

        {reporte && reporte.totalMetas > 0 && (
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor={idSelector} className="text-sm font-bold text-gray-600">Tipo de reporte</label>
              <select
                id={idSelector}
                value={tipoReporte}
                onChange={(e) => cambiarTipoReporte(e.target.value)}
                className="h-11 w-full rounded-xl border border-zinc-200 bg-white px-3 text-[15px] font-semibold text-gray-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 sm:w-80"
              >
                {TIPOS_REPORTE.map((t) => <option key={t.valor} value={t.valor}>{t.etiqueta}</option>)}
              </select>
            </div>
            {/*
              La key fuerza a montar una gráfica nueva al cambiar de tipo de reporte. Sin ella,
              react-chartjs-2 reutiliza el mismo Chart entre "por tipo" (barras horizontales,
              indexAxis 'y') y "por plazo" (barras verticales) y se queda con los ejes del anterior.
            */}
            <div key={tipoReporte}>{grafica()}</div>
          </div>
        )}
      </main>
    </div>
  );
}
