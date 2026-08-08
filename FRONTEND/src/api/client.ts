import axios from 'axios';
import type { AxiosInstance } from 'axios';
import type {
  AuthTokens, User, WorkflowExecution, Quote, ComparisonResult,
  Negotiation, PurchaseOrder, DashboardData, MarketIntelligence,
  VendorIntelligenceBundle, EvaluationReport, Vendor
} from '../types';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

class ApiClient {
  private http: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: BASE_URL,
      timeout: 30000,
      headers: { 'Content-Type': 'application/json' },
    });

    // Attach JWT token from localStorage automatically
    this.http.interceptors.request.use((config) => {
      const token = localStorage.getItem('procureai_token');
      if (token) config.headers.Authorization = `Bearer ${token}`;
      return config;
    });

    // Handle 401 -> redirect to login
    this.http.interceptors.response.use(
      (res) => res,
      (error) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('procureai_token');
          localStorage.removeItem('procureai_user');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // ── Auth ────────────────────────────────────────────────────────────────────

  async login(email: string, password: string): Promise<AuthTokens> {
    const { data } = await this.http.post<AuthTokens>('/api/auth/login', { email, password });
    localStorage.setItem('procureai_token', data.token);
    localStorage.setItem('procureai_user', JSON.stringify(data.user));
    return data;
  }

  async register(email: string, name: string, password: string): Promise<AuthTokens> {
    const { data } = await this.http.post<AuthTokens>('/api/auth/register', { email, name, password });
    localStorage.setItem('procureai_token', data.token);
    localStorage.setItem('procureai_user', JSON.stringify(data.user));
    return data;
  }

  async getCurrentUser(): Promise<User> {
    const { data } = await this.http.get<User>('/api/auth/me');
    return data;
  }

  logout() {
    localStorage.removeItem('procureai_token');
    localStorage.removeItem('procureai_user');
  }

  // ── Workflows ───────────────────────────────────────────────────────────────

  async listWorkflows(): Promise<WorkflowExecution[]> {
    const { data } = await this.http.get<WorkflowExecution[]>('/api/workflows');
    return data;
  }

  async getWorkflow(id: number): Promise<WorkflowExecution> {
    const { data } = await this.http.get<WorkflowExecution>(`/api/workflows/${id}`);
    return data;
  }

  async getWorkflowReport(id: number): Promise<EvaluationReport> {
    const { data } = await this.http.get<EvaluationReport>(`/api/workflows/${id}/report`);
    return data;
  }

  // ── Quotes ──────────────────────────────────────────────────────────────────

  async listQuotes(workflowId?: number): Promise<Quote[]> {
    const url = workflowId ? `/api/quotes?workflowId=${workflowId}` : '/api/quotes';
    const { data } = await this.http.get<Quote[]>(url);
    return data;
  }

  async getQuote(id: number): Promise<Quote> {
    const { data } = await this.http.get<Quote>(`/api/quotes/${id}`);
    return data;
  }

  async uploadQuote(workflowId: number, vendorName: string, file: File): Promise<Quote> {
    const form = new FormData();
    form.append('file', file);
    form.append('vendorName', vendorName);
    form.append('workflowId', workflowId.toString());
    const { data } = await this.http.post<Quote>('/api/quotes/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
  }

  async submitQuoteText(workflowId: number, vendorName: string, rawText: string): Promise<Quote> {
    const { data } = await this.http.post<Quote>('/api/quotes', {
      workflowId,
      vendorName,
      rawText,
    });
    return data;
  }

  async createWorkflow(title: string, description?: string): Promise<WorkflowExecution> {
    const { data } = await this.http.post<WorkflowExecution>('/api/quotes/workflows', {
      title,
      description,
    });
    return data;
  }

  // ── Comparison ──────────────────────────────────────────────────────────────

  async compareWorkflow(workflowId: number): Promise<ComparisonResult> {
    const { data } = await this.http.post<ComparisonResult>(
      `/api/comparison/workflows/${workflowId}`
    );
    return data;
  }

  async getLatestComparison(): Promise<ComparisonResult> {
    const { data } = await this.http.get<ComparisonResult>('/api/comparison');
    return data;
  }

  // ── Negotiations ────────────────────────────────────────────────────────────

  async listNegotiations(): Promise<Negotiation[]> {
    const { data } = await this.http.get<Negotiation[]>('/api/negotiations');
    return data;
  }

  async draftNegotiation(quoteId: number): Promise<Negotiation> {
    const { data } = await this.http.post<Negotiation>('/api/negotiations', { quoteId });
    return data;
  }

  async approveNegotiation(
    id: number,
    approve: boolean,
    editedEmailBody?: string,
    notes?: string
  ): Promise<Negotiation> {
    const { data } = await this.http.post<Negotiation>(`/api/negotiations/${id}/approve`, {
      approve,
      editedEmailBody,
      notes,
    });
    return data;
  }

  async simulateVendorResponse(id: number, counterPrice: number): Promise<Negotiation> {
    const { data } = await this.http.post<Negotiation>(
      `/api/negotiations/${id}/simulate-response`,
      { counterPrice }
    );
    return data;
  }

  // ── Purchase Orders ─────────────────────────────────────────────────────────

  async listPurchaseOrders(): Promise<PurchaseOrder[]> {
    const { data } = await this.http.get<PurchaseOrder[]>('/api/purchase-orders');
    return data;
  }

  async generatePO(quoteId: number, workflowId: number, negotiationId?: number): Promise<PurchaseOrder> {
    const { data } = await this.http.post<PurchaseOrder>('/api/purchase-orders/generate', {
      quoteId,
      workflowId,
      negotiationId,
    });
    return data;
  }

  getPdfUrl(poId: number): string {
    return `${BASE_URL}/api/purchase-orders/${poId}/pdf`;
  }

  // ── Dashboard ───────────────────────────────────────────────────────────────

  async getDashboard(): Promise<DashboardData> {
    const { data } = await this.http.get<DashboardData>('/api/dashboard');
    return data;
  }

  // ── Market Intelligence ─────────────────────────────────────────────────────

  async getMarketCategories(): Promise<Record<string, MarketIntelligence>> {
    const { data } = await this.http.get<Record<string, MarketIntelligence>>('/api/market-intelligence');
    return data;
  }

  async getMarketData(category: string): Promise<MarketIntelligence> {
    const { data } = await this.http.get<MarketIntelligence>(`/api/market-intelligence/${category}`);
    return data;
  }

  async getVendorIntelligence(quoteId: number): Promise<VendorIntelligenceBundle> {
    const { data } = await this.http.get<VendorIntelligenceBundle>(
      `/api/market-intelligence/quotes/${quoteId}/intelligence`
    );
    return data;
  }

  // ── Vendors ─────────────────────────────────────────────────────────────────

  async listVendors(): Promise<Vendor[]> {
    const { data } = await this.http.get<Vendor[]>('/api/vendors');
    return data;
  }

  // ── Demo ─────────────────────────────────────────────────────────────────────

  async runDemo(): Promise<Record<string, unknown>> {
    const { data } = await this.http.post<Record<string, unknown>>('/api/demo/run');
    return data;
  }

  async seedDemo(): Promise<Record<string, unknown>> {
    const { data } = await this.http.post<Record<string, unknown>>('/api/demo/seed');
    return data;
  }
}

export const api = new ApiClient();
