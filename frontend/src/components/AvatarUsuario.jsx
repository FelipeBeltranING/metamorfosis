import { User } from 'lucide-react';

/**
 * HU-23: avatar redondo del usuario: su foto, o sus iniciales si no tiene, o un ícono si
 * tampoco hay nombre. `clase` da el tamaño (p. ej. "size-12 text-base").
 */
export default function AvatarUsuario({ foto, iniciales, clase = 'size-12 text-base' }) {
  if (foto) {
    return <img src={foto} alt="" className={`${clase} shrink-0 rounded-full object-cover`} />;
  }
  return (
    <span
      aria-hidden="true"
      className={`${clase} flex shrink-0 items-center justify-center rounded-full bg-violet-100 font-extrabold text-violet-700`}
    >
      {iniciales || <User className="size-1/2" />}
    </span>
  );
}
