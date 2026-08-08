import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft, RefreshCw, FileText, Package,
  CheckCircle2, AlertTriangle, Star,
  Handshake, BarChart3
} from 'lucide-react';
import { api } from '../api/client';
import type { Quote, ComparisonResult, Negotiation, EvaluationReport } from '../types';

type Tab = 'quotes' | 'comparison' | 'negotiation' | 'report';

function BenchmarkBadge({ status }: { status: string }) {
  if (status === 'BELOW') return <span className="px-1.5 py-0.5 rounded text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">↓ Below Market</span>;
  if (status === 'WITHIN') return <span className="px-1.5 py-0.5 rounded text-xs bg-yellow-500/10 text-yellow-400 border border-yellow-500/20">● At Market</span>;
  if (status === 'ABOVE') return <span className="px-1.5 py-0.5 rounded text-xs bg-red-500/10 text-red-400 border border-red-500/20">↑ Above Market</span>;
  return <span className="px-1.5 py-0.5 rounded text-xs bg-[#1E2330] text-[#6B7280]">Unknown</span>;
}

function QuoteCard({ quote, rank }: { quote: Quote; rank: number }) {
  const isTop = rank === 1;
  return (
    <div className={`bg-[#12151C] border rounded-2xl p-5 space-y-4 ${isTop ? 'border-[#4F7CFF]/40 ring-1 ring-[#4F7CFF]/20' : 'border-[#1E2330]'}`}>
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-2">
            {isTop && <span className="px-2 py-0.5 bg-[#4F7CFF]/10 text-[#4F7CFF] border border-[#4F7CFF]/20 rounded-full text-xs font-medium">⭐ Recommended</span>}
          </div>
          <h3 className="font-semibold text-white mt-1">{quote.vendor.name}</h3>
          <div className="text-xs text-[#6B7280] mt-0.5">{quote.sourceFileName ?? 'Submitted via API'}</div>
        </div>
        <div className="text-right">
          {quote.vendorScore != null && (
            <div className="text-xl font-bold text-white">{quote.vendorScore.toFixed(1)}</div>
          )}
          <div className="text-xs text-[#6B7280]">Score</div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        {[
          { label: 'Total (incl. tax)', value: `₹${Number(quote.calculatedTotal).toLocaleString('en-IN')}` },
          { label: 'Warranty', value: quote.warrantyMonths ? `${quote.warrantyMonths} months` : '—' },
          { label: 'Delivery', value: quote.deliveryDays ? `${quote.deliveryDays} days` : '—' },
          { label: 'Payment Terms', value: quote.paymentTerms ?? '—' },
        ].map(({ label, value }) => (
          <div key={label} className="bg-[#0B0D12] rounded-xl p-2.5">
            <div className="text-xs text-[#6B7280] mb-0.5">{label}</div>
            <div className="text-sm font-medium text-white">{value}</div>
          </div>
        ))}
      </div>

      <div className="flex items-center justify-between">
        <BenchmarkBadge status={quote.benchmarkStatus} />
        <span className={`text-xs px-2 py-0.5 rounded-full border ${
          quote.extractionStatus === 'VALIDATED'
            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
            : 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
        }`}>
          {quote.extractionStatus}
        </span>
      </div>
    </div>
  );
}

function NegotiationView({ negotiations }: { negotiations: Negotiation[] }) {
  if (negotiations.length === 0) {
    return (
      <div className="text-center py-10 text-[#6B7280]">
        <Handshake className="w-10 h-10 mx-auto mb-3 opacity-20" />
        <p>No negotiations started yet for this workflow.</p>
      </div>
    );
  }
  const neg = negotiations[negotiations.length - 1];
  const statusColors: Record<string, string> = {
    ACCEPTED: 'text-emerald-400 bg-emerald-500/10',
    FAILED: 'text-red-400 bg-red-500/10',
    PENDING_APPROVAL: 'text-yellow-400 bg-yellow-500/10',
    SENT: 'text-blue-400 bg-blue-500/10',
    DRAFTED: 'text-purple-400 bg-purple-500/10',
  };
  const sc = statusColors[neg.status] ?? 'text-[#9AA1AE] bg-[#1E2330]';

  return (
    <div className="space-y-4">
      <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-white">Negotiation #{neg.id}</h3>
          <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${sc}`}>{neg.status.replace(/_/g, ' ')}</span>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { label: 'Current Price', value: `₹${Number(neg.currentPrice).toLocaleString('en-IN')}` },
            { label: 'Target Price', value: `₹${Number(neg.targetPrice).toLocaleString('en-IN')}` },
            { label: 'Max Approved', value: `₹${Number(neg.maxApprovedPrice).toLocaleString('en-IN')}` },
            { label: 'Final Agreed', value: neg.finalAgreedPrice ? `₹${Number(neg.finalAgreedPrice).toLocaleString('en-IN')}` : '—' },
          ].map(({ label, value }) => (
            <div key={label} className="bg-[#0B0D12] rounded-xl p-3">
              <div className="text-xs text-[#6B7280] mb-1">{label}</div>
              <div className="text-sm font-semibold text-white">{value}</div>
            </div>
          ))}
        </div>
        {neg.aiStrategy && (
          <div className="mt-4 p-3 bg-[#4F7CFF]/5 border border-[#4F7CFF]/15 rounded-xl">
            <div className="text-xs font-medium text-[#4F7CFF] mb-1">AI Strategy</div>
            <p className="text-xs text-[#9AA1AE]">{neg.aiStrategy}</p>
          </div>
        )}
        {neg.draftEmailBody && (
          <div className="mt-3 p-3 bg-[#0B0D12] border border-[#1E2330] rounded-xl">
            <div className="text-xs font-medium text-[#9AA1AE] mb-1">Draft Email</div>
            <pre className="text-xs text-[#6B7280] whitespace-pre-wrap font-sans">{neg.draftEmailBody}</pre>
          </div>
        )}
      </div>

      {neg.rounds?.length > 0 && (
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
          <h3 className="font-semibold text-white mb-3 text-sm">Negotiation Rounds</h3>
          <div className="space-y-2">
            {neg.rounds.map((r) => (
              <div key={r.id} className="flex items-center justify-between p-3 bg-[#0B0D12] rounded-xl">
                <div className="text-xs text-[#9AA1AE]">Round {r.roundNumber + 1}</div>
                <div className="text-xs text-white">AI Offer: ₹{Number(r.offeredPriceByAi).toLocaleString('en-IN')}</div>
                {r.vendorCounterPrice && <div className="text-xs text-yellow-400">Counter: ₹{Number(r.vendorCounterPrice).toLocaleString('en-IN')}</div>}
                <span className="text-xs px-2 py-0.5 rounded bg-[#1E2330] text-[#9AA1AE]">{r.outcome ?? '—'}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function ReportView({ report }: { report: EvaluationReport }) {
  return (
    <div className="space-y-5">
      {/* Executive summary */}
      <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
        <div className="flex items-center gap-2 mb-3">
          <BarChart3 className="w-4 h-4 text-[#4F7CFF]" />
          <h3 className="font-semibold text-white text-sm">Executive Summary</h3>
          <span className={`ml-auto px-2 py-0.5 rounded-full text-xs border ${
            report.overallOutcome === 'COMPLETED' ? 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20' : 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20'
          }`}>{report.overallOutcome}</span>
        </div>
        <p className="text-sm text-[#9AA1AE]">{report.executiveSummary}</p>
        <div className="grid grid-cols-3 gap-3 mt-4">
          {[
            { label: 'Total Spend', value: `₹${Number(report.totalSpend).toLocaleString('en-IN')}` },
            { label: 'Est. Savings', value: `₹${Number(report.estimatedSavings).toLocaleString('en-IN')}` },
            { label: 'Savings %', value: `${report.savingsPercent.toFixed(1)}%` },
          ].map(({ label, value }) => (
            <div key={label} className="bg-[#0B0D12] rounded-xl p-3 text-center">
              <div className="text-lg font-bold text-white">{value}</div>
              <div className="text-xs text-[#6B7280]">{label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Vendor scorecard table */}
      <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 overflow-x-auto">
        <h3 className="font-semibold text-white text-sm mb-3">Vendor Scorecard</h3>
        <table className="w-full text-xs">
          <thead>
            <tr className="text-[#6B7280] border-b border-[#1E2330]">
              {['Vendor', 'Unit Price', 'Total Cost', 'Warranty', 'Delivery', 'Score', 'Intel', 'Status'].map(h => (
                <th key={h} className="text-left py-2 pr-4 font-medium">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {report.vendorScorecard.map((v, i) => (
              <tr key={v.quoteId} className={`border-b border-[#1E2330]/50 ${i === 0 ? 'text-white' : 'text-[#9AA1AE]'}`}>
                <td className="py-2.5 pr-4 font-medium">
                  {i === 0 && <span className="text-[#4F7CFF] mr-1">★</span>}
                  {v.vendorName}
                </td>
                <td className="py-2.5 pr-4">₹{Number(v.unitPrice).toLocaleString('en-IN')}</td>
                <td className="py-2.5 pr-4">₹{Number(v.totalCost).toLocaleString('en-IN')}</td>
                <td className="py-2.5 pr-4">{v.warrantyMonths}m</td>
                <td className="py-2.5 pr-4">{v.deliveryDays}d</td>
                <td className="py-2.5 pr-4 font-mono">{v.vendorScore?.toFixed(1) ?? '—'}</td>
                <td className="py-2.5 pr-4 font-mono">{v.intelligenceScore.toFixed(1)}/10</td>
                <td className="py-2.5"><BenchmarkBadge status={v.benchmarkStatus} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Key findings + risk flags */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
          <h3 className="text-sm font-semibold text-white mb-3 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Key Findings
          </h3>
          <ul className="space-y-1.5">
            {report.keyFindings.map((f, i) => (
              <li key={i} className="text-xs text-[#9AA1AE] flex items-start gap-2">
                <span className="text-[#4F7CFF] mt-0.5">•</span> {f}
              </li>
            ))}
          </ul>
        </div>
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
          <h3 className="text-sm font-semibold text-white mb-3 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-yellow-400" /> Risk Flags
          </h3>
          {report.riskFlags.length === 0 ? (
            <p className="text-xs text-emerald-400">✓ No significant risk flags identified</p>
          ) : (
            <ul className="space-y-1.5">
              {report.riskFlags.map((f, i) => (
                <li key={i} className="text-xs text-[#9AA1AE]">{f}</li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

export function WorkflowDetail() {
  const { id } = useParams<{ id: string }>();
  const workflowId = Number(id);

  const [tab, setTab] = useState<Tab>('quotes');
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [comparison, setComparison] = useState<ComparisonResult | null>(null);
  const [negotiations, setNegotiations] = useState<Negotiation[]>([]);
  const [report, setReport] = useState<EvaluationReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [q, comp, negs] = await Promise.all([
        api.listQuotes(workflowId),
        api.compareWorkflow(workflowId).catch(() => null),
        api.listNegotiations().catch(() => [] as Negotiation[]),
      ]);
      setQuotes(q);
      setComparison(comp);
      setNegotiations(negs);
    } catch (e: any) {
      setError(e?.response?.data?.message ?? 'Failed to load workflow data');
    } finally {
      setLoading(false);
    }
  };

  const loadReport = async () => {
    try {
      const r = await api.getWorkflowReport(workflowId);
      setReport(r);
    } catch (e) {
      // report may not be ready
    }
  };

  useEffect(() => { load(); }, [workflowId]);
  useEffect(() => { if (tab === 'report' && !report) loadReport(); }, [tab]);

  const tabs: { key: Tab; label: string; icon: any }[] = [
    { key: 'quotes', label: 'Quotes', icon: Package },
    { key: 'comparison', label: 'Comparison', icon: BarChart3 },
    { key: 'negotiation', label: 'Negotiation', icon: Handshake },
    { key: 'report', label: 'Report', icon: FileText },
  ];

  return (
    <div className="p-6 space-y-5">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Link to="/workflows" className="p-2 rounded-xl bg-[#1E2330] text-[#9AA1AE] hover:text-white transition-colors">
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div>
          <h1 className="text-lg font-bold text-white">Workflow #{workflowId}</h1>
          <p className="text-sm text-[#6B7280]">{quotes.length} quotes · {loading ? 'Loading...' : 'Ready'}</p>
        </div>
        <button onClick={load} disabled={loading} className="ml-auto p-2 bg-[#1E2330] rounded-xl text-[#9AA1AE] hover:text-white transition-colors">
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-3 text-yellow-400 text-sm">
          <AlertTriangle className="w-4 h-4" /> {error}
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-[#1E2330]">
        {tabs.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-all -mb-px ${
              tab === key
                ? 'border-[#4F7CFF] text-[#4F7CFF]'
                : 'border-transparent text-[#6B7280] hover:text-white'
            }`}
          >
            <Icon className="w-3.5 h-3.5" />
            {label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {tab === 'quotes' && (
        <div>
          {quotes.length === 0 ? (
            <div className="text-center py-10 text-[#6B7280]">
              <Package className="w-10 h-10 mx-auto mb-3 opacity-20" />
              <p>No quotes found for this workflow.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {(comparison?.rankedQuotes ?? quotes).map((q, i) => (
                <QuoteCard key={q.id} quote={q} rank={i + 1} />
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'comparison' && (
        <div>
          {!comparison ? (
            <div className="text-center py-10 text-[#6B7280]">
              <BarChart3 className="w-10 h-10 mx-auto mb-3 opacity-20" />
              <p>No comparison available yet. Upload at least 2 quotes.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {comparison.aiExplanation && (
                <div className="bg-[#4F7CFF]/5 border border-[#4F7CFF]/20 rounded-2xl p-5">
                  <div className="flex items-center gap-2 mb-2">
                    <Star className="w-4 h-4 text-[#4F7CFF]" />
                    <span className="text-sm font-medium text-white">AI Recommendation</span>
                  </div>
                  <p className="text-sm text-[#9AA1AE]">{comparison.aiExplanation}</p>
                </div>
              )}
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {comparison.rankedQuotes.map((q, i) => (
                  <QuoteCard key={q.id} quote={q} rank={i + 1} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {tab === 'negotiation' && <NegotiationView negotiations={negotiations} />}

      {tab === 'report' && (
        <div>
          {!report ? (
            <div className="text-center py-10 text-[#6B7280]">
              <FileText className="w-10 h-10 mx-auto mb-3 opacity-20" />
              <p className="mb-2">Evaluation report will appear after workflow completes.</p>
              <button onClick={loadReport} className="text-[#4F7CFF] text-sm hover:underline">Load Report</button>
            </div>
          ) : (
            <ReportView report={report} />
          )}
        </div>
      )}
    </div>
  );
}
