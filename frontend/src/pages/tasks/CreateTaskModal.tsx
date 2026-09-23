import React, { useState } from 'react';
import { Play, MapPin, Tag, Sliders, Globe, Navigation } from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { tasksApi } from '../../api/client';

interface CreateTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: (taskId?: number) => void;
}

export const CreateTaskModal: React.FC<CreateTaskModalProps> = ({
  isOpen,
  onClose,
  onCreated,
}) => {
  const [location, setLocation] = useState('');
  const [keyword, setKeyword] = useState('');
  const [maxResults, setMaxResults] = useState(25);
  const [maxPagesPerSite, setMaxPagesPerSite] = useState(5);
  const [searchRadiusKm, setSearchRadiusKm] = useState<number | ''>('');
  const [requiredFields, setRequiredFields] = useState('EMAIL, PHONE');
  const [autoStart, setAutoStart] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!location.trim() || !keyword.trim()) {
      setErrorMessage('Location and Business Category are required');
      return;
    }

    try {
      setIsLoading(true);
      const createdTask = await tasksApi.createTask({
        location: location.trim(),
        keyword: keyword.trim(),
        maxResults,
        maxPagesPerSite,
        searchRadiusKm: searchRadiusKm !== '' ? searchRadiusKm : undefined,
        requiredFields,
      });

      if (autoStart) {
        await tasksApi.startTask(createdTask.id);
      }

      // Reset form
      setLocation('');
      setKeyword('');
      setMaxResults(25);
      setMaxPagesPerSite(5);
      setSearchRadiusKm('');
      onCreated(createdTask.id);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to create discovery task');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Create Task"
      subtitle="Configure target location, business category, and crawling parameters"
      maxWidth="580px"
    >
      {errorMessage && (
        <div
          style={{
            padding: '0.75rem 1rem',
            backgroundColor: 'var(--accent-rose-subtle)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            borderRadius: '8px',
            color: 'var(--accent-rose)',
            fontSize: '0.8125rem',
            marginBottom: '1.25rem',
          }}
        >
          {errorMessage}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <Input
            label="Location"
            placeholder="e.g. Austin, TX or Berlin"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            icon={<MapPin size={16} />}
            required
          />

          <Input
            label="Business Category"
            placeholder="e.g. Software, Dental Clinic, Law Firm"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            icon={<Tag size={16} />}
            required
          />
        </div>

        <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div className="form-group">
            <label className="form-label">Maximum Results</label>
            <div className="input-wrapper">
              <span className="input-icon-left"><Sliders size={16} /></span>
              <select
                className="form-select input-with-icon-left"
                value={maxResults}
                onChange={(e) => setMaxResults(Number(e.target.value))}
              >
                <option value={10}>10 Results</option>
                <option value={25}>25 Results (Standard)</option>
                <option value={50}>50 Results</option>
                <option value={100}>100 Results</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Maximum Pages</label>
            <div className="input-wrapper">
              <span className="input-icon-left"><Globe size={16} /></span>
              <select
                className="form-select input-with-icon-left"
                value={maxPagesPerSite}
                onChange={(e) => setMaxPagesPerSite(Number(e.target.value))}
              >
                <option value={1}>1 Page (Home Only)</option>
                <option value={3}>3 Pages (Home, Contact, About)</option>
                <option value={5}>5 Pages (Recommended)</option>
                <option value={10}>10 Pages (Full Crawl)</option>
              </select>
            </div>
          </div>
        </div>

        <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <div className="form-group">
            <label className="form-label">Search Radius</label>
            <div className="input-wrapper">
              <span className="input-icon-left"><Navigation size={16} /></span>
              <select
                className="form-select input-with-icon-left"
                value={searchRadiusKm}
                onChange={(e) => setSearchRadiusKm(e.target.value === '' ? '' : Number(e.target.value))}
              >
                <option value="">Any Distance (No Radius)</option>
                <option value={5}>5 km radius</option>
                <option value={10}>10 km radius</option>
                <option value={25}>25 km radius</option>
                <option value={50}>50 km radius</option>
                <option value={100}>100 km radius</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Required Contact Fields</label>
            <div className="input-wrapper">
              <input
                className="form-input"
                placeholder="EMAIL, PHONE, SOCIAL"
                value={requiredFields}
                onChange={(e) => setRequiredFields(e.target.value)}
              />
            </div>
          </div>
        </div>

        <div style={{ margin: '0.75rem 0 1.25rem 0' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
            <input
              type="checkbox"
              checked={autoStart}
              onChange={(e) => setAutoStart(e.target.checked)}
              style={{ accentColor: 'var(--accent-indigo)', width: '15px', height: '15px', borderRadius: '4px' }}
            />
            <span>Start task immediately after creation</span>
          </label>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem' }}>
          <Button
            type="button"
            variant="secondary"
            size="md"
            onClick={onClose}
          >
            Cancel
          </Button>

          <Button
            type="submit"
            variant="indigo"
            size="md"
            isLoading={isLoading}
            icon={<Play size={16} />}
          >
            {autoStart ? 'Start Discovery' : 'Create Task'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
