import { describe, expect, it } from 'vitest';
import { REQUISITOS_CONTRASENA, validarContrasenaNueva } from './contrasena';
import { FILTROS_POR_DEFECTO, filtrosAUrl, filtrosDesdeUrl, hayFiltros } from './filtrosMetas';
import { comoGanarla, porcentajeAvance } from './insignias';
import { textoParticipantes } from './participantes';
import { iniciales, mesYAnio } from './perfil';
import { REPORTE_POR_DEFECTO, etiquetaMes, tablasDelReporte, tipoReporteValido } from './reporte';
import { textoDias, textoObjetivo } from './subMetas';
import { aFecha, aIso, diaCorto, diaYMes } from './fechas';
import { REGEX_EMAIL } from '../constants/validaciones';

const params = (texto) => new URLSearchParams(texto);
// La traducción de prueba devuelve la clave, para ver qué mensaje se eligió.
const claves = (clave) => clave;

describe('HU32 y HU33 · validarContrasenaNueva', () => {
  it('sin la nueva pide escribirla', () => {
    expect(validarContrasenaNueva({ nueva: '', confirmacion: '' }, claves)).toEqual({ nueva: 'contrasena.faltaNueva' });
  });

  it('una contraseña que no cumple la regla y no coincide da los dos errores', () => {
    expect(validarContrasenaNueva({ nueva: 'corta', confirmacion: 'otra' }, claves))
      .toEqual({ nueva: 'contrasena.noCumple', confirmacion: 'contrasena.noCoinciden' });
  });

  it('una contraseña válida y confirmada no tiene errores', () => {
    expect(validarContrasenaNueva({ nueva: 'Prueba123', confirmacion: 'Prueba123' }, claves)).toEqual({});
  });

  it('cada requisito revisa una sola condición (igual que ReglaContrasena del servidor)', () => {
    const cumple = (v) => REQUISITOS_CONTRASENA.map((r) => r.cumple(v));
    expect(cumple('abcdefgh')).toEqual([true, false, true, false]);
    expect(cumple('ABCDEFG1')).toEqual([true, true, false, true]);
    expect(cumple('Ab1')).toEqual([false, true, true, true]);
  });
});

describe('HU27 · filtros de Mis metas en la URL', () => {
  it('sin parámetros quedan los filtros por defecto (activas)', () => {
    expect(filtrosDesdeUrl(params(''))).toEqual(FILTROS_POR_DEFECTO);
  });

  it('lee los tres filtros válidos', () => {
    expect(filtrosDesdeUrl(params('estado=TODAS&tipo=ACADEMICA&plazo=CORTO')))
      .toEqual({ estado: 'TODAS', tipo: 'ACADEMICA', plazo: 'CORTO' });
  });

  it('un valor que no existe se ignora y la página no se rompe', () => {
    expect(filtrosDesdeUrl(params('estado=XYZ&tipo=DEPORTE&plazo=corto'))).toEqual(FILTROS_POR_DEFECTO);
  });

  it('a la URL solo van los filtros distintos del de por defecto', () => {
    expect(filtrosAUrl(FILTROS_POR_DEFECTO)).toEqual({});
    expect(filtrosAUrl({ estado: 'CUMPLIDA', tipo: '', plazo: 'LARGO' })).toEqual({ estado: 'CUMPLIDA', plazo: 'LARGO' });
  });

  it('hayFiltros solo es verdadero si algo cambió (muestra «Limpiar filtros»)', () => {
    expect(hayFiltros(FILTROS_POR_DEFECTO)).toBe(false);
    expect(hayFiltros({ ...FILTROS_POR_DEFECTO, tipo: 'OTRO' })).toBe(true);
  });
});

describe('HU12 · insignias', () => {
  it('explica cómo ganar cada criterio del catálogo', () => {
    expect(comoGanarla({ criterio: 'METAS_CUMPLIDAS', cantidadRequerida: 1 })).toBe('Cumple tu primera meta');
    expect(comoGanarla({ criterio: 'METAS_CUMPLIDAS', cantidadRequerida: 5 })).toBe('Cumple 5 metas');
    expect(comoGanarla({ criterio: 'METAS_CUMPLIDAS_PLAZO', plazoRequerido: 'CORTO', cantidadRequerida: 3 }))
      .toBe('Cumple 3 metas de corto plazo');
    expect(comoGanarla({ criterio: 'TIPOS_DISTINTOS', cantidadRequerida: 3 })).toBe('Cumple metas de 3 tipos distintos');
  });

  it('el porcentaje de avance va de 0 a 100 y no divide por cero', () => {
    expect(porcentajeAvance(3, 5)).toBe(60);
    expect(porcentajeAvance(12, 10)).toBe(100);
    expect(porcentajeAvance(1, 0)).toBe(0);
    expect(porcentajeAvance(1, 3)).toBe(33);
  });
});

describe('HU13 · textoParticipantes', () => {
  const yo = { codigoUsuario: 1, nombre: 'Ana', estado: 'ACEPTADA' };
  const laura = { codigoUsuario: 2, nombre: 'Laura', estado: 'ACEPTADA' };
  const andres = { codigoUsuario: 3, nombre: 'Andrés', estado: 'ACEPTADA' };
  const beto = { codigoUsuario: 4, nombre: 'Beto', estado: 'PENDIENTE' };

  it('no cuenta al usuario y nombra a los demás', () => {
    expect(textoParticipantes([yo], 1)).toBe('');
    expect(textoParticipantes([yo, laura], 1)).toBe('Con Laura');
    expect(textoParticipantes([yo, laura, andres], 1)).toBe('Con Laura y Andrés');
  });

  it('con más de dos resume y agrega las invitaciones pendientes', () => {
    const cuarto = { codigoUsuario: 5, nombre: 'Mateo', estado: 'ACEPTADA' };
    expect(textoParticipantes([yo, laura, andres, cuarto, beto], 1)).toBe('Con Laura y 2 más · 1 invitación pendiente');
  });
});

describe('HU22 · perfil', () => {
  it('iniciales del avatar', () => {
    expect(iniciales('felipe', 'beltrán')).toBe('FB');
    expect(iniciales('  Ana ', null)).toBe('A');
    expect(iniciales('', '')).toBe('?');
  });

  it('fecha de registro como mes y año', () => {
    expect(mesYAnio('2026-09-20')).toBe('septiembre de 2026');
    expect(mesYAnio(null)).toBe('');
  });
});

describe('HU7 · reporte', () => {
  it('un tipo de reporte que no existe vuelve al general', () => {
    expect(tipoReporteValido('por-mes')).toBe('por-mes');
    expect(tipoReporteValido('xyz')).toBe(REPORTE_POR_DEFECTO);
    expect(tipoReporteValido(null)).toBe('general');
  });

  it('etiqueta de cada mes', () => {
    expect(etiquetaMes('2026-11')).toBe('nov 2026');
  });

  it('la tabla alternativa tiene los mismos datos que las gráficas', () => {
    const tablas = tablasDelReporte({
      totalMetas: 7, metasActivas: 3, metasCumplidas: 4,
      porTipo: [{ tipo: 'ACADEMICA', total: 3, cumplidas: 1, porcentaje: 33 }],
      porPlazo: [{ plazo: 'CORTO', activas: 2, cumplidas: 3 }],
      cumplidasPorMes: [{ mes: '2026-11', cumplidas: 1 }],
    });
    expect(tablas.general.filas).toEqual([['Cumplidas', 4], ['Activas', 3], ['Total', 7]]);
    expect(tablas.porTipo.filas).toEqual([['Académica', 3, 1, '33 %']]);
    expect(tablas.porPlazo.filas).toEqual([['Corto plazo', 2, 3]]);
    expect(tablas.porMes.filas).toEqual([['nov 2026', 1]]);
  });
});

describe('HU28 y HU5 · textos de los pasos', () => {
  it('objetivo de cada tipo de hábito', () => {
    expect(textoObjetivo({ tipo: 'SI_NO' })).toBe('Sí / No');
    expect(textoObjetivo({ tipo: 'MEDIBLE', tipoObjetivo: 'AL_MENOS', objetivo: 15, unidad: 'páginas' })).toBe('Al menos 15 páginas');
  });

  it('días en que toca el hábito', () => {
    expect(textoDias(null)).toBe('diariamente');
    expect(textoDias([1, 2, 3, 4, 5, 6, 7])).toBe('diariamente');
    expect(textoDias([5, 1, 2, 3, 4])).toBe('lunes a viernes');
    expect(textoDias([7, 6])).toBe('fines de semana');
    expect(textoDias([3])).toBe('solo miércoles');
    expect(textoDias([5, 1, 3])).toBe('lunes, miércoles y viernes');
  });
});

describe('Fechas (zona local, sin correr el día)', () => {
  it('ida y vuelta entre ISO y Date', () => {
    expect(aIso(aFecha('2026-11-02'))).toBe('2026-11-02');
    expect(aIso(aFecha('2026-01-31'))).toBe('2026-01-31');
  });

  it('nombre corto del día con el lunes primero y día con mes', () => {
    expect(diaCorto('2026-11-02')).toBe('Lun'); // 2 nov 2026 es lunes
    expect(diaCorto('2026-11-08')).toBe('Dom');
    expect(diaYMes('2026-11-02')).toBe('2 nov');
  });
});

describe('Validación del correo (HU8 y HU33)', () => {
  it('acepta correos con formato válido y rechaza los demás', () => {
    expect(REGEX_EMAIL.test('ana@metamorfosis.test')).toBe(true);
    expect(REGEX_EMAIL.test('no-es-un-correo')).toBe(false);
    expect(REGEX_EMAIL.test('a b@c.co')).toBe(false);
  });
});
