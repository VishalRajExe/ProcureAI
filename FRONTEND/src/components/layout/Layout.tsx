import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, FileText, Scale, Handshake, ShieldCheck,
  Mail, ShoppingBag, TrendingUp, PlayCircle, Cpu, LogOut, ChevronRight
} from 'lucide-react';
import { clsx } from 'clsx';
import { api } from '../../api/client';

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, exact: true },
  { to: '/quotes', label: 'Quotes & Ingestion', icon: FileText },
  { to: '/comparison', label: 'Quote Comparison', icon: Scale },
  { to: '/negotiation', label: 'AI Negotiation Center', icon: Handshake },
  { to: '/approvals', label: 'Human Approvals', icon: ShieldCheck },
  { to: '/vendor-inbox', label: 'Vendor Inbox & Simulator', icon: Mail },
  { to: '/purchase-orders', label: 'Purchase Orders', icon: ShoppingBag },
  { to: '/market', label: 'Market Intelligence', icon: TrendingUp },
  { to: '/demo', label: 'Run Full Demo', icon: PlayCircle, highlight: true },
];

export function Layout() {
  const navigate = useNavigate();
  const rawUser = localStorage.getItem('procureai_user');
  const user = rawUser ? JSON.parse(rawUser) : null;

  const handleLogout = () => {
    api.logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-[#0B0D12] text-[#F5F6F8] flex">
      {/* Sidebar */}
      <aside className="w-64 flex-shrink-0 border-r border-[#1E2330] bg-[#0D1017] flex flex-col sticky top-0 h-screen">
        {/* Brand */}
        <div className="px-5 py-5 border-b border-[#1E2330] flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#3E52FF] to-[#7C5CFF] flex items-center justify-center shadow-lg shadow-blue-500/20 flex-shrink-0">
            <Cpu className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="font-bold text-white tracking-tight flex items-center gap-1.5">
              ProcureAI
              <span className="px-1.5 py-0.2 bg-[#3E52FF]/20 text-[#BDC2FF] text-[9px] rounded font-mono border border-[#3E52FF]/30">PRO</span>
            </div>
            <div className="text-[10px] text-[#8F8FA2] font-mono uppercase tracking-widest">Autonomous Procurement</div>
          </div>
        </div>

        {/* Navigation List */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(({ to, label, icon: Icon, exact, highlight }) => (
            <NavLink
              key={to}
              to={to}
              end={exact}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all group',
                  highlight
                    ? isActive
                      ? 'bg-gradient-to-r from-[#3E52FF] to-indigo-600 text-white shadow-lg shadow-blue-500/25'
                      : 'bg-[#3E52FF]/10 text-[#BDC2FF] border border-[#3E52FF]/30 hover:bg-[#3E52FF]/20 hover:text-white'
                    : isActive
                    ? 'bg-[#3E52FF]/15 text-[#BDC2FF] border border-[#3E52FF]/25 shadow-sm'
                    : 'text-[#9AA1AE] hover:bg-[#1E2330] hover:text-white'
                )
              }
            >
              <Icon className={clsx('w-4 h-4 flex-shrink-0', highlight && 'text-[#BDC2FF]')} />
              <span className="truncate">{label}</span>
              <ChevronRight className="w-3 h-3 ml-auto opacity-0 group-hover:opacity-50 transition-opacity flex-shrink-0" />
            </NavLink>
          ))}
        </nav>

        {/* User Footer */}
        <div className="border-t border-[#1E2330] p-4 bg-[#0B0D12]/50">
          {user ? (
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#3E52FF] to-purple-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0 shadow-md">
                {user.name?.[0]?.toUpperCase() ?? 'U'}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-white truncate">{user.name}</div>
                <div className="text-xs text-[#8F8FA2] truncate">{user.role}</div>
              </div>
              <button
                onClick={handleLogout}
                className="text-[#8F8FA2] hover:text-rose-400 transition-colors p-1"
                title="Logout"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => navigate('/login')}
              className="w-full py-2 bg-[#1E2330] rounded-xl text-sm font-medium text-[#BDC2FF] hover:text-white hover:bg-[#3E52FF]/20 transition-all text-center border border-[#1E2330]"
            >
              Sign In
            </button>
          )}
        </div>
      </aside>

      {/* Main Content Viewport */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}
