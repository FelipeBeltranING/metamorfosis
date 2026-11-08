import { useEffect, useRef, useState } from 'react';
import {
  ChevronRight, MailOpen, PencilLine, UserRound,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import AvatarUsuario from './AvatarUsuario';
import { obtenerCuenta } from '../services/cuentaService';
import { listarInvitaciones } from '../services/invitacionService';
import { aplicarApariencia, desdeServidor } from '../tema/apariencia';
import {
  EVENTO_CUENTA, actualizarSesion, datosPerfil, obtenerSesion,
} from '../services/sesionService';

/**
 * Avatar del encabezado (foto o iniciales, HU-23): muestra el nombre y el correo de la cuenta
 * y las opciones "Ver mi perfil" (HU22), "Editar perfil" (HU-23) e "Invitaciones" (HU14, con
 * cuántas hay pendientes; si hay, el avatar lleva un punto).
 * "Cerrar sesión" está en el encabezado (BotonCerrarSesion).
 * Se cierra al tocar fuera o con Esc.
 */
export default function MenuPerfil() {
  const [perfil, setPerfil] = useState(() => datosPerfil(obtenerSesion()));
  const [abierto, setAbierto] = useState(false);
  const [invitaciones, setInvitaciones] = useState(0);
  const contenedor = useRef(null);
  const boton = useRef(null);

  // HU14: cuántas invitaciones pendientes hay (se vuelve a contar al responder una).
  useEffect(() => {
    if (!obtenerSesion()) return undefined;
    let activo = true;
    const contar = () => listarInvitaciones().then((l) => activo && setInvitaciones(l.length)).catch(() => {});
    contar();
    window.addEventListener('metamorfosis:invitaciones', contar);
    return () => {
      activo = false;
      window.removeEventListener('metamorfosis:invitaciones', contar);
    };
  }, []);

  // HU-23: si cambian el nombre o la foto (Editar perfil), el avatar se actualiza.
  useEffect(() => {
    const alCambiarCuenta = () => setPerfil(datosPerfil(obtenerSesion()));
    window.addEventListener(EVENTO_CUENTA, alCambiarCuenta);
    return () => window.removeEventListener(EVENTO_CUENTA, alCambiarCuenta);
  }, []);

  // HU-23: el login no trae la foto; se pide la cuenta una vez por sesión, sin bloquear nada.
  // HU19: de paso se aplica la apariencia guardada en la cuenta.
  useEffect(() => {
    const sesion = obtenerSesion();
    if (!sesion || sesion.cuentaSincronizada) return;
    obtenerCuenta()
      .then(({ nombre, apellido, email, foto, apariencia }) => {
        aplicarApariencia(desdeServidor(apariencia), true);
        actualizarSesion({
          nombre, apellido, email, foto: foto ?? null, cuentaSincronizada: true,
        });
      })
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
        aria-label={`${perfil?.nombre ? `Tu cuenta: ${perfil.nombre}` : 'Tu cuenta'}${invitaciones ? ` (${invitaciones} ${invitaciones === 1 ? 'invitación pendiente' : 'invitaciones pendientes'})` : ''}`}
        className="relative rounded-full transition hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 focus-visible:ring-offset-2"
      >
        <AvatarUsuario foto={perfil?.foto} iniciales={perfil?.iniciales} clase="size-10 text-sm sm:size-12 sm:text-base" />
        {invitaciones > 0 && (
          <span aria-hidden="true" className="absolute -right-0.5 -top-0.5 size-3.5 rounded-full border-2 border-white bg-violet-600" />
        )}
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
          <div className="my-1 border-t border-zinc-100" />
          {/* HU22 */}
          <Link
            to="/perfil"
            onClick={() => setAbierto(false)}
            className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-[15px] font-semibold text-gray-700 hover:bg-violet-50 hover:text-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
          >
            <UserRound className="size-5 text-violet-600" aria-hidden="true" />
            <span className="flex-1">Ver mi perfil</span>
            <ChevronRight className="size-4 text-gray-400" aria-hidden="true" />
          </Link>
          {/* HU-23 */}
          <Link
            to="/perfil/editar"
            onClick={() => setAbierto(false)}
            className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-[15px] font-semibold text-gray-700 hover:bg-violet-50 hover:text-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
          >
            <PencilLine className="size-5 text-violet-600" aria-hidden="true" />
            <span className="flex-1">Editar perfil</span>
            <ChevronRight className="size-4 text-gray-400" aria-hidden="true" />
          </Link>
          {/* HU14 */}
          <Link
            to="/invitaciones"
            onClick={() => setAbierto(false)}
            className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-[15px] font-semibold text-gray-700 hover:bg-violet-50 hover:text-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
          >
            <MailOpen className="size-5 text-violet-600" aria-hidden="true" />
            <span className="flex-1">Invitaciones</span>
            {invitaciones > 0
              ? <span className="rounded-full bg-violet-600 px-2 py-0.5 text-xs font-extrabold text-white">{invitaciones}</span>
              : <ChevronRight className="size-4 text-gray-400" aria-hidden="true" />}
          </Link>
        </div>
      )}
    </div>
  );
}
