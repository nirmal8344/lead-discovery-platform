import React, { useEffect, useState } from 'react';
import {
  Users,
  CheckCircle2,
  ListTodo,
  Activity,
  Plus,
  Play,
  ArrowRight,
} from 'lucide-react';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { leadsApi, tasksApi } from '../../api/client';
import type { LeadListItem, ScrapingTask } from '../../types';
import { TaskProgressModal } from '../tasks/TaskProgressModal';
import { LeadDetailsModal } from '../leads/LeadDetailsModal';

interface DashboardPageProps {
  onOpenCreateTask: () => void;
  onNavigateTab: (tab: 'discover' | 'tasks' | 'leads' | 'export') => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  onOpenCreateTask,
  onNavigateTab,
}) => {
  const [tasks, setTasks] = useState<ScrapingTask[]>([]);
  const [recentLeads, setRecentLeads] = useState<LeadListItem[]>([]);
  const [totalLeadsCount, setTotalLeadsCount] = useState<number>(0);
  const [verifiedLeadsCount, setVerifiedLeadsCount] = useState<number>(0);
  const [totalTasksCount, setTotalTasksCount] = useState<number>(0);
  const [activeJobsCount, setActiveJobsCount] = useState<number>(0);

  // Modals state
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [selectedLeadId, setSelectedLeadId] = useState<number | null>(null);

  const fetchDashboardData = async () => {
    try {
      const [tasksPage, leadsPage, verifiedPage] = await Promise.all([
        tasksApi.listTasks(0, 5),
        leadsApi.listLeads({ page: 0, size: 5, sortBy: 'createdAt', sortDirection: 'DESC' }),
        leadsApi.listLeads({ page: 0, size: 1, verificationStatus: 'VERIFIED' }),
      ]);

      setTasks(tasksPage.content || []);
      setTotalTasksCount(tasksPage.totalElements || 0);

      const activeCount = (tasksPage.content || []).filter(
        (t) => t.status === 'RUNNING' || t.status === 'QUEUED'
      ).length;
      setActiveJobsCount(activeCount);

      setRecentLeads(leadsPage.content || []);
      setTotalLeadsCount(leadsPage.totalElements || 0);
      setVerifiedLeadsCount(verifiedPage.totalElements || 0);
    } catch (err) {
      console.error('Failed to load dashboard metrics', err);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleStartTask = async (taskId: number) => {
    try {
      await tasksApi.startTask(taskId);
      setSelectedTaskId(taskId);
      fetchDashboardData();
    } catch (err) {
      console.error('Error starting task', err);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem' }}>
      {/* Main Section Hero */}
      <div
        className="hero-banner"
        style={{
          background: 'var(--hero-bg)',
          borderRadius: '16px',
          padding: '2rem 2.25rem',
          color: 'var(--hero-text)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1.5rem',
          boxShadow: 'var(--shadow-md)',
          border: '1px solid var(--border-subtle)',
        }}
      >
        <div style={{ maxWidth: '620px' }}>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.025em', marginBottom: '0.5rem', lineHeight: 1.25, color: '#ffffff' }}>
            Find Business Leads
          </h2>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8', lineHeight: 1.6, margin: 0 }}>
            Search for businesses by location and category, discover their official websites, and collect available contact information.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Button
            variant="indigo"
            size="md"
            icon={<Plus size={16} />}
            onClick={onOpenCreateTask}
          >
            Create Task
          </Button>

          <Button
            variant="secondary"
            size="md"
            icon={<Users size={16} />}
            onClick={() => onNavigateTab('leads')}
          >
            View Leads
          </Button>

          <Button
            variant="secondary"
            size="md"
            icon={<ListTodo size={16} />}
            onClick={() => onNavigateTab('tasks')}
          >
            View Tasks
          </Button>
        </div>
      </div>

      {/* 4 Stat Metric Cards */}
      <div className="stat-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(220px, 100%), 1fr))', gap: '1rem' }}>
        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-label">Total Leads</span>
            <div className="stat-icon" style={{ backgroundColor: 'var(--accent-indigo-subtle)', color: 'var(--accent-indigo)' }}>
              <Users size={19} />
            </div>
          </div>
          <div className="stat-value">{totalLeadsCount}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--accent-emerald)', fontWeight: 600, marginTop: '0.5rem' }}>
            Saved business leads
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-label">Verified Leads</span>
            <div className="stat-icon" style={{ backgroundColor: 'var(--accent-emerald-subtle)', color: 'var(--accent-emerald)' }}>
              <CheckCircle2 size={19} />
            </div>
          </div>
          <div className="stat-value">{verifiedLeadsCount}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
            High-confidence contacts
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-label">Discovery Tasks</span>
            <div className="stat-icon" style={{ backgroundColor: 'var(--accent-sky-subtle)', color: 'var(--accent-sky)' }}>
              <ListTodo size={19} />
            </div>
          </div>
          <div className="stat-value">{totalTasksCount}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
            Total tasks created
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-label">Active Tasks</span>
            <div className="stat-icon" style={{ backgroundColor: 'var(--accent-amber-subtle)', color: 'var(--accent-amber)' }}>
              <Activity size={19} />
            </div>
          </div>
          <div className="stat-value">{activeJobsCount}</div>
          <div style={{ fontSize: '0.75rem', color: activeJobsCount > 0 ? 'var(--accent-amber)' : 'var(--text-muted)', fontWeight: 600, marginTop: '0.5rem' }}>
            {activeJobsCount > 0 ? 'Currently running' : 'No active tasks'}
          </div>
        </div>
      </div>

      {/* Two-Column Grid: Recent Tasks & Recent Discovered Leads */}
      <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(280px, 100%), 1fr))', gap: '1.25rem' }}>
        {/* Recent Tasks */}
        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>
                Recent Tasks
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Latest discovery runs</p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              icon={<ArrowRight size={14} />}
              onClick={() => onNavigateTab('tasks')}
            >
              View All
            </Button>
          </div>

          {tasks.length === 0 ? (
            <div style={{ padding: '2.5rem 1rem', textAlign: 'center', color: 'var(--text-dim)' }}>
              <ListTodo size={32} style={{ margin: '0 auto 0.5rem auto', opacity: 0.4 }} />
              <div style={{ fontWeight: 600, color: 'var(--text-muted)' }}>No tasks created yet</div>
              <p style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>Click "Create Task" to begin</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
              {tasks.map((task, idx) => (
                <div
                  key={task.id || idx}
                  style={{
                    padding: '0.75rem 1rem',
                    borderRadius: '8px',
                    border: '1px solid var(--border-subtle)',
                    backgroundColor: 'var(--bg-surface)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-main)' }}>
                        {task.keyword}
                      </span>
                      <Badge status={task.status} />
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span>📍 {task.location}</span>
                      <span>•</span>
                      <span>{task.leadsSaved} leads saved</span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                    {task.status === 'CREATED' ? (
                      <Button
                        variant="primary"
                        size="sm"
                        icon={<Play size={12} />}
                        onClick={() => handleStartTask(task.id)}
                      >
                        Start
                      </Button>
                    ) : (
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => setSelectedTaskId(task.id)}
                      >
                        Progress
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Leads */}
        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>
                Recent Leads
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Latest discovered contacts</p>
            </div>
            <Button
              variant="ghost"
              size="sm"
              icon={<ArrowRight size={14} />}
              onClick={() => onNavigateTab('leads')}
            >
              View All
            </Button>
          </div>

          {recentLeads.length === 0 ? (
            <div style={{ padding: '2.5rem 1rem', textAlign: 'center', color: 'var(--text-dim)' }}>
              <Users size={32} style={{ margin: '0 auto 0.5rem auto', opacity: 0.4 }} />
              <div style={{ fontWeight: 600, color: 'var(--text-muted)' }}>No leads found yet</div>
              <p style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>Run a discovery task to find businesses</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
              {recentLeads.map((lead, idx) => (
                <div
                  key={lead.id ?? lead.leadId ?? idx}
                  style={{
                    padding: '0.75rem 1rem',
                    borderRadius: '8px',
                    border: '1px solid var(--border-subtle)',
                    backgroundColor: 'var(--bg-surface)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-main)' }}>
                        {lead.businessName}
                      </span>
                      <Badge status={lead.verificationStatus} />
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      {lead.primaryEmail ? <span>📧 {lead.primaryEmail}</span> : <span>📍 {lead.city || 'N/A'}</span>}
                      <span>•</span>
                      <span style={{ color: 'var(--accent-emerald)', fontWeight: 600 }}>{lead.confidenceScore}% confidence</span>
                    </div>
                  </div>

                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => setSelectedLeadId(lead.id ?? lead.leadId)}
                  >
                    Details
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Task Progress Modal */}
      <TaskProgressModal
        taskId={selectedTaskId}
        isOpen={!!selectedTaskId}
        onClose={() => setSelectedTaskId(null)}
        onViewLeads={() => onNavigateTab('leads')}
      />

      {/* Lead Details Modal */}
      <LeadDetailsModal
        leadId={selectedLeadId}
        isOpen={!!selectedLeadId}
        onClose={() => setSelectedLeadId(null)}
      />
    </div>
  );
};
