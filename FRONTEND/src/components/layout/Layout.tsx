import { useState, useEffect } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, FileText, Scale, Handshake, ShieldCheck,
  Mail, ShoppingBag, TrendingUp, PlayCircle, Cpu, LogOut, ChevronRight, Menu, ChevronLeft, X
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
  const [isCollapsed, setIsCollapsed] = useState(() => {
    return localStorage.getItem('procureai_sidebar_collapsed') === 'true';
  });
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  const token = localStorage.getItem('procureai_token');

  useEffect(() => {
    if (!token) {
      navigate('/login');
    }
  }, [token, navigate]);

  if (!token) {
    return null;
  }

  let user: { name: string; email?: string; role: string } | null = null;
  try {
    const rawUser = localStorage.getItem('procureai_user');
    if (rawUser && rawUser !== 'undefined' && rawUser !== 'null') {
      user = JSON.parse(rawUser);
    }
  } catch (e) {
    user = null;
  }

  if (!user && localStorage.getItem('procureai_token')) {
    user = { name: 'Admin Officer', role: 'ADMIN' };
  }

  const handleLogout = () => {
    api.logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-[#0B0D12] text-[#F5F6F8] flex flex-col md:flex-row relative">
      {/* Mobile Backdrop Overlay */}
      {isMobileOpen && (
        <div
          onClick={() => setIsMobileOpen(false)}
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 md:hidden transition-opacity"
        />
      )}

      {/* Sidebar */}
      <aside
        className={clsx(
          "fixed inset-y-0 left-0 z-50 flex flex-col bg-[#0D1017] border-r border-[#1E2330] transition-all duration-300",
          "md:static md:translate-x-0 md:h-screen md:sticky md:top-0",
          isMobileOpen ? "translate-x-0" : "-translate-x-full",
          isCollapsed ? "w-64 md:w-16" : "w-64"
        )}
      >
        {/* Brand */}
        <div className={clsx("px-5 py-5 border-b border-[#1E2330] flex items-center justify-between gap-3 flex-shrink-0", isCollapsed ? "md:justify-center md:px-2" : "")}>
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#3E52FF] to-[#7C5CFF] flex items-center justify-center shadow-lg shadow-blue-500/20 flex-shrink-0">
              <Cpu className="w-5 h-5 text-white" />
            </div>
            <div className={clsx("transition-all duration-200", isCollapsed ? "md:hidden" : "block")}>
              <div className="font-bold text-white tracking-tight flex items-center gap-1.5">
                ProcureAI
                <span className="px-1.5 py-0.5 bg-[#3E52FF]/20 text-[#BDC2FF] text-[9px] rounded font-mono border border-[#3E52FF]/30">PRO</span>
              </div>
              <div className="text-[10px] text-[#8F8FA2] font-mono uppercase tracking-widest truncate">Autonomous Procurement</div>
            </div>
          </div>
          {/* Close Menu (Mobile only) */}
          <button
            onClick={() => setIsMobileOpen(false)}
            className="md:hidden p-1.5 rounded-lg bg-[#1E2330] hover:bg-rose-500/20 text-[#9AA1AE] hover:text-rose-400 transition-all flex-shrink-0"
            aria-label="Close menu"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Navigation List */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto" aria-label="Main Navigation">
          {navItems.map(({ to, label, icon: Icon, exact, highlight }) => (
            <NavLink
              key={to}
              to={to}
              end={exact}
              onClick={() => setIsMobileOpen(false)}
              title={isCollapsed ? label : undefined}
              className={({ isActive }) =>
                clsx(
                  'flex items-center rounded-xl text-sm font-medium transition-all group relative',
                  isCollapsed ? 'md:justify-center md:p-2.5' : 'gap-3 px-3 py-2.5',
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
              <span className={clsx("truncate", isCollapsed ? "md:hidden" : "block")}>{label}</span>
              {!isCollapsed && (
                <ChevronRight className="w-3 h-3 ml-auto opacity-0 group-hover:opacity-50 transition-opacity flex-shrink-0" />
              )}
            </NavLink>
          ))}
        </nav>

        {/* Desktop Collapse Toggle */}
        <div className="hidden md:block px-3 py-2 border-t border-[#1E2330] flex-shrink-0">
          <button
            onClick={() => {
              const nextVal = !isCollapsed;
              setIsCollapsed(nextVal);
              localStorage.setItem('procureai_sidebar_collapsed', String(nextVal));
            }}
            className={clsx(
              "w-full flex items-center rounded-xl text-xs font-medium text-[#9AA1AE] hover:bg-[#1E2330] hover:text-white transition-all",
              isCollapsed ? "justify-center p-2.5" : "gap-3 px-3 py-2"
            )}
            title={isCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
          >
            {isCollapsed ? (
              <ChevronRight className="w-4 h-4" />
            ) : (
              <>
                <ChevronLeft className="w-4 h-4" />
                <span>Collapse Navigation</span>
              </>
            )}
          </button>
        </div>

        {/* User Footer */}
        <div className="border-t border-[#1E2330] p-4 bg-[#0B0D12]/50 flex-shrink-0">
          {user ? (
            <div className={clsx("flex items-center gap-3", isCollapsed ? "md:flex-col md:justify-center md:gap-2" : "")}>
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#3E52FF] to-purple-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0 shadow-md">
                {user.name?.[0]?.toUpperCase() ?? 'A'}
              </div>
              <div className={clsx("flex-1 min-w-0", isCollapsed ? "md:hidden" : "block")}>
                <div className="text-sm font-medium text-white truncate">{user.name}</div>
                <div className="text-xs text-emerald-400 font-mono flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" /> {user.role}
                </div>
              </div>
              <button
                onClick={handleLogout}
                className={clsx("text-[#8F8FA2] hover:text-rose-400 transition-colors p-1", isCollapsed ? "md:mt-1" : "")}
                title="Logout"
                aria-label="Logout"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => {
                setIsMobileOpen(false);
                navigate('/login');
              }}
              className={clsx(
                "w-full bg-[#1E2330] rounded-xl text-sm font-medium text-[#BDC2FF] hover:text-white hover:bg-[#3E52FF]/20 transition-all text-center border border-[#1E2330]",
                isCollapsed ? "p-2" : "py-2"
              )}
            >
              {isCollapsed ? <LogOut className="w-4 h-4 mx-auto" /> : "Sign In"}
            </button>
          )}
        </div>
      </aside>

      {/* Main Content Wrapper */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Mobile Header */}
        <header className="h-14 border-b border-[#1E2330] bg-[#0D1017] flex items-center justify-between px-4 md:hidden sticky top-0 z-40 flex-shrink-0">
          <button
            onClick={() => setIsMobileOpen(true)}
            className="p-1.5 rounded-lg bg-[#1E2330] text-[#9AA1AE] hover:text-white"
            aria-label="Open navigation menu"
          >
            <Menu className="w-5 h-5" />
          </button>
          <span className="font-bold text-white tracking-tight flex items-center gap-1.5">
            ProcureAI
            <span className="px-1.5 py-0.5 bg-[#3E52FF]/20 text-[#BDC2FF] text-[9px] rounded font-mono border border-[#3E52FF]/30">PRO</span>
          </span>
          <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#3E52FF] to-purple-600 flex items-center justify-center text-white text-xs font-bold">
            {user?.name?.[0]?.toUpperCase() ?? 'A'}
          </div>
        </header>

        {/* Main Content Viewport */}
        <main className="flex-1 overflow-y-auto" role="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
