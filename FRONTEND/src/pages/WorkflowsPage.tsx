import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { WorkflowExecution } from '../types';
import { GitBranch, Plus, RefreshCw, ChevronRight, Clock, AlertTriangle } from 'lucide-react';
import { clsx } from 'clsx';

const STATUS_CONFIG: Record<string, { label: string; cls: string }> = {
  PROCESSING: { label: 'Processing', cls: 'text-blue-400 bg-blue-500/10 border-blue-500/20' },
  COMPARED: { label: 'Compared', cls: 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20' },
  NEGOTIATING: { label: 'Negotiating', cls: 'text-purple-400 bg-purple-500/10 border-purple-500/20' },
  AWAITING_VENDOR_RESPONSE: { label: 'Awaiting Response', cls: 'text-orange-400 bg-orange-500/10 border-orange-500/20' },
  RE_EVALUATING: { label: 'Re-Evaluating', cls: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20' },
  VENDOR_SELECTED: { label: 'Vendor Selected', cls: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20' },
  COMPLETED: { label: 'Completed', cls: 'text-green-400 bg-green-500/10 border-green-500/20' },
  FAILED: { label: 'Failed', cls: 'text-red-400 bg-red-500/10 border-red-500/20' },
};

function WorkflowCard({ wf }: { wf: WorkflowExecution }) {
  const cfg = STATUS_CONFIG[wf.status] ?? { label: wf.status, cls: 'text-[#9AA1AE] bg-[#1E2330] border-[#2A2F3E]' };
  return (
    <Link
      to={`/workflows/${wf.id}`}
      className="block bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 hover:border-[#4F7CFF]/40 hover:bg-[#12151C]/80 transition-all group"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-[#4F7CFF]/10 flex items-center justify-center flex-shrink-0">
            <GitBranch className="w-4 h-4 text-[#4F7CFF]" />
          </div>
          <div>
            <div className="font-semibold text-white group-hover:text-[#4F7CFF] transition-colors text-sm">
              {wf.title}
            </div>
            <div className="text-xs text-[#4B5563]">Workflow #{wf.id}</div>
          </div>
        </div>
        <ChevronRight className="w-4 h-4 text-[#4B5563] group-hover:text-[#4F7CFF] transition-colors" />
      </div>

      {wf.description && (
        <p className="text-xs text-[#6B7280] mb-3 line-clamp-2">{wf.description}</p>
      )}

      <div className="flex items-center justify-between">
        <span className={clsx('inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border', cfg.cls)}>
          {cfg.label}
        </span>
        <span className="text-xs text-[#4B5563] flex items-center gap-1">
          <Clock className="w-3 h-3" />
          {new Date(wf.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
        </span>
      </div>
    </Link>
  );
}

export function WorkflowsPage() {
  const [workflows, setWorkflows] = useState<WorkflowExecution[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [creating, setCreating] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.listWorkflows();
      setWorkflows(data);
    } catch (e: any) {
      setError(e?.response?.data?.message ?? 'Failed to load workflows');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    try {
      await api.createWorkflow(title, description);
      setShowCreate(false);
      setTitle('');
      setDescription('');
      load();
    } catch (e: any) {
      setError(e?.response?.data?.message ?? 'Failed to create workflow');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Procurement Workflows</h1>
          <p className="text-sm text-[#6B7280]">{workflows.length} workflows total</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={load}
            disabled={loading}
            className="p-2 bg-[#1E2330] border border-[#2A2F3E] rounded-xl text-[#9AA1AE] hover:text-white transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 px-4 py-2 bg-[#4F7CFF] text-white rounded-xl text-sm font-medium hover:bg-[#3B6AE8] transition-colors"
          >
            <Plus className="w-4 h-4" />
            New Workflow
          </button>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-4 text-yellow-400 text-sm">
          <AlertTriangle className="w-4 h-4 flex-shrink-0" />
          {error}
        </div>
      )}

      {/* Create modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 shadow-2xl">
            <h2 className="text-lg font-bold text-white mb-4">New Procurement Workflow</h2>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[#9AA1AE] mb-1.5">Workflow Title *</label>
                <input
                  value={title}
                  onChange={e => setTitle(e.target.value)}
                  placeholder="e.g. Procurement of 50 Business Laptops"
                  required
                  className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 text-sm text-white placeholder-[#4B5563] focus:outline-none focus:border-[#4F7CFF] transition-colors"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-[#9AA1AE] mb-1.5">Description</label>
                <textarea
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  placeholder="Optional description..."
                  rows={3}
                  className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 text-sm text-white placeholder-[#4B5563] focus:outline-none focus:border-[#4F7CFF] transition-colors resize-none"
                />
              </div>
              <div className="flex gap-3 justify-end">
                <button
                  type="button"
                  onClick={() => setShowCreate(false)}
                  className="px-4 py-2 text-sm text-[#9AA1AE] hover:text-white transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="px-5 py-2 bg-[#4F7CFF] text-white text-sm rounded-xl font-medium hover:bg-[#3B6AE8] disabled:opacity-50 transition-colors"
                >
                  {creating ? 'Creating...' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Workflow list */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 animate-pulse">
              <div className="h-4 bg-[#1E2330] rounded mb-3 w-3/4" />
              <div className="h-3 bg-[#1E2330] rounded mb-2 w-1/2" />
              <div className="h-3 bg-[#1E2330] rounded w-1/4" />
            </div>
          ))}
        </div>
      ) : workflows.length === 0 ? (
        <div className="text-center py-20 text-[#6B7280]">
          <GitBranch className="w-12 h-12 mx-auto mb-4 opacity-20" />
          <p className="text-lg font-medium text-white mb-2">No workflows yet</p>
          <p className="text-sm mb-4">Create your first procurement workflow or run the demo.</p>
          <Link to="/demo" className="inline-flex items-center gap-2 bg-[#4F7CFF] text-white px-5 py-2.5 rounded-xl text-sm font-medium hover:bg-[#3B6AE8] transition-colors">
            Run Demo Workflow
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {workflows.map(wf => <WorkflowCard key={wf.id} wf={wf} />)}
        </div>
      )}
    </div>
  );
}
