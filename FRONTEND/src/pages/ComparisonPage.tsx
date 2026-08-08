import { useState, useEffect } from 'react';
import {
  Scale, RefreshCw, Cpu, Award, Zap, ChevronRight, Handshake
} from 'lucide-react';
import { api } from '../api/client';
import type { ComparisonResult, WorkflowExecution } from '../types';
import { useToast } from '../components/Toast';
import { Link, useNavigate } from 'react-router-dom';

export function ComparisonPage() {
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [workflows, setWorkflows] = useState<WorkflowExecution[]>([]);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<number | null>(null);
  const [comparison, setComparison] = useState<ComparisonResult | null>(null);
  const [comparing, setComparing] = useState(false);
  const [draftingId, setDraftingId] = useState<number | null>(null);

  const handleInitiateNegotiation = async (quoteId: number) => {
    setDraftingId(quoteId);
    try {
      await api.draftNegotiation(quoteId);
      showToast('Negotiation Drafted', 'AI strategy & email drafted', 'success');
      navigate('/negotiation');
    } catch (err: any) {
      showToast('Error', err?.response?.data?.message ?? 'Failed to draft negotiation', 'error');
    } finally {
      setDraftingId(null);
    }
  };

  const loadWorkflows = async () => {
    try {
      const wfs = await api.listWorkflows().catch(() => []);
      setWorkflows(wfs);
      if (wfs.length > 0) {
        setSelectedWorkflowId(wfs[0].id);
        fetchComparison(wfs[0].id);
      }
    } catch (err: any) {
      showToast('Error', 'Failed to load comparison data', 'error');
    }
  };

  const fetchComparison = async (wfId: number) => {
    setComparing(true);
    try {
      const res = await api.compareWorkflow(wfId);
      setComparison(res);
    } catch (err: any) {
      const latest = await api.getLatestComparison().catch(() => null);
      setComparison(latest);
    } finally {
      setComparing(false);
    }
  };

  useEffect(() => { loadWorkflows(); }, []);

  const handleWorkflowChange = (id: number) => {
    setSelectedWorkflowId(id);
    fetchComparison(id);
  };

  const topQuote = comparison?.recommended;

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-[#3E52FF] uppercase tracking-wider mb-1">
            <Cpu className="w-3.5 h-3.5" /> AI Decision Intelligence Engine
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Vendor Quote Comparison</h1>
          <p className="text-sm text-[#8F8FA2]">Multi-criteria scoring matrix, benchmark analysis, and AI vendor recommendations</p>
        </div>

        <div className="flex items-center gap-3">
          {workflows.length > 0 && (
            <select
              value={selectedWorkflowId ?? ''}
              onChange={(e) => handleWorkflowChange(Number(e.target.value))}
              className="bg-[#12151C] border border-[#1E2330] rounded-xl px-3.5 py-2 text-sm text-white focus:outline-none focus:border-[#3E52FF]"
            >
              {workflows.map((wf) => (
                <option key={wf.id} value={wf.id}>
                  {wf.title} (#{wf.id})
                </option>
              ))}
            </select>
          )}
          <button
            onClick={() => selectedWorkflowId && fetchComparison(selectedWorkflowId)}
            disabled={comparing}
            className="flex items-center gap-2 px-3.5 py-2 bg-[#12151C] border border-[#1E2330] rounded-xl text-sm font-medium text-[#E0E3E5] hover:text-white hover:border-[#3E52FF]/50 transition-all"
          >
            <RefreshCw className={`w-4 h-4 ${comparing ? 'animate-spin' : ''}`} />
            Re-evaluate
          </button>
        </div>
      </div>

      {comparing ? (
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-12 text-center text-[#8F8FA2]">
          <RefreshCw className="w-8 h-8 text-[#3E52FF] mx-auto mb-3 animate-spin" />
          <p className="text-sm font-medium text-white">Running AI Multi-criteria Scoring & Benchmark Analysis...</p>
        </div>
      ) : !comparison || comparison.rankedQuotes.length === 0 ? (
        <div className="bg-[#12151C] border border-dashed border-[#1E2330] rounded-2xl p-12 text-center">
          <Scale className="w-12 h-12 text-[#8F8FA2] mx-auto mb-3 opacity-30" />
          <p className="text-base font-semibold text-white">No comparison data available for this workflow</p>
          <p className="text-xs text-[#8F8FA2] mt-1 mb-4">Ingest at least 2 vendor quotes or run the automated demo workflow</p>
          <Link
            to="/demo"
            className="inline-flex items-center gap-2 px-4 py-2 bg-[#3E52FF] text-white text-sm font-medium rounded-xl shadow-lg shadow-blue-500/20"
          >
            <Zap className="w-4 h-4" /> Run Automated Demo Workflow
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {/* AI Recommendation Banner */}
          {topQuote && (
            <div className="bg-gradient-to-r from-[#12151C] via-[#161B28] to-[#12151C] border border-[#3E52FF]/40 rounded-2xl p-6 shadow-2xl relative overflow-hidden">
              <div className="absolute top-0 right-0 w-64 h-64 bg-[#3E52FF]/10 rounded-full blur-3xl -z-0 pointer-events-none" />

              <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
                <div className="space-y-2 max-w-3xl">
                  <div className="inline-flex items-center gap-2 px-3 py-1 bg-[#3E52FF]/20 text-[#BDC2FF] border border-[#3E52FF]/30 rounded-full text-xs font-semibold uppercase tracking-wider">
                    <Award className="w-3.5 h-3.5 text-[#3E52FF]" /> AI Selected Top Vendor
                  </div>
                  <h2 className="text-xl font-bold text-white tracking-tight">
                    {topQuote.vendor.name} — Recommended Choice
                  </h2>
                  <p className="text-sm text-[#E0E3E5] leading-relaxed">
                    {comparison.aiExplanation ??
                      `Recommended vendor ${topQuote.vendor.name} based on lowest verified actual total cost of ₹${Number(topQuote.calculatedTotal).toLocaleString('en-IN')} combined with acceptable warranty and delivery terms.`}
                  </p>
                </div>

                <div className="flex flex-col items-end gap-3 flex-shrink-0">
                  <div className="text-right">
                    <div className="text-xs text-[#8F8FA2] uppercase font-mono">Calculated Total</div>
                    <div className="text-2xl font-bold font-mono text-emerald-400">
                      ₹{Number(topQuote.calculatedTotal).toLocaleString('en-IN')}
                    </div>
                  </div>

                  <button
                    onClick={() => handleInitiateNegotiation(topQuote.id)}
                    disabled={draftingId !== null}
                    className="inline-flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-[#3E52FF] to-indigo-600 text-white font-medium text-sm rounded-xl shadow-lg shadow-blue-500/25 hover:opacity-95 transition-all disabled:opacity-50"
                  >
                    {draftingId === topQuote.id ? 'Drafting...' : 'Proceed to Negotiation'} <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Multi-criteria Vendor Scoring Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {comparison.rankedQuotes.map((quote, idx) => {
              const isRecommended = quote.id === topQuote?.id;
              return (
                <div
                  key={quote.id}
                  className={`bg-[#12151C] border rounded-2xl p-6 space-y-4 transition-all duration-300 relative hover:scale-[1.01] hover:border-[#3E52FF] hover:ring-2 hover:ring-[#3E52FF]/20 hover:shadow-xl hover:shadow-blue-950/30 ${
                    isRecommended
                      ? 'border-[#3E52FF]/40 shadow-md shadow-blue-950/10'
                      : 'border-[#1E2330]'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="text-xs font-mono text-[#8F8FA2]">Rank #{idx + 1}</div>
                      <h3 className="text-lg font-bold text-white mt-0.5">{quote.vendor.name}</h3>
                      <div className="text-xs text-[#8F8FA2] font-mono mt-0.5">{quote.sourceFileName ?? 'Uploaded Quote'}</div>
                    </div>
                    {quote.vendorScore != null && (
                      <div className="text-right">
                        <div className="text-2xl font-bold font-mono text-white">{quote.vendorScore.toFixed(1)}</div>
                        <div className="text-[10px] text-[#8F8FA2] uppercase tracking-wider font-mono">Score / 100</div>
                      </div>
                    )}
                  </div>

                  <div className="space-y-2 pt-2 border-t border-[#1E2330]">
                    <div className="flex justify-between text-xs">
                      <span className="text-[#8F8FA2]">Calculated Total</span>
                      <span className="font-mono font-semibold text-white">₹{Number(quote.calculatedTotal).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-[#8F8FA2]">Warranty</span>
                      <span className="text-white font-medium">{quote.warrantyMonths ? `${quote.warrantyMonths} months` : '—'}</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-[#8F8FA2]">Delivery Time</span>
                      <span className="text-white font-medium">{quote.deliveryDays ? `${quote.deliveryDays} days` : '—'}</span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-[#8F8FA2]">Payment Terms</span>
                      <span className="text-white font-medium">{quote.paymentTerms ?? 'Net 30'}</span>
                    </div>
                  </div>

                  <div className="pt-2 flex items-center justify-between">
                    <span className={`text-xs px-2.5 py-0.5 rounded-full border font-medium ${
                      quote.benchmarkStatus === 'BELOW'
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                        : quote.benchmarkStatus === 'WITHIN'
                        ? 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
                        : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                    }`}>
                      {quote.benchmarkStatus === 'BELOW' ? '↓ Below Market' : quote.benchmarkStatus === 'WITHIN' ? '● At Market' : '↑ Above Market'}
                    </span>

                    <span className="text-xs text-[#8F8FA2] font-mono">
                      Conf: {Math.round((quote.extractionConfidence ?? 0.95) * 100)}%
                    </span>
                  </div>

                  <button
                    onClick={() => handleInitiateNegotiation(quote.id)}
                    disabled={draftingId !== null}
                    className="w-full mt-2 py-2 bg-[#3E52FF]/10 hover:bg-[#3E52FF] text-[#BDC2FF] hover:text-white disabled:opacity-50 border border-[#3E52FF]/30 rounded-xl text-xs font-semibold transition-all flex items-center justify-center gap-1.5"
                  >
                    {draftingId === quote.id ? (
                      <span>Drafting...</span>
                    ) : (
                      <>
                        <Handshake className="w-3.5 h-3.5" /> Select & Negotiate
                      </>
                    )}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
