import React, { useState } from 'react';
import { Search, MapPin, Tag, Sliders, Globe, CheckCircle2, Play, Navigation, RotateCcw } from 'lucide-react';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { tasksApi } from '../../api/client';
import { TaskProgressModal } from '../tasks/TaskProgressModal';

interface DiscoverLeadsPageProps {
  onViewTasks: () => void;
  onViewLeads: () => void;
}

export const DiscoverLeadsPage: React.FC<DiscoverLeadsPageProps> = ({
  onViewTasks,
  onViewLeads,
}) => {
  const [location, setLocation] = useState('');
  const [keyword, setKeyword] = useState('');
  const [maxResults, setMaxResults] = useState(25);
  const [maxPagesPerSite, setMaxPagesPerSite] = useState(5);
  const [searchRadiusKm, setSearchRadiusKm] = useState<number | undefined>(undefined);
  const [requiredFields, setRequiredFields] = useState('EMAIL, PHONE, SOCIAL');
  const [isLoading, setIsLoading] = useState(false);
  const [activeTaskId, setActiveTaskId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!location.trim() || !keyword.trim()) {
      setErrorMessage('Please provide both Location and Business Category');
      return;
    }

    try {
      setIsLoading(true);
      const task = await tasksApi.createTask({
        location: location.trim(),
        keyword: keyword.trim(),
        maxResults,
        maxPagesPerSite,
        searchRadiusKm,
        requiredFields,
      });

      // Start the task immediately
      await tasksApi.startTask(task.id);
      setActiveTaskId(task.id);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to start lead discovery process');
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    setLocation('');
    setKeyword('');
    setMaxResults(25);
    setMaxPagesPerSite(5);
    setSearchRadiusKm(undefined);
    setRequiredFields('EMAIL, PHONE, SOCIAL');
    setErrorMessage(null);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem', maxWidth: '1000px', margin: '0 auto' }}>
      {/* Header Banner */}
      <div
        style={{
          background: 'var(--hero-bg)',
          borderRadius: '16px',
          padding: '2rem 2.25rem',
          color: 'var(--hero-text)',
          boxShadow: 'var(--shadow-md)',
          border: '1px solid var(--border-subtle)',
        }}
      >
        <div style={{ maxWidth: '640px' }}>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.025em', marginBottom: '0.5rem', color: '#ffffff' }}>
            Discover Leads
          </h2>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8', lineHeight: 1.6, margin: 0 }}>
            Search for businesses by location and category and collect publicly available business information.
          </p>
        </div>
      </div>

      {/* Discovery Form Card */}
      <div className="card" style={{ padding: '2rem' }}>
        <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '1.5rem' }}>
          Search Details
        </h3>

        {errorMessage && (
          <div
            style={{
              padding: '0.75rem 1rem',
              backgroundColor: 'var(--accent-rose-subtle)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              borderRadius: '8px',
              color: 'var(--accent-rose)',
              fontSize: '0.8125rem',
              marginBottom: '1.5rem',
            }}
          >
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem' }}>
            <Input
              label="Location"
              placeholder="e.g. San Francisco, London, Sydney, Mumbai"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              icon={<MapPin size={16} />}
              required
            />

            <Input
              label="Business Category"
              placeholder="e.g. Software, Dental Clinic, Real Estate"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              icon={<Tag size={16} />}
              required
            />
          </div>

          <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem', marginTop: '0.5rem' }}>
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
                  <option value={100}>100 Results (Maximum)</option>
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
                  <option value={1}>1 Page (Homepage only)</option>
                  <option value={3}>3 Pages (Contact & About)</option>
                  <option value={5}>5 Pages (Home, About, Contact, Team)</option>
                  <option value={10}>10 Pages (Full Crawl)</option>
                </select>
              </div>
            </div>
          </div>

          <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem', marginTop: '0.5rem' }}>
            <div className="form-group">
              <label className="form-label">Search Radius</label>
              <div className="input-wrapper">
                <span className="input-icon-left"><Navigation size={16} /></span>
                <select
                  className="form-select input-with-icon-left"
                  value={searchRadiusKm ?? ''}
                  onChange={(e) => setSearchRadiusKm(e.target.value ? Number(e.target.value) : undefined)}
                >
                  <option value="">Any Distance (No Radius Filter)</option>
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

          <div className="form-actions" style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
            <Button
              type="button"
              variant="secondary"
              size="md"
              icon={<RotateCcw size={16} />}
              onClick={handleReset}
            >
              Reset
            </Button>

            <Button
              type="button"
              variant="secondary"
              size="md"
              onClick={onViewTasks}
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
              Start Discovery
            </Button>
          </div>
        </form>
      </div>

      {/* 3 Step Workflow Overview */}
      <div className="three-col-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.25rem' }}>
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ color: 'var(--accent-indigo)', marginBottom: '0.75rem' }}>
            <Search size={22} />
          </div>
          <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.25rem' }}>
            1. Search & Discovery
          </h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
            Identifies businesses matching your target location and category, filtering out directory aggregators.
          </p>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ color: 'var(--accent-sky)', marginBottom: '0.75rem' }}>
            <Globe size={22} />
          </div>
          <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.25rem' }}>
            2. Website Research
          </h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
            Visits official websites, traversing contact and about pages while respecting robots.txt and rate limits.
          </p>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ color: 'var(--accent-emerald)', marginBottom: '0.75rem' }}>
            <CheckCircle2 size={22} />
          </div>
          <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.25rem' }}>
            3. Contact Extraction & Validation
          </h4>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
            Extracts email addresses, phone numbers, WhatsApp, and social links with confidence scores.
          </p>
        </div>
      </div>

      {/* Task Progress Modal */}
      <TaskProgressModal
        taskId={activeTaskId}
        isOpen={!!activeTaskId}
        onClose={() => setActiveTaskId(null)}
        onViewLeads={() => {
          setActiveTaskId(null);
          onViewLeads();
        }}
      />
    </div>
  );
};
