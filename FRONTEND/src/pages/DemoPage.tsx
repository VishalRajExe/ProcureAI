import { useState, useEffect, useRef } from 'react';
import {
  Rocket, CheckCircle2, Clock, AlertCircle, RefreshCw,
  ExternalLink, Terminal, Zap, FileText, ArrowRight, Layers
} from 'lucide-react';
import { api } from '../api/client';
import { Link } from 'react-router-dom';

interface ProgressStep {
  label: string;
  status: 'pending' | 'running' | 'done' | 'error';
  detail?: string;
}

const DEMO_STEPS: ProgressStep[] = [
  { label: 'Create Procurement Workflow', status: 'pending' },
  { label: 'Ingest 3 Vendor Quotes (Dell, HP, Lenovo)', status: 'pending' },
  { label: 'AI Extraction & Normalization', status: 'pending' },
  { label: 'Market Benchmarking', status: 'pending' },
  { label: 'Multi-criteria Vendor Comparison', status: 'pending' },
  { label: 'AI Scoring & Recommendation', status: 'pending' },
  { label: 'Draft Negotiation Strategy + Email', status: 'pending' },
  { label: 'Human Approval (Auto-approved for demo)', status: 'pending' },
  { label: 'Send Negotiation Email to Vendor', status: 'pending' },
  { label: 'Simulate Vendor Counter-offer', status: 'pending' },
  { label: 'AI Re-evaluation of Counter Price', status: 'pending' },
  { label: 'Select Best Vendor', status: 'pending' },
  { label: 'Generate Purchase Order (PDF)', status: 'pending' },
];

export function DemoPage() {
  const [steps, setSteps] = useState<ProgressStep[]>(DEMO_STEPS);
  const [running, setRunning] = useState(false);
  const [done, setDone] = useState(false);
  const [result, setResult] = useState<Record<string, any> | null>(null);
  const [error, setError] = useState('');
  const [selectedVendor, setSelectedVendor] = useState<'LENOVO' | 'HP' | 'DELL'>('LENOVO');
  const timerRef = useRef<number | null>(null);

  const runStep = (idx: number, detail?: string) => {
    setSteps(prev => prev.map((s, i) =>
      i === idx ? { ...s, status: 'running', detail } :
      i < idx ? { ...s, status: 'done' } : s
    ));
  };

  const handleRun = async () => {
    setRunning(true);
    setDone(false);
    setError('');
    setResult(null);
    setSteps(DEMO_STEPS.map(s => ({ ...s, status: 'pending', detail: undefined })));

    const delays = [300, 400, 600, 400, 500, 400, 600, 300, 400, 300, 400, 300, 500];
    let stepIdx = 0;
    const tick = () => {
      if (stepIdx < DEMO_STEPS.length) {
        runStep(stepIdx);
        stepIdx++;
        timerRef.current = window.setTimeout(tick, delays[stepIdx - 1] ?? 400);
      }
    };
    tick();

    try {
      const data = await api.runDemo(selectedVendor);
      if (timerRef.current) clearTimeout(timerRef.current);
      setSteps(DEMO_STEPS.map((s, i) => ({
        ...s,
        status: 'done' as const,
        detail: i === 5 ? `Recommended: ${data.recommendedVendor}` :
                i === 12 ? `PO: ${data.poNumber}` : undefined,
      })));
      setResult(data);
      setDone(true);
    } catch (e: any) {
      if (timerRef.current) clearTimeout(timerRef.current);
      setError(e?.response?.data?.message ?? e?.message ?? 'Demo run failed');
      setSteps(prev => prev.map(s =>
        s.status === 'running' ? { ...s, status: 'error' } : s
      ));
    } finally {
      setRunning(false);
    }
  };

  useEffect(() => () => { if (timerRef.current) clearTimeout(timerRef.current); }, []);

  const StepIcon = ({ status }: { status: ProgressStep['status'] }) => {
    if (status === 'done') return <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0" />;
    if (status === 'running') return <RefreshCw className="w-4 h-4 text-[#3E52FF] flex-shrink-0 animate-spin" />;
    if (status === 'error') return <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />;
    return <Clock className="w-4 h-4 text-[#8F8FA2] flex-shrink-0" />;
  };

  return (
    <div className="p-6 space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 bg-[#3E52FF]/10 border border-[#3E52FF]/20 rounded-full text-xs text-[#BDC2FF] font-semibold mb-3">
          <Zap className="w-3.5 h-3.5 text-[#3E52FF]" />
          Full Procurement Lifecycle Demo & Scenario Engine
        </div>
        <h1 className="text-2xl font-bold text-white tracking-tight mb-1">Run Demo Procurement</h1>
        <p className="text-sm text-[#8F8FA2] max-w-2xl">
          Executes the complete AI procurement workflow end-to-end. Choose a winning vendor scenario (Lenovo, HP, or Dell) to test dynamic AI scoring, negotiation, and PO generation.
        </p>
      </div>

      {/* Scenario Selector */}
      <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-sm font-semibold text-white flex items-center gap-2">
            <Layers className="w-4 h-4 text-[#3E52FF]" /> Select Preferred Target Vendor Scenario
          </span>
          <span className="text-xs font-mono text-[#BDC2FF] bg-[#3E52FF]/20 px-2.5 py-0.5 rounded-full border border-[#3E52FF]/30">
            Active: {selectedVendor} Preferred
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {[
            { id: 'LENOVO', name: 'Lenovo Corporate Sales', price: '₹58,000/unit', badge: 'Top Recommended (Lenovo Wins!)' },
            { id: 'HP', name: 'HP Business Solutions', price: '₹63,500/unit', badge: 'Standard Winner (HP Wins!)' },
            { id: 'DELL', name: 'Dell Direct Enterprise', price: '₹55,000/unit', badge: 'Budget Winner (Dell Wins!)' },
          ].map((sc) => {
            const active = selectedVendor === sc.id;
            return (
              <button
                key={sc.id}
                type="button"
                onClick={() => setSelectedVendor(sc.id as any)}
                className={`p-4 rounded-xl border text-left transition-all relative ${
                  active
                    ? 'bg-[#191C26] border-[#3E52FF] ring-2 ring-[#3E52FF]/20 shadow-lg'
                    : 'bg-[#0B0D12] border-[#1E2330] hover:border-[#3E52FF]/40'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-bold text-white text-sm">{sc.name}</span>
                  {active && <CheckCircle2 className="w-4 h-4 text-[#3E52FF]" />}
                </div>
                <div className="text-xs font-mono text-emerald-400 font-semibold mb-2">{sc.price}</div>
                <div className="text-[11px] text-[#8F8FA2] font-mono">{sc.badge}</div>
              </button>
            );
          })}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Steps panel */}
        <div className="lg:col-span-3 bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2 text-sm font-medium text-white">
              <Terminal className="w-4 h-4 text-[#3E52FF]" />
              Workflow Steps Execution
            </div>
            {done && (
              <span className="text-xs px-2.5 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full font-mono">
                Complete
              </span>
            )}
          </div>

          <div className="space-y-2">
            {steps.map((step, i) => (
              <div
                key={i}
                className={`flex items-start gap-3 p-3 rounded-xl transition-all ${
                  step.status === 'running' ? 'bg-[#3E52FF]/10 border border-[#3E52FF]/30' :
                  step.status === 'done' ? 'opacity-80' : 'opacity-40'
                }`}
              >
                <StepIcon status={step.status} />
                <div className="min-w-0">
                  <div className="text-sm text-white">{step.label}</div>
                  {step.detail && (
                    <div className="text-xs text-[#BDC2FF] mt-0.5 font-mono">{step.detail}</div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {error && (
            <div className="mt-4 flex items-start gap-2 bg-red-500/10 border border-red-500/20 rounded-xl p-3 text-sm text-red-400">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <div>
                <div className="font-medium">Demo failed</div>
                <div className="text-xs mt-0.5 opacity-80">{error}</div>
              </div>
            </div>
          )}

          <button
            onClick={handleRun}
            disabled={running}
            className="w-full mt-5 flex items-center justify-center gap-2 bg-gradient-to-r from-[#3E52FF] to-indigo-600 text-white font-semibold py-3 rounded-xl shadow-lg shadow-blue-500/25 hover:opacity-95 transition-all disabled:opacity-50"
          >
            {running ? (
              <><RefreshCw className="w-4 h-4 animate-spin" /> Executing {selectedVendor} Scenario...</>
            ) : done ? (
              <><RefreshCw className="w-4 h-4" /> Run {selectedVendor} Scenario Again</>
            ) : (
              <><Rocket className="w-4 h-4" /> Launch {selectedVendor} Procurement Demo</>
            )}
          </button>
        </div>

        {/* Result panel */}
        <div className="lg:col-span-2 space-y-4">
          {/* Scenario Details */}
          <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 space-y-3">
            <h3 className="text-sm font-semibold text-white">Active Scenario Specs</h3>
            <div className="space-y-2 text-xs text-[#8F8FA2]">
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#3E52FF]" /> 50 Enterprise Laptops required</div>
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#3E52FF]" /> Dell Direct Enterprise — ₹68K/unit</div>
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#3E52FF]" /> HP Business Solutions — ₹63.5K/unit</div>
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#3E52FF]" /> Lenovo Corporate Sales — ₹58K-71K/unit</div>
              <div className="flex items-center gap-2 pt-2 border-t border-[#1E2330]">
                <ArrowRight className="w-3 h-3 text-emerald-400" /> Target Choice: <strong className="text-white ml-1">{selectedVendor}</strong>
              </div>
            </div>
          </div>

          {/* Results */}
          {result && (
            <div className="bg-[#12151C] border border-emerald-500/30 rounded-2xl p-5 space-y-3 shadow-xl">
              <div className="flex items-center gap-2 text-sm font-semibold text-emerald-400">
                <CheckCircle2 className="w-4 h-4" />
                Demo Complete ({selectedVendor} Winner)
              </div>
              <div className="space-y-2">
                {[
                  { label: 'Workflow ID', value: `#${result.workflowId}` },
                  { label: 'Recommended Vendor', value: result.recommendedVendor as string },
                  { label: 'Negotiation Status', value: String(result.negotiationStatus) },
                  { label: 'Final Price', value: result.finalAgreedPrice ? `₹${Number(result.finalAgreedPrice).toLocaleString('en-IN')}` : '—' },
                  { label: 'PO Number', value: result.poNumber as string },
                  { label: 'PO Total', value: result.poTotal ? `₹${Number(result.poTotal).toLocaleString('en-IN')}` : '—' },
                ].map(({ label, value }) => (
                  <div key={label} className="flex items-center justify-between">
                    <span className="text-xs text-[#8F8FA2]">{label}</span>
                    <span className="text-xs font-mono text-white font-semibold">{value}</span>
                  </div>
                ))}
              </div>
              <div className="pt-2 border-t border-[#1E2330] space-y-2">
                <Link
                  to="/comparison"
                  className="flex items-center justify-center gap-2 w-full py-2 bg-[#3E52FF]/10 border border-[#3E52FF]/30 text-[#BDC2FF] rounded-xl text-xs font-medium hover:bg-[#3E52FF] hover:text-white transition-all"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                  View Quote Comparison
                </Link>
                <a
                  href={api.getPdfUrl(result.purchaseOrderId)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-2 w-full py-2 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs font-medium hover:bg-emerald-500 hover:text-white transition-all"
                >
                  <FileText className="w-3.5 h-3.5" />
                  Download PO PDF ({result.recommendedVendor})
                </a>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
