
import { CircleAlert, LoaderCircle, Trash2, X } from 'lucide-react';
import { Trans, useTranslation } from 'react-i18next';

export default function ConfirmarEliminarModal({
  meta,
  resumen,
  eliminando,
  error,
  onConfirmar,
  onCancelar,
}) {
  const { t } = useTranslation();
  const nombreMeta = meta?.titulo || meta?.nombre || t('eliminarMeta.estaMeta');

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !eliminando) {
          onCancelar();
        }
      }}
    >
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="titulo-confirmar-eliminar"
        aria-describedby="descripcion-confirmar-eliminar"
        className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"
      >
        <div className="mb-5 flex items-start justify-between">
          <div className="flex size-12 items-center justify-center rounded-xl bg-red-50 text-red-600">
            <Trash2 className="size-6" aria-hidden="true" />
          </div>

          <button
            type="button"
            onClick={onCancelar}
            disabled={eliminando}
            aria-label={t('eliminarMeta.cerrarVentana')}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 disabled:opacity-50"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <h2
          id="titulo-confirmar-eliminar"
          className="text-xl font-bold text-gray-900"
        >
          {t('eliminarMeta.titulo')}
        </h2>

        <p
          id="descripcion-confirmar-eliminar"
          className="mt-3 text-sm leading-6 text-gray-600"
        >
          <Trans i18nKey="eliminarMeta.seguro" values={{ nombre: nombreMeta }} components={{ b: <strong /> }} />
          {resumen?.totalSubMetas > 0 ? t('eliminarMeta.conCamino') : t('eliminarMeta.sinDeshacer')}
        </p>

        {/* HU4 + sub-metas: se borran en cascada sus sub-metas y registros. */}
        {resumen?.totalSubMetas > 0 && (
          <>
            <ul className="mt-3 flex flex-col gap-1.5 text-sm text-gray-700">
              <li className="rounded-lg bg-slate-50 px-3 py-2">
                <strong className="text-gray-900">{resumen.totalSubMetas}</strong>
                {t('eliminarMeta.subMetas', { count: resumen.totalSubMetas })}
              </li>
              <li className="rounded-lg bg-slate-50 px-3 py-2">
                <strong className="text-gray-900">{resumen.totalRegistros}</strong>
                {t('eliminarMeta.dias', { count: resumen.totalRegistros })}
              </li>
            </ul>
            <p className="mt-3 text-sm text-gray-600">{t('eliminarMeta.noSeDeshace')}</p>
          </>
        )}

        {error && (
          <div
            role="alert"
            className="mt-4 flex gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-700"
          >
            <CircleAlert
              className="size-5 shrink-0"
              aria-hidden="true"
            />
            <p>{error}</p>
          </div>
        )}

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancelar}
            disabled={eliminando}
            className="h-11 rounded-xl border border-gray-200 px-5 font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {t('eliminarMeta.cancelar')}
          </button>

          <button
            type="button"
            onClick={onConfirmar}
            disabled={eliminando}
            className="flex h-11 items-center justify-center gap-2 rounded-xl bg-red-600 px-5 font-semibold text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {eliminando ? (
              <>
                <LoaderCircle
                  className="size-4 animate-spin"
                  aria-hidden="true"
                />
                {t('eliminarMeta.eliminando')}
              </>
            ) : (
              <>
                <Trash2 className="size-4" aria-hidden="true" />
                {t('eliminarMeta.eliminar')}
              </>
            )}
          </button>
        </div>
      </section>
    </div>
  );
}
