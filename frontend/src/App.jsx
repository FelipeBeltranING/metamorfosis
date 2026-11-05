import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MisMetasPage from './pages/MisMetasPage';
import CaminoMetaPage from './pages/CaminoMetaPage';
import SubMetaFormPage from './pages/SubMetaFormPage';
import EstadisticasSubMetaPage from './pages/EstadisticasSubMetaPage';
import CrearMetaPage from './pages/CrearMetaPage';
import EditarMetaPage from './pages/EditarMetaPage';
import EditarPerfilPage from './pages/EditarPerfilPage';
import RegistroPage from './pages/RegistroPage';
import SolicitarRegistroPage from './pages/SolicitarRegistroPage';
import RechazarRegistroPage from './pages/RechazarRegistroPage';
import AutorizarRegistroPage from './pages/AutorizarRegistroPage';
import LoginPage from './pages/LoginPage';
import LogroPublicoPage from './pages/LogroPublicoPage';
import RutaProtegida from './components/RutaProtegida';
import AvisosInsigniaProvider from './components/AvisosInsignia';

// HU7: el reporte (y Chart.js) se descarga solo cuando el usuario entra a /reporte,
// así el resto de la app no carga las librerías de gráficas.
const ReportePage = lazy(() => import('./pages/ReportePage'));

/** Fondo vacío mientras llega el código del reporte (suele tardar menos de un segundo). */
function CargandoPagina() {
  return <div className="min-h-screen bg-slate-50" aria-busy="true" />;
}

export default function App() {
  return (
    <BrowserRouter>
      {/* HU11: los avisos de insignias se ven en cualquier pantalla. */}
      <AvisosInsigniaProvider>
        <Routes>
          <Route path="/" element={<Navigate to="/metas" replace />} />
          {/* HU8: primero se autoriza el correo; el formulario solo abre con el enlace. */}
          <Route path="/registro" element={<SolicitarRegistroPage />} />
          <Route path="/registro/autorizar" element={<AutorizarRegistroPage />} />
          <Route path="/registro/completar" element={<RegistroPage />} />
          <Route path="/registro/rechazar" element={<RechazarRegistroPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/logros/:token" element={<LogroPublicoPage />} />
          <Route element={<RutaProtegida />}>
            <Route path="/metas" element={<MisMetasPage />} />
            <Route path="/metas/nueva" element={<CrearMetaPage />} />
            <Route path="/metas/:codigoMeta/editar" element={<EditarMetaPage />} />
            {/* Mis logros se retiró: las cumplidas están en el filtro de Mis metas. */}
            <Route path="/mis-logros" element={<Navigate to="/metas?estado=CUMPLIDA" replace />} />
            <Route
              path="/reporte"
              element={(
                <Suspense fallback={<CargandoPagina />}>
                  <ReportePage />
                </Suspense>
              )}
            />
            {/* Sub-metas: el camino de cada meta. */}
            <Route path="/metas/:codigoMeta" element={<CaminoMetaPage />} />
            <Route path="/metas/:codigoMeta/submetas/nueva" element={<SubMetaFormPage />} />
            <Route path="/submetas/:codigoSubMeta" element={<EstadisticasSubMetaPage />} />
            <Route path="/submetas/:codigoSubMeta/editar" element={<SubMetaFormPage />} />
            {/* HU-23: editar nombre, apellido y foto, desde el menú de la cuenta. */}
            <Route path="/perfil/editar" element={<EditarPerfilPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/metas" replace />} />
        </Routes>
      </AvisosInsigniaProvider>
    </BrowserRouter>
  );
}