import { Award, Compass, Flame, Footprints, Mountain, Rocket, Zap } from 'lucide-react';

/**
 * HU11: ícono de una insignia. El backend manda el nombre del ícono de lucide-react;
 * cada caso se escribe aparte para que el componente sea estático (regla react-hooks/static-components).
 */
export default function IconoInsignia({ nombre, ...props }) {
  switch (nombre) {
    case 'Footprints': return <Footprints {...props} />;
    case 'Flame': return <Flame {...props} />;
    case 'Rocket': return <Rocket {...props} />;
    case 'Zap': return <Zap {...props} />;
    case 'Mountain': return <Mountain {...props} />;
    case 'Compass': return <Compass {...props} />;
    default: return <Award {...props} />;
  }
}
