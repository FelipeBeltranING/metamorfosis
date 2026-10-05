/**
 * Opción seleccionable (radio con apariencia de botón).
 * vertical = true: estilo de "plazo" (círculo arriba); false: estilo de "tipo" (icono a la izquierda).
 */
export default function BotonOpcion({
  grupo, valor, etiqueta, Icono, seleccionado, conError, onChange, vertical = false,
}) {
  const borde = seleccionado
    ? 'border-violet-600 bg-violet-50'
    : conError
      ? 'border-red-500'
      : 'border-zinc-200';
  const disposicion = vertical ? 'min-h-16 flex-col justify-center text-center' : 'min-h-14';

  return (
    <label
      className={`flex cursor-pointer items-center gap-2 rounded-xl border bg-white px-2.5 text-sm font-semibold text-gray-900 transition hover:border-violet-400 focus-within:ring-2 focus-within:ring-violet-300 ${disposicion} ${borde}`}
    >
      <input
        type="radio"
        name={grupo}
        value={valor}
        checked={seleccionado}
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