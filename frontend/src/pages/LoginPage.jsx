import { useEffect, useState } from 'react';
import { CircleCheck } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import { REGEX_EMAIL, TEXTO_USUARIO_MAX } from '../constants/validaciones';

function validar({ email, password }) {
  const errores = {};
  if (!REGEX_EMAIL.test(email.trim())) errores.email = 'Ingresa un correo válido';
  if (!password) errores.password = 'Ingresa tu contraseña';
  return errores;
}

/** Solo la pantalla: el envío al backend se conecta cuando exista el login (ver TODO en `enviar`). */
export default function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [formulario, setFormulario] = useState({ email: '', password: '' });
  const [errores, setErrores] = useState({});
  const [cuentaCreada, setCuentaCreada] = useState(() => Boolean(location.state?.cuentaCreada));

  // Aviso de cuenta creada: se limpia el estado de navegación y se oculta a los 4 s.
  useEffect(() => {
    if (!cuentaCreada) return undefined;
    navigate(location.pathname, { replace: true, state: null });
    const temporizador = setTimeout(() => setCuentaCreada(false), 4000);
    return () => clearTimeout(temporizador);
  }, [cuentaCreada, navigate, location.pathname]);

  const actualizar = (campo, valor) => {
    setFormulario((anterior) => ({ ...anterior, [campo]: valor }));
    setErrores((anterior) => ({ ...anterior, [campo]: undefined }));
  };

  const enviar = (evento) => {
    evento.preventDefault();

    const erroresCliente = validar(formulario);
    setErrores(erroresCliente);
    const primerCampoInvalido = Object.keys(erroresCliente)[0];
    if (primerCampoInvalido) {
      document.getElementById(primerCampoInvalido)?.focus();
      return;
    }

  };

  return (
    <LayoutAuth titulo="Iniciar sesión" subtitulo="Bienvenido de nuevo. Ingresa a tu cuenta.">
      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <CampoTexto
          id="email"
          etiqueta="Correo electrónico"
          tipo="email"
          valor={formulario.email}
          onChange={(v) => actualizar('email', v)}
          placeholder="nombre@correo.com"
          error={errores.email}
          autoComplete="email"
          maxLength={TEXTO_USUARIO_MAX}
        />

        <CampoTexto
          id="password"
          etiqueta="Contraseña"
          tipo="password"
          valor={formulario.password}
          onChange={(v) => actualizar('password', v)}
          placeholder="Tu contraseña"
          error={errores.password}
          autoComplete="current-password"
        />

        <button
          type="submit"
          className="flex h-[46px] items-center justify-center rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700"
        >
          Iniciar sesión
        </button>

        <p className="text-center text-sm text-gray-500">
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="font-bold text-violet-600 hover:underline">Regístrate</Link>
        </p>
      </form>

      {cuentaCreada && (
        <div
          role="status"
          className="fixed bottom-10 left-1/2 flex w-[calc(100%-2rem)] max-w-[440px] -translate-x-1/2 items-center gap-3 rounded-xl bg-emerald-400 px-4 py-3 text-sm font-bold text-gray-900 shadow-lg"
        >
          <CircleCheck className="size-[18px] shrink-0" aria-hidden="true" />
          ¡Cuenta creada! Ya puedes iniciar sesión.
        </div>
      )}
    </LayoutAuth>
  );
}
