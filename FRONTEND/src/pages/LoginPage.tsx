import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Eye, EyeOff, AlertCircle, ShieldCheck, UserCheck, Lock } from 'lucide-react';

export function LoginPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('admin@procureai.demo');
  const [password, setPassword] = useState('Admin@12345');
  const [name, setName] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (localStorage.getItem('procureai_token')) {
      navigate('/');
    }
  }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (mode === 'login') {
        await api.login(email, password);
      } else {
        await api.register(email, name, password);
      }
      navigate('/');
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        err.message ||
        'Authentication failed. Please check your credentials.'
      );
    } finally {
      setLoading(false);
    }
  };

  const quickFill = (preset: 'admin' | 'approver') => {
    if (preset === 'admin') { setEmail('admin@procureai.demo'); setPassword('Admin@12345'); }
    else { setEmail('approver@procureai.demo'); setPassword('Approver@12345'); }
  };

  return (
    <div className="min-h-screen bg-[#0B0D12] text-[#F5F6F8] flex flex-col items-center justify-center p-4 relative overflow-hidden">
      {/* Ambient background glow */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-[#3E52FF]/15 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-10 right-10 w-72 h-72 bg-indigo-600/10 rounded-full blur-[100px] pointer-events-none" />

      <div className="w-full max-w-md relative z-10 space-y-6">
        {/* Brand Header */}
        <div className="text-center space-y-2 flex flex-col items-center">
          <img
            src="/logo.png"
            alt="ProcureAI Logo"
            className="w-20 h-20 object-contain drop-shadow-2xl mb-1 transition-transform hover:scale-105"
          />
          <h1 className="text-3xl font-extrabold text-white tracking-tight">ProcureAI</h1>
          <p className="text-xs font-mono text-[#BDC2FF] uppercase tracking-widest">
            Autonomous Enterprise Procurement System
          </p>
        </div>

        {/* Auth Card Container */}
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6 backdrop-blur-xl">

          {/* Mode Tab Switcher */}
          <div className="flex rounded-xl bg-[#0B0D12] p-1 border border-[#1E2330]">
            <button
              type="button"
              onClick={() => setMode('login')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                mode === 'login'
                  ? 'bg-[#3E52FF] text-white shadow-md'
                  : 'text-[#8F8FA2] hover:text-white'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => setMode('register')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                mode === 'register'
                  ? 'bg-[#3E52FF] text-white shadow-md'
                  : 'text-[#8F8FA2] hover:text-white'
              }`}
            >
              Create Account
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === 'register' && (
              <div className="space-y-1.5">
                <label htmlFor="reg-name" className="block text-xs font-medium text-[#8F8FA2]">Full Name</label>
                <input
                  id="reg-name"
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Vishal Raj"
                  required={mode === 'register'}
                  className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 text-sm text-white placeholder-[#525866] focus:outline-none focus:border-[#3E52FF] transition-all"
                />
              </div>
            )}

            <div className="space-y-1.5">
              <label htmlFor="auth-email" className="block text-xs font-medium text-[#8F8FA2]">Email Address</label>
              <input
                id="auth-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@procureai.demo"
                required
                className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 text-sm text-white placeholder-[#525866] focus:outline-none focus:border-[#3E52FF] transition-all"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="auth-password" className="block text-xs font-medium text-[#8F8FA2]">Password</label>
              <div className="relative">
                <input
                  id="auth-password"
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  required
                  className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 pr-10 text-sm text-white placeholder-[#525866] focus:outline-none focus:border-[#3E52FF] transition-all"
                />
                <button
                  type="button"
                  aria-label={showPass ? "Hide password" : "Show password"}
                  onClick={() => setShowPass(!showPass)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8F8FA2] hover:text-white transition-colors p-1"
                >
                  {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 text-rose-400 bg-rose-500/10 border border-rose-500/20 rounded-xl p-3.5 text-xs font-mono">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-gradient-to-r from-[#3E52FF] to-indigo-600 hover:opacity-95 text-white font-semibold text-sm rounded-xl shadow-lg shadow-blue-500/25 transition-all disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {loading ? (
                <span>Authenticating...</span>
              ) : mode === 'login' ? (
                <>
                  <Lock className="w-4 h-4" /> Sign In to ProcureAI
                </>
              ) : (
                <>
                  <UserCheck className="w-4 h-4" /> Register Account
                </>
              )}
            </button>
          </form>

          {/* Quick preset account pills */}
          {mode === 'login' && (
            <div className="pt-4 border-t border-[#1E2330] space-y-2.5">
              <div className="flex items-center justify-between text-[11px] text-[#8F8FA2] font-mono uppercase tracking-wider">
                <span>Quick Preset Credentials</span>
                <ShieldCheck className="w-3.5 h-3.5 text-[#3E52FF]" />
              </div>
              <div className="grid grid-cols-2 gap-2.5">
                <button
                  type="button"
                  onClick={() => quickFill('admin')}
                  className="py-2 px-3 bg-[#0B0D12] border border-[#1E2330] hover:border-[#3E52FF]/50 rounded-xl text-xs font-medium text-[#BDC2FF] hover:text-white transition-all text-center"
                >
                  Admin Officer
                </button>
                <button
                  type="button"
                  onClick={() => quickFill('approver')}
                  className="py-2 px-3 bg-[#0B0D12] border border-[#1E2330] hover:border-purple-500/50 rounded-xl text-xs font-medium text-purple-300 hover:text-white transition-all text-center"
                >
                  Approver Officer
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer info */}
        <div className="text-center text-[11px] text-[#8F8FA2] font-mono">
          ProcureAI Multi-Agent System • Security Hardened Java & React Architecture
        </div>
      </div>
    </div>
  );
}
