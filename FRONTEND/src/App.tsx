import { Shield, Cpu, CheckCircle2, Server, Zap } from 'lucide-react';

export function App() {
  return (
    <div className="min-h-screen bg-[#0B0D12] text-[#F5F6F8] flex flex-col font-sans">
      {/* Top Navbar */}
      <header className="border-b border-[#242832] bg-[#12151C]/80 backdrop-blur-md px-6 py-4 sticky top-0 z-50 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-lg bg-[#4F7CFF] flex items-center justify-center text-white font-bold text-lg shadow-lg shadow-[#4F7CFF]/20">
            <Cpu className="w-5 h-5" />
          </div>
          <div>
            <h1 className="font-bold text-lg tracking-tight text-white flex items-center gap-2">
              ProcureAI <span className="text-xs px-2 py-0.5 rounded-full bg-[#4F7CFF]/15 text-[#4F7CFF] border border-[#4F7CFF]/30 font-mono">v1.0-scaffold</span>
            </h1>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <span className="text-xs px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1.5 font-medium">
            <CheckCircle2 className="w-3.5 h-3.5" /> Firebase & Railway Ready
          </span>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-6 py-12 flex flex-col gap-8">
        {/* Hero Section */}
        <section className="text-center py-8 space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#12151C] border border-[#242832] text-xs text-[#9AA1AE] mb-2">
            <Zap className="w-3.5 h-3.5 text-[#4F7CFF]" />
            AI Procurement Employee Architecture
          </div>
          <h2 className="text-4xl md:text-5xl font-extrabold tracking-tight bg-gradient-to-r from-white via-[#F5F6F8] to-[#9AA1AE] bg-clip-text text-transparent">
            From vendor quotes to purchase order — automatically.
          </h2>
          <p className="text-[#9AA1AE] max-w-2xl mx-auto text-base">
            ProcureAI automates the entire quote-to-PO lifecycle with AI-driven extraction, quote normalization, market benchmarking, and human-in-the-loop negotiations.
          </p>
        </section>

        {/* Status Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="p-6 rounded-xl bg-[#12151C] border border-[#242832] space-y-3 hover:border-[#4F7CFF]/50 transition-colors">
            <div className="w-10 h-10 rounded-lg bg-[#4F7CFF]/10 text-[#4F7CFF] flex items-center justify-center">
              <Cpu className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-lg text-white">Frontend Stack</h3>
            <p className="text-xs text-[#9AA1AE] leading-relaxed">
              React + Vite + TypeScript + Tailwind CSS with Lucide Icons, Recharts, Framer Motion, and Firebase SDK configured.
            </p>
          </div>

          <div className="p-6 rounded-xl bg-[#12151C] border border-[#242832] space-y-3 hover:border-emerald-500/50 transition-colors">
            <div className="w-10 h-10 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <Server className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-lg text-white">Backend Container</h3>
            <p className="text-xs text-[#9AA1AE] leading-relaxed">
              Java 17 + Spring Boot 3 + MySQL service configured with multi-stage Dockerfile and Railway.json deployment settings.
            </p>
          </div>

          <div className="p-6 rounded-xl bg-[#12151C] border border-[#242832] space-y-3 hover:border-purple-500/50 transition-colors">
            <div className="w-10 h-10 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center">
              <Shield className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-lg text-white">Safety & Control</h3>
            <p className="text-xs text-[#9AA1AE] leading-relaxed">
              Structured JSON enforcement, hard negotiation limits, deterministic scoring engine, and human approval workflow.
            </p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-[#242832] py-6 text-center text-xs text-[#6B7280]">
        ProcureAI — AI Procurement Platform
      </footer>
    </div>
  );
}

export default App;
