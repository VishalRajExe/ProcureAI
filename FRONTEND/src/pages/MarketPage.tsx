import { useState, useEffect } from 'react';
import { api } from '../api/client';
import type { MarketIntelligence } from '../types';
import { AlertTriangle, RefreshCw, ChevronDown, ChevronUp } from 'lucide-react';

const RISK_CONFIG: Record<string, { cls: string; label: string }> = {
  LOW: { cls: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20', label: '✓ Low Risk' },
  MEDIUM: { cls: 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20', label: '⚠ Medium Risk' },
  HIGH: { cls: 'text-red-400 bg-red-500/10 border-red-500/20', label: '✕ High Risk' },
};

function MarketCard({ category, data }: { category: string; data: MarketIntelligence }) {
  const [expanded, setExpanded] = useState(false);
  const risk = RISK_CONFIG[data.supplyChainRisk] ?? RISK_CONFIG['LOW'];
  const median = `₹${Number(data.medianPrice).toLocaleString('en-IN')}`;

  return (
    <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl overflow-hidden">
      <div
        className="p-5 cursor-pointer hover:bg-[#1A1E27] transition-colors"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-start justify-between mb-3">
          <div>
            <h3 className="font-semibold text-white">{data.category}</h3>
            <div className="text-xs text-[#6B7280] mt-0.5 capitalize">{category}</div>
          </div>
          <div className="flex items-center gap-2">
            <span className={`px-2.5 py-0.5 rounded-full text-xs border font-medium ${risk.cls}`}>
              {risk.label}
            </span>
            {expanded ? <ChevronUp className="w-4 h-4 text-[#4B5563]" /> : <ChevronDown className="w-4 h-4 text-[#4B5563]" />}
          </div>
        </div>

        <div className="grid grid-cols-3 gap-3">
          <div className="bg-[#0B0D12] rounded-xl p-3 text-center">
            <div className="text-xs text-[#6B7280] mb-1">Min</div>
            <div className="text-sm font-semibold text-emerald-400">₹{Number(data.minPrice).toLocaleString('en-IN')}</div>
          </div>
          <div className="bg-[#4F7CFF]/5 border border-[#4F7CFF]/20 rounded-xl p-3 text-center">
            <div className="text-xs text-[#6B7280] mb-1">Median</div>
            <div className="text-sm font-semibold text-white">{median}</div>
          </div>
          <div className="bg-[#0B0D12] rounded-xl p-3 text-center">
            <div className="text-xs text-[#6B7280] mb-1">Max</div>
            <div className="text-sm font-semibold text-red-400">₹{Number(data.maxPrice).toLocaleString('en-IN')}</div>
          </div>
        </div>
      </div>

      {expanded && (
        <div className="border-t border-[#1E2330] p-5 space-y-4">
          {data.trends.length > 0 && (
            <div>
              <div className="text-xs font-medium text-[#4F7CFF] mb-2">📈 Industry Trends</div>
              <ul className="space-y-1.5">
                {data.trends.map((t, i) => (
                  <li key={i} className="text-xs text-[#9AA1AE] flex items-start gap-2">
                    <span className="text-[#4F7CFF] mt-0.5 flex-shrink-0">→</span> {t}
                  </li>
                ))}
              </ul>
            </div>
          )}
          {data.supplyChainRisks.length > 0 && (
            <div>
              <div className="text-xs font-medium text-yellow-400 mb-2">⚠ Supply Chain Risks</div>
              <ul className="space-y-1.5">
                {data.supplyChainRisks.map((r, i) => (
                  <li key={i} className="text-xs text-[#9AA1AE] flex items-start gap-2">
                    <span className="text-yellow-400 mt-0.5 flex-shrink-0">•</span> {r}
                  </li>
                ))}
              </ul>
            </div>
          )}
          {data.competitorInsights.length > 0 && (
            <div>
              <div className="text-xs font-medium text-purple-400 mb-2">🔍 Competitor Insights</div>
              <ul className="space-y-1.5">
                {data.competitorInsights.map((c, i) => (
                  <li key={i} className="text-xs text-[#9AA1AE] flex items-start gap-2">
                    <span className="text-purple-400 mt-0.5 flex-shrink-0">•</span> {c}
                  </li>
                ))}
              </ul>
            </div>
          )}
          {data.regulatoryChanges.length > 0 && (
            <div>
              <div className="text-xs font-medium text-cyan-400 mb-2">📋 Regulatory Notes</div>
              <ul className="space-y-1.5">
                {data.regulatoryChanges.map((r, i) => (
                  <li key={i} className="text-xs text-[#9AA1AE] flex items-start gap-2">
                    <span className="text-cyan-400 mt-0.5 flex-shrink-0">•</span> {r}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export function MarketPage() {
  const [categories, setCategories] = useState<Record<string, MarketIntelligence>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.getMarketCategories();
      setCategories(data);
    } catch (e: any) {
      setError('Failed to load market intelligence data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Market Intelligence</h1>
          <p className="text-sm text-[#6B7280]">
            Real-time procurement market data — price ranges, supply chain risks, and industry trends.
          </p>
        </div>
        <button
          onClick={load}
          disabled={loading}
          className="p-2 bg-[#1E2330] border border-[#2A2F3E] rounded-xl text-[#9AA1AE] hover:text-white transition-colors"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/20 rounded-xl p-4 text-yellow-400 text-sm">
          <AlertTriangle className="w-4 h-4 flex-shrink-0" /> {error}
        </div>
      )}

      {/* Stats bar */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-[#12151C] border border-[#1E2330] rounded-xl p-4 text-center">
          <div className="text-2xl font-bold text-white">{Object.keys(categories).length}</div>
          <div className="text-xs text-[#6B7280]">Market Categories</div>
        </div>
        <div className="bg-[#12151C] border border-[#1E2330] rounded-xl p-4 text-center">
          <div className="text-2xl font-bold text-emerald-400">
            {Object.values(categories).filter(c => c.supplyChainRisk === 'LOW').length}
          </div>
          <div className="text-xs text-[#6B7280]">Low Risk Categories</div>
        </div>
        <div className="bg-[#12151C] border border-[#1E2330] rounded-xl p-4 text-center">
          <div className="text-2xl font-bold text-red-400">
            {Object.values(categories).filter(c => c.supplyChainRisk === 'HIGH').length}
          </div>
          <div className="text-xs text-[#6B7280]">High Risk Categories</div>
        </div>
      </div>

      {/* Market cards */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-5 animate-pulse h-40" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Object.entries(categories).map(([cat, data]) => (
            <MarketCard key={cat} category={cat} data={data} />
          ))}
        </div>
      )}
    </div>
  );
}
