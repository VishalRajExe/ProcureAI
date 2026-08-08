import { useState, useEffect } from 'react';
import {
  ShieldCheck, CheckCircle2, Clock, RefreshCw, Send
} from 'lucide-react';
import { api } from '../api/client';
import type { Negotiation } from '../types';
import { useToast } from '../components/Toast';

export function ApprovalsPage() {
  const { showToast } = useToast();
  const [negotiations, setNegotiations] = useState<Negotiation[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const negs = await api.listNegotiations().catch(() => []);
      setNegotiations(negs);
    } catch (err: any) {
      showToast('Error', 'Failed to load approvals queue', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const pendingApprovals = negotiations.filter((n) => n.status === 'PENDING_APPROVAL' || n.status === 'DRAFTED');
  const pastApprovals = negotiations.filter((n) => n.status !== 'PENDING_APPROVAL' && n.status !== 'DRAFTED');

  const handleApprove = async (negId: number, body?: string) => {
    setActionLoading(true);
    try {
      await api.approveNegotiation(negId, true, body, 'Approved via Human Approvals Queue');
      showToast('Approved', 'Negotiation approved and email dispatched via Brevo API', 'success');
      await loadData();
    } catch (err: any) {
      showToast('Error', err?.response?.data?.message ?? 'Failed to approve', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async (negId: number) => {
    setActionLoading(true);
    try {
      await api.approveNegotiation(negId, false, undefined, 'Rejected via Human Approvals Queue');
      showToast('Rejected', 'Negotiation request rejected', 'info');
      await loadData();
    } catch (err: any) {
      showToast('Error', err?.response?.data?.message ?? 'Failed to reject', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-[#3E52FF] uppercase tracking-wider mb-1">
            <ShieldCheck className="w-3.5 h-3.5" /> Human Governance Gateway
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Human Approval Queue</h1>
          <p className="text-sm text-[#8F8FA2]">Review and approve AI-generated procurement actions and Brevo outbound emails</p>
        </div>

        <button
          onClick={loadData}
          disabled={loading}
          className="flex items-center gap-2 px-3.5 py-2 bg-[#12151C] border border-[#1E2330] rounded-xl text-sm font-medium text-[#E0E3E5] hover:text-white hover:border-[#3E52FF]/50 transition-all"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {loading ? (
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-12 text-center text-[#8F8FA2]">
          <RefreshCw className="w-8 h-8 text-[#3E52FF] mx-auto mb-3 animate-spin" />
          <p className="text-sm">Loading approval requests...</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Pending Approvals Section */}
          <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-[#1E2330] pb-4">
              <h2 className="font-semibold text-white flex items-center gap-2">
                <Clock className="w-4 h-4 text-amber-400" /> Pending Approval ({pendingApprovals.length})
              </h2>
              <span className="text-xs font-mono text-amber-400 bg-amber-500/10 px-2.5 py-0.5 rounded border border-amber-500/20">
                Action Required
              </span>
            </div>

            {pendingApprovals.length === 0 ? (
              <div className="text-center py-8 text-[#8F8FA2]">
                <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto mb-2 opacity-50" />
                <p className="text-sm font-medium text-white">All approval requests cleared!</p>
                <p className="text-xs text-[#8F8FA2] mt-1">No pending negotiation or financial actions waiting for approval</p>
              </div>
            ) : (
              <div className="space-y-4">
                {pendingApprovals.map((neg) => (
                  <div key={neg.id} className="bg-[#0B0D12] border border-[#1E2330] rounded-xl p-5 space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div>
                        <div className="text-xs font-mono text-[#3E52FF]">Negotiation #{neg.id}</div>
                        <h3 className="text-base font-bold text-white mt-0.5">{neg.quote.vendor.name}</h3>
                      </div>

                      <div className="flex items-center gap-4 text-xs font-mono">
                        <div>
                          <span className="text-[#8F8FA2] block">Current</span>
                          <span className="text-white font-semibold">₹{Number(neg.currentPrice).toLocaleString('en-IN')}</span>
                        </div>
                        <div>
                          <span className="text-[#8F8FA2] block">Target</span>
                          <span className="text-emerald-400 font-semibold">₹{Number(neg.targetPrice).toLocaleString('en-IN')}</span>
                        </div>
                      </div>
                    </div>

                    {neg.draftEmailBody && (
                      <div className="bg-[#12151C] p-3 rounded-lg border border-[#1E2330] text-xs font-mono text-[#E0E3E5]">
                        <div className="text-[#8F8FA2] text-[10px] uppercase mb-1">Draft Email Preview (Brevo Delivery)</div>
                        <p className="line-clamp-3">{neg.draftEmailBody}</p>
                      </div>
                    )}

                    <div className="flex items-center justify-end gap-3 pt-2">
                      <button
                        onClick={() => handleReject(neg.id)}
                        disabled={actionLoading}
                        className="px-4 py-2 bg-[#1E2330] text-rose-400 hover:text-white rounded-xl text-xs font-semibold transition-all"
                      >
                        Reject
                      </button>
                      <button
                        onClick={() => handleApprove(neg.id, neg.draftEmailBody)}
                        disabled={actionLoading}
                        className="px-4 py-2 bg-gradient-to-r from-[#3E52FF] to-indigo-600 text-white rounded-xl text-xs font-semibold shadow-md hover:opacity-90 transition-all flex items-center gap-1.5"
                      >
                        <Send className="w-3.5 h-3.5" /> Approve & Send Email
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Past Approvals History */}
          <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
            <h2 className="font-semibold text-white text-sm border-b border-[#1E2330] pb-3">
              Approval Audit Trail History ({pastApprovals.length})
            </h2>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-[#1E2330] text-xs font-mono uppercase text-[#8F8FA2]">
                    <th className="pb-3 px-3">Negotiation ID</th>
                    <th className="pb-3 px-3">Vendor</th>
                    <th className="pb-3 px-3">Target Price</th>
                    <th className="pb-3 px-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#1E2330]/50">
                  {pastApprovals.map((neg) => (
                    <tr key={neg.id} className="hover:bg-[#191C24] transition-colors">
                      <td className="py-3 px-3 font-mono text-xs text-[#BDC2FF]">#{neg.id}</td>
                      <td className="py-3 px-3 font-medium text-white">{neg.quote.vendor.name}</td>
                      <td className="py-3 px-3 font-mono text-xs text-white">₹{Number(neg.targetPrice).toLocaleString('en-IN')}</td>
                      <td className="py-3 px-3">
                        <span className={`text-xs px-2.5 py-0.5 rounded-full font-medium border ${
                          neg.status === 'APPROVED' || neg.status === 'ACCEPTED' || neg.status === 'SENT'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                        }`}>
                          {neg.status.replace(/_/g, ' ')}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
