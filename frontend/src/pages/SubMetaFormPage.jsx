import { useEffect, useState } from 'react';
import {
  ChartColumn, Check, ChevronLeft, CircleAlert, Flag, Info, Lightbulb, LoaderCircle, TriangleAlert,
} from 'lucide-react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Trans, useTranslation } from 'react-i18next';
import i18n from '../i18n';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import EliminarSubMetaModal from '../components/EliminarSubMetaModal';
import {
  crearSubMeta, editarSubMeta, eliminarSubMeta, obtenerCamino, obtenerPaso,
} from '../services/subMetaService';

const TIPOS_PASO = ['HITO', 'SI_NO', 'MEDIBLE'];
import { diasLargos, formatearNumero, textoDias } from '../utils/subMetas';

const VACIO = {
  nombre: '', pregunta: '', unidad: '', objetivo: '', tipoObjetivo: 'AL_MENOS', modoDias: 'todos', dias: [1, 2, 3, 4, 5], notas: '',
};

function validar(f, tipo) {
  const e = {};
  const t = (clave, opciones) => i18n.t(`subMetaForm.validacion.${clave}`, opciones);
  if (!f.nombre.trim()) e.nombre = t('nombreObligatorio');
  else if (f.nombre.trim().length > 100) e.nombre = t('maximo', { max: 100 });
  if (f.notas.trim().length > 500) e.notas = t('maximo', { max: 500 });
  if (tipo === 'HITO') return e; // Un hito solo tiene nombre y notas.
  if (f.pregunta.trim().length > 200) e.pregunta = t('maximo', { max: 200 });
  if (tipo === 'MEDIBLE') {
    if (!f.unidad.trim()) e.unidad = t('unidad');
    else if (f.unidad.trim().length > 30) e.unidad = t('maximo', { max: 30 });
    const objetivo = Number(String(f.objetivo).replace(',', '.'));
    if (f.objetivo === '' || !Number.isFinite(objetivo)) e.objetivo = t('objetivo');
    else if (objetivo <= 0) e.objetivo = t('objetivoPositivo');
    else if (objetivo > 1000000) e.objetivo = t('objetivoMaximo');
  }
  if (f.modoDias === 'especificos' && f.dias.length === 0) e.dias = t('dias');
  return e;
}

/**
 * HU28: agregar un paso al camino de una meta (primero se elige hito, hábito Sí/No o hábito medible,
 * con ideas según el tipo de meta) o editar uno existente. El formulario pide lo esencial; la pregunta
 * del día y las notas van en "Más opciones".
 * Rutas: /metas/:codigoMeta/submetas/nueva[?tipo=HITO|SI_NO|MEDIBLE] y /submetas/:codigoSubMeta/editar.
 */
export default function SubMetaFormPage() {
  const { t } = useTranslation();
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
      obtenerPaso(codigoSubMeta).then((respuesta) => {
        if (!activo) return;
        const s = respuesta.paso;
        const sinDias = !s.dias.length || s.dias.length === 7;
        setCarga({ fase: 'lista', subMeta: s, nombreMeta: respuesta.nombreMeta, codigoMeta: s.codigoMeta, soloLectura: !respuesta.puedeEditar });
        setFormulario({
          nombre: s.nombre, pregunta: s.pregunta ?? '', unidad: s.unidad ?? '',
          objetivo: s.objetivo !== null ? String(Number(s.objetivo)) : '', tipoObjetivo: s.tipoObjetivo ?? 'AL_MENOS',
          modoDias: sinDias ? 'todos' : 'especificos', dias: sinDias ? [1, 2, 3, 4, 5] : s.dias, notas: s.notas ?? '',
        });
      }).catch(fallar);
    } else {
      obtenerCamino(codigoMeta).then((camino) => activo && setCarga({
        fase: 'lista', nombreMeta: camino.nombre, codigoMeta: camino.codigoMeta, tipoMeta: camino.tipo, soloLectura: !camino.puedeEditar,
      })).catch(fallar);
    }
    return () => {
      activo = false;
    };
  }, [editando, codigoMeta, codigoSubMeta, navigate]);

  const tipo = editando ? carga.subMeta?.tipo : parametros.get('tipo');
  const volverA = carga.codigoMeta ? `/metas/${carga.codigoMeta}` : '/metas';

  // Ideas según el tipo de meta: al elegir una, el formulario empieza con su tipo y su nombre.
  const ideas = carga.tipoMeta ? t(`ideasCamino.${carga.tipoMeta}`, { returnObjects: true }) : [];
  const usarIdea = (tipoIdea, nombre) => {
    setFormulario((f) => ({ ...f, nombre }));
    setParametros({ tipo: tipoIdea });
  };

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
      setErrorServidor([t('subMetaForm.validacion.revisa', { count: Object.keys(e).length })]);
      return;
    }
    const esHito = tipo === 'HITO';
    const datos = {
      nombre: formulario.nombre.trim(),
      pregunta: esHito ? null : formulario.pregunta.trim() || null,
      tipo,
      unidad: tipo === 'MEDIBLE' ? formulario.unidad.trim() : null,
      objetivo: tipo === 'MEDIBLE' ? Number(String(formulario.objetivo).replace(',', '.')) : null,
      tipoObjetivo: tipo === 'MEDIBLE' ? formulario.tipoObjetivo : null,
      dias: esHito || formulario.modoDias === 'todos' ? [1, 2, 3, 4, 5, 6, 7] : formulario.dias,
      notas: formulario.notas.trim() || null,
    };
    setGuardando(true);
    try {
      if (editando) await editarSubMeta(codigoSubMeta, datos);
      else await crearSubMeta(codigoMeta, datos);
      navigate(volverA, { replace: true });
    } catch (error) {
      if (error.estado === 401) navigate('/login', { replace: true });
      else setErrorServidor(error.errores?.length ? error.errores : [error.estado ? error.message : t('subMetaForm.errorGuardar')]);
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
      setErrorEliminar(error.estado ? error.message : t('subMetaForm.errorEliminar'));
      setEliminando(false);
    }
  };

  const contenido = () => {
    if (carga.fase === 'cargando') return <div aria-busy="true" className="h-96 animate-pulse rounded-2xl bg-white" />;
    if (carga.fase === 'error') return <EstadoError mensaje={carga.mensaje} onReintentar={() => window.location.reload()} />;
    if (carga.soloLectura) {
      return (
        <p role="alert" className="rounded-2xl bg-amber-50 p-5 text-[15px] text-amber-900">
          {t('subMetaForm.soloLectura')}
        </p>
      );
    }

    // Paso 1 (solo al crear): elegir hito o hábito, o empezar con una idea para este tipo de meta.
    if (!editando && !TIPOS_PASO.includes(tipo)) {
      const opcion = (valor, Icono, colorIcono, titulo, texto) => (
        <button
          type="button"
          onClick={() => setParametros({ tipo: valor })}
          className="flex items-start gap-4 rounded-2xl border border-zinc-200 bg-white p-5 text-left hover:border-violet-400"
        >
          <span className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${colorIcono}`}><Icono className="size-6" strokeWidth={2.5} aria-hidden="true" /></span>
          <span>
            <span className="block text-lg font-extrabold">{titulo}</span>
            <span className="block text-[15px] leading-6 text-gray-600">{texto}</span>
          </span>
        </button>
      );
      return (
        <div className="flex flex-col gap-3">
          <p className="text-[15px] text-gray-600"><Trans i18nKey="subMetaForm.comoMedir" values={{ meta: carga.nombreMeta }} components={{ b: <strong className="text-gray-900" /> }} /></p>
          <h2 className="mt-1 text-sm font-extrabold uppercase tracking-wide text-gray-600">{t('subMetaPaso.grupoHito')}</h2>
          {opcion('HITO', Flag, 'bg-violet-600 text-white', t('subMetaPaso.hito'), t('subMetaPaso.hitoTexto'))}
          <h2 className="mt-2 text-sm font-extrabold uppercase tracking-wide text-gray-600">{t('subMetaPaso.grupoHabito')}</h2>
          {opcion('SI_NO', Check, 'bg-emerald-50 text-emerald-700', t('subMetaPaso.habitoSiNo'), t('subMetaForm.siNoTexto'))}
          {opcion('MEDIBLE', ChartColumn, 'bg-violet-100 text-violet-800', t('subMetaPaso.habitoMedible'), t('subMetaForm.medibleTexto'))}
          {Array.isArray(ideas) && ideas.length > 0 && (
            <section aria-labelledby="titulo-ideas" className="mt-2 flex flex-col gap-2.5 rounded-2xl bg-violet-50 p-5">
              <h2 id="titulo-ideas" className="flex items-center gap-2 text-base font-extrabold text-violet-900">
                <Lightbulb className="size-5" aria-hidden="true" /> {t('subMetaPaso.ideas')}
              </h2>
              <p className="text-sm text-gray-700">{t('subMetaPaso.ideasTexto')}</p>
              <div className="flex flex-wrap gap-2">
                {ideas.map(([tipoIdea, nombre]) => (
                  <button
                    key={nombre}
                    type="button"
                    onClick={() => usarIdea(tipoIdea, nombre)}
                    className="flex items-center gap-1.5 rounded-xl border border-violet-200 bg-white px-3 py-2 text-sm font-bold text-violet-900 hover:border-violet-400"
                  >
                    {tipoIdea === 'HITO' ? <Flag className="size-4" aria-hidden="true" /> : <Check className="size-4" aria-hidden="true" />}
                    {nombre}
                  </button>
                ))}
              </div>
            </section>
          )}
        </div>
      );
    }

    const objetivoCambio = editando && tipo === 'MEDIBLE' && carga.subMeta
      && (Number(String(formulario.objetivo).replace(',', '.')) !== Number(carga.subMeta.objetivo) || formulario.tipoObjetivo !== carga.subMeta.tipoObjetivo);
    const campo = (id, etiqueta, opcional, props) => (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={id} className="text-sm font-extrabold">
          {etiqueta} {opcional ? <span className="font-semibold text-gray-600">{t('subMetaForm.opcional')}</span> : '*'}
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
    const resumenDias = formulario.modoDias === 'todos' ? t('subMetaForm.todosLosDiasMinuscula') : formulario.dias.length ? textoDias(formulario.dias) : t('subMetaForm.ningunDia');

    return (
      <form onSubmit={guardar} noValidate className="flex flex-col gap-5">
        {errorServidor.length > 0 && (
          <div role="alert" className="flex gap-2.5 rounded-xl bg-red-50 p-4 text-[15px] font-bold text-red-700">
            <CircleAlert className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
            <ul>{errorServidor.map((m) => <li key={m}>{m}</li>)}</ul>
          </div>
        )}
        <div className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
          {campo('nombre', t('subMetaForm.nombre'), false, {
            maxLength: 100,
            placeholder: {
              HITO: t('subMetaPaso.nombrePlaceholderHito'),
              MEDIBLE: t('subMetaForm.nombrePlaceholderMedible'),
            }[tipo] ?? t('subMetaForm.nombrePlaceholderSiNo'),
          })}

          {tipo === 'MEDIBLE' && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <span id="etiqueta-tipo-objetivo" className="text-sm font-extrabold">{t('subMetaForm.tipoObjetivo')}</span>
                <div role="group" aria-labelledby="etiqueta-tipo-objetivo" className="flex gap-1 rounded-xl bg-slate-100 p-1">
                  {[['AL_MENOS', t('subMetaForm.alMenos')], ['COMO_MAXIMO', t('subMetaForm.comoMaximo')]].map(([v, texto]) => (
                    <button key={v} type="button" aria-pressed={formulario.tipoObjetivo === v} onClick={() => cambiar('tipoObjetivo', v)} className={`${pastilla(formulario.tipoObjetivo === v)} flex-1 px-2`}>{texto}</button>
                  ))}
                </div>
              </div>
              {campo('objetivo', t('subMetaForm.objetivo'), false, { type: 'number', inputMode: 'decimal', min: 0, step: 'any', placeholder: t('subMetaForm.objetivoPlaceholder') })}
              {campo('unidad', t('subMetaForm.unidad'), false, { maxLength: 30, placeholder: t('subMetaForm.unidadPlaceholder') })}
            </div>
          )}

          {objetivoCambio && (
            <p role="status" className="flex gap-2.5 rounded-xl bg-amber-50 p-4 text-[15px] leading-6 text-amber-900">
              <TriangleAlert className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
              <span>
                {t('subMetaForm.cambioObjetivo')}
                {carga.subMeta.objetivo !== null && (
                  <Trans i18nKey="subMetaForm.cambioObjetivoDe" values={{ valor: formatearNumero(carga.subMeta.objetivo) }} components={{ b: <strong /> }} />
                )}
                {t('subMetaForm.cambioObjetivoTexto')}
              </span>
            </p>
          )}

          {tipo !== 'HITO' && (
          <fieldset className="flex flex-col gap-2" aria-describedby={errores.dias ? 'error-dias' : undefined}>
            <legend className="mb-1.5 text-sm font-extrabold">{t('subMetaForm.queDias')}</legend>
            <div className="flex gap-1 self-start rounded-xl bg-slate-100 p-1">
              {[['todos', t('subMetaForm.todosLosDias')], ['especificos', t('subMetaForm.diasEspecificos')]].map(([v, texto]) => (
                <button key={v} type="button" aria-pressed={formulario.modoDias === v} onClick={() => cambiar('modoDias', v)} className={pastilla(formulario.modoDias === v)}>{texto}</button>
              ))}
            </div>
            {formulario.modoDias === 'especificos' && (
              <div className="flex flex-wrap gap-2">
                {t('subMetaForm.letras', { returnObjects: true }).map((l, i) => {
                  const activo = formulario.dias.includes(i + 1);
                  return (
                    <button
                      key={i}
                      type="button"
                      aria-pressed={activo}
                      aria-label={diasLargos()[i]}
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
          )}

          {/* HU28: lo opcional va plegado para que agregar un paso sea rápido. Se abre solo si ya tiene datos o errores. */}
          <details
            open={Boolean(formulario.pregunta || formulario.notas || errores.pregunta || errores.notas)}
            className="group rounded-xl border border-zinc-200 px-4 py-3"
          >
            <summary className="cursor-pointer text-sm font-extrabold text-violet-800">{t('subMetaPaso.masOpciones')}</summary>
            <div className="mt-4 flex flex-col gap-4">
              {tipo !== 'HITO' && campo('pregunta', t('subMetaForm.pregunta'), true, { maxLength: 200, placeholder: tipo === 'MEDIBLE' ? t('subMetaForm.preguntaPlaceholderMedible') : t('subMetaForm.preguntaPlaceholderSiNo') })}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="notas" className="text-sm font-extrabold">{t('subMetaForm.notas')} <span className="font-semibold text-gray-600">{t('subMetaForm.opcional')}</span></label>
                <textarea id="notas" rows={2} maxLength={500} value={formulario.notas} onChange={(e) => cambiar('notas', e.target.value)} className="resize-y rounded-xl border border-zinc-200 px-3.5 py-3 text-[15px] outline-none focus:ring-2 focus:ring-violet-300" />
              </div>
            </div>
          </details>

          <p className="flex items-start gap-2.5 rounded-xl bg-violet-50 p-4 text-[15px] text-gray-700">
            <Info className="mt-0.5 size-5 shrink-0 text-violet-700" aria-hidden="true" />
            {tipo === 'HITO' && <span>{t('subMetaPaso.resumenHito')}</span>}
            {tipo === 'HITO' ? null : tipo === 'MEDIBLE'
              ? (
                <span>
                  <Trans
                    i18nKey="subMetaForm.resumenMedible"
                    values={{
                      tipo: formulario.tipoObjetivo === 'COMO_MAXIMO' ? t('subMetaForm.comoMaximoMinuscula') : t('subMetaForm.alMenosMinuscula'),
                      objetivo: formulario.objetivo || '…',
                      unidad: formulario.unidad || '…',
                      dias: resumenDias,
                    }}
                    components={{ b: <strong /> }}
                  />
                </span>
              )
              : <span><Trans i18nKey="subMetaForm.resumenSiNo" values={{ dias: resumenDias }} components={{ b: <strong />, c: <strong /> }} /></span>}
          </p>

          <div className="flex flex-wrap justify-end gap-3">
            <Link to={volverA} className="flex h-12 items-center rounded-xl border border-zinc-200 px-5 font-bold text-gray-700 hover:bg-gray-50">{t('subMetaForm.cancelar')}</Link>
            <button type="submit" disabled={guardando} className="flex h-12 items-center gap-2 rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 disabled:bg-violet-400">
              {guardando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
              {editando ? t('subMetaForm.guardarCambios') : t('subMetaForm.guardar')}
            </button>
          </div>
        </div>

        {editando && (
          <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-red-200 bg-white p-5">
            <div className="flex-[1_1_280px]">
              <h2 className="text-base font-extrabold text-red-700">{t('subMetaForm.eliminarTitulo')}</h2>
              <p className="text-sm text-gray-600">{t('subMetaForm.eliminarTexto')}</p>
            </div>
            <button type="button" onClick={() => setEliminar(true)} className="h-11 rounded-xl border border-red-200 px-4 font-bold text-red-700 hover:bg-red-50">{t('subMetaForm.eliminar')}</button>
          </section>
        )}
      </form>
    );
  };

  let titulo = t('subMetaForm.tituloNueva');
  if (editando) titulo = tipo === 'HITO' ? t('subMetaPaso.tituloEditarHito') : t('subMetaPaso.tituloEditarHabito');
  else if (tipo === 'HITO') titulo = t('subMetaPaso.tituloHito');
  else if (tipo === 'SI_NO') titulo = t('subMetaForm.tituloSiNo');
  else if (tipo === 'MEDIBLE') titulo = t('subMetaForm.tituloMedible');
  const nombreTipo = { HITO: t('subMetaPaso.hito'), MEDIBLE: t('subMetaPaso.habitoMedible') }[tipo] ?? t('subMetaPaso.habitoSiNo');

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto flex w-full max-w-[720px] flex-col gap-5 px-4 pb-16 pt-6 sm:pt-8">
        {!editando && TIPOS_PASO.includes(tipo) ? (
          <button type="button" onClick={() => setParametros({})} className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
            <ChevronLeft className="size-[18px]" aria-hidden="true" /> {t('subMetaForm.cambiarTipo')}
          </button>
        ) : (
          <Link to={volverA} className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
            <ChevronLeft className="size-[18px]" aria-hidden="true" /> {carga.nombreMeta ?? t('subMetaForm.volver')}
          </Link>
        )}
        <div>
          <h1 className="text-2xl font-extrabold sm:text-3xl">{titulo}</h1>
          {carga.nombreMeta && (
            <p className="mt-1 text-[15px] text-gray-600">
              {editando ? `${nombreTipo} · ` : ''}{t('subMetaForm.meta', { meta: carga.nombreMeta })}
              {editando && t('subMetaForm.tipoNoCambia')}
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
