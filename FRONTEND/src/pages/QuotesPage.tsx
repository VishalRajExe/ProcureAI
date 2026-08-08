import { useState, useEffect } from 'react';
import {
  FileText, Upload, Plus, CheckCircle2, RefreshCw,
  Search, Cpu, FileUp
} from 'lucide-react';
import { api } from '../api/client';
import type { Quote, WorkflowExecution } from '../types';
import { useToast } from '../components/Toast';

export function QuotesPage() {
  const { showToast } = useToast();
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [workflows, setWorkflows] = useState<WorkflowExecution[]>([]);
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [vendorName, setVendorName] = useState('');
  const [vendorEmail, setVendorEmail] = useState('');
  const [rawText, setRawText] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [activeTab, setActiveTab] = useState<'upload_file' | 'upload_text'>('upload_file');
  const [search, setSearch] = useState('');

  const loadData = async () => {
    setLoading(true);
    try {
      const [wfs, qList] = await Promise.all([
        api.listWorkflows().catch(() => []),
        api.listQuotes().catch(() => []),
      ]);
      setWorkflows(wfs);
      setQuotes(qList);
      if (wfs.length > 0 && !selectedWorkflowId) {
        setSelectedWorkflowId(wfs[0].id);
      }
    } catch (err: any) {
      showToast('Error', 'Failed to load quotes', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleFileUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!vendorName.trim()) {
      showToast('Validation Error', 'Please enter a vendor name', 'error');
      return;
    }
    if (!file && !rawText.trim()) {
      showToast('Validation Error', 'Please select a PDF file or enter quote text', 'error');
      return;
    }

    setUploading(true);
    try {
      let wfId = selectedWorkflowId;
      if (!wfId && workflows.length > 0) wfId = workflows[0].id;
      if (!wfId) {
        const newWf = await api.createWorkflow('Procurement ' + new Date().toLocaleDateString());
        wfId = newWf.id;
      }

      if (file) {
        await api.uploadQuote(wfId, vendorName, file);
        showToast('Quote Ingested', `PDF for ${vendorName} processed via Gemini AI`);
      } else {
        await api.submitQuoteText(wfId, vendorName, rawText, vendorEmail);
        showToast('Quote Ingested', `Quote text for ${vendorName} parsed via Gemini AI`);
      }

      setVendorName('');
      setVendorEmail('');
      setRawText('');
      setFile(null);
      await loadData();
    } catch (err: any) {
      showToast('Ingestion Failed', err?.response?.data?.message ?? err?.message ?? 'Failed to ingest quote', 'error');
    } finally {
      setUploading(false);
    }
  };

  const filteredQuotes = quotes.filter((q) =>
    q.vendor.name.toLowerCase().includes(search.toLowerCase()) ||
    (q.sourceFileName && q.sourceFileName.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-[#3E52FF] uppercase tracking-wider mb-1">
            <Cpu className="w-3.5 h-3.5" /> AI Quote Ingestion Gateway
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Quotes & Ingestion</h1>
          <p className="text-sm text-[#8F8FA2]">Ingest vendor quotations (PDF/Text) with real-time Gemini AI parsing</p>
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload Form Panel */}
        <div className="lg:col-span-1 bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 space-y-5">
          <div className="flex items-center justify-between border-b border-[#1E2330] pb-4">
            <h2 className="font-semibold text-white flex items-center gap-2">
              <FileUp className="w-4 h-4 text-[#3E52FF]" /> Ingest New Quote
            </h2>
            <span className="text-xs font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
              Gemini Active
            </span>
          </div>

          <div className="flex rounded-xl bg-[#0B0D12] p-1 border border-[#1E2330]">
            <button
              onClick={() => setActiveTab('upload_file')}
              className={`flex-1 py-1.5 text-xs font-medium rounded-lg transition-all ${
                activeTab === 'upload_file' ? 'bg-[#3E52FF] text-white shadow' : 'text-[#8F8FA2] hover:text-white'
              }`}
            >
              PDF Document
            </button>
            <button
              onClick={() => setActiveTab('upload_text')}
              className={`flex-1 py-1.5 text-xs font-medium rounded-lg transition-all ${
                activeTab === 'upload_text' ? 'bg-[#3E52FF] text-white shadow' : 'text-[#8F8FA2] hover:text-white'
              }`}
            >
              Plain Text / JSON
            </button>
          </div>

          <form onSubmit={handleFileUpload} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-[#8F8FA2] mb-1.5">Vendor Name *</label>
              <input
                type="text"
                required
                value={vendorName}
                onChange={(e) => setVendorName(e.target.value)}
                placeholder="e.g. Dell Business Solutions"
                className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-3.5 py-2 text-sm text-white focus:outline-none focus:border-[#3E52FF] transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-[#8F8FA2] mb-1.5">Vendor Email (for Brevo sending)</label>
              <input
                type="email"
                value={vendorEmail}
                onChange={(e) => setVendorEmail(e.target.value)}
                placeholder="sales@dell-demo.com"
                className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-3.5 py-2 text-sm text-white focus:outline-none focus:border-[#3E52FF] transition-colors"
              />
            </div>

            {activeTab === 'upload_file' ? (
              <div>
                <label className="block text-xs font-medium text-[#8F8FA2] mb-1.5">PDF Quote File *</label>
                <div className="border-2 border-dashed border-[#1E2330] hover:border-[#3E52FF]/50 rounded-xl p-6 text-center cursor-pointer transition-colors bg-[#0B0D12]/50 relative">
                  <input
                    type="file"
                    accept=".pdf,.txt"
                    onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                  />
                  <Upload className="w-8 h-8 text-[#8F8FA2] mx-auto mb-2" />
                  <p className="text-xs text-[#E0E3E5] font-medium">
                    {file ? file.name : 'Click or drop PDF quotation file here'}
                  </p>
                  <p className="text-[11px] text-[#8F8FA2] mt-1">PDF up to 10 MB (Magic-bytes verified)</p>
                </div>
              </div>
            ) : (
              <div>
                <label className="block text-xs font-medium text-[#8F8FA2] mb-1.5">Raw Quote Text / Specifications *</label>
                <textarea
                  rows={5}
                  value={rawText}
                  onChange={(e) => setRawText(e.target.value)}
                  placeholder="Vendor: Lenovo&#10;Product: ThinkPad P16&#10;Quantity: 10&#10;Unit Price: 145000&#10;Warranty: 24 months&#10;Delivery: 5 days"
                  className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl p-3 text-sm text-white font-mono focus:outline-none focus:border-[#3E52FF] transition-colors"
                />
              </div>
            )}

            <button
              type="submit"
              disabled={uploading}
              className="w-full py-2.5 bg-gradient-to-r from-[#3E52FF] to-indigo-600 rounded-xl text-sm font-semibold text-white shadow-lg shadow-blue-500/20 hover:opacity-95 transition-all flex items-center justify-center gap-2"
            >
              {uploading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" /> Ingesting with Gemini AI...
                </>
              ) : (
                <>
                  <Plus className="w-4 h-4" /> Extract & Ingest Quote
                </>
              )}
            </button>
          </form>
        </div>

        {/* Quotes Table Panel */}
        <div className="lg:col-span-2 bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <h2 className="font-semibold text-white flex items-center gap-2">
              <FileText className="w-4 h-4 text-[#3E52FF]" /> Ingested Quotations ({quotes.length})
            </h2>

            <div className="relative">
              <Search className="w-4 h-4 text-[#8F8FA2] absolute left-3 top-2.5" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search vendor or file..."
                className="bg-[#0B0D12] border border-[#1E2330] rounded-xl pl-9 pr-3 py-1.5 text-xs text-white placeholder-[#8F8FA2] focus:outline-none focus:border-[#3E52FF]"
              />
            </div>
          </div>

          {loading ? (
            <div className="text-center py-12 text-[#8F8FA2]">
              <RefreshCw className="w-8 h-8 mx-auto mb-2 animate-spin text-[#3E52FF]" />
              <p className="text-sm">Loading quotations...</p>
            </div>
          ) : filteredQuotes.length === 0 ? (
            <div className="text-center py-12 border border-dashed border-[#1E2330] rounded-xl">
              <FileText className="w-10 h-10 text-[#8F8FA2] mx-auto mb-3 opacity-40" />
              <p className="text-sm text-white font-medium">No quotations ingested yet</p>
              <p className="text-xs text-[#8F8FA2] mt-1">Use the upload panel or Run Demo to seed vendor quotes</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-[#1E2330] text-xs font-mono uppercase text-[#8F8FA2]">
                    <th className="pb-3 px-3">Vendor</th>
                    <th className="pb-3 px-3">Items / Model</th>
                    <th className="pb-3 px-3 text-right">Total Price</th>
                    <th className="pb-3 px-3">Terms</th>
                    <th className="pb-3 px-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#1E2330]/50">
                  {filteredQuotes.map((q) => (
                    <tr key={q.id} className="hover:bg-[#191C24] transition-colors">
                      <td className="py-3.5 px-3">
                        <div className="font-medium text-white">{q.vendor.name}</div>
                        <div className="text-xs text-[#8F8FA2] font-mono">{q.sourceType} • {q.sourceFileName ?? 'Direct Input'}</div>
                      </td>
                      <td className="py-3.5 px-3">
                        {q.items.length > 0 ? (
                          <div>
                            <div className="text-white text-xs font-medium">{q.items[0].productName}</div>
                            <div className="text-[11px] text-[#8F8FA2]">Qty: {q.items[0].quantity} @ ₹{Number(q.items[0].unitPrice).toLocaleString('en-IN')}</div>
                          </div>
                        ) : (
                          <span className="text-xs text-[#8F8FA2]">—</span>
                        )}
                      </td>
                      <td className="py-3.5 px-3 text-right font-mono font-semibold text-white">
                        ₹{Number(q.calculatedTotal).toLocaleString('en-IN')}
                      </td>
                      <td className="py-3.5 px-3 text-xs text-[#E0E3E5]">
                        <div>{q.warrantyMonths ? `${q.warrantyMonths}m Warranty` : 'No Warranty'}</div>
                        <div className="text-[#8F8FA2]">{q.deliveryDays ? `${q.deliveryDays}d Delivery` : ''}</div>
                      </td>
                      <td className="py-3.5 px-3">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border ${
                          q.extractionStatus === 'VALIDATED'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
                        }`}>
                          <CheckCircle2 className="w-3 h-3" />
                          {q.extractionStatus}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
