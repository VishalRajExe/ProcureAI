import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/layout/Layout';
import { Dashboard } from './pages/Dashboard';
import { WorkflowsPage } from './pages/WorkflowsPage';
import { WorkflowDetail } from './pages/WorkflowDetail';
import { DemoPage } from './pages/DemoPage';
import { LoginPage } from './pages/LoginPage';
import { MarketPage } from './pages/MarketPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/workflows" element={<WorkflowsPage />} />
          <Route path="/workflows/:id" element={<WorkflowDetail />} />
          <Route path="/demo" element={<DemoPage />} />
          <Route path="/market" element={<MarketPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
