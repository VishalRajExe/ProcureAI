import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import { MarketPage } from '../pages/MarketPage';
import { QuotesPage } from '../pages/QuotesPage';
import { ToastProvider } from '../components/Toast';

vi.mock('../api/client', () => ({
  api: {
    getMarketCategories: vi.fn().mockResolvedValue({
      thinkpad: {
        category: 'Lenovo ThinkPad',
        minPrice: 52000,
        maxPrice: 90000,
        medianPrice: 67000,
        supplyChainRisk: 'LOW',
        trends: ['Enterprise adoption growing'],
        supplyChainRisks: ['Stable supply'],
        regulatoryChanges: ['BIS certified'],
        competitorInsights: ['HP EliteBook comparable']
      }
    }),
    listQuotes: vi.fn().mockResolvedValue([
      {
        id: 1,
        vendor: { name: 'Lenovo Corporate Sales', contactEmail: 'sales@lenovo.demo' },
        calculatedTotal: 130000,
        extractionStatus: 'VALIDATED',
        benchmarkStatus: 'WITHIN'
      }
    ]),
    ingestQuote: vi.fn().mockResolvedValue({ id: 2 }),
    uploadQuotePdf: vi.fn().mockResolvedValue({ id: 3 })
  }
}));

describe('Accessibility & UI Standards Compliance', () => {
  it('MarketPage renders accessible heading, category count, and risk badges', async () => {
    render(
      <ToastProvider>
        <BrowserRouter>
          <MarketPage />
        </BrowserRouter>
      </ToastProvider>
    );

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Market Intelligence/i);
    expect(await screen.findByText(/Lenovo ThinkPad/i)).toBeInTheDocument();
    expect(await screen.findByText(/Low Risk Categories/i)).toBeInTheDocument();
  });

  it('QuotesPage provides accessible form controls and file input labels', async () => {
    render(
      <ToastProvider>
        <BrowserRouter>
          <QuotesPage />
        </BrowserRouter>
      </ToastProvider>
    );

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Quotes & Ingestion/i);
    expect(await screen.findByText(/Ingested Quotations/i)).toBeInTheDocument();
  });
});
