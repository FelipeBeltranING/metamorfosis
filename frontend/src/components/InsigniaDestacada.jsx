import IconoInsignia from './IconoInsignia';

/** HU19: la insignia que la persona eligió mostrar junto a su nombre. */
export default function InsigniaDestacada({ insignia }) {
  if (!insignia) return null;
  return (
    <span
      title={insignia.descripcion}
      className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-violet-600 py-1 pl-1.5 pr-3 text-[13px] font-bold text-white"
    >
      <span className="flex size-5 items-center justify-center rounded-full bg-white/20">
        <IconoInsignia nombre={insignia.icono} className="size-3.5" aria-hidden="true" />
      </span>
      {insignia.nombre}
    </span>
  );
}
