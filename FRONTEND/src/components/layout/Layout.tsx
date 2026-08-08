import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, GitBranch, PlayCircle, TrendingUp,
  Cpu, LogOut, ChevronRight
} from 'lucide-react';
import { clsx } from 'clsx';
import { api } from '../../api/client';

const navItems = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, exact: true },
  { to: '/workflows', label: 'Workflows', icon: GitBranch },
  { to: '/market', label: 'Market Intel', icon: TrendingUp },
  { to: '/demo', label: 'Run Demo', icon: PlayCircle },
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
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#4F7CFF] to-[#7C5CFF] flex items-center justify-center shadow-lg shadow-blue-500/20 flex-shrink-0">
            <Cpu className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="font-bold text-white tracking-tight">ProcureAI</div>
            <div className="text-[10px] text-[#4F7CFF] font-mono uppercase tracking-widest">Procurement Agent</div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map(({ to, label, icon: Icon, exact }) => (
            <NavLink
              key={to}
              to={to}
              end={exact}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all group',
                  isActive
                    ? 'bg-[#4F7CFF]/15 text-[#4F7CFF] border border-[#4F7CFF]/20'
                    : 'text-[#9AA1AE] hover:bg-[#1E2330] hover:text-white'
                )
              }
            >
              <Icon className="w-4 h-4 flex-shrink-0" />
              <span>{label}</span>
              <ChevronRight className="w-3 h-3 ml-auto opacity-0 group-hover:opacity-50 transition-opacity" />
            </NavLink>
          ))}
        </nav>

        {/* User footer */}
        <div className="border-t border-[#1E2330] p-4">
          {user ? (
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#4F7CFF] to-purple-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                {user.name?.[0]?.toUpperCase() ?? 'U'}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-white truncate">{user.name}</div>
                <div className="text-xs text-[#6B7280] truncate">{user.role}</div>
              </div>
              <button
                onClick={handleLogout}
                className="text-[#6B7280] hover:text-red-400 transition-colors"
                title="Logout"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => navigate('/login')}
              className="w-full text-sm text-[#4F7CFF] hover:text-white transition-colors"
            >
              Sign In
            </button>
          )}
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}
