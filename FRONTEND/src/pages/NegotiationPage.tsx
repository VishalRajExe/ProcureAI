import { useState, useEffect } from 'react';
import {
  Handshake, Mail, CheckCircle2,
  RefreshCw, Cpu, Send, ArrowRight, DollarSign, CornerDownRight
} from 'lucide-react';
import { api } from '../api/client';
import type { Negotiation, Quote } from '../types';
import { useToast } from '../components/Toast';
import { Link } from 'react-router-dom';
import { clsx } from 'clsx';

export function NegotiationPage() {
  const { showToast } = useToast();
  const [negotiations, setNegotiations] = useState<Negotiation[]>([]);
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [selectedNeg, setSelectedNeg] = useState<Negotiation | null>(null);
  const [editedEmail, setEditedEmail] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [counterPrice, setCounterPrice] = useState<number | ''>('');
  const [emails, setEmails] = useState<any[]>([]);
  const [loadingEmails, setLoadingEmails] = useState(false);
  const [resendingId, setResendingId] = useState<number | null>(null);
  const [activeWorkspaceTab, setActiveWorkspaceTab] = useState<'workspace' | 'emails'>('workspace');
  const [emailsPage, setEmailsPage] = useState(1);

  const emailsPageSize = 3;
  const totalEmailPages = Math.ceil(emails.length / emailsPageSize);
  const startIndex = (emailsPage - 1) * emailsPageSize;
  const paginatedEmails = emails.slice(startIndex, startIndex + emailsPageSize);

  useEffect(() => {
    setActiveWorkspaceTab('workspace');
    setEmailsPage(1);
  }, [selectedNeg]);

  const fetchEmailsForNegotiation = async (negId: number) => {
    setLoadingEmails(true);
    try {
      const list = await api.listEmailsForNegotiation(negId);
      setEmails(list);
    } catch (err) {
      console.error('Failed to load emails', err);
    } finally {
      setLoadingEmails(false);
    }
  };

  const handleResendEmail = async (emailId: number) => {
    setResendingId(emailId);
    try {
      await api.retryEmail(emailId);
      showToast('Email Resent', 'Outbound email successfully re-dispatched!', 'success');
      if (selectedNeg) {
        fetchEmailsForNegotiation(selectedNeg.id);
      }
    } catch (err: any) {
      showToast('Resend Failed', err?.response?.data?.message ?? 'Failed to resend email', 'error');
    } finally {
      setResendingId(null);
    }
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const [negs, qList] = await Promise.all([
        api.listNegotiations().catch(() => []),
        api.listQuotes().catch(() => []),
      ]);
      setNegotiations(negs);
      setQuotes(qList);
      if (negs.length > 0) {
        const latest = negs[negs.length - 1];
        setSelectedNeg(latest);
        setEditedEmail(latest.draftEmailBody ?? '');
        fetchEmailsForNegotiation(latest.id);
      }
    } catch (err: any) {
      showToast('Error', 'Failed to load negotiation details', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleDraftForQuote = async (quoteId: number) => {
    setActionLoading(true);
    try {
      const draft = await api.draftNegotiation(quoteId);
      showToast('Negotiation Drafted', 'AI strategy & email drafted', 'success');
      await loadData();
      setSelectedNeg(draft);
      setEditedEmail(draft.draftEmailBody ?? '');
      fetchEmailsForNegotiation(draft.id);
    } catch (err: any) {
      showToast('Error', err?.response?.data?.message ?? 'Failed to draft negotiation', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleApproveAndSend = async () => {
    if (!selectedNeg) return;
    setActionLoading(true);
    try {
      const updated = await api.approveNegotiation(selectedNeg.id, true, editedEmail, 'Approved by Human Procurement Officer');
      showToast('Approved & Sent', `Negotiation email sent to vendor!`, 'success');
      setSelectedNeg(updated);
      await loadData();
      fetchEmailsForNegotiation(selectedNeg.id);
    } catch (err: any) {
      showToast('Action Failed', err?.response?.data?.message ?? 'Failed to approve negotiation', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!selectedNeg) return;
    setActionLoading(true);
    try {
      const updated = await api.approveNegotiation(selectedNeg.id, false, undefined, 'Rejected by Human Officer');
      showToast('Negotiation Rejected', 'Negotiation cancelled', 'info');
      setSelectedNeg(updated);
      await loadData();
      fetchEmailsForNegotiation(selectedNeg.id);
    } catch (err: any) {
      showToast('Error', err?.response?.data?.message ?? 'Failed to reject', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSimulateVendorResponse = async () => {
    if (!selectedNeg || !counterPrice || Number(counterPrice) <= 0) {
      showToast('Validation', 'Please enter a valid counter price', 'error');
      return;
    }
    setActionLoading(true);
    try {
      const updated = await api.simulateVendorResponse(selectedNeg.id, Number(counterPrice));
      showToast('Vendor Counter Received', `AI evaluated counter price of ₹${Number(counterPrice).toLocaleString('en-IN')}`);
      setSelectedNeg(updated);
      setCounterPrice('');
      await loadData();
      fetchEmailsForNegotiation(selectedNeg.id);
    } catch (err: any) {
      showToast('Error', err?.response?.data?.message ?? 'Failed to process vendor counter', 'error');
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
            <Cpu className="w-3.5 h-3.5" /> Autonomous Negotiation Engine
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">AI Negotiation Center</h1>
          <p className="text-sm text-[#8F8FA2]">Human-in-the-loop AI negotiation drafting, email dispatch, and counter-offer evaluation</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={loadData}
            disabled={loading}
            className="flex items-center gap-2 px-3.5 py-2 bg-[#12151C] border border-[#1E2330] rounded-xl text-sm font-medium text-[#E0E3E5] hover:text-white hover:border-[#3E52FF]/50 transition-all"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {loading ? (
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-12 text-center text-[#8F8FA2]">
          <RefreshCw className="w-8 h-8 text-[#3E52FF] mx-auto mb-3 animate-spin" />
          <p className="text-sm font-medium text-white">Loading negotiation center...</p>
        </div>
      ) : negotiations.length === 0 ? (
        <div className="bg-[#12151C] border border-dashed border-[#1E2330] rounded-2xl p-12 text-center space-y-4">
          <Handshake className="w-12 h-12 text-[#8F8FA2] mx-auto opacity-30" />
          <div>
            <p className="text-base font-semibold text-white">No active negotiations drafted</p>
            <p className="text-xs text-[#8F8FA2] mt-1">Select a quote to draft an AI negotiation strategy</p>
          </div>
          {quotes.length > 0 && (
            <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
              {quotes.map((q) => (
                <button
                  key={q.id}
                  onClick={() => handleDraftForQuote(q.id)}
                  disabled={actionLoading}
                  className="px-4 py-2 bg-[#3E52FF]/10 text-[#BDC2FF] border border-[#3E52FF]/30 rounded-xl text-xs font-medium hover:bg-[#3E52FF] hover:text-white transition-all flex items-center gap-2"
                >
                  <Cpu className="w-3.5 h-3.5" /> Draft for {q.vendor.name}
                </button>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Active Negotiations List */}
          <div className="lg:col-span-1 bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 space-y-4">
            <h2 className="font-semibold text-white text-sm flex items-center justify-between border-b border-[#1E2330] pb-3">
              <span>Negotiations ({negotiations.length})</span>
              <span className="text-xs font-mono text-[#3E52FF]">AI & Email Service Active</span>
            </h2>

            <div className="space-y-3 max-h-[600px] overflow-y-auto pr-1">
              {negotiations.map((neg) => {
                const isSelected = selectedNeg?.id === neg.id;
                return (
                  <div
                    key={neg.id}
                    onClick={() => {
                      setSelectedNeg(neg);
                      setEditedEmail(neg.draftEmailBody ?? '');
                      fetchEmailsForNegotiation(neg.id);
                    }}
                    className={`p-4 rounded-xl border cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-[#191C26] border-[#3E52FF] ring-1 ring-[#3E52FF]/30'
                        : 'bg-[#0B0D12] border-[#1E2330] hover:border-[#3E52FF]/40'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="font-semibold text-white text-sm">{neg.quote.vendor.name}</div>
                      <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full uppercase border ${
                        neg.status === 'ACCEPTED'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : neg.status === 'PENDING_APPROVAL'
                          ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                          : neg.status === 'SENT'
                          ? 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                          : 'bg-[#1E2330] text-[#8F8FA2] border-[#2A2F3E]'
                      }`}>
                        {neg.status.replace(/_/g, ' ')}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                      <div>
                        <span className="text-[#8F8FA2] block text-[10px]">CURRENT</span>
                        <span className="text-white font-semibold">₹{Number(neg.currentPrice).toLocaleString('en-IN')}</span>
                      </div>
                      <div>
                        <span className="text-[#8F8FA2] block text-[10px]">TARGET</span>
                        <span className="text-emerald-400 font-semibold">₹{Number(neg.targetPrice).toLocaleString('en-IN')}</span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Selected Negotiation Workspace */}
          {selectedNeg && (
            <div className="lg:col-span-2 space-y-6">
              {/* Tab Header Selector */}
              <div className="flex border-b border-[#1E2330] gap-6 flex-shrink-0">
                <button
                  onClick={() => setActiveWorkspaceTab('workspace')}
                  className={clsx(
                    "pb-3 text-sm font-semibold transition-all border-b-2",
                    activeWorkspaceTab === 'workspace'
                      ? "text-[#3E52FF] border-[#3E52FF]"
                      : "text-[#8F8FA2] border-transparent hover:text-white"
                  )}
                >
                  Negotiation Workspace
                </button>
                <button
                  onClick={() => setActiveWorkspaceTab('emails')}
                  className={clsx(
                    "pb-3 text-sm font-semibold transition-all border-b-2 flex items-center gap-1.5",
                    activeWorkspaceTab === 'emails'
                      ? "text-[#3E52FF] border-[#3E52FF]"
                      : "text-[#8F8FA2] border-transparent hover:text-white"
                  )}
                >
                  <Mail className="w-4 h-4" /> Email Logs ({emails.length})
                </button>
              </div>

              {activeWorkspaceTab === 'workspace' ? (
                <div className="space-y-6">
                  {/* Strategy & Reasoning Card */}
                  <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
                    <div className="flex items-center justify-between border-b border-[#1E2330] pb-4">
                      <div>
                        <span className="text-xs font-mono text-[#3E52FF]">Negotiation #{selectedNeg.id}</span>
                        <h2 className="text-lg font-bold text-white mt-0.5">{selectedNeg.quote.vendor.name} Strategy</h2>
                      </div>

                      <div className="flex items-center gap-2">
                        <span className="text-xs text-[#8F8FA2] font-mono">Round {selectedNeg.currentRound} / {selectedNeg.maxRounds}</span>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div className="bg-[#0B0D12] p-3 rounded-xl border border-[#1E2330]">
                        <div className="text-xs text-[#8F8FA2] mb-1">Current Quote</div>
                        <div className="text-lg font-bold font-mono text-white">₹{Number(selectedNeg.currentPrice).toLocaleString('en-IN')}</div>
                      </div>
                      <div className="bg-[#0B0D12] p-3 rounded-xl border border-[#1E2330]">
                        <div className="text-xs text-[#8F8FA2] mb-1">AI Target</div>
                        <div className="text-lg font-bold font-mono text-emerald-400">₹{Number(selectedNeg.targetPrice).toLocaleString('en-IN')}</div>
                      </div>
                      <div className="bg-[#0B0D12] p-3 rounded-xl border border-[#1E2330]">
                        <div className="text-xs text-[#8F8FA2] mb-1">Max Approved</div>
                        <div className="text-lg font-bold font-mono text-[#BDC2FF]">₹{Number(selectedNeg.maxApprovedPrice).toLocaleString('en-IN')}</div>
                      </div>
                    </div>

                    <div className="bg-[#0B0D12]/80 border border-[#3E52FF]/20 rounded-xl p-4 space-y-2">
                      <div className="flex items-center gap-2 text-xs font-mono text-[#3E52FF] font-semibold">
                        <Cpu className="w-3.5 h-3.5" /> AI Reasoning Strategy
                      </div>
                      <p className="text-xs text-[#E0E3E5] leading-relaxed font-mono">
                        {selectedNeg.aiStrategy ?? selectedNeg.aiReason}
                      </p>
                    </div>
                  </div>

                  {/* Negotiation Email Editor & Actions */}
                  <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="font-semibold text-white text-sm flex items-center gap-2">
                        <Mail className="w-4 h-4 text-[#3E52FF]" /> Negotiation Email Draft (Human Approval Required)
                      </h3>
                    </div>

                    <textarea
                      value={editedEmail}
                      onChange={(e) => setEditedEmail(e.target.value)}
                      disabled={selectedNeg.status !== 'PENDING_APPROVAL' && selectedNeg.status !== 'DRAFTED'}
                      rows={8}
                      className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl p-4 text-xs font-mono text-white focus:outline-none focus:border-[#3E52FF] leading-relaxed"
                    />

                    {selectedNeg.status === 'PENDING_APPROVAL' || selectedNeg.status === 'DRAFTED' ? (
                      <div className="flex items-center justify-end gap-3 pt-2">
                        <button
                          onClick={handleReject}
                          disabled={actionLoading}
                          className="px-4 py-2.5 bg-[#1E2330] text-rose-400 hover:text-white hover:bg-rose-500/20 rounded-xl text-sm font-medium transition-all"
                        >
                          Reject Draft
                        </button>

                        <button
                          onClick={handleApproveAndSend}
                          disabled={actionLoading}
                          className="px-5 py-2.5 bg-gradient-to-r from-[#3E52FF] to-indigo-600 text-white rounded-xl text-sm font-semibold shadow-lg shadow-blue-500/20 hover:opacity-95 transition-all flex items-center gap-2"
                        >
                          {actionLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                          Approve & Send Email
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center justify-between p-3 bg-[#0B0D12] border border-[#1E2330] rounded-xl text-xs">
                        <span className="text-[#8F8FA2]">Status: <strong className="text-white">{selectedNeg.status}</strong></span>
                        {selectedNeg.status === 'ACCEPTED' ? (
                          <Link to="/purchase-orders" className="text-[#3E52FF] hover:underline font-semibold flex items-center gap-1">
                            Proceed to Purchase Order <ArrowRight className="w-3.5 h-3.5" />
                          </Link>
                        ) : (
                          <span className="text-emerald-400 flex items-center gap-1 font-mono">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Email Dispatched
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Vendor Reply Simulator */}
                  {(selectedNeg.status === 'SENT' || selectedNeg.status === 'RE_EVALUATING') && (
                    <div className="bg-[#12151C] border border-[#3E52FF]/30 rounded-2xl p-6 space-y-4">
                      <h3 className="font-semibold text-white text-sm flex items-center gap-2">
                        <CornerDownRight className="w-4 h-4 text-[#3E52FF]" /> Vendor Counter-Offer Inbox Simulator
                      </h3>

                      <div className="flex items-center gap-3">
                        <div className="relative flex-1">
                          <DollarSign className="w-4 h-4 text-[#8F8FA2] absolute left-3 top-3" />
                          <input
                            type="number"
                            value={counterPrice}
                            onChange={(e) => setCounterPrice(e.target.value ? Number(e.target.value) : '')}
                            placeholder="e.g. 52000 (Vendor counter price in ₹)"
                            className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-[#3E52FF]"
                          />
                        </div>
                        <button
                          onClick={handleSimulateVendorResponse}
                          disabled={actionLoading}
                          className="px-4 py-2 bg-[#3E52FF] text-white text-sm font-medium rounded-xl shadow-lg shadow-blue-500/20 hover:opacity-90 transition-all flex items-center gap-2"
                        >
                          {actionLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : 'Simulate Vendor Reply'}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                /* Communication Dispatch Logs */
                <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
                  <div className="flex items-center justify-between border-b border-[#1E2330] pb-3">
                    <h3 className="font-semibold text-white text-sm flex items-center gap-2">
                      <Mail className="w-4 h-4 text-[#3E52FF]" /> Email Communication Logs
                    </h3>
                    <span className="text-xs text-[#8F8FA2] font-mono">Outbound Audit Feed</span>
                  </div>

                  {loadingEmails ? (
                    <div className="text-center py-4 text-[#8F8FA2]">
                      <RefreshCw className="w-5 h-5 mx-auto animate-spin mb-2" />
                      <p className="text-xs">Loading logs...</p>
                    </div>
                  ) : emails.length === 0 ? (
                    <div className="text-center py-6 text-[#8F8FA2]">
                      <Mail className="w-8 h-8 mx-auto opacity-30 mb-2" />
                      <p className="text-xs">No email messages dispatched yet for this negotiation.</p>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      <div className="space-y-3">
                        {paginatedEmails.map((msg) => (
                          <div key={msg.id} className="bg-[#0B0D12] border border-[#1E2330] rounded-xl p-4 space-y-2">
                            <div className="flex items-center justify-between text-xs">
                              <span className="text-[#8F8FA2] font-mono">To: <strong className="text-white">{msg.toAddress}</strong></span>
                              <span className="text-[#8F8FA2] font-mono">{new Date(msg.createdAt).toLocaleString('en-IN')}</span>
                            </div>
                            <div className="text-sm font-semibold text-white">{msg.subject}</div>
                            <div className="text-xs text-[#8F8FA2] font-mono bg-[#12151C]/60 p-2.5 rounded-lg whitespace-pre-wrap leading-relaxed border border-[#1E2330]/50">
                              {msg.body}
                            </div>
                            <div className="flex items-center justify-between pt-1">
                              <div className="flex items-center gap-2">
                                <span className={clsx(
                                  "text-[10px] font-mono px-2 py-0.5 rounded-full border uppercase",
                                  msg.status === 'SENT'
                                    ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                                    : msg.status === 'FAILED'
                                    ? "bg-rose-500/10 text-rose-400 border-rose-500/20"
                                    : "bg-amber-500/10 text-amber-400 border-amber-500/20"
                                )}>
                                  {msg.status}
                                </span>
                                {msg.errorMessage && (
                                  <span className="text-[10px] text-rose-400 font-mono max-w-[200px] truncate" title={msg.errorMessage}>
                                    {msg.errorMessage}
                                  </span>
                                )}
                              </div>

                              <button
                                onClick={() => handleResendEmail(msg.id)}
                                disabled={resendingId !== null}
                                className="px-2.5 py-1 bg-[#3E52FF]/10 hover:bg-[#3E52FF] text-[#BDC2FF] hover:text-white border border-[#3E52FF]/20 hover:border-transparent rounded-lg text-[10px] font-semibold transition-all flex items-center gap-1"
                              >
                                {resendingId === msg.id ? (
                                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                ) : (
                                  <Send className="w-3 h-3" />
                                )}
                                Resend Email
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Pagination Controls */}
                      {totalEmailPages > 1 && (
                        <div className="flex items-center justify-between pt-4 border-t border-[#1E2330]">
                          <span className="text-xs text-[#8F8FA2]">
                            Showing {startIndex + 1}-{Math.min(startIndex + emailsPageSize, emails.length)} of {emails.length} logs
                          </span>
                          <div className="flex items-center gap-2">
                            <button
                              disabled={emailsPage === 1}
                              onClick={() => setEmailsPage(emailsPage - 1)}
                              className="px-2.5 py-1 text-xs font-semibold bg-[#1E2330] hover:bg-[#3E52FF] text-[#BDC2FF] hover:text-white rounded-lg disabled:opacity-40 disabled:hover:bg-[#1E2330] transition-colors"
                            >
                              Prev
                            </button>
                            <span className="text-xs text-white font-mono">{emailsPage} / {totalEmailPages}</span>
                            <button
                              disabled={emailsPage === totalEmailPages}
                              onClick={() => setEmailsPage(emailsPage + 1)}
                              className="px-2.5 py-1 text-xs font-semibold bg-[#1E2330] hover:bg-[#3E52FF] text-[#BDC2FF] hover:text-white rounded-lg disabled:opacity-40 disabled:hover:bg-[#1E2330] transition-colors"
                            >
                              Next
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
