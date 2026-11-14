import { useMemo } from 'react';
import { dibujoAvatar } from '../constants/avatar';

/**
 * HU20: dibujo del avatar (estilo «Big Smile» de DiceBear). `volteado` lo hace mirar a la izquierda,
 * hacia el nombre, cuando va asomado a la derecha de la cabecera del perfil.
 */
export default function AvatarIlustrado({
  avatar, className = '', alt = '', volteado = false,
}) {
  const src = useMemo(() => dibujoAvatar(avatar), [avatar]);
  return (
    <img
      src={src}
      alt={alt}
      aria-hidden={alt ? undefined : true}
      draggable={false}
      className={`${volteado ? '-scale-x-100' : ''} select-none ${className}`}
    />
  );
}
