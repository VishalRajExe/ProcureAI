import { useState, useEffect } from 'react';
import {
  Mail, RefreshCw, DollarSign,
  Inbox, CornerDownRight
} from 'lucide-react';
import { api } from '../api/client';
import type { Negotiation } from '../types';
import { useToast } from '../components/Toast';

export function VendorInboxPage() {
  const { showToast } = useToast();
  const [negotiations, setNegotiations] = useState<Negotiation[]>([]);
  const [selectedNeg, setSelectedNeg] = useState<Negotiation | null>(null);
  const [counterPrice, setCounterPrice] = useState<number | ''>('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const negs = await api.listNegotiations().catch(() => []);
      setNegotiations(negs);
      if (negs.length > 0 && !selectedNeg) {
        setSelectedNeg(negs[negs.length - 1]);
      }
    } catch (err: any) {
      showToast('Error', 'Failed to load vendor inbox', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleSimulateReply = async () => {
    if (!selectedNeg || !counterPrice || Number(counterPrice) <= 0) {
      showToast('Validation Error', 'Please enter a valid counter offer price', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const updated = await api.simulateVendorResponse(selectedNeg.id, Number(counterPrice));
      showToast('Vendor Counter Submitted', `Gemini AI evaluated counter price of ₹${Number(counterPrice).toLocaleString('en-IN')}`);
      setSelectedNeg(updated);
      setCounterPrice('');
      await loadData();
    } catch (err: any) {
      showToast('Error', err?.response?.data?.message ?? 'Failed to submit counter offer', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const sentNegotiations = negotiations.filter((n) => n.status === 'SENT' || n.status === 'RE_EVALUATING' || n.status === 'ACCEPTED');

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-[#3E52FF] uppercase tracking-wider mb-1">
            <Inbox className="w-3.5 h-3.5" /> Outbound Communication Simulator
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Vendor Inbox & Response Simulator</h1>
          <p className="text-sm text-[#8F8FA2]">Simulate vendor responses to Brevo outbound negotiation emails</p>
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
          <p className="text-sm">Loading vendor inbox...</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Email Inbox Panel */}
          <div className="lg:col-span-1 bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 space-y-4">
            <h2 className="font-semibold text-white text-sm border-b border-[#1E2330] pb-3 flex items-center justify-between">
              <span>Dispatched Emails ({sentNegotiations.length})</span>
              <span className="text-xs text-emerald-400 font-mono">Brevo Active</span>
            </h2>

            {sentNegotiations.length === 0 ? (
              <div className="text-center py-8 text-[#8F8FA2]">
                <Mail className="w-8 h-8 mx-auto mb-2 opacity-30" />
                <p className="text-xs">No dispatched emails yet</p>
              </div>
            ) : (
              <div className="space-y-3">
                {sentNegotiations.map((neg) => {
                  const isSelected = selectedNeg?.id === neg.id;
                  return (
                    <div
                      key={neg.id}
                      onClick={() => setSelectedNeg(neg)}
                      className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                        isSelected
                          ? 'bg-[#191C26] border-[#3E52FF] ring-1 ring-[#3E52FF]/30'
                          : 'bg-[#0B0D12] border-[#1E2330] hover:border-[#3E52FF]/40'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-medium text-white text-xs">{neg.quote.vendor.name}</span>
                        <span className="text-[10px] text-emerald-400 font-mono">SENT</span>
                      </div>
                      <div className="text-xs text-[#8F8FA2] truncate">Subject: Quotation Discussion</div>
                      <div className="text-[11px] text-[#3E52FF] font-mono mt-1">Target: ₹{Number(neg.targetPrice).toLocaleString('en-IN')}</div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Simulator View */}
          {selectedNeg ? (
            <div className="lg:col-span-2 space-y-6">
              <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
                <div className="flex items-center justify-between border-b border-[#1E2330] pb-4">
                  <div>
                    <span className="text-xs font-mono text-[#3E52FF]">Negotiation #{selectedNeg.id}</span>
                    <h2 className="text-lg font-bold text-white mt-0.5">{selectedNeg.quote.vendor.name} Email View</h2>
                  </div>
                  <span className="text-xs font-mono px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    STATUS: {selectedNeg.status}
                  </span>
                </div>

                {/* Email Content Display */}
                <div className="bg-[#0B0D12] border border-[#1E2330] rounded-xl p-4 space-y-3 font-mono text-xs text-[#E0E3E5]">
                  <div className="text-[#8F8FA2] text-[11px] border-b border-[#1E2330] pb-2">
                    <div><strong>From:</strong> ProcureAI Team &lt;procurement@procureai.demo&gt;</div>
                    <div><strong>To:</strong> {selectedNeg.quote.vendor.name} &lt;{selectedNeg.quote.vendor.contactEmail ?? 'sales@vendor-demo.com'}&gt;</div>
                    <div><strong>Subject:</strong> Quotation Discussion — {selectedNeg.quote.items?.[0]?.productName ?? 'Equipment'}</div>
                  </div>
                  <p className="whitespace-pre-wrap leading-relaxed">{selectedNeg.draftEmailBody}</p>
                </div>

                {/* Counter Offer Submission Form */}
                <div className="bg-[#191C26] border border-[#3E52FF]/30 rounded-xl p-5 space-y-3">
                  <h3 className="font-semibold text-white text-sm flex items-center gap-2">
                    <CornerDownRight className="w-4 h-4 text-[#3E52FF]" /> Simulate Vendor Counter-Offer Reply
                  </h3>
                  <p className="text-xs text-[#8F8FA2]">
                    Submit a counter price to trigger real-time Gemini AI re-evaluation against maximum approved budget limits.
                  </p>

                  <div className="flex flex-col sm:flex-row items-center gap-3 pt-2">
                    <div className="relative flex-1 w-full">
                      <DollarSign className="w-4 h-4 text-[#8F8FA2] absolute left-3 top-3" />
                      <input
                        type="number"
                        value={counterPrice}
                        onChange={(e) => setCounterPrice(e.target.value ? Number(e.target.value) : '')}
                        placeholder="e.g. 52000 (Counter offer in ₹)"
                        className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-[#3E52FF]"
                      />
                    </div>
                    <button
                      onClick={handleSimulateReply}
                      disabled={submitting}
                      className="w-full sm:w-auto px-5 py-2 bg-gradient-to-r from-[#3E52FF] to-indigo-600 text-white text-sm font-semibold rounded-xl shadow-lg shadow-blue-500/20 hover:opacity-90 transition-all flex items-center justify-center gap-2"
                    >
                      {submitting ? <RefreshCw className="w-4 h-4 animate-spin" /> : 'Submit Counter Offer'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="lg:col-span-2 bg-[#12151C] border border-dashed border-[#1E2330] rounded-2xl p-12 text-center text-[#8F8FA2]">
              <Mail className="w-10 h-10 mx-auto mb-2 opacity-30" />
              <p className="text-sm text-white font-medium">Select a dispatched email from the left panel</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
