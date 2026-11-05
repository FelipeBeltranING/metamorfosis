import { createContext, useContext } from 'react';

/** HU11: función para anunciar insignias nuevas. La provee AvisosInsigniaProvider (App.jsx). */
export const AvisosInsigniaContexto = createContext(() => {});

/** Devuelve mostrarInsignias(lista): muestra un aviso por cada insignia ganada. */
export function useAvisosInsignia() {
  return useContext(AvisosInsigniaContexto);
}
