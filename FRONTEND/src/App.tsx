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
import { LoginPage } from './pages/LoginPage';
import { MarketPage } from './pages/MarketPage';

function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
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
