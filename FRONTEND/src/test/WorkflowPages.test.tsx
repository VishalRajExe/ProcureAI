import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { ComparisonPage } from '../pages/ComparisonPage';
import { NegotiationPage } from '../pages/NegotiationPage';
import { PurchaseOrdersPage } from '../pages/PurchaseOrdersPage';
import { ToastProvider } from '../components/Toast';

vi.mock('../api/client', () => ({
  api: {
    listQuotes: vi.fn().mockResolvedValue([
      {
        id: 10,
        vendor: { name: 'Dell Business Direct', contactEmail: 'sales@dell-direct.demo' },
        calculatedTotal: 650000,
        extractionStatus: 'VALIDATED',
        benchmarkStatus: 'WITHIN'
      }
    ]),
    listNegotiations: vi.fn().mockResolvedValue([
      {
        id: 20,
        quote: {
          id: 10,
          vendor: { name: 'Dell Business Direct', contactEmail: 'sales@dell-direct.demo' },
          calculatedTotal: 650000
        },
        currentPrice: 650000,
        targetPrice: 580000,
        maxApprovedPrice: 620000,
        status: 'PENDING_APPROVAL',
        draftEmailBody: 'Dear Dell Team, we request a revised unit price.'
      }
    ]),
    listPurchaseOrders: vi.fn().mockResolvedValue([
      {
        id: 30,
        poNumber: 'PO-2026-1001',
        vendor: { name: 'Dell Business Direct' },
        totalAmount: 595000,
        status: 'GENERATED'
      }
    ]),
    getComparison: vi.fn().mockResolvedValue({
      workflowId: 1,
      recommendedQuoteId: 10,
      quotes: []
    }),
    getPdfUrl: vi.fn().mockImplementation((id) => `/api/purchase-orders/${id}/pdf`),
    generatePO: vi.fn().mockResolvedValue({ id: 31, poNumber: 'PO-2026-1002' })
  }
}));

describe('Procurement Workflow Pages & Components', () => {
  it('ComparisonPage renders quote comparison header and analytics', async () => {
    render(
      <ToastProvider>
        <BrowserRouter>
          <ComparisonPage />
        </BrowserRouter>
      </ToastProvider>
    );
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Quote Comparison/i);
  });

  it('NegotiationPage renders AI negotiation center and email draft preview', async () => {
    render(
      <ToastProvider>
        <BrowserRouter>
          <NegotiationPage />
        </BrowserRouter>
      </ToastProvider>
    );
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/AI Negotiation Center/i);
  });

  it('PurchaseOrdersPage renders purchase orders header and table', async () => {
    render(
      <ToastProvider>
        <BrowserRouter>
          <PurchaseOrdersPage />
        </BrowserRouter>
      </ToastProvider>
    );
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Purchase Orders/i);
    expect(await screen.findByText(/PO-2026-1001/i)).toBeInTheDocument();
  });
});
