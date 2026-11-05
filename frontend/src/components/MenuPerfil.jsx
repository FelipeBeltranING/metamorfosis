import { useEffect, useRef, useState } from 'react';
import { ChevronRight, User, UserRound } from 'lucide-react';
import { Link } from 'react-router-dom';
import { datosPerfil, obtenerSesion } from '../services/sesionService';

/**
 * Avatar del encabezado: muestra el nombre y el correo de la cuenta y la opción "Ver mi perfil" (HU22).
 * "Cerrar sesión" está en el encabezado (BotonCerrarSesion).
 * Se cierra al tocar fuera o con Esc.
 */
export default function MenuPerfil() {
  const [perfil] = useState(() => datosPerfil(obtenerSesion()));
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);
  const boton = useRef(null);

  useEffect(() => {
    if (!abierto) return undefined;
    const alTocarFuera = (evento) => {
      if (!contenedor.current?.contains(evento.target)) setAbierto(false);
    };
    const alPresionarTecla = (evento) => {
      if (evento.key === 'Escape') {
        setAbierto(false);
        boton.current?.focus();
      }
    };
    document.addEventListener('pointerdown', alTocarFuera);
    document.addEventListener('keydown', alPresionarTecla);
    return () => {
      document.removeEventListener('pointerdown', alTocarFuera);
      document.removeEventListener('keydown', alPresionarTecla);
    };
  }, [abierto]);

  const avatar = 'flex size-10 items-center justify-center rounded-full bg-violet-100 sm:size-12';

  if (!perfil) {
    return (
      <div className={avatar}>
        <User className="size-6 text-violet-600" aria-label="Perfil" />
      </div>
    );
  }

  return (
    <div ref={contenedor} className="relative">
      <button
        ref={boton}
        type="button"
        onClick={() => setAbierto((actual) => !actual)}
        aria-haspopup="dialog"
        aria-expanded={abierto}
        aria-label="Tu cuenta"
        className={`${avatar} transition hover:bg-violet-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300`}
      >
        <User className="size-6 text-violet-600" aria-hidden="true" />
      </button>

      {abierto && (
        <div
          role="dialog"
          aria-label="Tu cuenta"
          className="absolute right-0 top-full z-20 mt-2 w-max min-w-[210px] max-w-[280px] rounded-xl border border-zinc-200 bg-white p-2 shadow-[0_4px_20px_rgba(30,27,46,0.08)]"
        >
          <div className="px-3 py-2">
            {perfil.nombre && (
              <p className="truncate text-[15px] font-bold text-gray-900">{perfil.nombre}</p>
            )}
            {perfil.email && <p className="truncate text-[13px] text-gray-500">{perfil.email}</p>}
          </div>
          <div className="my-1 border-t border-zinc-100" />
          <Link
            to="/perfil"
            onClick={() => setAbierto(false)}
            className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-[15px] font-semibold text-gray-700 hover:bg-violet-50 hover:text-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
          >
            <UserRound className="size-5 text-violet-600" aria-hidden="true" />
            <span className="flex-1">Ver mi perfil</span>
            <ChevronRight className="size-4 text-gray-400" aria-hidden="true" />
          </Link>
        </div>
      )}
    </div>
  );
}
