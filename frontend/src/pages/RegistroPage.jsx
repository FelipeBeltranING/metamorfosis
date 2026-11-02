import { useEffect, useState } from 'react';
import { LoaderCircle, MailCheck } from 'lucide-react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import AlertaFormulario from '../components/AlertaFormulario';
import EnlaceNoValido from '../components/EnlaceNoValido';
import { consultarSolicitud, registrar } from '../services/authService';
import { TEXTO_USUARIO_MAX } from '../constants/validaciones';

const FORMULARIO_VACIO = { nombre: '', apellido: '', password: '' };

function validar({ nombre, apellido, password }) {
  const errores = {};
  if (!nombre.trim()) errores.nombre = 'El nombre es obligatorio';
  if (!apellido.trim()) errores.apellido = 'El apellido es obligatorio';
  if (password.length < 8) errores.password = 'Mínimo 8 caracteres';
  else if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/\d/.test(password)) {
    errores.password = 'Debe incluir mayúscula, minúscula y número';
  }
  return errores;
}

/**
 * HU8 paso 3: llega desde el enlace del correo (/registro/completar?token=...).
 * Solo si el enlace es válido se muestra el formulario, con el email ya autorizado.
 */
export default function RegistroPage() {
  const navigate = useNavigate();
  const [parametros] = useSearchParams();
  const token = parametros.get('token') ?? '';
  // { fase: 'cargando' | 'invalido' | 'lista', email?, mensaje? }
  const [solicitud, setSolicitud] = useState(() => (token ? { fase: 'cargando' } : { fase: 'invalido' }));
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Comprobar el enlace antes de mostrar el formulario.
  useEffect(() => {
    if (!token) return undefined;
    let activo = true;
    consultarSolicitud(token)
      .then((datos) => activo && setSolicitud({ fase: 'lista', email: datos.email }))
      .catch((e) => activo && setSolicitud({
        fase: 'invalido',
        mensaje: e.estado === 410 ? e.message : 'No pudimos comprobar el enlace. Intenta de nuevo.',
      }));
    return () => {
      activo = false;
    };
  }, [token]);

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
      await registrar({
        token,
        nombre: formulario.nombre.trim(),
        apellido: formulario.apellido.trim(),
        password: formulario.password,
      });
      navigate('/login', { state: { cuentaCreada: true } });
    } catch (error) {
      if (error.estado === 410) {
        setSolicitud({ fase: 'invalido', mensaje: error.message });
      } else if (error.estado === 409) {
        setErrorServidor('Este correo ya está registrado. Inicia sesión o usa otro correo.');
      } else {
        const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
        setErrorServidor(`${error.message}${detalle}`);
      }
    } finally {
      setEnviando(false);
    }
  };

  if (solicitud.fase === 'cargando') {
    return (
      <LayoutAuth titulo="Crear cuenta" subtitulo="Comprobando tu enlace...">
        <p role="status" className="flex items-center justify-center gap-2 py-8 text-sm text-gray-600">
          <LoaderCircle className="size-5 animate-spin text-violet-600" aria-hidden="true" />
          Comprobando el enlace...
        </p>
      </LayoutAuth>
    );
  }

  if (solicitud.fase === 'invalido') {
    return (
      <LayoutAuth titulo="Enlace no válido" subtitulo="Para registrarte necesitas un enlace vigente.">
        <EnlaceNoValido mensaje={solicitud.mensaje} />
      </LayoutAuth>
    );
  }

  return (
    <LayoutAuth titulo="Completa tu registro" subtitulo="Tu correo ya está autorizado. Solo faltan tus datos.">
      <AlertaFormulario texto={errorServidor} />

      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <CampoTexto
            id="nombre"
            etiqueta="Nombre"
            valor={formulario.nombre}
            onChange={(v) => actualizar('nombre', v)}
            placeholder="Ej. Laura"
            error={errores.nombre}
            deshabilitado={enviando}
            autoComplete="given-name"
            maxLength={TEXTO_USUARIO_MAX}
          />
          <CampoTexto
            id="apellido"
            etiqueta="Apellido"
            valor={formulario.apellido}
            onChange={(v) => actualizar('apellido', v)}
            placeholder="Ej. Gómez"
            error={errores.apellido}
            deshabilitado={enviando}
            autoComplete="family-name"
            maxLength={TEXTO_USUARIO_MAX}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-bold text-gray-900">Correo electrónico</span>
          <p className="flex h-[46px] items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 text-[15px] text-gray-900">
            <MailCheck className="size-[18px] shrink-0 text-emerald-700" aria-hidden="true" />
            <span className="truncate">{solicitud.email}</span>
            <span className="ml-auto shrink-0 text-xs font-bold text-emerald-700">Autorizado</span>
          </p>
        </div>

        <CampoTexto
          id="password"
          etiqueta="Contraseña"
          tipo="password"
          valor={formulario.password}
          onChange={(v) => actualizar('password', v)}
          placeholder="Mínimo 8 caracteres"
          error={errores.password}
          deshabilitado={enviando}
          autoComplete="new-password"
        />

        <button
          type="submit"
          disabled={enviando}
          className="flex h-[46px] items-center justify-center gap-2 rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
        >
          {enviando ? (
            <>
              <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />
              Creando cuenta...
            </>
          ) : (
            'Crear cuenta'
          )}
        </button>

        <p className="text-center text-sm text-gray-500">
          ¿Ya tienes una cuenta?{' '}
          <Link to="/login" className="font-bold text-violet-600 hover:underline">Inicia sesión</Link>
        </p>
      </form>
    </LayoutAuth>
  );
}
