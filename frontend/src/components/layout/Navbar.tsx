import React from 'react';
import { Menu, Plus, Sun, Moon } from 'lucide-react';
import { Button } from '../ui/Button';
import { useTheme } from '../../context/ThemeContext';
import type { NavTab } from './Sidebar';

interface NavbarProps {
  activeTab: NavTab;
  onOpenMobileMenu: () => void;
  onOpenCreateTask: () => void;
}

const tabTitles: Record<NavTab, { title: string; subtitle: string }> = {
  dashboard: { title: 'Dashboard', subtitle: 'Monitor discovery tasks and manage the leads you\'ve collected.' },
  discover: { title: 'Discover Leads', subtitle: 'Search for businesses by location and category and collect publicly available business information.' },
  tasks: { title: 'Tasks', subtitle: 'Manage background discovery tasks and monitor progress.' },
  leads: { title: 'Leads', subtitle: 'View, search and manage discovered business leads.' },
  export: { title: 'Export', subtitle: 'Download structured CSV and Excel spreadsheets of your leads.' },
  settings: { title: 'Settings', subtitle: 'Manage your preferences and discovery defaults.' },
};

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  onOpenMobileMenu,
  onOpenCreateTask,
}) => {
  const { theme, toggleTheme } = useTheme();
  const currentTabInfo = tabTitles[activeTab] || tabTitles.dashboard;

  return (
    <header
      style={{
        height: '68px',
        backgroundColor: 'var(--header-bg)',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 1.75rem',
        position: 'sticky',
        top: 0,
        zIndex: 80,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', overflow: 'hidden' }}>
        <button
          onClick={onOpenMobileMenu}
          className="mobile-only-btn"
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-main)',
            cursor: 'pointer',
            padding: '0.375rem',
            display: 'none',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: '6px',
          }}
          aria-label="Toggle navigation menu"
        >
          <Menu size={22} />
        </button>

        <div>
          <h1 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-main)', lineHeight: 1.2 }}>
            {currentTabInfo.title}
          </h1>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {currentTabInfo.subtitle}
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        {/* Theme Toggle Button */}
        <button
          onClick={toggleTheme}
          title={theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
          style={{
            width: '36px',
            height: '36px',
            borderRadius: '8px',
            border: '1px solid var(--border-subtle)',
            backgroundColor: 'var(--bg-surface)',
            color: 'var(--text-secondary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
          aria-label="Toggle light/dark theme"
        >
          {theme === 'light' ? <Moon size={17} /> : <Sun size={17} />}
        </button>

        {/* Quick Launch Button */}
        <Button
          variant="indigo"
          size="sm"
          icon={<Plus size={16} />}
          onClick={onOpenCreateTask}
        >
          <span>Create Task</span>
        </Button>
      </div>
    </header>
  );
};
