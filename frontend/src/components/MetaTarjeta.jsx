import { Check, Clock, Share2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import MenuTarjeta from './MenuTarjeta';
import AnilloConstancia from './AnilloConstancia';
import { formatearNumero } from '../utils/subMetas';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p]));

const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

/** "2026-11-01" → "1 nov 2026". Se arma con la fecha local para no correr el día por la zona horaria. */
function formatearFecha(iso) {
  if (!iso) return '';
  const [anio, mes, dia] = iso.split('-').map(Number);
  return FORMATO_FECHA.format(new Date(anio, mes - 1, dia)).replace('.', '');
}

function Chip({ Icono, texto }) {
  return (
    <span className="flex items-center gap-1.5 rounded-xl bg-slate-100 px-3 py-2 text-sm font-semibold text-gray-600">
      <Icono className="size-4" aria-hidden="true" />
      {texto}
    </span>
  );
}

/**
 * HU6: círculo de la tarjeta. Con la meta activa es un botón que abre la confirmación;
 * con la meta cumplida solo muestra el check, porque no se puede desmarcar.
 */
function CirculoCumplida({ nombreMeta, cumplida, onClick }) {
  const base = 'flex size-10 shrink-0 items-center justify-center rounded-full border';
  if (cumplida) {
    return (
      <span
        role="img"
        aria-label={`${nombreMeta} está cumplida`}
        title="Meta cumplida"
        className={`${base} border-emerald-500 bg-emerald-500 text-white`}
      >
        <Check className="size-5" aria-hidden="true" />
      </span>
    );
  }
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={`Marcar ${nombreMeta} como cumplida`}
      title="Marcar como cumplida"
      className={`${base} border-zinc-300 bg-white text-transparent transition hover:border-emerald-400 hover:text-emerald-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300`}
    >
      <Check className="size-5" aria-hidden="true" />
    </button>
  );
}

/** Sub-metas: valor de hoy de una sub-meta en la tarjeta ("20 / 15 págs", "Hecho", "Pendiente"). */
function valorHoy({ subMeta, dia }) {
  if (dia.valor === null || dia.valor === undefined) return 'Pendiente';
  if (subMeta.tipo === 'SI_NO') return Number(dia.valor) >= 1 ? 'Hecho' : 'No hecho';
  return `${formatearNumero(dia.valor)} / ${formatearNumero(subMeta.objetivo)} ${subMeta.unidad}`;
}

export default function MetaTarjeta({
  meta, resumen, onMarcarCumplida, onEditar, onEliminar, onCompartir,
}) {
  const tipo = TIPO_POR_VALOR[meta.tipo] ?? TIPO_POR_VALOR.OTRO;
  const plazo = PLAZO_POR_VALOR[meta.plazo];
  const cumplida = meta.estado === 'CUMPLIDA';

  return (
    <li className={`flex flex-col gap-5 rounded-2xl border p-5 shadow-[0_4px_20px_rgba(30,27,46,0.03)] transition sm:p-8 ${
      cumplida ? 'border-emerald-300 bg-emerald-50/40' : 'border-zinc-200 bg-white hover:border-violet-300'
    }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50">
          <tipo.Icono className="size-6 text-violet-600" aria-hidden="true" />
        </div>
        <div className="flex items-center gap-2">
          {cumplida ? (
            <span className="rounded-xl bg-violet-600 px-3 py-2 text-sm font-bold text-white">Cumplida</span>
          ) : (
            <span className="flex items-center gap-1.5 rounded-xl bg-emerald-100 px-3 py-2 text-sm font-bold text-emerald-800">
              <span className="text-[10px]" aria-hidden="true">●</span>
              Activa
            </span>
          )}
          {onMarcarCumplida && (
            <CirculoCumplida
              nombreMeta={meta.nombre}
              cumplida={cumplida}
              onClick={() => onMarcarCumplida(meta)}
            />
          )}
          {(onEditar || onEliminar) && (
            <MenuTarjeta
              nombre={meta.nombre}
              onEditar={onEditar && (() => onEditar(meta))}
              editarBloqueado={cumplida}
              onEliminar={onEliminar && (() => onEliminar(meta))}
            />
          )}
        </div>
      </div>
      <h2 className="line-clamp-2 break-words text-xl font-bold text-gray-900 sm:text-2xl">
        {meta.nombre}
      </h2>
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          <Chip Icono={tipo.Icono} texto={tipo.etiqueta} />
          {plazo && <Chip Icono={Clock} texto={plazo.etiqueta} />}
        </div>
        {resumen?.totalSubMetas > 0 && <AnilloConstancia porcentaje={resumen.constancia7} />}
      </div>

      {/* Sub-metas: lo que toca hoy y el acceso a "Tu camino". */}
      {resumen && !cumplida && (
        resumen.totalSubMetas > 0 ? (
          <div className="flex flex-col gap-2.5 border-t border-zinc-100 pt-4">
            <p className="text-sm font-bold text-gray-600">
              {resumen.tocanHoy > 0
                ? `Hoy · ${resumen.cumplidasHoy} de ${resumen.tocanHoy} sub-metas`
                : 'Hoy no toca ninguna sub-meta'}
            </p>
            <ul className="flex flex-col gap-2">
              {resumen.hoy.map((item) => (
                <li key={item.subMeta.codigoSubMeta} className="flex items-center gap-2.5 text-[15px]">
                  {item.dia.cumplido ? (
                    <span aria-hidden="true" className="flex size-[22px] shrink-0 items-center justify-center rounded-full bg-emerald-500 text-white">
                      <Check className="size-3.5" strokeWidth={3} />
                    </span>
                  ) : (
                    <span aria-hidden="true" className="block size-[22px] shrink-0 rounded-full border-2 border-zinc-300" />
                  )}
                  <span className="min-w-0 flex-1 truncate">{item.subMeta.nombre}</span>
                  <span className={`text-sm font-bold ${item.dia.cumplido ? 'text-emerald-700' : 'text-gray-600'}`}>
                    {valorHoy(item)}
                  </span>
                </li>
              ))}
            </ul>
            <Link
              to={`/metas/${meta.codigoMeta}`}
              className="mt-1 flex h-11 items-center justify-center rounded-xl border border-violet-300 bg-violet-50 font-bold text-violet-800 hover:bg-violet-100"
            >
              Ver mi camino
            </Link>
          </div>
        ) : (
          <div className="flex flex-col gap-3 border-t border-zinc-100 pt-4">
            <p className="text-[15px] leading-6 text-gray-600">
              Esta meta aún no tiene un camino. Divídela en sub-metas diarias para saber qué hacer cada día.
            </p>
            <Link
              to={`/metas/${meta.codigoMeta}`}
              className="flex h-11 items-center justify-center rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700"
            >
              + Agregar la primera sub-meta
            </Link>
          </div>
        )
      )}
      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-zinc-100 pt-4 text-sm text-gray-500">
        {meta.fechaCreacion && <span>Creada el {formatearFecha(meta.fechaCreacion)}</span>}
        {cumplida && meta.fechaCumplida ? (
          <span className="flex items-center gap-1 font-semibold text-emerald-700">
            <Check className="size-4" aria-hidden="true" />
            Cumplida el {formatearFecha(meta.fechaCumplida)}
          </span>
        ) : (
          <span>Pendiente</span>
        )}
      </div>
      {/* HU21: una meta cumplida se puede compartir como logro. */}
      {cumplida && onCompartir && (
        <button
          type="button"
          onClick={() => onCompartir(meta)}
          className="flex h-11 items-center justify-center gap-2 rounded-xl border border-violet-200 bg-white font-semibold text-violet-700 hover:bg-violet-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
        >
          <Share2 className="size-4" aria-hidden="true" />
          Compartir logro
        </button>
      )}
    </li>
  );
}
