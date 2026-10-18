/**
 * Opción seleccionable (radio con apariencia de botón).
 * vertical = true: estilo de "plazo" (círculo arriba); false: estilo de "tipo" (icono a la izquierda).
 */
export default function BotonOpcion({
  grupo, valor, etiqueta, Icono, seleccionado, conError, deshabilitado = false, onChange, vertical = false,
}) {
  const borde = seleccionado
    ? 'border-violet-600 bg-violet-50'
    : conError
      ? 'border-red-500 bg-white'
      : deshabilitado
        ? 'border-zinc-200 bg-slate-100'
        : 'border-zinc-200 bg-white';
  const interaccion = deshabilitado
    ? 'cursor-not-allowed'
    : 'cursor-pointer transition hover:border-violet-400 focus-within:ring-2 focus-within:ring-violet-300';
  const disposicion = vertical ? 'min-h-16 flex-col justify-center text-center' : 'min-h-14';

  return (
    <label
      className={`flex items-center gap-2 rounded-xl border px-2.5 text-sm font-semibold text-gray-900 ${interaccion} ${disposicion} ${borde}`}
    >
      <input
        type="radio"
        name={grupo}
        value={valor}
        checked={seleccionado}
        disabled={deshabilitado}
        onChange={() => onChange(valor)}
        className="sr-only"
      />
      {vertical ? (
        <span
          aria-hidden="true"
          className={`size-4 rounded-full border-[1.5px] ${
            seleccionado ? 'border-violet-600 bg-violet-600 ring-2 ring-inset ring-white' : 'border-zinc-300 bg-white'
          }`}
        />
      ) : (
        Icono && (
          <Icono
            aria-hidden="true"
            className={`size-4 shrink-0 ${seleccionado ? 'text-violet-600' : 'text-gray-500'}`}
          />
        )
      )}
      <span className={vertical ? '' : 'flex-1 leading-4'}>{etiqueta}</span>
    </label>
  );
}