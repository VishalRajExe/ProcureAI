import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Eye, EyeOff, AlertCircle, UserCheck, Lock } from 'lucide-react';

export function LoginPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register' | 'forgot'>('login');
  const [forgotStep, setForgotStep] = useState<'email' | 'reset'>('email');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [name, setName] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    if (localStorage.getItem('procureai_token')) {
      navigate('/');
    }
  }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      if (mode === 'login') {
        await api.login(email, password);
        navigate('/');
      } else if (mode === 'register') {
        await api.register(email, name, password);
        navigate('/');
      } else if (mode === 'forgot') {
        if (forgotStep === 'email') {
          const res = await api.forgotPassword(email);
          setSuccessMsg(res?.message || 'A 6-digit verification code has been sent via Brevo to your email.');
          setForgotStep('reset');
        } else {
          const res = await api.resetPassword(email, otp, newPassword);
          setSuccessMsg(res?.message || 'Your password has been successfully reset! Please sign in.');
          setMode('login');
          setForgotStep('email');
          setPassword('');
          setOtp('');
        }
      }
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
              onClick={() => { setMode('login'); setError(''); setSuccessMsg(''); }}
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
              onClick={() => { setMode('register'); setError(''); setSuccessMsg(''); }}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                mode === 'register'
                  ? 'bg-[#3E52FF] text-white shadow-md'
                  : 'text-[#8F8FA2] hover:text-white'
              }`}
            >
              Create Account
            </button>
          </div>

          {mode === 'forgot' && (
            <div className="flex items-center justify-between border-b border-[#1E2330] pb-3">
              <span className="text-xs font-semibold text-white font-mono flex items-center gap-1.5">
                <Lock className="w-3.5 h-3.5 text-[#3E52FF]" /> Password Recovery
              </span>
              <button
                type="button"
                onClick={() => { setMode('login'); setError(''); setSuccessMsg(''); }}
                className="text-xs font-mono text-[#3E52FF] hover:underline"
              >
                ← Back to Sign In
              </button>
            </div>
          )}

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

            {(mode === 'login' || mode === 'register' || (mode === 'forgot' && forgotStep === 'email')) && (
              <div className="space-y-1.5">
                <label htmlFor="auth-email" className="block text-xs font-medium text-[#8F8FA2]">
                  {mode === 'forgot' ? 'Account Email Address' : 'Email Address'}
                </label>
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
            )}

            {(mode === 'login' || mode === 'register') && (
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="auth-password" className="block text-xs font-medium text-[#8F8FA2]">Password</label>
                  {mode === 'login' && (
                    <button
                      type="button"
                      onClick={() => { setMode('forgot'); setForgotStep('email'); setError(''); setSuccessMsg(''); }}
                      className="text-xs text-[#BDC2FF] hover:text-white transition-colors"
                    >
                      Forgot?
                    </button>
                  )}
                </div>
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
            )}

            {mode === 'forgot' && forgotStep === 'reset' && (
              <>
                <div className="space-y-1.5">
                  <label htmlFor="auth-otp" className="block text-xs font-medium text-[#8F8FA2]">
                    6-Digit Verification Code (OTP)
                  </label>
                  <input
                    id="auth-otp"
                    type="text"
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value)}
                    placeholder="123456"
                    required
                    className="w-full bg-[#0B0D12] border border-[#3E52FF]/50 rounded-xl px-4 py-2.5 text-center font-mono tracking-widest text-base text-white placeholder-[#525866] focus:outline-none focus:border-[#3E52FF] transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label htmlFor="new-password" className="block text-xs font-medium text-[#8F8FA2]">
                    New Password
                  </label>
                  <input
                    id="new-password"
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Minimum 8 characters"
                    required
                    minLength={8}
                    className="w-full bg-[#0B0D12] border border-[#1E2330] rounded-xl px-4 py-2.5 text-sm text-white placeholder-[#525866] focus:outline-none focus:border-[#3E52FF] transition-all"
                  />
                </div>
              </>
            )}

            {successMsg && (
              <div className="flex items-center gap-2 text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-3.5 text-xs font-mono">
                <span>{successMsg}</span>
              </div>
            )}

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
                <span>Processing...</span>
              ) : mode === 'login' ? (
                <>
                  <Lock className="w-4 h-4" /> Sign In to ProcureAI
                </>
              ) : mode === 'register' ? (
                <>
                  <UserCheck className="w-4 h-4" /> Register Account
                </>
              ) : forgotStep === 'email' ? (
                <>
                  <Lock className="w-4 h-4" /> Send Reset OTP Code via Brevo
                </>
              ) : (
                <>
                  <UserCheck className="w-4 h-4" /> Verify OTP & Reset Password
                </>
              )}
            </button>
          </form>
        </div>

        {/* Footer info */}
        <div className="text-center text-[11px] text-[#8F8FA2] font-mono">
          ProcureAI Multi-Agent System • Security Hardened Java & React Architecture
        </div>
      </div>
    </div>
  );
}
