import { createAvatar } from '@dicebear/core';
import * as bigSmile from '@dicebear/big-smile';

/**
 * HU20: piezas del avatar. Son los valores del estilo «Big Smile» de DiceBear (ilustraciones de
 * Ashley Seo, CC BY 4.0) y coinciden con CatalogoAvatar del backend y la migración V26.
 * El orden es el del editor; el primero de cada lista no es el inicial (ese va en AVATAR_INICIAL).
 */
export const PIEZAS_AVATAR = [
  { campo: 'piel', tipo: 'color', valores: ['ffe4c0', 'f5d7b1', 'efcc9f', 'e2ba87', 'c99c62', 'a47539', '8c5a2b', '643d19'] },
  {
    campo: 'peinado',
    tipo: 'pieza',
    valores: ['shortHair', 'curlyShortHair', 'bowlCutHair', 'mohawk', 'halfShavedHead', 'shavedHead', 'bangs',
      'wavyBob', 'curlyBob', 'straightHair', 'braids', 'bunHair', 'froBun'],
  },
  { campo: 'colorPelo', tipo: 'color', valores: ['220f00', '3a1a00', '71472d', 'e2ba87', 'e9b729', 'd56c0c', '605de4', '238d80'] },
  { campo: 'ojos', tipo: 'pieza', valores: ['cheery', 'normal', 'winking', 'starstruck', 'sleepy', 'confused', 'sad', 'angry'] },
  {
    campo: 'boca',
    tipo: 'pieza',
    valores: ['openedSmile', 'teethSmile', 'gapSmile', 'awkwardSmile', 'kawaii', 'braces', 'unimpressed', 'openSad'],
  },
  {
    campo: 'accesorio',
    tipo: 'pieza',
    valores: [null, 'glasses', 'sunglasses', 'catEars', 'sailormoonCrown', 'mustache', 'clownNose', 'sleepMask', 'faceMask'],
  },
];

/** El avatar de todo usuario nuevo (igual que CatalogoAvatar en el backend). */
export const AVATAR_INICIAL = {
  piel: 'f5d7b1', peinado: 'shortHair', colorPelo: '71472d', ojos: 'cheery', boca: 'openedSmile', accesorio: null,
};

/** Completa lo que falte con el avatar inicial (p. ej. si el servidor aún no lo manda). */
export function avatarCompleto(avatar) {
  return { ...AVATAR_INICIAL, ...(avatar ?? {}), accesorio: avatar?.accesorio || null };
}

export function mismoAvatar(a, b) {
  const x = avatarCompleto(a);
  const y = avatarCompleto(b);
  return PIEZAS_AVATAR.every(({ campo }) => x[campo] === y[campo]);
}

/** Un avatar al azar con piezas del catálogo (botón «Sorpréndeme»). */
export function avatarAlAzar() {
  const azar = (lista) => lista[Math.floor(Math.random() * lista.length)];
  return Object.fromEntries(PIEZAS_AVATAR.map(({ campo, valores }) => [campo, azar(valores)]));
}

/** Arma el dibujo (SVG en data URL) con DiceBear. Se dibuja en el navegador; no llama a ningún servidor. */
export function dibujoAvatar(avatar) {
  const a = avatarCompleto(avatar);
  return createAvatar(bigSmile, {
    seed: 'metamorfosis',
    skinColor: [a.piel],
    hair: [a.peinado],
    hairColor: [a.colorPelo],
    eyes: [a.ojos],
    mouth: [a.boca],
    accessories: a.accesorio ? [a.accesorio] : [],
    accessoriesProbability: a.accesorio ? 100 : 0,
  }).toDataUri();
}

/** Tono más oscuro de la piel para el contorno de las manos que se apoyan en el panel. */
export function pielOscura(piel) {
  return `#${piel.match(/../g).map((c) => Math.round(parseInt(c, 16) * 0.78).toString(16).padStart(2, '0')).join('')}`;
}
