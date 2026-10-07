import { CircleAlert } from 'lucide-react';

export default function EstadoError({ mensaje, onReintentar }) {
  return (
    <div
      role="alert"
      className="flex flex-col gap-4 rounded-2xl bg-red-50 p-5 text-red-700 sm:flex-row sm:items-center sm:gap-5 sm:p-6"
    >
      <CircleAlert className="size-6 shrink-0" aria-hidden="true" />
      <div className="flex flex-1 flex-col gap-1">
        <p className="text-base font-bold">No pudimos cargar tus metas</p>
        <p className="text-base">{mensaje || 'Comprueba tu conexión e inténtalo de nuevo.'}</p>
      </div>
      <button
        type="button"
        onClick={onReintentar}
        className="h-12 rounded-xl border border-zinc-200 bg-white px-6 text-base font-bold text-violet-600 hover:bg-slate-50 sm:w-32"
      >
        Reintentar
      </button>
    </div>
  );
}