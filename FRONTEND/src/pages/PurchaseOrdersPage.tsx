import { useState, useEffect } from 'react';
import {
  ShoppingBag, FileText, Download, Send, RefreshCw, CheckCircle2, Plus
} from 'lucide-react';
import { api } from '../api/client';
import type { PurchaseOrder } from '../types';
import { useToast } from '../components/Toast';

export function PurchaseOrdersPage() {
  const { showToast } = useToast();
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [generating, setGenerating] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const pos = await api.listPurchaseOrders().catch(() => []);
      setPurchaseOrders(pos);
    } catch (err: any) {
      showToast('Error', 'Failed to load purchase orders', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleGeneratePo = async () => {
    setGenerating(true);
    try {
      const po = await api.generatePO();
      showToast('Purchase Order Generated', `Generated ${po.poNumber} for ${po.vendor.name}`, 'success');
      await loadData();
    } catch (err: any) {
      showToast('Generation Failed', err?.response?.data?.message ?? 'Failed to generate PO', 'error');
    } finally {
      setGenerating(false);
    }
  };

  const handleSendPoEmail = async (poId: number) => {
    setActionLoading(poId);
    try {
      await api.sendPoEmail(poId);
      showToast('PO Issued', 'Purchase Order PDF dispatched to vendor via Email', 'success');
      await loadData();
    } catch (err: any) {
      showToast('Dispatch Failed', err?.response?.data?.message ?? 'Failed to send PO email', 'error');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-[#3E52FF] uppercase tracking-wider mb-1">
            <ShoppingBag className="w-3.5 h-3.5" /> Dynamic Order Generation & Dispatch
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Purchase Orders</h1>
          <p className="text-sm text-[#8F8FA2]">Server-generated PDF purchase orders and automated email dispatch</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleGeneratePo}
            disabled={generating}
            className="flex items-center gap-2 px-4 py-2 bg-[#3E52FF] text-white rounded-xl text-sm font-semibold hover:opacity-95 transition-all shadow-lg shadow-blue-500/20"
          >
            {generating ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            Generate Official PO
          </button>

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
          <p className="text-sm">Loading purchase orders...</p>
        </div>
      ) : purchaseOrders.length === 0 ? (
        <div className="bg-[#12151C] border border-dashed border-[#1E2330] rounded-2xl p-12 text-center space-y-4">
          <ShoppingBag className="w-12 h-12 mx-auto opacity-30 text-[#3E52FF]" />
          <div>
            <p className="text-base font-semibold text-white">No Purchase Orders generated yet</p>
            <p className="text-xs text-[#8F8FA2] mt-1">Generate a PO for current workflow or run an automated demo scenario</p>
          </div>
          <button
            onClick={handleGeneratePo}
            disabled={generating}
            className="inline-flex items-center gap-2 px-4 py-2 bg-[#3E52FF] text-white text-sm font-medium rounded-xl shadow-lg shadow-blue-500/20"
          >
            <Plus className="w-4 h-4" /> Generate Purchase Order Now
          </button>
        </div>
      ) : (
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between border-b border-[#1E2330] pb-4">
            <h2 className="font-semibold text-white flex items-center gap-2">
              <FileText className="w-4 h-4 text-[#3E52FF]" /> Issued & Generated POs ({purchaseOrders.length})
            </h2>
            <span className="text-xs font-mono text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded border border-emerald-500/20">
              PDFBox Renderer Active
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-[#1E2330] text-xs font-mono uppercase text-[#8F8FA2]">
                  <th className="pb-3 px-3">PO Number</th>
                  <th className="pb-3 px-3">Vendor</th>
                  <th className="pb-3 px-3 text-right">Total Amount</th>
                  <th className="pb-3 px-3">Currency</th>
                  <th className="pb-3 px-3">Status</th>
                  <th className="pb-3 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1E2330]/50">
                {purchaseOrders.map((po) => (
                  <tr key={po.id} className="hover:bg-[#191C24] transition-colors">
                    <td className="py-4 px-3 font-mono text-xs font-bold text-[#BDC2FF]">{po.poNumber}</td>
                    <td className="py-4 px-3 font-medium text-white">{po.vendor.name}</td>
                    <td className="py-4 px-3 font-mono font-semibold text-emerald-400 text-right">
                      ₹{Number(po.totalAmount).toLocaleString('en-IN')}
                    </td>
                    <td className="py-4 px-3 text-xs text-[#E0E3E5]">
                      {po.currency ?? 'INR'}
                    </td>
                    <td className="py-4 px-3">
                      <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border ${
                        po.status === 'ISSUED'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : 'bg-blue-500/10 text-blue-400 border-blue-500/20'
                      }`}>
                        <CheckCircle2 className="w-3 h-3" />
                        {po.status}
                      </span>
                    </td>
                    <td className="py-4 px-3 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <a
                          href={api.getPdfUrl(po.id)}
                          target="_blank"
                          rel="noreferrer"
                          className="px-3 py-1.5 bg-[#0B0D12] border border-[#1E2330] rounded-xl text-xs font-medium text-white hover:border-[#3E52FF] transition-all inline-flex items-center gap-1.5"
                        >
                          <Download className="w-3.5 h-3.5 text-[#3E52FF]" /> PDF
                        </a>

                        <button
                          onClick={() => handleSendPoEmail(po.id)}
                          disabled={actionLoading === po.id}
                          className="px-3 py-1.5 bg-[#3E52FF] text-white rounded-xl text-xs font-medium hover:opacity-90 transition-all inline-flex items-center gap-1.5 shadow-md shadow-blue-500/20"
                        >
                          {actionLoading === po.id ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                          Send PO via Email
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
