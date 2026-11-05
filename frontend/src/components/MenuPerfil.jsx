import { useEffect, useRef, useState } from 'react';
import { PencilLine } from 'lucide-react';
import { Link } from 'react-router-dom';
import AvatarUsuario from './AvatarUsuario';
import { obtenerCuenta } from '../services/cuentaService';
import {
  EVENTO_CUENTA, actualizarSesion, datosPerfil, obtenerSesion,
} from '../services/sesionService';

/**
 * Avatar del encabezado (foto o iniciales, HU-23): muestra el nombre y el correo de la cuenta
 * y la opción "Editar perfil". "Cerrar sesión" está en el encabezado (BotonCerrarSesion).
 * Se cierra al tocar fuera o con Esc.
 */
export default function MenuPerfil() {
  const [perfil, setPerfil] = useState(() => datosPerfil(obtenerSesion()));
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);
  const boton = useRef(null);

  // HU-23: si cambian el nombre o la foto (Editar perfil), el avatar se actualiza.
  useEffect(() => {
    const alCambiarCuenta = () => setPerfil(datosPerfil(obtenerSesion()));
    window.addEventListener(EVENTO_CUENTA, alCambiarCuenta);
    return () => window.removeEventListener(EVENTO_CUENTA, alCambiarCuenta);
  }, []);

  // HU-23: el login no trae la foto; se pide la cuenta una vez por sesión, sin bloquear nada.
  useEffect(() => {
    const sesion = obtenerSesion();
    if (!sesion || sesion.cuentaSincronizada) return;
    obtenerCuenta()
      .then(({ nombre, apellido, email, foto }) => actualizarSesion({
        nombre, apellido, email, foto: foto ?? null, cuentaSincronizada: true,
      }))
      .catch(() => {
        // Sin conexión o error: se quedan las iniciales; se reintenta en la próxima página.
      });
  }, []);

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

  return (
    <div ref={contenedor} className="relative">
      <button
        ref={boton}
        type="button"
        onClick={() => setAbierto((actual) => !actual)}
        aria-haspopup="dialog"
        aria-expanded={abierto}
        aria-label={perfil?.nombre ? `Tu cuenta: ${perfil.nombre}` : 'Tu cuenta'}
        className="rounded-full transition hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 focus-visible:ring-offset-2"
      >
        <AvatarUsuario foto={perfil?.foto} iniciales={perfil?.iniciales} clase="size-10 text-sm sm:size-12 sm:text-base" />
      </button>

      {abierto && perfil && (
        <div
          role="dialog"
          aria-label="Tu cuenta"
          className="absolute right-0 top-full z-20 mt-2 w-max min-w-[210px] max-w-[280px] rounded-xl border border-zinc-200 bg-white p-2 shadow-[0_4px_20px_rgba(30,27,46,0.08)]"
        >
          <div className="flex items-center gap-3 px-3 py-2">
            <AvatarUsuario foto={perfil.foto} iniciales={perfil.iniciales} clase="size-11 text-base" />
            <div className="min-w-0">
              {perfil.nombre && (
                <p className="truncate text-[15px] font-bold text-gray-900">{perfil.nombre}</p>
              )}
              {perfil.email && <p className="truncate text-[13px] text-gray-500">{perfil.email}</p>}
            </div>
          </div>
          {/* HU-23 */}
          <div className="mt-1 border-t border-zinc-100 pt-1">
            <Link
              to="/perfil/editar"
              onClick={() => setAbierto(false)}
              className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-[15px] font-bold text-violet-700 hover:bg-violet-50 focus-visible:bg-violet-50 focus-visible:outline-none"
            >
              <PencilLine className="size-[18px]" aria-hidden="true" />
              Editar perfil
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
