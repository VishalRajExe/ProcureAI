// Types matching the Java Spring Boot backend DTOs

export interface User {
  id: number;
  name: string;
  email: string;
  role: 'ADMIN' | 'APPROVER' | 'PROCUREMENT_USER' | 'VIEWER';
}

export interface AuthTokens {
  token: string;
  user: User;
}

export interface Vendor {
  id: number;
  name: string;
  contactEmail?: string;
  address?: string;
  reliabilityScore?: number;
  gstNumber?: string;
  trustSealVerified?: boolean;
  yearsExperience?: number;
  responseRate?: number;
  category?: string;
  location?: string;
}

export interface QuoteItem {
  id: number;
  productName: string;
  model?: string;
  quantity: number;
  unitPrice: number;
}

export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'EXTRACTED' | 'VALIDATED' | 'FAILED';
export type BenchmarkStatus = 'BELOW' | 'WITHIN' | 'ABOVE' | 'UNKNOWN';

export interface Quote {
  id: number;
  vendor: Vendor;
  sourceFileName?: string;
  sourceType: string;
  extractionStatus: ExtractionStatus;
  extractionError?: string;
  extractionConfidence?: number;
  currency: string;
  discountPercent: number;
  taxPercent: number;
  shippingCost: number;
  vendorDeclaredTotal?: number;
  calculatedTotal: number;
  warrantyMonths?: number;
  deliveryDays?: number;
  paymentTerms?: string;
  validUntil?: string;
  benchmarkStatus: BenchmarkStatus;
  vendorScore?: number;
  items: QuoteItem[];
  createdAt: string;
}

export type WorkflowStatus =
  | 'PROCESSING'
  | 'COMPARED'
  | 'NEGOTIATING'
  | 'AWAITING_VENDOR_RESPONSE'
  | 'RE_EVALUATING'
  | 'VENDOR_SELECTED'
  | 'COMPLETED'
  | 'FAILED';

export interface WorkflowExecution {
  id: number;
  title: string;
  description?: string;
  status: WorkflowStatus;
  createdByUserId?: number;
  createdAt: string;
}

export interface ComparisonResult {
  rankedQuotes: Quote[];
  recommended?: Quote;
  aiExplanation: string;
}

export type NegotiationStatus =
  | 'DRAFTED'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED_BY_HUMAN'
  | 'SENT'
  | 'VENDOR_RESPONDED'
  | 'RE_EVALUATING'
  | 'ACCEPTED'
  | 'FAILED'
  | 'COMPLETED';

export interface NegotiationRound {
  id: number;
  roundNumber: number;
  offeredPriceByAi: number;
  vendorCounterPrice?: number;
  aiEvaluationNotes?: string;
  outcome?: string;
  withinMaxApproved?: boolean;
}

export interface Negotiation {
  id: number;
  quote: Quote;
  currentPrice: number;
  targetPrice: number;
  maxApprovedPrice: number;
  aiAction?: string;
  aiStrategy?: string;
  aiReason?: string;
  aiConfidence?: number;
  draftEmailBody?: string;
  status: NegotiationStatus;
  maxRounds?: number;
  currentRound?: number;
  finalAgreedPrice?: number;
  rounds: NegotiationRound[];
  createdAt: string;
}

export interface PurchaseOrder {
  id: number;
  poNumber: string;
  vendor: Vendor;
  status: string;
  totalAmount: number;
  negotiatedSavings?: number;
  currency: string;
  createdAt: string;
}

export interface DashboardData {
  totalWorkflows: number;
  completedWorkflows: number;
  totalVendors: number;
  totalQuotes: number;
  totalSpend?: number;
  totalSavings?: number;
  averageSavingsPercent?: number;
  pendingApprovals?: number;
  purchaseOrdersGenerated?: number;
  recentWorkflows?: WorkflowExecution[];
  spendByCategory?: Array<{ category: string; amount: number }>;
}

// Market Intelligence types
export interface MarketIntelligence {
  category: string;
  minPrice: number;
  maxPrice: number;
  medianPrice: number;
  supplyChainRisk: 'LOW' | 'MEDIUM' | 'HIGH';
  trends: string[];
  supplyChainRisks: string[];
  regulatoryChanges: string[];
  competitorInsights: string[];
}

// Vendor Intelligence types
export interface VendorIntelligenceBundle {
  quoteId: number;
  vendorName: string;
  rfpCompliance: {
    score: number;
    complianceLevel: string;
    metRequirements: string[];
    missingRequirements: string[];
    summary: string;
  };
  reputation: {
    score: number;
    reputationLevel: string;
    reliabilityScore: number;
    trustSealVerified: boolean;
    yearsExperience: number;
    notes: string;
  };
  marketIntelligence: {
    marketRisk: string;
    pricePositioning: string;
    marketMedianPrice: number;
    savingsVsMarket: number;
    industryTrends: string[];
    supplyChainRisks: string[];
    summary: string;
  };
  legalCompliance: {
    overallRisk: string;
    criteria: Array<{ name: string; status: string; detail: string }>;
    summary: string;
  };
  negotiationStrategy: {
    approach: string;
    leveragePoints: string;
    riskMitigation: string;
    suggestedTargetPrice: number;
    walkAwayPrice: number;
    rationale: string;
  };
  overallScore: number;
  overallRecommendation: string;
}

// Evaluation Report types
export interface VendorScorecardEntry {
  quoteId: number;
  vendorName: string;
  unitPrice: number;
  totalCost: number;
  warrantyMonths: number;
  deliveryDays: number;
  paymentTerms: string;
  benchmarkStatus: string;
  vendorScore?: number;
  recommendation: string;
  intelligenceScore: number;
}

export interface EvaluationReport {
  workflowId: number;
  workflowTitle: string;
  generatedAt: string;
  overallOutcome: string;
  executiveSummary: string;
  totalVendorsEvaluated: number;
  recommendedVendor: string;
  recommendedVendorScore?: number;
  vendorScorecard: VendorScorecardEntry[];
  negotiationSummary?: {
    status: string;
    originalPrice: number;
    finalPrice: number;
    savingsAmount: number;
    savingsPercent: number;
    roundsCompleted: number;
  };
  totalSpend: number;
  estimatedSavings: number;
  savingsPercent: number;
  keyFindings: string[];
  riskFlags: string[];
  poNumber?: string;
  auditEventCount: number;
}

export interface ProgressEvent {
  workflowId: number;
  stage: string;
  label: string;
  progress: number;
  message: string;
  timestamp: string;
}
