import AvatarUsuario from './AvatarUsuario';
import { inicialesDe } from '../services/sesionService';

/** HU13: hasta tres avatares encimados y "+N" si hay más. */
export function AvataresEncimados({ personas, clase = 'size-8 text-xs', maximo = 3 }) {
  const visibles = personas.slice(0, maximo);
  const resto = personas.length - visibles.length;
  return (
    <span className="flex items-center" aria-hidden="true">
      {visibles.map((p, i) => (
        <span
          key={p.codigoUsuario}
          className={`rounded-full ring-2 ring-white ${i > 0 ? '-ml-1' : ''} ${p.estado === 'PENDIENTE' ? 'opacity-60' : ''}`}
        >
          <AvatarUsuario foto={p.foto} iniciales={inicialesDe(p.nombre, p.apellido)} clase={clase} />
        </span>
      ))}
      {resto > 0 && (
        <span className={`-ml-1 flex items-center justify-center rounded-full bg-slate-100 font-extrabold text-gray-700 ring-2 ring-white ${clase}`}>
          +{resto}
        </span>
      )}
    </span>
  );
}
