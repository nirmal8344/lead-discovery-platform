import React from 'react';
import {
  LayoutDashboard,
  Search,
  ListTodo,
  Users,
  Download,
  LogOut,
  Settings,
  X,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export type NavTab = 'dashboard' | 'discover' | 'tasks' | 'leads' | 'export' | 'settings';

interface SidebarProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  isMobileOpen?: boolean;
  onCloseMobile?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  isMobileOpen = false,
  onCloseMobile,
}) => {
  const { user, logout } = useAuth();

  const navItems: { id: NavTab; label: string; icon: React.ReactNode }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard size={18} /> },
    { id: 'discover', label: 'Discover Leads', icon: <Search size={18} /> },
    { id: 'tasks', label: 'Tasks', icon: <ListTodo size={18} /> },
    { id: 'leads', label: 'Leads', icon: <Users size={18} /> },
    { id: 'export', label: 'Export', icon: <Download size={18} /> },
    { id: 'settings', label: 'Settings', icon: <Settings size={18} /> },
  ];

  const handleNavClick = (tab: NavTab) => {
    onTabChange(tab);
    if (onCloseMobile) onCloseMobile();
  };

  return (
    <>
      {/* Mobile background overlay */}
      {isMobileOpen && (
        <div
          onClick={onCloseMobile}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.6)',
            backdropFilter: 'blur(2px)',
            zIndex: 90,
          }}
          aria-hidden="true"
        />
      )}

      <aside
        style={{
          width: '260px',
          height: '100vh',
          backgroundColor: 'var(--sidebar-bg)',
          color: 'var(--sidebar-text-active)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          borderRight: '1px solid var(--sidebar-border)',
          position: 'fixed',
          top: 0,
          left: 0,
          zIndex: 100,
          transition: 'transform 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
        }}
        className={`sidebar-nav ${isMobileOpen ? 'open' : ''}`}
      >
        {/* Top Header & Branding */}
        <div>
          <div
            style={{
              padding: '1.25rem 1.25rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderBottom: '1px solid var(--sidebar-border)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div
                style={{
                  width: '34px',
                  height: '34px',
                  borderRadius: '10px',
                  background: 'var(--accent-indigo)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#ffffff',
                }}
              >
                <Search size={18} />
              </div>
              <div>
                <div style={{ fontWeight: 800, fontSize: '1rem', letterSpacing: '-0.02em', color: '#ffffff' }}>
                  Lead Discovery
                </div>
                <div style={{ fontSize: '0.6875rem', color: '#94a3b8' }}>Business Intelligence</div>
              </div>
            </div>

            {/* Mobile close button */}
            {isMobileOpen && (
              <button
                onClick={onCloseMobile}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#94a3b8',
                  cursor: 'pointer',
                  padding: '0.25rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
                aria-label="Close menu"
              >
                <X size={20} />
              </button>
            )}
          </div>

          {/* Navigation Menu */}
          <nav style={{ padding: '1rem 0.75rem' }}>
            <div
              style={{
                fontSize: '0.6875rem',
                fontWeight: 700,
                color: '#64748b',
                padding: '0.5rem 0.75rem',
                textTransform: 'uppercase',
                letterSpacing: '0.08em',
              }}
            >
              Navigation
            </div>

            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
              {navItems.map((item) => {
                const isActive = activeTab === item.id;
                return (
                  <li key={item.id}>
                    <button
                      onClick={() => handleNavClick(item.id)}
                      style={{
                        width: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.75rem',
                        padding: '0.625rem 0.875rem',
                        borderRadius: '8px',
                        border: 'none',
                        backgroundColor: isActive ? 'var(--sidebar-nav-active-bg)' : 'transparent',
                        color: isActive ? '#ffffff' : '#94a3b8',
                        cursor: 'pointer',
                        fontWeight: isActive ? 600 : 500,
                        fontSize: '0.875rem',
                        transition: 'all 0.15s ease',
                        borderLeft: isActive ? '3px solid var(--accent-indigo)' : '3px solid transparent',
                        textAlign: 'left',
                      }}
                      onMouseEnter={(e) => {
                        if (!isActive) {
                          e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.05)';
                          e.currentTarget.style.color = '#ffffff';
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (!isActive) {
                          e.currentTarget.style.backgroundColor = 'transparent';
                          e.currentTarget.style.color = '#94a3b8';
                        }
                      }}
                    >
                      <span style={{ color: isActive ? 'var(--accent-indigo)' : '#64748b' }}>{item.icon}</span>
                      <span>{item.label}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </nav>
        </div>

        {/* User Profile & Sign Out Footer */}
        <div
          style={{
            padding: '1rem',
            borderTop: '1px solid var(--sidebar-border)',
            backgroundColor: 'rgba(0, 0, 0, 0.2)',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.5rem',
              borderRadius: '8px',
              backgroundColor: 'rgba(255, 255, 255, 0.03)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', overflow: 'hidden' }}>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  backgroundColor: '#334155',
                  color: '#f8fafc',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.8125rem',
                  fontWeight: 700,
                  flexShrink: 0,
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                }}
              >
                {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
              </div>
              <div style={{ overflow: 'hidden' }}>
                <div
                  style={{
                    fontSize: '0.8125rem',
                    fontWeight: 600,
                    color: '#ffffff',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {user?.name || 'User'}
                </div>
                <div
                  style={{
                    fontSize: '0.6875rem',
                    color: '#94a3b8',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                >
                  {user?.email}
                </div>
              </div>
            </div>
            <button
              onClick={logout}
              title="Sign out"
              style={{
                background: 'none',
                border: 'none',
                color: '#94a3b8',
                cursor: 'pointer',
                padding: '0.375rem',
                borderRadius: '6px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.color = '#ef4444';
                e.currentTarget.style.backgroundColor = 'rgba(239, 68, 68, 0.1)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.color = '#94a3b8';
                e.currentTarget.style.backgroundColor = 'transparent';
              }}
              aria-label="Sign out"
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};
