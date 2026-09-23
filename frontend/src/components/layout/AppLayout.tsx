import React, { useState } from 'react';
import { Sidebar } from './Sidebar';
import type { NavTab } from './Sidebar';
import { Navbar } from './Navbar';
import { CreateTaskModal } from '../../pages/tasks/CreateTaskModal';

interface AppLayoutProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  onTaskCreated?: () => void;
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({
  activeTab,
  onTabChange,
  onTaskCreated,
  children,
}) => {
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isCreateTaskOpen, setIsCreateTaskOpen] = useState(false);

  const handleTaskCreated = () => {
    setIsCreateTaskOpen(false);
    if (onTaskCreated) onTaskCreated();
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', backgroundColor: 'var(--bg-canvas)' }}>
      {/* Sidebar */}
      <Sidebar
        activeTab={activeTab}
        onTabChange={onTabChange}
        isMobileOpen={isMobileOpen}
        onCloseMobile={() => setIsMobileOpen(false)}
      />

      {/* Main Content Area */}
      <div
        style={{
          flex: 1,
          marginLeft: '260px',
          display: 'flex',
          flexDirection: 'column',
          minWidth: 0,
          transition: 'margin-left 0.25s ease',
        }}
        className="app-main-content"
      >
        <Navbar
          activeTab={activeTab}
          onOpenMobileMenu={() => setIsMobileOpen(true)}
          onOpenCreateTask={() => setIsCreateTaskOpen(true)}
        />

        <main
          style={{
            flex: 1,
            padding: '1.75rem',
            maxWidth: '1440px',
            width: '100%',
            margin: '0 auto',
            boxSizing: 'border-box',
          }}
        >
          {children}
        </main>
      </div>

      {/* Global Create Task Modal */}
      <CreateTaskModal
        isOpen={isCreateTaskOpen}
        onClose={() => setIsCreateTaskOpen(false)}
        onCreated={handleTaskCreated}
      />
    </div>
  );
};
