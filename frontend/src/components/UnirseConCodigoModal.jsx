import { useEffect, useRef, useState } from 'react';
import { CircleAlert, KeyRound, LoaderCircle, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { unirseConCodigo } from '../services/invitacionService';

/** "abcd efgh" → "ABCD-EFGH" mientras se escribe (sin letras confusas no hace falta más). */
function formatear(texto) {
  const limpio = texto.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8);
  return limpio.length > 4 ? `${limpio.slice(0, 4)}-${limpio.slice(4)}` : limpio;
}

/** Unirse a una meta con el código que compartió quien la creó. Al lograrlo abre la meta. */
export default function UnirseConCodigoModal({ onCerrar }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [codigo, setCodigo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const campo = useRef(null);

  useEffect(() => { campo.current?.focus(); }, []);
  useEffect(() => {
    const alPresionar = (e) => e.key === 'Escape' && !enviando && onCerrar();
    document.addEventListener('keydown', alPresionar);
    return () => document.removeEventListener('keydown', alPresionar);
  }, [onCerrar, enviando]);

  const enviar = async (e) => {
    e.preventDefault();
    if (codigo.replace('-', '').length < 8) {
      setError(t('unirse.codigoCorto'));
      return;
    }
    setEnviando(true);
    setError('');
    try {
      const meta = await unirseConCodigo(codigo);
      navigate(`/metas/${meta.codigoMeta}`, { state: { unido: true } });
    } catch (err) {
      if (err.estado === 401) navigate('/login', { replace: true });
      setError(err.message);
      setEnviando(false);
    }
  };

  return (
    <div role="presentation" className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center sm:p-4" onMouseDown={(e) => e.target === e.currentTarget && !enviando && onCerrar()}>
      <form role="dialog" aria-modal="true" aria-labelledby="titulo-unirse" onSubmit={enviar} className="w-full max-w-md rounded-t-2xl bg-white p-6 shadow-2xl sm:rounded-2xl">
        <div className="mb-4 flex items-start justify-between">
          <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50 text-violet-600">
            <KeyRound className="size-6" aria-hidden="true" />
          </div>
          <button type="button" onClick={onCerrar} aria-label={t('unirse.cerrarVentana')} className="rounded-lg p-2 text-gray-500 hover:bg-gray-100">
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>
        <h2 id="titulo-unirse" className="text-xl font-bold text-gray-900">{t('unirse.titulo')}</h2>
        <p className="mt-1 text-sm leading-6 text-gray-600">{t('unirse.texto')}</p>
        <label htmlFor="codigo-union" className="mt-5 block text-sm font-bold text-gray-700">{t('unirse.codigo')}</label>
        <input
          ref={campo}
          id="codigo-union"
          value={codigo}
          onChange={(e) => { setCodigo(formatear(e.target.value)); setError(''); }}
          placeholder="ABCD-EFGH"
          autoComplete="off"
          autoCapitalize="characters"
          spellCheck={false}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? 'error-codigo-union' : undefined}
          className={`mt-1.5 h-14 w-full rounded-xl border bg-white px-4 text-center font-mono text-2xl font-extrabold tracking-[0.15em] text-gray-900 outline-none placeholder:text-gray-300 focus:ring-2 focus:ring-violet-300 ${error ? 'border-2 border-red-500' : 'border-zinc-300'}`}
        />
        {error && (
          <p id="error-codigo-union" role="alert" className="mt-2 flex items-start gap-1.5 text-sm font-semibold text-red-600">
            <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />{error}
          </p>
        )}
        <button type="submit" disabled={enviando} className="mt-5 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700 disabled:opacity-60">
          {enviando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
          {t('unirse.boton')}
        </button>
      </form>
    </div>
  );
}
