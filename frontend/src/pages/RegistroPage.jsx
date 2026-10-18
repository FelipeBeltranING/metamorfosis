import { useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import AlertaFormulario from '../components/AlertaFormulario';
import { registrar } from '../services/authService';
import { REGEX_EMAIL, TEXTO_USUARIO_MAX } from '../constants/validaciones';

const FORMULARIO_VACIO = { nombre: '', apellido: '', email: '', password: '' };

function validar({ nombre, apellido, email, password }) {
  const errores = {};
  if (!nombre.trim()) errores.nombre = 'El nombre es obligatorio';
  if (!apellido.trim()) errores.apellido = 'El apellido es obligatorio';
  if (!REGEX_EMAIL.test(email.trim())) errores.email = 'Ingresa un correo válido';
  if (password.length < 8) errores.password = 'Mínimo 8 caracteres';
  else if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/\d/.test(password)) {
    errores.password = 'Debe incluir mayúscula, minúscula y número';
  }
  return errores;
}

export default function RegistroPage() {
  const navigate = useNavigate();
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);

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
        nombre: formulario.nombre.trim(),
        apellido: formulario.apellido.trim(),
        email: formulario.email.trim(),
        password: formulario.password,
      });
      navigate('/login', { state: { cuentaCreada: true } });
    } catch (error) {
      if (error.estado === 409) {
        setErrores({ email: 'Este correo ya está registrado' });
        setErrorServidor('Este correo ya está registrado. Inicia sesión o usa otro correo.');
      } else {
        const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
        setErrorServidor(`${error.message}${detalle}`);
      }
    } finally {
      setEnviando(false);
    }
  };

  return (
    <LayoutAuth titulo="Crear cuenta" subtitulo="Empieza a convertir tus metas en hábitos.">
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
