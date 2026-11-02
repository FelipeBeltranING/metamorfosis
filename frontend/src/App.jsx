import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MisMetasPage from './pages/MisMetasPage';
import MisLogrosPage from './pages/MisLogrosPage';
import CrearMetaPage from './pages/CrearMetaPage';
import EditarMetaPage from './pages/EditarMetaPage';
import RegistroPage from './pages/RegistroPage';
import SolicitarRegistroPage from './pages/SolicitarRegistroPage';
import RechazarRegistroPage from './pages/RechazarRegistroPage';
import AutorizarRegistroPage from './pages/AutorizarRegistroPage';
import LoginPage from './pages/LoginPage';
import LogroPublicoPage from './pages/LogroPublicoPage';
import RutaProtegida from './components/RutaProtegida';

export default function App() {
  return (
    <BrowserRouter>
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
          <Route path="/mis-logros" element={<MisLogrosPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/metas" replace />} />
      </Routes>
    </BrowserRouter>
  );
}