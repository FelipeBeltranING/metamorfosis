import { useEffect, useState } from 'react';
import { ChartColumn, Check, ChevronLeft, CircleAlert, Info, LoaderCircle, TriangleAlert } from 'lucide-react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import EliminarSubMetaModal from '../components/EliminarSubMetaModal';
import {
  crearSubMeta, editarSubMeta, eliminarSubMeta, obtenerCamino, obtenerEstadisticas,
} from '../services/subMetaService';
import { DIAS_LARGOS, formatearNumero, textoDias } from '../utils/subMetas';

const LETRAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];
const VACIO = {
  nombre: '', pregunta: '', unidad: '', objetivo: '', tipoObjetivo: 'AL_MENOS', modoDias: 'todos', dias: [1, 2, 3, 4, 5], notas: '',
};

function validar(f, tipo) {
  const e = {};
  if (!f.nombre.trim()) e.nombre = 'El nombre es obligatorio';
  else if (f.nombre.trim().length > 100) e.nombre = 'Máximo 100 caracteres';
  if (f.pregunta.trim().length > 200) e.pregunta = 'Máximo 200 caracteres';
  if (f.notas.trim().length > 500) e.notas = 'Máximo 500 caracteres';
  if (tipo === 'MEDIBLE') {
    if (!f.unidad.trim()) e.unidad = 'Escribe en qué lo mides.';
    else if (f.unidad.trim().length > 30) e.unidad = 'Máximo 30 caracteres';
    const objetivo = Number(String(f.objetivo).replace(',', '.'));
    if (f.objetivo === '' || !Number.isFinite(objetivo)) e.objetivo = 'Escribe el objetivo.';
    else if (objetivo <= 0) e.objetivo = 'Debe ser mayor que 0.';
    else if (objetivo > 1000000) e.objetivo = 'Máximo 1.000.000.';
  }
  if (f.modoDias === 'especificos' && f.dias.length === 0) e.dias = 'Elige al menos un día.';
  return e;
}

/**
 * Sub-metas (HU-A): crear una sub-meta (primero se elige Sí/No o medible) o editar una existente.
 * Rutas: /metas/:codigoMeta/submetas/nueva y /submetas/:codigoSubMeta/editar.
 */
export default function SubMetaFormPage() {
  const { codigoMeta, codigoSubMeta } = useParams();
  const [parametros, setParametros] = useSearchParams();
  const navigate = useNavigate();
  const editando = Boolean(codigoSubMeta);
  const [carga, setCarga] = useState({ fase: 'cargando' });
  const [formulario, setFormulario] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState([]);
  const [guardando, setGuardando] = useState(false);
  const [eliminar, setEliminar] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [errorEliminar, setErrorEliminar] = useState('');

  // Contexto: nombre de la meta (crear) o la sub-meta a editar.
  useEffect(() => {
    let activo = true;
    const fallar = (error) => {
      if (!activo) return;
      if (error.estado === 401) navigate('/login', { replace: true });
      else setCarga({ fase: 'error', mensaje: error.message });
    };
    if (editando) {
      obtenerEstadisticas(codigoSubMeta).then((est) => {
        if (!activo) return;
        const s = est.subMeta;
        setCarga({ fase: 'lista', subMeta: s, nombreMeta: est.nombreMeta, codigoMeta: s.codigoMeta, soloLectura: est.soloLectura });
        setFormulario({
          nombre: s.nombre, pregunta: s.pregunta ?? '', unidad: s.unidad ?? '',
          objetivo: s.objetivo !== null ? String(Number(s.objetivo)) : '', tipoObjetivo: s.tipoObjetivo ?? 'AL_MENOS',
          modoDias: s.dias.length === 7 ? 'todos' : 'especificos', dias: s.dias.length === 7 ? [1, 2, 3, 4, 5] : s.dias, notas: s.notas ?? '',
        });
      }).catch(fallar);
    } else {
      obtenerCamino(codigoMeta).then((camino) => activo && setCarga({
        fase: 'lista', nombreMeta: camino.nombre, codigoMeta: camino.codigoMeta, soloLectura: !camino.puedeEditar,
      })).catch(fallar);
    }
    return () => {
      activo = false;
    };
  }, [editando, codigoMeta, codigoSubMeta, navigate]);

  const tipo = editando ? carga.subMeta?.tipo : parametros.get('tipo');
  const volverA = carga.codigoMeta ? `/metas/${carga.codigoMeta}` : '/metas';

  const cambiar = (campo, valor) => {
    setFormulario((f) => ({ ...f, [campo]: valor }));
    setErrores((e) => ({ ...e, [campo]: undefined }));
    setErrorServidor([]);
  };

  const alternarDia = (d) => {
    setFormulario((f) => ({ ...f, dias: f.dias.includes(d) ? f.dias.filter((x) => x !== d) : [...f.dias, d].sort() }));
    setErrores((e) => ({ ...e, dias: undefined }));
  };

  const guardar = async (evento) => {
    evento.preventDefault();
    const e = validar(formulario, tipo);
    setErrores(e);
    if (Object.keys(e).length) {
      setErrorServidor([`Revisa ${Object.keys(e).length === 1 ? 'el campo marcado' : `los ${Object.keys(e).length} campos marcados`} para guardar la sub-meta.`]);
      return;
    }
    const datos = {
      nombre: formulario.nombre.trim(),
      pregunta: formulario.pregunta.trim() || null,
      tipo,
      unidad: tipo === 'MEDIBLE' ? formulario.unidad.trim() : null,
      objetivo: tipo === 'MEDIBLE' ? Number(String(formulario.objetivo).replace(',', '.')) : null,
      tipoObjetivo: tipo === 'MEDIBLE' ? formulario.tipoObjetivo : null,
      dias: formulario.modoDias === 'todos' ? [1, 2, 3, 4, 5, 6, 7] : formulario.dias,
      notas: formulario.notas.trim() || null,
    };
    setGuardando(true);
    try {
      if (editando) await editarSubMeta(codigoSubMeta, datos);
      else await crearSubMeta(codigoMeta, datos);
      navigate(volverA, { replace: true });
    } catch (error) {
      if (error.estado === 401) navigate('/login', { replace: true });
      else setErrorServidor(error.errores?.length ? error.errores : [error.estado ? error.message : 'No se guardó: revisa tu conexión e inténtalo de nuevo.']);
    } finally {
      setGuardando(false);
    }
  };

  const confirmarEliminar = async () => {
    setEliminando(true);
    setErrorEliminar('');
    try {
      await eliminarSubMeta(codigoSubMeta);
      navigate(volverA, { replace: true });
    } catch (error) {
      setErrorEliminar(error.estado ? error.message : 'No se pudo eliminar: revisa tu conexión.');
      setEliminando(false);
    }
  };

  const contenido = () => {
    if (carga.fase === 'cargando') return <div aria-busy="true" className="h-96 animate-pulse rounded-2xl bg-white" />;
    if (carga.fase === 'error') return <EstadoError mensaje={carga.mensaje} onReintentar={() => window.location.reload()} />;
    if (carga.soloLectura) {
      return (
        <p role="alert" className="rounded-2xl bg-amber-50 p-5 text-[15px] text-amber-900">
          Esta meta ya está cumplida o no eres su creador: su camino no se puede cambiar.
        </p>
      );
    }

    // Paso 1 (solo al crear): elegir el tipo.
    if (!editando && tipo !== 'SI_NO' && tipo !== 'MEDIBLE') {
      return (
        <div className="flex flex-col gap-3">
          <p className="text-[15px] text-gray-600">¿Cómo vas a medir este paso de <strong className="text-gray-900">{carga.nombreMeta}</strong>?</p>
          <button
            type="button"
            onClick={() => setParametros({ tipo: 'SI_NO' })}
            className="flex items-start gap-4 rounded-2xl border border-zinc-200 bg-white p-5 text-left hover:border-violet-400"
          >
            <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700"><Check className="size-6" strokeWidth={2.5} aria-hidden="true" /></span>
            <span>
              <span className="block text-lg font-extrabold">Sí / No</span>
              <span className="block text-[15px] leading-6 text-gray-600">Algo que haces o no haces. Ej.: ¿Leíste antes de dormir? ¿Hiciste ejercicio?</span>
            </span>
          </button>
          <button
            type="button"
            onClick={() => setParametros({ tipo: 'MEDIBLE' })}
            className="flex items-start gap-4 rounded-2xl border border-zinc-200 bg-white p-5 text-left hover:border-violet-400"
          >
            <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-violet-100 text-violet-800"><ChartColumn className="size-6" aria-hidden="true" /></span>
            <span>
              <span className="block text-lg font-extrabold">Medible</span>
              <span className="block text-[15px] leading-6 text-gray-600">Una cantidad con un objetivo. Ej.: ¿Cuántas páginas leíste? ¿Cuántos km corriste?</span>
            </span>
          </button>
        </div>
      );
    }

    const objetivoCambio = editando && tipo === 'MEDIBLE' && carga.subMeta
      && (Number(String(formulario.objetivo).replace(',', '.')) !== Number(carga.subMeta.objetivo) || formulario.tipoObjetivo !== carga.subMeta.tipoObjetivo);
    const campo = (id, etiqueta, opcional, props) => (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={id} className="text-sm font-extrabold">
          {etiqueta} {opcional ? <span className="font-semibold text-gray-600">(opcional)</span> : '*'}
        </label>
        <input
          id={id}
          value={formulario[id]}
          onChange={(e) => cambiar(id, e.target.value)}
          aria-invalid={Boolean(errores[id])}
          aria-describedby={errores[id] ? `error-${id}` : undefined}
          className={`h-12 rounded-xl border px-3.5 text-[15px] outline-none focus:ring-2 focus:ring-violet-300 ${errores[id] ? 'border-red-400' : 'border-zinc-200'}`}
          {...props}
        />
        {errores[id] && <span id={`error-${id}`} className="text-[13px] font-bold text-red-700">{errores[id]}</span>}
      </div>
    );
    const pastilla = (activa) => `h-10 rounded-[9px] px-4 text-sm font-bold ${activa ? 'bg-white text-violet-800 shadow-sm' : 'text-gray-600'}`;
    const resumenDias = formulario.modoDias === 'todos' ? 'todos los días' : formulario.dias.length ? textoDias(formulario.dias) : 'ningún día';

    return (
      <form onSubmit={guardar} noValidate className="flex flex-col gap-5">
        {errorServidor.length > 0 && (
          <div role="alert" className="flex gap-2.5 rounded-xl bg-red-50 p-4 text-[15px] font-bold text-red-700">
            <CircleAlert className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
            <ul>{errorServidor.map((m) => <li key={m}>{m}</li>)}</ul>
          </div>
        )}
        <div className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
          {campo('nombre', 'Nombre', false, { maxLength: 100, placeholder: tipo === 'MEDIBLE' ? 'Ej. Leer páginas' : 'Ej. Leer antes de dormir' })}
          {campo('pregunta', 'Pregunta del día', true, { maxLength: 200, placeholder: tipo === 'MEDIBLE' ? 'Ej. ¿Cuántas páginas leíste hoy?' : 'Ej. ¿Leíste antes de dormir?' })}

          {tipo === 'MEDIBLE' && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <span id="etiqueta-tipo-objetivo" className="text-sm font-extrabold">Tipo de objetivo *</span>
                <div role="group" aria-labelledby="etiqueta-tipo-objetivo" className="flex gap-1 rounded-xl bg-slate-100 p-1">
                  {[['AL_MENOS', 'Al menos'], ['COMO_MAXIMO', 'Como máximo']].map(([v, t]) => (
                    <button key={v} type="button" aria-pressed={formulario.tipoObjetivo === v} onClick={() => cambiar('tipoObjetivo', v)} className={`${pastilla(formulario.tipoObjetivo === v)} flex-1 px-2`}>{t}</button>
                  ))}
                </div>
              </div>
              {campo('objetivo', 'Objetivo', false, { type: 'number', inputMode: 'decimal', min: 0, step: 'any', placeholder: 'Ej. 15' })}
              {campo('unidad', 'Unidad', false, { maxLength: 30, placeholder: 'Ej. páginas' })}
            </div>
          )}

          {objetivoCambio && (
            <p role="status" className="flex gap-2.5 rounded-xl bg-amber-50 p-4 text-[15px] leading-6 text-amber-900">
              <TriangleAlert className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
              <span>
                Cambiaste el objetivo{carga.subMeta.objetivo !== null && <> de <strong>{formatearNumero(carga.subMeta.objetivo)}</strong></>}. Tus registros se conservan, pero los días
                anteriores también se evaluarán con el nuevo objetivo: algunos días pueden dejar de contar como cumplidos.
              </span>
            </p>
          )}

          <fieldset className="flex flex-col gap-2" aria-describedby={errores.dias ? 'error-dias' : undefined}>
            <legend className="mb-1.5 text-sm font-extrabold">¿Qué días toca? *</legend>
            <div className="flex gap-1 self-start rounded-xl bg-slate-100 p-1">
              {[['todos', 'Todos los días'], ['especificos', 'Días específicos']].map(([v, t]) => (
                <button key={v} type="button" aria-pressed={formulario.modoDias === v} onClick={() => cambiar('modoDias', v)} className={pastilla(formulario.modoDias === v)}>{t}</button>
              ))}
            </div>
            {formulario.modoDias === 'especificos' && (
              <div className="flex flex-wrap gap-2">
                {LETRAS.map((l, i) => {
                  const activo = formulario.dias.includes(i + 1);
                  return (
                    <button
                      key={l}
                      type="button"
                      aria-pressed={activo}
                      aria-label={DIAS_LARGOS[i]}
                      onClick={() => alternarDia(i + 1)}
                      className={`h-11 w-12 rounded-xl border text-sm font-extrabold ${activo ? 'border-violet-600 bg-violet-600 text-white' : errores.dias ? 'border-red-400 bg-white text-gray-700' : 'border-zinc-200 bg-white text-gray-700'}`}
                    >
                      {l}
                    </button>
                  );
                })}
              </div>
            )}
            {errores.dias && <span id="error-dias" className="text-[13px] font-bold text-red-700">{errores.dias}</span>}
          </fieldset>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="notas" className="text-sm font-extrabold">Notas <span className="font-semibold text-gray-600">(opcional)</span></label>
            <textarea id="notas" rows={2} maxLength={500} value={formulario.notas} onChange={(e) => cambiar('notas', e.target.value)} className="resize-y rounded-xl border border-zinc-200 px-3.5 py-3 text-[15px] outline-none focus:ring-2 focus:ring-violet-300" />
          </div>

          <p className="flex items-start gap-2.5 rounded-xl bg-violet-50 p-4 text-[15px] text-gray-700">
            <Info className="mt-0.5 size-5 shrink-0 text-violet-700" aria-hidden="true" />
            {tipo === 'MEDIBLE'
              ? <span>Un día cuenta como cumplido si registras <strong>{formulario.tipoObjetivo === 'COMO_MAXIMO' ? 'como máximo' : 'al menos'} {formulario.objetivo || '…'} {formulario.unidad || '…'}</strong>, {resumenDias}.</span>
              : <span>Cada día que toca, marcas <strong>✓ hecho</strong> o <strong>× no hecho</strong>. Toca {resumenDias}.</span>}
          </p>

          <div className="flex flex-wrap justify-end gap-3">
            <Link to={volverA} className="flex h-12 items-center rounded-xl border border-zinc-200 px-5 font-bold text-gray-700 hover:bg-gray-50">Cancelar</Link>
            <button type="submit" disabled={guardando} className="flex h-12 items-center gap-2 rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 disabled:bg-violet-400">
              {guardando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
              {editando ? 'Guardar cambios' : 'Guardar sub-meta'}
            </button>
          </div>
        </div>

        {editando && (
          <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-red-200 bg-white p-5">
            <div className="flex-[1_1_280px]">
              <h2 className="text-base font-extrabold text-red-700">Eliminar esta sub-meta</h2>
              <p className="text-sm text-gray-600">Se borra junto con todos sus registros. No se puede deshacer.</p>
            </div>
            <button type="button" onClick={() => setEliminar(true)} className="h-11 rounded-xl border border-red-200 px-4 font-bold text-red-700 hover:bg-red-50">Eliminar</button>
          </section>
        )}
      </form>
    );
  };

  let titulo = 'Nueva sub-meta';
  if (editando) titulo = 'Editar sub-meta';
  else if (tipo === 'SI_NO') titulo = 'Nueva sub-meta Sí / No';
  else if (tipo === 'MEDIBLE') titulo = 'Nueva sub-meta medible';

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto flex w-full max-w-[720px] flex-col gap-5 px-4 pb-16 pt-6 sm:pt-8">
        {!editando && (tipo === 'SI_NO' || tipo === 'MEDIBLE') ? (
          <button type="button" onClick={() => setParametros({})} className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
            <ChevronLeft className="size-[18px]" aria-hidden="true" /> Cambiar tipo
          </button>
        ) : (
          <Link to={volverA} className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
            <ChevronLeft className="size-[18px]" aria-hidden="true" /> {carga.nombreMeta ?? 'Volver'}
          </Link>
        )}
        <div>
          <h1 className="text-2xl font-extrabold sm:text-3xl">{titulo}</h1>
          {carga.nombreMeta && (
            <p className="mt-1 text-[15px] text-gray-600">
              {editando ? `${tipo === 'MEDIBLE' ? 'Medible' : 'Sí / No'} · ` : ''}Meta: {carga.nombreMeta}
              {editando && ' · El tipo no se puede cambiar'}
            </p>
          )}
        </div>
        {contenido()}
      </main>
      {eliminar && carga.subMeta && (
        <EliminarSubMetaModal
          subMeta={carga.subMeta}
          nombreMeta={carga.nombreMeta}
          eliminando={eliminando}
          error={errorEliminar}
          onConfirmar={confirmarEliminar}
          onCancelar={() => setEliminar(false)}
        />
      )}
    </div>
  );
}
