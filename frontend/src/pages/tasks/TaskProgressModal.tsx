import React, { useEffect, useState } from 'react';
import { Play, StopCircle, AlertTriangle, Globe, Users } from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { tasksApi } from '../../api/client';
import type { TaskProgress } from '../../types';

interface TaskProgressModalProps {
  taskId: number | null;
  isOpen: boolean;
  onClose: () => void;
  onUpdate?: () => void;
  onViewLeads?: (taskId: number) => void;
}

export const TaskProgressModal: React.FC<TaskProgressModalProps> = ({
  taskId,
  isOpen,
  onClose,
  onUpdate,
  onViewLeads,
}) => {
  const [progress, setProgress] = useState<TaskProgress | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchProgress = async (id: number) => {
    try {
      const data = await tasksApi.getProgress(id);
      setProgress(data);
      if (onUpdate) onUpdate();
      return data;
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to fetch task progress');
      return null;
    }
  };

  useEffect(() => {
    if (!isOpen || !taskId) {
      setProgress(null);
      setErrorMessage(null);
      return;
    }

    setIsLoading(true);
    fetchProgress(taskId).finally(() => setIsLoading(false));

    // Polling interval
    const interval = setInterval(async () => {
      const data = await fetchProgress(taskId);
      if (data) {
        const isTerminal = ['COMPLETED', 'COMPLETED_WITH_NO_RESULTS', 'PARTIALLY_COMPLETED', 'FAILED', 'CANCELLED'].includes(data.status);
        if (isTerminal) {
          clearInterval(interval);
        }
      }
    }, 1200);

    return () => clearInterval(interval);
  }, [isOpen, taskId]);

  const handleCancel = async () => {
    if (!taskId) return;
    try {
      setIsCancelling(true);
      await tasksApi.cancelTask(taskId);
      await fetchProgress(taskId);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to cancel task');
    } finally {
      setIsCancelling(false);
    }
  };

  const handleStart = async () => {
    if (!taskId) return;
    try {
      setIsLoading(true);
      await tasksApi.startTask(taskId);
      await fetchProgress(taskId);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to start task');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen || !taskId) return null;

  const isRunning = progress?.status === 'RUNNING' || progress?.status === 'QUEUED';

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Task #${taskId} Progress`}
      subtitle="Background execution status and discovery metrics"
      maxWidth="620px"
    >
      {(errorMessage || progress?.errorMessage) && (
        <div
          style={{
            padding: '0.75rem 1rem',
            backgroundColor: 'var(--accent-rose-subtle)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            borderRadius: '8px',
            color: 'var(--accent-rose)',
            fontSize: '0.8125rem',
            marginBottom: '1rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <AlertTriangle size={16} />
          <span>{errorMessage || progress?.errorMessage}</span>
        </div>
      )}

      {/* Status & Progress Bar */}
      <div
        style={{
          padding: '1.25rem',
          backgroundColor: 'var(--bg-subtle)',
          borderRadius: '12px',
          border: '1px solid var(--border-subtle)',
          marginBottom: '1.25rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-main)' }}>
              Status:
            </span>
            <Badge status={progress?.status} />
          </div>
          <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--accent-indigo)' }}>
            {progress?.progressPercentage || 0}%
          </span>
        </div>

        <div className="progress-bar-container">
          <div
            className="progress-bar-fill"
            style={{
              width: `${progress?.progressPercentage || 0}%`,
              backgroundColor: progress?.status === 'FAILED' ? 'var(--accent-rose)' : undefined,
            }}
          />
        </div>

        {progress?.currentStage && (
          <div
            style={{
              marginTop: '0.75rem',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              display: 'flex',
              alignItems: 'center',
              gap: '0.375rem',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            <Globe size={14} style={{ flexShrink: 0 }} />
            <span>Current Stage: <strong style={{ color: 'var(--text-main)' }}>{progress.currentStage}</strong></span>
          </div>
        )}
      </div>

      {/* Metric Counters Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <div style={{ padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-subtle)', backgroundColor: 'var(--bg-surface)' }}>
          <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Leads Saved</div>
          <div style={{ fontSize: '1.375rem', fontWeight: 800, color: 'var(--accent-emerald)', marginTop: '0.25rem' }}>
            {progress?.totalSaved || 0}
          </div>
          <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>Discovered: {progress?.totalDiscovered || 0}</div>
        </div>

        <div style={{ padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-subtle)', backgroundColor: 'var(--bg-surface)' }}>
          <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Pages Crawled</div>
          <div style={{ fontSize: '1.375rem', fontWeight: 800, color: 'var(--accent-indigo)', marginTop: '0.25rem' }}>
            {progress?.totalCrawled || 0}
          </div>
          <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>Multi-page crawl</div>
        </div>

        <div style={{ padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-subtle)', backgroundColor: 'var(--bg-surface)' }}>
          <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>Failed / Skips</div>
          <div style={{ fontSize: '1.375rem', fontWeight: 800, color: (progress?.totalFailed || 0) > 0 ? 'var(--accent-rose)' : 'var(--text-main)', marginTop: '0.25rem' }}>
            {progress?.totalFailed || 0}
          </div>
          <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>Logged events</div>
        </div>
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--border-subtle)', paddingTop: '1.25rem' }}>
        <div>
          {onViewLeads && (progress?.totalSaved || 0) > 0 && (
            <Button
              variant="secondary"
              size="sm"
              icon={<Users size={14} />}
              onClick={() => {
                onClose();
                onViewLeads(taskId);
              }}
            >
              View Discovered Leads
            </Button>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          {isRunning ? (
            <Button
              variant="danger"
              size="sm"
              icon={<StopCircle size={14} />}
              isLoading={isCancelling}
              onClick={handleCancel}
            >
              Cancel Task
            </Button>
          ) : progress?.status === 'CREATED' ? (
            <Button
              variant="primary"
              size="sm"
              icon={<Play size={14} />}
              isLoading={isLoading}
              onClick={handleStart}
            >
              Start Task
            </Button>
          ) : null}

          <Button variant="secondary" size="sm" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </Modal>
  );
};
