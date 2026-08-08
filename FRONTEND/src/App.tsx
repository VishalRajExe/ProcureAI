import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider } from './components/Toast';
import { Layout } from './components/layout/Layout';
import { Dashboard } from './pages/Dashboard';
import { QuotesPage } from './pages/QuotesPage';
import { ComparisonPage } from './pages/ComparisonPage';
import { NegotiationPage } from './pages/NegotiationPage';
import { ApprovalsPage } from './pages/ApprovalsPage';
import { VendorInboxPage } from './pages/VendorInboxPage';
import { PurchaseOrdersPage } from './pages/PurchaseOrdersPage';
import { WorkflowsPage } from './pages/WorkflowsPage';
import { WorkflowDetail } from './pages/WorkflowDetail';
import { DemoPage } from './pages/DemoPage';
import { MarketPage } from './pages/MarketPage';

function App() {
  useEffect(() => {
    // Ensure default active session so login screen is completely bypassed
    if (!localStorage.getItem('procureai_token')) {
      localStorage.setItem('procureai_token', 'demo_active_session_token');
      localStorage.setItem('procureai_user', JSON.stringify({
        id: 1, email: 'admin@procureai.demo', name: 'Admin Officer', role: 'ADMIN'
      }));
    }
  }, []);

  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/quotes" element={<QuotesPage />} />
            <Route path="/comparison" element={<ComparisonPage />} />
            <Route path="/negotiation" element={<NegotiationPage />} />
            <Route path="/approvals" element={<ApprovalsPage />} />
            <Route path="/vendor-inbox" element={<VendorInboxPage />} />
            <Route path="/purchase-orders" element={<PurchaseOrdersPage />} />
            <Route path="/workflows" element={<WorkflowsPage />} />
            <Route path="/workflows/:id" element={<WorkflowDetail />} />
            <Route path="/demo" element={<DemoPage />} />
            <Route path="/market" element={<MarketPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}

export default App;
