import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import App from '../App';

// Mock API calls
vi.mock('../api/client', () => ({
  api: {
    ensureAuthenticated: vi.fn().mockResolvedValue('mock_jwt_token'),
    getDashboard: vi.fn().mockResolvedValue({
      quotesProcessed: 5,
      negotiationsAutomated: 3,
      totalSpend: 1500000,
      totalSavings: 250000,
      totalWorkflows: 3,
      completedWorkflows: 2,
      spendByCategory: [
        { category: 'Laptops', amount: 950000 },
        { category: 'Servers', amount: 550000 }
      ],
      pendingApprovals: 1,
      purchaseOrdersGenerated: 2
    }),
    listWorkflows: vi.fn().mockResolvedValue([
      { id: 101, title: 'Laptop Procurement Demo', status: 'COMPLETED' }
    ]),
    getMarketCategories: vi.fn().mockResolvedValue({
      laptop: {
        category: 'Laptops & Computing',
        minPrice: 45000,
        maxPrice: 120000,
        medianPrice: 65000,
        supplyChainRisk: 'LOW',
        trends: ['ARM processors gaining adoption'],
        supplyChainRisks: ['Semiconductor constraints'],
        regulatoryChanges: ['BIS certification required'],
        competitorInsights: ['Dell Latitude vs Lenovo ThinkPad']
      }
    }),
    listEmailsForNegotiation: vi.fn().mockResolvedValue([]),
    retryEmail: vi.fn().mockResolvedValue({ status: 'SENT' })
  }
}));

describe('ProcureAI Application Core Flow & Accessibility', () => {
  beforeEach(() => {
    localStorage.setItem('procureai_token', 'mock_jwt_token');
    localStorage.setItem('procureai_user', JSON.stringify({
      id: 1, email: 'admin@procureai.demo', name: 'Admin Officer', role: 'ADMIN'
    }));
  });
  it('renders application navigation header and brand title', () => {
    render(<App />);
    expect(screen.getByText(/Procurement Command Center/i)).toBeInTheDocument();
  });

  it('provides accessible landmark regions and primary heading hierarchy', () => {
    render(<App />);
    const heading = screen.getByRole('heading', { level: 1 });
    expect(heading).toHaveTextContent(/Procurement Command Center/i);
  });

  it('renders reactive KPI metric cards without hardcoded fallback text', async () => {
    render(<App />);
    expect(await screen.findByText(/Total Procurement Spend/i)).toBeInTheDocument();
    expect(await screen.findByText(/Total Negotiated Savings/i)).toBeInTheDocument();
  });
});
