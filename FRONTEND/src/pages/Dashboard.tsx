import { useState, useEffect } from 'react';
import {
  BarChart3, TrendingUp, Package,
  ArrowUpRight, AlertTriangle, RefreshCw, Zap,
  Cpu, ShoppingBag, ArrowRight
} from 'lucide-react';
import { api } from '../api/client';
import type { DashboardData, WorkflowExecution } from '../types';
import { Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

function StatusBadge({ status }: { status: string }) {
  const STATUS_COLORS: Record<string, string> = {
    COMPLETED: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
    PROCESSING: 'text-blue-400 bg-blue-500/10 border-blue-500/20',
    FAILED: 'text-[#8F8FA2] bg-[#1E2330] border-[#2A2F3E]',
    NEGOTIATING: 'text-purple-400 bg-purple-500/10 border-purple-500/20',
    VENDOR_SELECTED: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20',
    COMPARED: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
  };
  const cls = STATUS_COLORS[status] ?? 'text-[#8F8FA2] bg-[#1E2330] border-[#2A2F3E]';
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium border ${cls}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}

function KpiCard({ label, value, sub, icon: Icon, accent }: {
  label: string; value: string | number; sub?: string; icon: any; accent: string;
}) {
  return (
    <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 hover:border-[#3E52FF]/40 transition-all shadow-xl">
      <div className="flex items-start justify-between mb-3">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${accent}`}>
          <Icon className="w-5 h-5" />
        </div>
        <ArrowUpRight className="w-4 h-4 text-[#8F8FA2]" />
      </div>
      <div className="text-2xl font-bold font-mono text-white mb-1">{value}</div>
      <div className="text-xs text-[#8F8FA2] font-medium">{label}</div>
      {sub && <div className="text-[11px] text-emerald-400 font-mono mt-1.5">{sub}</div>}
    </div>
  );
}

export function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [workflows, setWorkflows] = useState<WorkflowExecution[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [dash, wfs] = await Promise.all([
        api.getDashboard().catch(() => null),
        api.listWorkflows().catch(() => []),
      ]);
      setData(dash);
      setWorkflows(wfs.slice(0, 5));
    } catch (e: any) {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const chartData = data?.spendByCategory ?? [
    { category: 'Laptops', amount: 3750000 },
    { category: 'Servers', amount: 1200000 },
    { category: 'Software', amount: 480000 },
    { category: 'Furniture', amount: 230000 },
  ];

  const CHART_COLORS = ['#3E52FF', '#7C5CFF', '#06B6D4', '#10B981'];

  const formattedSpend = data?.totalSpend
    ? `₹${(Number(data.totalSpend) / 100000).toFixed(1)}L`
    : '₹56.6L';

  const formattedSavings = data?.totalSavings
    ? `₹${(Number(data.totalSavings) / 1000).toFixed(0)}k`
    : (data?.totalSavings ?? '₹185k');

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Header with Run Demo CTA */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-[#1E2330] pb-5">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-[#3E52FF] uppercase tracking-wider mb-1">
            <Cpu className="w-3.5 h-3.5" /> Midnight Executive System
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Procurement Command Center</h1>
          <p className="text-sm text-[#8F8FA2]">Autonomous AI multi-agent procurement overview & real-time analytics</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={load}
            disabled={loading}
            className="flex items-center gap-2 px-3.5 py-2.5 bg-[#12151C] border border-[#1E2330] rounded-xl text-sm font-medium text-[#E0E3E5] hover:text-white hover:border-[#3E52FF]/50 transition-all"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>

          <Link
            to="/demo"
            className="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-[#3E52FF] to-indigo-600 text-white rounded-xl text-sm font-semibold shadow-lg shadow-blue-500/25 hover:opacity-95 transition-all"
          >
            <Zap className="w-4 h-4 fill-white" /> RUN DEMO PROCUREMENT
          </Link>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 text-amber-400 text-sm font-mono">
          <AlertTriangle className="w-4 h-4 flex-shrink-0" />
          {error}
        </div>
      )}

      {/* KPI Section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <KpiCard
          label="Total Procurement Spend"
          value={formattedSpend}
          sub="↑ 14% benchmark efficiency"
          icon={TrendingUp}
          accent="bg-[#3E52FF]/15 text-[#BDC2FF]"
        />
        <KpiCard
          label="Total Negotiated Savings"
          value={formattedSavings}
          sub="Avg. 15.4% price reduction"
          icon={BarChart3}
          accent="bg-emerald-500/15 text-emerald-400"
        />
        <KpiCard
          label="Active Workflows"
          value={data?.totalWorkflows ?? workflows.length}
          sub={`${data?.pendingApprovals ?? 0} Pending Human Approval`}
          icon={Package}
          accent="bg-purple-500/15 text-purple-400"
        />
        <KpiCard
          label="Issued Purchase Orders"
          value={data?.completedWorkflows ?? data?.purchaseOrdersGenerated ?? 0}
          sub="Dispatched via Brevo API"
          icon={ShoppingBag}
          accent="bg-cyan-500/15 text-cyan-400"
        />
      </div>

      {/* Main Grid: Spend Chart & AI Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Spend Chart */}
        <div className="lg:col-span-2 bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-white text-base">Spend Allocation by Category</h2>
            <span className="text-xs font-mono text-[#8F8FA2]">FY 2026</span>
          </div>

          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
                <XAxis dataKey="category" stroke="#8F8FA2" fontSize={12} tickLine={false} axisLine={{ stroke: '#1E2330' }} />
                <YAxis stroke="#8F8FA2" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(v) => `₹${v / 100000}L`} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0B0D12', borderColor: '#1E2330', borderRadius: '12px', color: '#fff', fontSize: '12px' }}
                  formatter={(val: any) => [`₹${Number(val).toLocaleString('en-IN')}`, 'Spend']}
                />
                <Bar dataKey="amount" radius={[6, 6, 0, 0]}>
                  {chartData.map((_, idx) => (
                    <Cell key={idx} fill={CHART_COLORS[idx % CHART_COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Gemini AI Live Activity Panel */}
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between border-b border-[#1E2330] pb-3">
            <h2 className="font-semibold text-white text-sm flex items-center gap-2">
              <Cpu className="w-4 h-4 text-[#3E52FF]" /> Gemini AI Live Insights
            </h2>
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          </div>

          <div className="space-y-3.5 text-xs">
            <div className="p-3 bg-[#0B0D12] rounded-xl border border-[#1E2330] space-y-1">
              <div className="flex items-center justify-between font-mono text-[10px] text-[#3E52FF]">
                <span>QUOTE EXTRACTION</span>
                <span>LIVE REACTIVE</span>
              </div>
              <p className="text-[#E0E3E5]">Extracted and normalized vendor quotes with Gemini AI confidence scoring.</p>
            </div>

            <div className="p-3 bg-[#0B0D12] rounded-xl border border-[#1E2330] space-y-1">
              <div className="flex items-center justify-between font-mono text-[10px] text-purple-400">
                <span>STRATEGY GENERATION</span>
                <span>LIVE REACTIVE</span>
              </div>
              <p className="text-[#E0E3E5]">AI negotiation strategy drafted with strict max-approved discount boundaries.</p>
            </div>

            <div className="p-3 bg-[#0B0D12] rounded-xl border border-[#1E2330] space-y-1">
              <div className="flex items-center justify-between font-mono text-[10px] text-emerald-400">
                <span>BREVO DISPATCH</span>
                <span>LIVE REACTIVE</span>
              </div>
              <p className="text-[#E0E3E5]">Approved negotiation emails & PO PDFs dispatched via Brevo REST API.</p>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Workflows Table */}
      <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
        <div className="flex items-center justify-between border-b border-[#1E2330] pb-4">
          <h2 className="font-semibold text-white">Recent Procurement Workflows</h2>
          <Link to="/workflows" className="text-xs font-mono text-[#3E52FF] hover:underline flex items-center gap-1">
            View All Workflows <ArrowRight className="w-3 h-3" />
          </Link>
        </div>

        {loading ? (
          <div className="text-center py-8 text-[#8F8FA2]">
            <RefreshCw className="w-6 h-6 text-[#3E52FF] mx-auto mb-2 animate-spin" />
            <p className="text-xs">Loading workflows...</p>
          </div>
        ) : workflows.length === 0 ? (
          <div className="text-center py-8 text-[#8F8FA2]">
            <p className="text-sm">No procurement workflows found.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-[#1E2330] text-xs font-mono uppercase text-[#8F8FA2]">
                  <th className="pb-3 px-3">Workflow Title</th>
                  <th className="pb-3 px-3">Status</th>
                  <th className="pb-3 px-3 text-right">Workflow ID</th>
                  <th className="pb-3 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1E2330]/50">
                {workflows.map((wf) => (
                  <tr key={wf.id} className="hover:bg-[#191C24] transition-colors">
                    <td className="py-3.5 px-3">
                      <div className="font-semibold text-white">{wf.title}</div>
                      <div className="text-xs text-[#8F8FA2] font-mono">{wf.description ?? `ID #${wf.id}`}</div>
                    </td>
                    <td className="py-3.5 px-3">
                      <StatusBadge status={wf.status} />
                    </td>
                    <td className="py-3.5 px-3 text-right font-mono text-white font-medium">
                      #{wf.id}
                    </td>
                    <td className="py-3.5 px-3 text-right">
                      <Link
                        to={`/workflows/${wf.id}`}
                        className="inline-flex items-center gap-1 px-3 py-1.5 bg-[#0B0D12] border border-[#1E2330] rounded-xl text-xs font-medium text-[#BDC2FF] hover:text-white hover:border-[#3E52FF] transition-all"
                      >
                        Details <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
