import React, { useEffect, useState } from 'react';
import {
  ListTodo,
  Plus,
  Play,
  StopCircle,
  RefreshCw,
  Eye,
  AlertTriangle,
  X,
  Users,
} from 'lucide-react';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Pagination } from '../../components/ui/Pagination';
import { tasksApi } from '../../api/client';
import type { ScrapingTask, TaskError } from '../../types';
import { CreateTaskModal } from './CreateTaskModal';
import { TaskProgressModal } from './TaskProgressModal';

interface TasksPageProps {
  onViewTaskLeads?: (taskId: number) => void;
}

export const TasksPage: React.FC<TasksPageProps> = ({ onViewTaskLeads }) => {
  const [tasks, setTasks] = useState<ScrapingTask[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(false);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);

  // Error drawer state
  const [errorDrawerTaskId, setErrorDrawerTaskId] = useState<number | null>(null);
  const [taskErrors, setTaskErrors] = useState<TaskError[]>([]);
  const [isLoadingErrors, setIsLoadingErrors] = useState(false);

  const openErrorDrawer = async (taskId: number) => {
    setErrorDrawerTaskId(taskId);
    setTaskErrors([]);
    setIsLoadingErrors(true);
    try {
      const errors = await tasksApi.getTaskErrors(taskId);
      setTaskErrors(errors || []);
    } catch (err) {
      console.error('Failed to load task errors', err);
    } finally {
      setIsLoadingErrors(false);
    }
  };

  const fetchTasks = async (page = 0, showLoading = true) => {
    try {
      if (showLoading) setIsLoading(true);
      const res = await tasksApi.listTasks(page, pageSize);
      setTasks(res.content || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
      setCurrentPage(res.number || 0);
    } catch (err) {
      console.error('Failed to load tasks', err);
    } finally {
      if (showLoading) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks(currentPage, true);
  }, [currentPage]);

  // Continuous background polling when any task is active (RUNNING or QUEUED)
  const hasActiveTasks = tasks.some((t) => t.status === 'RUNNING' || t.status === 'QUEUED');
  useEffect(() => {
    if (!hasActiveTasks) return;

    const interval = setInterval(() => {
      fetchTasks(currentPage, false);
    }, 1500);

    return () => clearInterval(interval);
  }, [hasActiveTasks, currentPage, pageSize]);

  const handleStartTask = async (taskId: number) => {
    try {
      await tasksApi.startTask(taskId);
      setSelectedTaskId(taskId);
      fetchTasks(currentPage, false);
    } catch (err) {
      console.error('Failed to start task', err);
    }
  };

  const handleCancelTask = async (taskId: number) => {
    try {
      await tasksApi.cancelTask(taskId);
      fetchTasks(currentPage, false);
    } catch (err) {
      console.error('Failed to cancel task', err);
    }
  };

  const filteredTasks = statusFilter === 'ALL'
    ? tasks
    : tasks.filter((t) => t.status === statusFilter);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Top action bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        {/* Status filters */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', flexWrap: 'wrap' }}>
          {['ALL', 'RUNNING', 'QUEUED', 'COMPLETED', 'CANCELLED', 'FAILED'].map((st) => {
            const isSelected = statusFilter === st;
            return (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                style={{
                  padding: '0.375rem 0.75rem',
                  borderRadius: '8px',
                  border: '1px solid',
                  borderColor: isSelected ? 'var(--accent-indigo)' : 'var(--border-subtle)',
                  backgroundColor: isSelected ? 'var(--accent-indigo)' : 'var(--bg-surface)',
                  color: isSelected ? '#ffffff' : 'var(--text-secondary)',
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
              >
                {st === 'ALL' ? 'All Tasks' : st.replace(/_/g, ' ')}
              </button>
            );
          })}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          <Button
            variant="secondary"
            size="sm"
            icon={<RefreshCw size={14} className={isLoading ? 'spinner' : ''} />}
            onClick={() => fetchTasks(currentPage, true)}
            title="Refresh tasks list"
          >
            Refresh
          </Button>

          <Button
            variant="indigo"
            size="sm"
            icon={<Plus size={16} />}
            onClick={() => setIsCreateOpen(true)}
          >
            Create Task
          </Button>
        </div>
      </div>

      {/* Tasks Table */}
      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Category</th>
              <th>Location</th>
              <th>Results</th>
              <th>Pages</th>
              <th>Status</th>
              <th>Progress</th>
              <th>Created</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredTasks.length === 0 ? (
              <tr>
                <td colSpan={9} style={{ textAlign: 'center', padding: '3.5rem 1rem', color: 'var(--text-dim)' }}>
                  <ListTodo size={32} style={{ margin: '0 auto 0.5rem auto', opacity: 0.4 }} />
                  <div style={{ fontWeight: 600, color: 'var(--text-muted)' }}>
                    {isLoading ? 'Loading discovery tasks...' : 'No tasks match the selected filter'}
                  </div>
                  {!isLoading && (
                    <p style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>
                      Click "Create Task" to start discovering business leads
                    </p>
                  )}
                </td>
              </tr>
            ) : (
              filteredTasks.map((task) => {
                const isRunning = task.status === 'RUNNING' || task.status === 'QUEUED';
                const hasErrors = (task.failedRecords || 0) > 0;
                const percent = task.progressPercentage !== undefined
                  ? task.progressPercentage
                  : task.maxResults && task.maxResults > 0
                  ? Math.min(100, Math.round(((task.leadsSaved || 0) / task.maxResults) * 100))
                  : 0;

                return (
                  <tr key={task.id}>
                    <td style={{ fontWeight: 700, color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
                      #{task.id}
                    </td>

                    <td>
                      <div style={{ fontWeight: 700, color: 'var(--text-main)' }}>{task.keyword}</div>
                      {task.requiredFields && (
                        <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                          Targets: {task.requiredFields}
                        </div>
                      )}
                    </td>

                    <td>
                      <span style={{ color: 'var(--text-main)', fontSize: '0.8125rem' }}>📍 {task.location}</span>
                      {task.searchRadiusKm && (
                        <span style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginLeft: '4px' }}>
                          ({task.searchRadiusKm} km)
                        </span>
                      )}
                    </td>

                    <td style={{ fontWeight: 600, color: 'var(--text-main)', fontSize: '0.8125rem' }}>
                      {task.leadsSaved || 0} / {task.maxResults || 25}
                    </td>

                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                      {task.processedWebsites || 0} websites
                    </td>

                    <td>
                      <Badge status={task.status} />
                    </td>

                    <td style={{ minWidth: '120px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <div className="progress-bar-container" style={{ flex: 1, height: '6px' }}>
                          <div
                            className="progress-bar-fill"
                            style={{
                              width: `${percent}%`,
                              backgroundColor: task.status === 'FAILED' ? 'var(--accent-rose)' : undefined,
                            }}
                          />
                        </div>
                        <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', minWidth: '32px' }}>
                          {percent}%
                        </span>
                      </div>
                    </td>

                    <td style={{ fontSize: '0.75rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                      {task.createdAt ? new Date(task.createdAt).toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'short' }) : '—'}
                    </td>

                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem' }}>
                        {task.status === 'CREATED' && (
                          <Button
                            variant="primary"
                            size="sm"
                            icon={<Play size={12} />}
                            onClick={() => handleStartTask(task.id)}
                            title="Start task"
                          >
                            Start
                          </Button>
                        )}

                        {isRunning && (
                          <>
                            <Button
                              variant="secondary"
                              size="sm"
                              icon={<Eye size={12} />}
                              onClick={() => setSelectedTaskId(task.id)}
                              title="View live progress"
                            >
                              Progress
                            </Button>
                            <Button
                              variant="danger"
                              size="sm"
                              icon={<StopCircle size={12} />}
                              onClick={() => handleCancelTask(task.id)}
                              title="Cancel task"
                            >
                              Cancel
                            </Button>
                          </>
                        )}

                        {!isRunning && task.status !== 'CREATED' && (
                          <Button
                            variant="secondary"
                            size="sm"
                            icon={<Eye size={12} />}
                            onClick={() => setSelectedTaskId(task.id)}
                            title="View task summary"
                          >
                            Progress
                          </Button>
                        )}

                        {onViewTaskLeads && (task.leadsSaved || 0) > 0 && (
                          <Button
                            variant="ghost"
                            size="sm"
                            icon={<Users size={12} />}
                            onClick={() => onViewTaskLeads(task.id)}
                            title="View leads for this task"
                          >
                            Leads
                          </Button>
                        )}

                        {hasErrors && (
                          <button
                            onClick={() => openErrorDrawer(task.id)}
                            title={`View errors`}
                            style={{
                              background: 'var(--accent-rose-subtle)',
                              border: '1px solid rgba(239, 68, 68, 0.25)',
                              borderRadius: '6px',
                              color: 'var(--accent-rose)',
                              padding: '0.3rem 0.5rem',
                              fontSize: '0.75rem',
                              fontWeight: 700,
                              cursor: 'pointer',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.25rem',
                            }}
                          >
                            <AlertTriangle size={12} />
                            <span>{task.failedRecords}</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>

        {/* Pagination footer */}
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={pageSize}
          onPageChange={(page) => setCurrentPage(page)}
        />
      </div>

      {/* Task Creation Modal */}
      <CreateTaskModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={() => {
          setIsCreateOpen(false);
          fetchTasks(0, true);
        }}
      />

      {/* Live Task Progress Modal */}
      <TaskProgressModal
        taskId={selectedTaskId}
        isOpen={!!selectedTaskId}
        onClose={() => setSelectedTaskId(null)}
        onUpdate={() => fetchTasks(currentPage, false)}
        onViewLeads={onViewTaskLeads}
      />

      {/* Error Details Drawer */}
      {errorDrawerTaskId && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'var(--modal-overlay)',
            zIndex: 999,
            display: 'flex',
            justifyContent: 'flex-end',
            animation: 'fadeIn 0.2s ease',
          }}
          onClick={() => setErrorDrawerTaskId(null)}
        >
          <div
            style={{
              width: '100%',
              maxWidth: '520px',
              height: '100%',
              backgroundColor: 'var(--bg-surface)',
              boxShadow: 'var(--shadow-xl)',
              padding: '1.75rem',
              display: 'flex',
              flexDirection: 'column',
              overflowY: 'auto',
              borderLeft: '1px solid var(--border-subtle)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem', paddingBottom: '0.75rem', borderBottom: '1px solid var(--border-subtle)' }}>
              <div>
                <h3 style={{ fontSize: '1.125rem', fontWeight: 800, color: 'var(--text-main)' }}>
                  Task #{errorDrawerTaskId} Errors
                </h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Logged crawling &amp; discovery warnings</p>
              </div>
              <button
                onClick={() => setErrorDrawerTaskId(null)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.25rem' }}
                aria-label="Close error drawer"
              >
                <X size={20} />
              </button>
            </div>

            {isLoadingErrors ? (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                <span className="spinner" style={{ width: '24px', height: '24px' }} />
                <div style={{ marginTop: '0.5rem', fontSize: '0.8125rem' }}>Loading error logs...</div>
              </div>
            ) : taskErrors.length === 0 ? (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-dim)' }}>
                <div style={{ fontWeight: 600, color: 'var(--text-muted)' }}>No detailed errors recorded</div>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {taskErrors.map((err, idx) => (
                  <div
                    key={err.id || idx}
                    style={{
                      padding: '0.875rem 1rem',
                      borderRadius: '8px',
                      backgroundColor: 'var(--accent-rose-subtle)',
                      border: '1px solid rgba(239, 68, 68, 0.25)',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                      <span style={{ fontWeight: 700, fontSize: '0.75rem', color: 'var(--accent-rose)' }}>
                        {err.errorType || 'ERROR'}
                      </span>
                      <span style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                        {err.createdAt ? new Date(err.createdAt).toLocaleTimeString() : ''}
                      </span>
                    </div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--text-main)', wordBreak: 'break-word' }}>
                      {err.errorMessage}
                    </div>
                    {err.url && (
                      <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem', wordBreak: 'break-all' }}>
                        URL: {err.url}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
