import { useState, useEffect, useRef } from 'react';
import {
  Rocket, CheckCircle2, Clock, AlertCircle, RefreshCw,
  ExternalLink, Terminal, Zap, FileText, ArrowRight
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

    // Animate through steps while calling the backend
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
      const data = await api.runDemo();
      // Mark all steps done
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
    if (status === 'running') return <RefreshCw className="w-4 h-4 text-[#4F7CFF] flex-shrink-0 animate-spin" />;
    if (status === 'error') return <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />;
    return <Clock className="w-4 h-4 text-[#4B5563] flex-shrink-0" />;
  };

  return (
    <div className="p-6 space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1 bg-[#4F7CFF]/10 border border-[#4F7CFF]/20 rounded-full text-xs text-[#4F7CFF] mb-3">
          <Zap className="w-3 h-3" />
          Full Procurement Lifecycle Demo
        </div>
        <h1 className="text-xl font-bold text-white mb-1">Run Demo Procurement</h1>
        <p className="text-sm text-[#6B7280] max-w-2xl">
          Executes the complete AI procurement workflow end-to-end: 3 vendor quotes (50 laptops) →
          AI extraction → comparison → negotiation → human approval → PO generation. No external
          credentials required.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Steps panel */}
        <div className="lg:col-span-3 bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2 text-sm font-medium text-white">
              <Terminal className="w-4 h-4 text-[#4F7CFF]" />
              Workflow Steps
            </div>
            {done && (
              <span className="text-xs px-2 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full">
                Complete
              </span>
            )}
          </div>

          <div className="space-y-2">
            {steps.map((step, i) => (
              <div
                key={i}
                className={`flex items-start gap-3 p-3 rounded-xl transition-all ${
                  step.status === 'running' ? 'bg-[#4F7CFF]/5 border border-[#4F7CFF]/20' :
                  step.status === 'done' ? 'opacity-70' : 'opacity-40'
                }`}
              >
                <StepIcon status={step.status} />
                <div className="min-w-0">
                  <div className="text-sm text-white">{step.label}</div>
                  {step.detail && (
                    <div className="text-xs text-[#4F7CFF] mt-0.5">{step.detail}</div>
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
                <div className="text-xs mt-1 text-[#9AA1AE]">Make sure the backend is running: <code className="bg-[#0B0D12] px-1 rounded">mvn spring-boot:run</code></div>
              </div>
            </div>
          )}

          <button
            onClick={handleRun}
            disabled={running}
            className="w-full mt-5 flex items-center justify-center gap-2 bg-gradient-to-r from-[#4F7CFF] to-purple-600 text-white font-semibold py-3 rounded-xl hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {running ? (
              <><RefreshCw className="w-4 h-4 animate-spin" /> Running demo...</>
            ) : done ? (
              <><RefreshCw className="w-4 h-4" /> Run Again</>
            ) : (
              <><Rocket className="w-4 h-4" /> Launch Demo Procurement</>
            )}
          </button>
        </div>

        {/* Result panel */}
        <div className="lg:col-span-2 space-y-4">
          {/* Scenario */}
          <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 space-y-3">
            <h3 className="text-sm font-semibold text-white">Demo Scenario</h3>
            <div className="space-y-2 text-xs text-[#9AA1AE]">
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#4F7CFF]" />50 Business Laptops required</div>
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#4F7CFF]" />Dell Direct Enterprise — ₹68K/unit</div>
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#4F7CFF]" />HP Business Solutions — ₹63.5K/unit</div>
              <div className="flex items-center gap-2"><ArrowRight className="w-3 h-3 text-[#4F7CFF]" />Lenovo Corporate Sales — ₹71K/unit</div>
              <div className="flex items-center gap-2 pt-1 border-t border-[#1E2330]">
                <ArrowRight className="w-3 h-3 text-purple-400" />AI negotiates 6% discount on winner
              </div>
            </div>
          </div>

          {/* Results */}
          {result && (
            <div className="bg-[#12151C] border border-emerald-500/20 rounded-2xl p-5 space-y-3">
              <div className="flex items-center gap-2 text-sm font-semibold text-emerald-400">
                <CheckCircle2 className="w-4 h-4" />
                Demo Complete!
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
                    <span className="text-xs text-[#6B7280]">{label}</span>
                    <span className="text-xs font-mono text-white">{value}</span>
                  </div>
                ))}
              </div>
              <div className="pt-2 border-t border-[#1E2330] space-y-2">
                <Link
                  to={`/workflows/${result.workflowId}`}
                  className="flex items-center justify-center gap-2 w-full py-2 bg-[#4F7CFF]/10 border border-[#4F7CFF]/20 text-[#4F7CFF] rounded-xl text-xs font-medium hover:bg-[#4F7CFF]/20 transition-colors"
                >
                  <ExternalLink className="w-3 h-3" />
                  View Workflow Detail
                </Link>
                <a
                  href={`http://localhost:8080/api/purchase-orders/${result.purchaseOrderId}/pdf`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-2 w-full py-2 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs font-medium hover:bg-emerald-500/20 transition-colors"
                >
                  <FileText className="w-3 h-3" />
                  Download PO PDF
                </a>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
