import { useEffect, useState } from 'react';
import { CircleCheck, LoaderCircle } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import AlertaFormulario from '../components/AlertaFormulario';
import { iniciarSesion } from '../services/authService';
import { guardarSesion } from '../services/sesionService';
import { REGEX_EMAIL, TEXTO_USUARIO_MAX } from '../constants/validaciones';

const MENSAJE_CREDENCIALES = 'Correo o contraseña incorrectos.';

const AVISOS = {
  cuentaCreada: '¡Cuenta creada! Ya puedes iniciar sesión.',
  sesionCerrada: 'Sesión cerrada.',
};

/** Qué aviso mostrar al llegar (viene en el estado de navegación). */
function avisoInicial(estadoNavegacion) {
  if (estadoNavegacion?.cuentaCreada) return 'cuentaCreada';
  if (estadoNavegacion?.sesionCerrada) return 'sesionCerrada';
  return null;
}

/** Validación del cliente. Las claves coinciden con los id de los campos. */
function validar({ email, password }) {
  const errores = {};
  if (!REGEX_EMAIL.test(email.trim())) errores.email = 'Ingresa un correo válido';
  if (!password) errores.password = 'La contraseña es obligatoria';
  return errores;
}

export default function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [formulario, setFormulario] = useState({ email: '', password: '' });
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState(() => avisoInicial(location.state));

  // Aviso de éxito: se limpia el estado de navegación y se oculta a los 4 s.
  useEffect(() => {
    if (!aviso) return undefined;
    navigate(location.pathname, { replace: true, state: null });
    const temporizador = setTimeout(() => setAviso(null), 4000);
    return () => clearTimeout(temporizador);
  }, [aviso, navigate, location.pathname]);

  const actualizar = (campo, valor) => {
    setFormulario((anterior) => ({ ...anterior, [campo]: valor }));
    setErrores((anterior) => ({ ...anterior, [campo]: undefined }));
    setErrorServidor('');
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    setErrorServidor('');

    const erroresCliente = validar(formulario);
    setErrores(erroresCliente);
    const primerCampoInvalido = Object.keys(erroresCliente)[0];
    if (primerCampoInvalido) {
      document.getElementById(primerCampoInvalido)?.focus();
      return;
    }

    setEnviando(true);
    try {
      const sesion = await iniciarSesion({
        email: formulario.email.trim(),
        password: formulario.password,
      });
      guardarSesion(sesion);
      navigate('/metas', { replace: true, state: { sesionIniciada: true } });
    } catch (error) {
      if (error.estado === 401) {
        setErrorServidor(MENSAJE_CREDENCIALES);
      } else {
        const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
        setErrorServidor(`${error.message}${detalle}`);
      }
      setEnviando(false);
    }
  };

  return (
    <LayoutAuth titulo="Iniciar sesión" subtitulo="Bienvenido de nuevo. Ingresa a tu cuenta.">
      <AlertaFormulario texto={errorServidor} />

      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <CampoTexto
          id="email"
          etiqueta="Correo electrónico"
          tipo="email"
          valor={formulario.email}
          onChange={(v) => actualizar('email', v)}
          placeholder="nombre@correo.com"
          error={errores.email}
          deshabilitado={enviando}
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
          deshabilitado={enviando}
          autoComplete="current-password"
        />

        <button
          type="submit"
          disabled={enviando}
          className="flex h-[46px] items-center justify-center gap-2 rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
        >
          {enviando ? (
            <>
              <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />
              Iniciando sesión...
            </>
          ) : (
            'Iniciar sesión'
          )}
        </button>

        <p className="text-center text-sm text-gray-500">
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="font-bold text-violet-600 hover:underline">Regístrate</Link>
        </p>
      </form>

      {aviso && (
        <div
          role="status"
          className="fixed bottom-10 left-1/2 flex w-[calc(100%-2rem)] max-w-[440px] -translate-x-1/2 items-center gap-3 rounded-xl bg-emerald-400 px-4 py-3 text-sm font-bold text-gray-900 shadow-lg"
        >
          <CircleCheck className="size-[18px] shrink-0" aria-hidden="true" />
          {AVISOS[aviso]}
        </div>
      )}
    </LayoutAuth>
  );
}
