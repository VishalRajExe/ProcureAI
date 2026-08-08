import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Cpu, Eye, EyeOff, AlertCircle } from 'lucide-react';

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
    // Auto-redirect if already logged in
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
      setError(err?.response?.data?.message ?? err?.message ?? 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const quickFill = (preset: 'admin' | 'approver') => {
    if (preset === 'admin') { setEmail('admin@procureai.demo'); setPassword('Admin@12345'); }
    else { setEmail('approver@procureai.demo'); setPassword('Approver@12345'); }
  };

  return (
    <div className="min-h-screen bg-[#0B0D12] flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-[#4F7CFF] to-purple-600 shadow-2xl shadow-blue-500/30 mb-4">
            <Cpu className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white">ProcureAI</h1>
          <p className="text-[#6B7280] text-sm mt-1">AI-Powered Procurement Platform</p>
        </div>

        {/* Card */}
        <div className="bg-[#12151C] border border-[#1E2330] rounded-2xl p-6 shadow-2xl">
          {/* Tab switch */}
          <div className="flex rounded-xl bg-[#0B0D12] p-1 mb-6">
            {(['login', 'register'] as const).map((m) => (
              <button
                key={m}
                onClick={() => setMode(m)}
                className={`flex-1 py-2 text-sm font-medium rounded-lg transition-all capitalize ${
                  mode === m
                    ? 'bg-[#4F7CFF] text-white shadow-lg'
                    : 'text-[#6B7280] hover:text-white'
                }`}
              >
                {m}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === 'register' && (
              <div>
                <label className="block text-xs font-medium text-[#9AA1AE] mb-1.5">Full Name</label>
                <input
                  value={name}
                  onChange={e => setName(e.target.value)}
                  placeholder="John Doe"
                  required={mode === 'register'}
                  className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 text-sm text-white placeholder-[#4B5563] focus:outline-none focus:border-[#4F7CFF] transition-colors"
                />
              </div>
            )}
            <div>
              <label className="block text-xs font-medium text-[#9AA1AE] mb-1.5">Email</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
                className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 text-sm text-white placeholder-[#4B5563] focus:outline-none focus:border-[#4F7CFF] transition-colors"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-[#9AA1AE] mb-1.5">Password</label>
              <div className="relative">
                <input
                  type={showPass ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 pr-10 text-sm text-white placeholder-[#4B5563] focus:outline-none focus:border-[#4F7CFF] transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPass(!showPass)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#4B5563] hover:text-white transition-colors"
                >
                  {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 text-red-400 bg-red-500/10 border border-red-500/20 rounded-xl px-4 py-3 text-sm">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-[#4F7CFF] to-purple-600 text-white font-semibold py-3 rounded-xl hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed text-sm"
            >
              {loading ? 'Please wait...' : mode === 'login' ? 'Sign In' : 'Create Account'}
            </button>
          </form>

          {/* Demo credentials */}
          {mode === 'login' && (
            <div className="mt-5 pt-4 border-t border-[#1E2330]">
              <p className="text-xs text-[#6B7280] text-center mb-3">Quick demo accounts</p>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => quickFill('admin')}
                  className="text-xs py-2 px-3 rounded-xl bg-[#1E2330] text-[#9AA1AE] hover:bg-[#4F7CFF]/10 hover:text-[#4F7CFF] transition-all border border-transparent hover:border-[#4F7CFF]/30"
                >
                  Admin Account
                </button>
                <button
                  onClick={() => quickFill('approver')}
                  className="text-xs py-2 px-3 rounded-xl bg-[#1E2330] text-[#9AA1AE] hover:bg-purple-500/10 hover:text-purple-400 transition-all border border-transparent hover:border-purple-500/30"
                >
                  Approver Account
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
