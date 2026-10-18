import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import MisMetasPage from './pages/MisMetasPage';
import CrearMetaPage from './pages/CrearMetaPage';
import EditarMetaPage from './pages/EditarMetaPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/metas" replace />} />
        <Route path="/metas" element={<MisMetasPage />} />
        <Route path="/metas/nueva" element={<CrearMetaPage />} />
        <Route path="/metas/:codigoMeta/editar" element={<EditarMetaPage />} />
        <Route path="*" element={<Navigate to="/metas" replace />} />
      </Routes>
    </BrowserRouter>
  );
}