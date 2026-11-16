import { describe, expect, it } from 'vitest';

// Internacionalización: los dos idiomas tienen los mismos archivos y las mismas claves,
// así ningún texto queda sin traducir ni se muestra la clave en pantalla.
const es = import.meta.glob('./es/*.json', { eager: true, import: 'default' });
const en = import.meta.glob('./en/*.json', { eager: true, import: 'default' });

const archivo = (ruta) => ruta.split('/').pop();
const claves = (objeto, prefijo = '') => Object.entries(objeto).flatMap(([k, v]) =>
  (v && typeof v === 'object' && !Array.isArray(v) ? claves(v, `${prefijo}${k}.`) : [`${prefijo}${k}`]));
const porArchivo = (archivos) => Object.fromEntries(Object.entries(archivos).map(([r, v]) => [archivo(r), v]));

describe('Internacionalización · paridad de traducciones', () => {
  const textosEs = porArchivo(es);
  const textosEn = porArchivo(en);

  it('español e inglés tienen los mismos archivos', () => {
    expect(Object.keys(textosEn).sort()).toEqual(Object.keys(textosEs).sort());
  });

  it.each(Object.keys(textosEs))('%s tiene las mismas claves en los dos idiomas', (nombre) => {
    expect(claves(textosEn[nombre]).sort()).toEqual(claves(textosEs[nombre]).sort());
  });

  it('ningún texto en inglés quedó vacío', () => {
    const vacios = Object.entries(textosEn).flatMap(([nombre, textos]) =>
      claves(textos).filter((c) => c.split('.').reduce((o, k) => o?.[k], textos) === '').map((c) => `${nombre}: ${c}`));
    expect(vacios).toEqual([]);
  });

  it('las listas (días y meses) tienen el mismo largo en los dos idiomas', () => {
    const { fechas: fEs } = textosEs['comun.json'];
    const { fechas: fEn } = textosEn['comun.json'];
    for (const lista of ['diasCortos', 'diasLargos', 'mesesCortos', 'mesesLargos']) {
      expect(fEn[lista]).toHaveLength(fEs[lista].length);
    }
  });
});
