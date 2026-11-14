import { useEffect, useState } from 'react';
import {
  Check, Copy, KeyRound, LoaderCircle, RefreshCw,
} from 'lucide-react';
import {
  desactivarCodigoMeta, generarCodigoMeta, mostrarCodigo, verCodigoMeta,
} from '../services/invitacionService';
import { useTranslation } from 'react-i18next';
import { localeActual } from '../i18n';

const fechaCorta = (fecha) => new Intl.DateTimeFormat(localeActual(), { day: 'numeric', month: 'short' }).format(fecha).replace('.', '');

/** Código para unirse a la meta: crearlo, copiarlo, cambiarlo o desactivarlo (solo el creador). */
export default function CodigoInvitacion({ codigoMeta }) {
  const { t } = useTranslation();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [ocupado, setOcupado] = useState('');
  const [copiado, setCopiado] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let activo = true;
    verCodigoMeta(codigoMeta)
      .then((r) => activo && setEstado({ fase: 'listo', ...r }))
      .catch((e) => activo && setEstado({ fase: 'error', mensaje: e.message }));
    return () => { activo = false; };
  }, [codigoMeta]);

  useEffect(() => {
    if (!copiado) return undefined;
    const temporizador = setTimeout(() => setCopiado(false), 2500);
    return () => clearTimeout(temporizador);
  }, [copiado]);

  const ejecutar = async (que, accion) => {
    setOcupado(que);
    setError('');
    try {
      const r = await accion();
      setEstado({ fase: 'listo', codigo: r?.codigo ?? null, expira: r?.expira ?? null });
    } catch (e) {
      setError(e.message);
    } finally {
      setOcupado('');
    }
  };

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(mostrarCodigo(estado.codigo));
      setCopiado(true);
    } catch {
      setError(t('codigo.errorCopiar'));
    }
  };

  return (
    <div className="flex flex-col gap-3 rounded-xl border border-zinc-200 p-4">
      <div className="flex items-start gap-3">
        <KeyRound className="mt-0.5 size-5 shrink-0 text-violet-600" aria-hidden="true" />
        <div>
          <h3 className="text-sm font-bold text-gray-900">{t('codigo.titulo')}</h3>
          <p className="text-[13px] leading-5 text-gray-600">{t('codigo.texto')}</p>
        </div>
      </div>

      {estado.fase === 'cargando' && <LoaderCircle className="size-5 animate-spin text-gray-400" aria-label={t('codigo.cargando')} />}
      {estado.fase === 'error' && <p role="alert" className="text-sm font-semibold text-red-600">{estado.mensaje}</p>}

      {estado.fase === 'listo' && !estado.codigo && (
        <button
          type="button"
          onClick={() => ejecutar('crear', () => generarCodigoMeta(codigoMeta))}
          disabled={ocupado !== ''}
          className="flex h-10 items-center justify-center gap-2 self-start rounded-xl border border-violet-300 bg-white px-4 text-sm font-bold text-violet-700 hover:bg-violet-50 disabled:opacity-60"
        >
          {ocupado === 'crear' && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
          {t('codigo.crear')}
        </button>
      )}

      {estado.fase === 'listo' && estado.codigo && (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <output aria-label={t('codigo.etiqueta')} className="select-all rounded-xl bg-violet-50 px-4 py-2.5 font-mono text-2xl font-extrabold tracking-[0.15em] text-violet-800">
              {mostrarCodigo(estado.codigo)}
            </output>
            <button
              type="button"
              onClick={copiar}
              className="flex h-10 items-center gap-1.5 rounded-xl bg-violet-600 px-4 text-sm font-bold text-white hover:bg-violet-700"
            >
              {copiado ? <Check className="size-4" aria-hidden="true" /> : <Copy className="size-4" aria-hidden="true" />}
              {copiado ? t('codigo.copiado') : t('codigo.copiar')}
            </button>
          </div>
          <p className="text-[13px] text-gray-500">
            {t('codigo.sirveHasta', { fecha: fechaCorta(new Date(estado.expira)) })}
          </p>
          <div className="flex flex-wrap gap-x-4 gap-y-1">
            <button
              type="button"
              onClick={() => ejecutar('cambiar', () => generarCodigoMeta(codigoMeta))}
              disabled={ocupado !== ''}
              className="flex items-center gap-1.5 text-sm font-bold text-gray-700 hover:underline disabled:opacity-60"
            >
              {ocupado === 'cambiar' ? <LoaderCircle className="size-4 animate-spin" aria-hidden="true" /> : <RefreshCw className="size-4" aria-hidden="true" />}
              {t('codigo.cambiar')}
            </button>
            <button
              type="button"
              onClick={() => ejecutar('desactivar', () => desactivarCodigoMeta(codigoMeta))}
              disabled={ocupado !== ''}
              className="text-sm font-bold text-red-600 hover:underline disabled:opacity-60"
            >
              {t('codigo.desactivar')}
            </button>
          </div>
        </>
      )}
      {error && <p role="alert" className="text-sm font-semibold text-red-600">{error}</p>}
    </div>
  );
}
