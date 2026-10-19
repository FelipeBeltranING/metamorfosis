import { useEffect, useRef, useState } from 'react';
import { LogOut, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cerrarSesionRemota } from '../services/authService';
import { cerrarSesion, datosPerfil, obtenerSesion } from '../services/sesionService';

/**
 * Avatar del encabezado con el menú de la cuenta (nombre, correo y "Cerrar sesión").
 * Se cierra al elegir una opción, al tocar fuera o con Esc.
 */
export default function MenuPerfil() {
  const navigate = useNavigate();
  const [perfil] = useState(() => datosPerfil(obtenerSesion()));
  const [abierto, setAbierto] = useState(false);
  const [cerrando, setCerrando] = useState(false);
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

  const cerrarLaSesion = async () => {
    setCerrando(true);
    const sesion = obtenerSesion();
    try {
      if (sesion) await cerrarSesionRemota(sesion.token);
    } catch {
      // Si el servidor no responde (o el token ya no servía), la sesión se cierra igual aquí.
    }
    cerrarSesion();
    navigate('/login', { replace: true, state: { sesionCerrada: true } });
  };

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
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-label="Menú de la cuenta"
        className={`${avatar} transition hover:bg-violet-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300`}
      >
        <User className="size-6 text-violet-600" aria-hidden="true" />
      </button>

      {abierto && (
        <div
          role="menu"
          className="absolute right-0 top-full z-20 mt-2 w-max min-w-[210px] max-w-[280px] rounded-xl border border-zinc-200 bg-white p-2 shadow-[0_4px_20px_rgba(30,27,46,0.08)]"
        >
          <div className="px-3 pb-3 pt-2">
            {perfil.nombre && (
              <p className="truncate text-[15px] font-bold text-gray-900">{perfil.nombre}</p>
            )}
            {perfil.email && <p className="truncate text-[13px] text-gray-500">{perfil.email}</p>}
          </div>
          <div className="border-t border-zinc-200 pt-2">
            <button
              type="button"
              role="menuitem"
              onClick={cerrarLaSesion}
              disabled={cerrando}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left text-[15px] font-bold text-violet-700 transition hover:bg-violet-50 focus-visible:bg-violet-50 focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-60"
            >
              <LogOut className="size-[18px]" aria-hidden="true" />
              Cerrar sesión
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
