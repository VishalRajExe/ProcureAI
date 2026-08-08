import { useState, useEffect } from 'react';
import {
  BarChart3, TrendingUp, Package, Briefcase,
  ArrowUpRight, AlertTriangle, Clock, RefreshCw
} from 'lucide-react';
import { api } from '../api/client';
import type { DashboardData, WorkflowExecution } from '../types';
import { Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
  PROCESSING: 'text-blue-400 bg-blue-500/10 border-blue-500/20',
  FAILED: 'text-red-400 bg-red-500/10 border-red-500/20',
  NEGOTIATING: 'text-purple-400 bg-purple-500/10 border-purple-500/20',
  VENDOR_SELECTED: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20',
  COMPARED: 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20',
};

function StatusBadge({ status }: { status: string }) {
  const cls = STATUS_COLORS[status] ?? 'text-[#9AA1AE] bg-[#1E2330] border-[#2A2F3E]';
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium border ${cls}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}

function KpiCard({ label, value, sub, icon: Icon, accent }: {
  label: string; value: string | number; sub?: string; icon: any; accent: string;
}) {
  return (
    <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 hover:border-[#2A2F3E] transition-all">
      <div className="flex items-start justify-between mb-4">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${accent}`}>
          <Icon className="w-5 h-5" />
        </div>
        <ArrowUpRight className="w-4 h-4 text-[#4B5563]" />
      </div>
      <div className="text-2xl font-bold text-white mb-1">{value}</div>
      <div className="text-sm text-[#6B7280]">{label}</div>
      {sub && <div className="text-xs text-[#4B5563] mt-1">{sub}</div>}
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

  const CHART_COLORS = ['#4F7CFF', '#7C5CFF', '#06B6D4', '#10B981'];

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Procurement Dashboard</h1>
          <p className="text-sm text-[#6B7280]">AI-powered procurement overview and analytics</p>
        </div>
        <button
          onClick={load}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2 bg-[#1E2330] border border-[#2A2F3E] rounded-xl text-sm text-[#9AA1AE] hover:text-white hover:border-[#4F7CFF]/50 transition-all"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-4 text-yellow-400 text-sm">
          <AlertTriangle className="w-4 h-4 flex-shrink-0" />
          {error} — Backend may not be running yet. <Link to="/demo" className="underline ml-1">Run Demo</Link>
        </div>
      )}

      {/* KPI Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          label="Total Workflows"
          value={data?.totalWorkflows ?? workflows.length ?? 0}
          sub={`${data?.completedWorkflows ?? 0} completed`}
          icon={Briefcase}
          accent="bg-[#4F7CFF]/10 text-[#4F7CFF]"
        />
        <KpiCard
          label="Total Vendors"
          value={data?.totalVendors ?? 0}
          sub="evaluated"
          icon={Package}
          accent="bg-purple-500/10 text-purple-400"
        />
        <KpiCard
          label="Quotes Processed"
          value={data?.totalQuotes ?? 0}
          sub="normalized & benchmarked"
          icon={BarChart3}
          accent="bg-cyan-500/10 text-cyan-400"
        />
        <KpiCard
          label="Est. Savings"
          value={data?.totalSavings ? `₹${(data.totalSavings / 1000).toFixed(0)}K` : '—'}
          sub={data?.averageSavingsPercent ? `Avg ${data.averageSavingsPercent.toFixed(1)}% per deal` : 'Run a demo to see'}
          icon={TrendingUp}
          accent="bg-emerald-500/10 text-emerald-400"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Spend chart */}
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
          <h3 className="text-sm font-semibold text-white mb-4">Spend by Category</h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={chartData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
              <XAxis dataKey="category" tick={{ fill: '#6B7280', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: '#6B7280', fontSize: 11 }} axisLine={false} tickLine={false}
                tickFormatter={(v) => `₹${(v / 1000).toFixed(0)}K`} />
              <Tooltip
                contentStyle={{ background: '#12151C', border: '1px solid #1E2330', borderRadius: 12 }}
                labelStyle={{ color: '#9AA1AE' }}
                formatter={(v: any) => [`₹${(v / 1000).toFixed(0)}K`, 'Spend']}
              />
              <Bar dataKey="amount" radius={[6, 6, 0, 0]}>
                {chartData.map((_, i) => (
                  <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Recent Workflows */}
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-white">Recent Workflows</h3>
            <Link to="/workflows" className="text-xs text-[#4F7CFF] hover:text-white transition-colors">
              View all →
            </Link>
          </div>
          {workflows.length === 0 ? (
            <div className="text-center py-8 text-[#6B7280] text-sm">
              <Clock className="w-8 h-8 mx-auto mb-2 opacity-30" />
              No workflows yet.{' '}
              <Link to="/demo" className="text-[#4F7CFF] hover:underline">Run the demo</Link> to get started.
            </div>
          ) : (
            <div className="space-y-3">
              {workflows.map((wf) => (
                <Link
                  key={wf.id}
                  to={`/workflows/${wf.id}`}
                  className="flex items-center justify-between p-3 rounded-xl bg-[#0B0D12] border border-[#1E2330] hover:border-[#4F7CFF]/30 transition-all group"
                >
                  <div className="min-w-0">
                    <div className="text-sm font-medium text-white truncate group-hover:text-[#4F7CFF] transition-colors">
                      {wf.title}
                    </div>
                    <div className="text-xs text-[#4B5563] mt-0.5">
                      #{wf.id} · {new Date(wf.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  <StatusBadge status={wf.status} />
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Quick actions */}
      <div className="bg-gradient-to-r from-[#4F7CFF]/10 to-purple-500/10 border border-[#4F7CFF]/20 rounded-2xl p-5 flex items-center justify-between">
        <div>
          <h3 className="font-semibold text-white mb-1">Ready to see ProcureAI in action?</h3>
          <p className="text-sm text-[#9AA1AE]">Run the complete 50-laptop procurement demo with AI negotiation, PO generation, and audit trail.</p>
        </div>
        <Link
          to="/demo"
          className="flex-shrink-0 bg-[#4F7CFF] text-white px-5 py-2.5 rounded-xl font-medium text-sm hover:bg-[#3B6AE8] transition-colors whitespace-nowrap"
        >
          Run Demo →
        </Link>
      </div>
    </div>
  );
}
