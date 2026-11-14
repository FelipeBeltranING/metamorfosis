import { useState } from 'react';
import { CircleAlert, LoaderCircle, X } from 'lucide-react';
import { Trans, useTranslation } from 'react-i18next';
import { diaCorto, diaYMes } from '../utils/fechas';
import { formatearNumero } from '../utils/subMetas';

/**
 * Sub-metas (registrar el día): escribir el valor de una sub-meta medible en un día, o
 * corregirlo / quitarlo si ya estaba registrado. En el celular sube desde abajo.
 */
export default function RegistrarValorModal({ subMeta, fecha, esHoy, valorActual, onGuardar, onQuitar, onCerrar }) {
  const { t } = useTranslation();
  const yaRegistrado = valorActual !== null && valorActual !== undefined;
  const [texto, setTexto] = useState(yaRegistrado ? String(Number(valorActual)) : '');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');

  const objetivo = Number(subMeta.objetivo);
  const alMenos = subMeta.tipoObjetivo !== 'COMO_MAXIMO';
  const valor = texto === '' ? null : Number(texto.replace(',', '.'));
  const valido = valor !== null && Number.isFinite(valor) && valor >= 0;
  const cumple = valido && (alMenos ? valor >= objetivo : valor <= objetivo);

  const sumar = (n) => setTexto((actual) => String(Math.max(0, Math.round(((Number(actual.replace(',', '.')) || 0) + n) * 100) / 100)));

  const ejecutar = async (accion) => {
    setGuardando(true);
    setError('');
    try {
      await accion();
    } catch (e) {
      setError(e.estado
        ? e.message
        : t('registrarValor.errorGuardar'));
      setGuardando(false);
    }
  };

  const unidad = subMeta.unidad;
  let mensaje = t('registrarValor.escribe', {
    tipo: alMenos ? t('registrarValor.alMenos') : t('registrarValor.comoMaximo'), objetivo: formatearNumero(objetivo), unidad,
  });
  if (valido && cumple) mensaje = t('registrarValor.cumplido', { valor: formatearNumero(valor), objetivo: formatearNumero(objetivo) });
  else if (valido && alMenos) mensaje = t('registrarValor.faltan', { diferencia: formatearNumero(objetivo - valor), unidad, objetivo: formatearNumero(objetivo) });
  else if (valido) mensaje = t('registrarValor.pasaste', { diferencia: formatearNumero(valor - objetivo), unidad, objetivo: formatearNumero(objetivo) });
  const progreso = valido ? Math.min(100, Math.round((100 * valor) / objetivo)) : 0;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center sm:p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !guardando) onCerrar();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-registrar-valor"
        className="w-full max-w-md rounded-t-3xl bg-white px-5 pb-7 pt-3 shadow-2xl sm:rounded-2xl sm:p-7"
      >
        <div aria-hidden="true" className="mx-auto mb-3 h-1 w-10 rounded-full bg-gray-300 sm:hidden" />
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs font-extrabold uppercase tracking-wide text-violet-700">
              {subMeta.nombre} · {esHoy ? t('registrarValor.hoy') : diaCorto(fecha)}, {diaYMes(fecha)}
            </p>
            <h2 id="titulo-registrar-valor" className="mt-1 text-xl font-extrabold text-gray-900">
              {subMeta.pregunta || t('registrarValor.cuanto', { unidad: subMeta.unidad })}
            </h2>
          </div>
          <button
            type="button"
            onClick={onCerrar}
            disabled={guardando}
            aria-label={t('registrarValor.cerrarVentana')}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 disabled:opacity-50"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        {yaRegistrado && (
          <p className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-sm text-gray-600">
            <Trans i18nKey="registrarValor.habias" values={{ valor: formatearNumero(valorActual), unidad: subMeta.unidad }} components={{ b: <strong /> }} />
          </p>
        )}

        <div className="mt-5 flex items-center justify-center gap-4">
          <button
            type="button"
            onClick={() => sumar(-1)}
            disabled={guardando}
            aria-label={t('registrarValor.restar', { unidad: subMeta.unidad })}
            className="flex size-[52px] items-center justify-center rounded-full border border-zinc-200 bg-white text-2xl font-bold text-gray-700 hover:bg-gray-50"
          >
            −
          </button>
          <div className="flex flex-col items-center">
            <label htmlFor="valor-dia" className="sr-only">{t('registrarValor.cantidad', { unidad: subMeta.unidad })}</label>
            <input
              id="valor-dia"
              type="number"
              inputMode="decimal"
              min="0"
              step="any"
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              placeholder="0"
              className="w-36 bg-transparent text-center text-5xl font-extrabold text-gray-900 outline-none [appearance:textfield] focus:ring-2 focus:ring-violet-300 rounded-xl [&::-webkit-inner-spin-button]:appearance-none"
            />
            <span className="text-sm font-bold text-gray-600">{subMeta.unidad}</span>
          </div>
          <button
            type="button"
            onClick={() => sumar(1)}
            disabled={guardando}
            aria-label={t('registrarValor.sumar', { unidad: subMeta.unidad })}
            className="flex size-[52px] items-center justify-center rounded-full bg-violet-600 text-2xl font-bold text-white hover:bg-violet-700"
          >
            +
          </button>
        </div>
        <div className="mt-3 flex justify-center gap-2">
          {[5, 10, 20].map((n) => (
            <button
              key={n}
              type="button"
              onClick={() => sumar(n)}
              disabled={guardando}
              className="h-9 rounded-full border border-zinc-200 bg-slate-50 px-3.5 text-sm font-bold text-gray-700 hover:bg-gray-100"
            >
              +{n}
            </button>
          ))}
        </div>

        <div className="mt-5">
          <div role="img" aria-label={valido ? t('registrarValor.progreso', { valor: formatearNumero(valor), objetivo: formatearNumero(objetivo), unidad: subMeta.unidad }) : t('registrarValor.sinValor')} className="h-3 overflow-hidden rounded-full bg-slate-100">
            <div className={`h-3 rounded-full ${cumple ? 'bg-emerald-500' : 'bg-violet-400'}`} style={{ width: `${progreso}%` }} />
          </div>
          <p aria-live="polite" className={`mt-2 text-[15px] font-bold ${cumple ? 'text-emerald-700' : 'text-gray-700'}`}>{mensaje}</p>
        </div>

        {error && (
          <p role="alert" className="mt-4 flex gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-700">
            <CircleAlert className="size-5 shrink-0" aria-hidden="true" />
            <span>{error}</span>
          </p>
        )}

        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={onCerrar}
            disabled={guardando}
            className="h-12 flex-1 rounded-xl border border-zinc-200 font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            {t('registrarValor.cancelar')}
          </button>
          <button
            type="button"
            onClick={() => ejecutar(() => onGuardar(valor))}
            disabled={guardando || !valido}
            className="flex h-12 flex-1 items-center justify-center gap-2 rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-300"
          >
            {guardando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
            {error ? t('registrarValor.reintentar') : t('registrarValor.guardar')}
          </button>
        </div>
        {yaRegistrado && (
          <div className="mt-4 flex items-center justify-between gap-3 border-t border-zinc-100 pt-4">
            <span className="text-sm text-gray-600">{t('registrarValor.porError')}</span>
            <button
              type="button"
              onClick={() => ejecutar(onQuitar)}
              disabled={guardando}
              className="h-10 rounded-lg border border-red-200 px-3.5 text-sm font-bold text-red-700 hover:bg-red-50 disabled:opacity-50"
            >
              {t('registrarValor.quitar')}
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
