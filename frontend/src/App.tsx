import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { AppLayout } from './components/layout/AppLayout';
import type { NavTab } from './components/layout/Sidebar';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { DiscoverLeadsPage } from './pages/discover/DiscoverLeadsPage';
import { TasksPage } from './pages/tasks/TasksPage';
import { LeadsPage } from './pages/leads/LeadsPage';
import { ExportPage } from './pages/export/ExportPage';
import { SettingsPage } from './pages/settings/SettingsPage';
import { CreateTaskModal } from './pages/tasks/CreateTaskModal';

const MainApplication: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const [authView, setAuthView] = useState<'login' | 'register'>('login');
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');
  const [selectedTaskLeadsId, setSelectedTaskLeadsId] = useState<number | null>(null);
  const [isGlobalCreateTaskOpen, setIsGlobalCreateTaskOpen] = useState(false);

  if (isLoading) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'var(--bg-canvas)',
          color: 'var(--text-main)',
          gap: '1rem',
        }}
      >
        <span className="spinner" style={{ width: '36px', height: '36px', borderColor: 'rgba(99, 102, 241, 0.3)', borderTopColor: '#6366f1' }} />
        <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontWeight: 600 }}>
          Initializing Lead Discovery...
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    if (authView === 'register') {
      return <RegisterPage onSwitchToLogin={() => setAuthView('login')} />;
    }
    return <LoginPage onSwitchToRegister={() => setAuthView('register')} />;
  }

  const handleViewTaskLeads = (taskId: number) => {
    setSelectedTaskLeadsId(taskId);
    setActiveTab('leads');
  };

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return (
          <DashboardPage
            onOpenCreateTask={() => setIsGlobalCreateTaskOpen(true)}
            onNavigateTab={(tab) => {
              setActiveTab(tab);
            }}
          />
        );
      case 'discover':
        return (
          <DiscoverLeadsPage
            onViewTasks={() => setActiveTab('tasks')}
            onViewLeads={() => setActiveTab('leads')}
          />
        );
      case 'tasks':
        return <TasksPage onViewTaskLeads={handleViewTaskLeads} />;
      case 'leads':
        return <LeadsPage initialTaskId={selectedTaskLeadsId} />;
      case 'export':
        return <ExportPage />;
      case 'settings':
        return <SettingsPage />;
      default:
        return (
          <DashboardPage
            onOpenCreateTask={() => setIsGlobalCreateTaskOpen(true)}
            onNavigateTab={(tab) => setActiveTab(tab)}
          />
        );
    }
  };

  return (
    <>
      <AppLayout
        activeTab={activeTab}
        onTabChange={(tab) => {
          if (tab !== 'leads') {
            setSelectedTaskLeadsId(null);
          }
          setActiveTab(tab);
        }}
        onTaskCreated={() => {
          setActiveTab('tasks');
        }}
      >
        {renderContent()}
      </AppLayout>

      <CreateTaskModal
        isOpen={isGlobalCreateTaskOpen}
        onClose={() => setIsGlobalCreateTaskOpen(false)}
        onCreated={() => {
          setIsGlobalCreateTaskOpen(false);
          setActiveTab('tasks');
        }}
      />
    </>
  );
};

export function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <MainApplication />
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
