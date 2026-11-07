import { NavLink } from 'react-router-dom';
import { Sprout } from 'lucide-react';
import MenuPerfil from './MenuPerfil';
import BotonCerrarSesion from './BotonCerrarSesion';

const claseEnlace = ({ isActive }) => `rounded-lg px-2.5 py-2 text-sm font-semibold sm:px-3 sm:text-base ${
  isActive ? 'bg-violet-50 text-violet-700' : 'text-gray-600 hover:bg-gray-100'
}`;

export default function Encabezado() {
  return (
    <header className="flex h-16 items-center justify-between gap-2 border-b border-zinc-200 bg-white px-4 sm:h-20 sm:px-12">
      <div className="flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-xl bg-violet-600">
          <Sprout className="size-6 text-white" aria-hidden="true" />
        </div>
        <span className="hidden text-xl font-bold text-gray-900 sm:inline sm:text-2xl">Metamorfosis</span>
      </div>
      <div className="flex items-center gap-1 sm:gap-4">
        {/* Navegación: las metas cumplidas están en el filtro de Mis metas (HU27). HU7: reporte general. */}
        <nav aria-label="Secciones" className="flex items-center gap-1">
          <NavLink to="/metas" end className={claseEnlace}>Mis metas</NavLink>
          <NavLink to="/reporte" className={claseEnlace}>Reporte</NavLink>
          {/* HU24: buscar personas y ver su perfil */}
          <NavLink to="/usuarios" className={claseEnlace}>Personas</NavLink>
        </nav>
        <BotonCerrarSesion />
        <MenuPerfil />
      </div>
    </header>
  );
}
