import React, { useState, useEffect } from 'react';
import {
  User,
  Sliders,
  CheckCircle2,
  Save,
  RotateCcw,
  Sun,
  Moon,
  Palette,
  Shield,
} from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { useTheme } from '../../context/ThemeContext';
import { authApi } from '../../api/client';
import type { User as UserType } from '../../types';

const SETTINGS_KEY = 'lead_discovery_settings';

interface AppSettings {
  defaultMaxResults: number;
  defaultMaxPagesPerSite: number;
  defaultSearchRadiusKm: string;
  defaultRequiredFields: string;
  domainRateLimitMs: number;
  maxRetries: number;
}

const DEFAULT_SETTINGS: AppSettings = {
  defaultMaxResults: 25,
  defaultMaxPagesPerSite: 5,
  defaultSearchRadiusKm: '',
  defaultRequiredFields: 'EMAIL, PHONE, SOCIAL',
  domainRateLimitMs: 400,
  maxRetries: 2,
};

function loadSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (raw) return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {}
  return { ...DEFAULT_SETTINGS };
}

function saveSettings(s: AppSettings) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
}

export const SettingsPage: React.FC = () => {
  const { theme, setTheme } = useTheme();
  const [user, setUser] = useState<UserType | null>(null);
  const [settings, setSettings] = useState<AppSettings>(loadSettings());
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    authApi.getMe().then(setUser).catch(() => {});
  }, []);

  const handleSave = () => {
    saveSettings(settings);
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const handleReset = () => {
    setSettings({ ...DEFAULT_SETTINGS });
    saveSettings({ ...DEFAULT_SETTINGS });
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const set = (field: keyof AppSettings, value: any) =>
    setSettings((prev) => ({ ...prev, [field]: value }));

  const sectionHeader = (icon: React.ReactNode, title: string, sub: string) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' }}>
      <div style={{ width: 36, height: 36, borderRadius: '8px', background: 'var(--accent-indigo-subtle)', color: 'var(--accent-indigo)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        {icon}
      </div>
      <div>
        <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-main)' }}>{title}</div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{sub}</div>
      </div>
    </div>
  );

  return (
    <div style={{ maxWidth: '860px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '1.25rem', paddingBottom: '2rem' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.375rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
            Settings
          </h2>
          <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', margin: 0 }}>
            Manage appearance, defaults, and account preferences
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.625rem' }}>
          <Button variant="secondary" size="sm" icon={<RotateCcw size={14} />} onClick={handleReset}>
            Reset Defaults
          </Button>
          <Button variant="indigo" size="sm" icon={<Save size={14} />} onClick={handleSave}>
            {saved ? '✓ Saved!' : 'Save Changes'}
          </Button>
        </div>
      </div>

      {/* Appearance Section */}
      <div className="card" style={{ padding: '1.5rem' }}>
        {sectionHeader(<Palette size={18} />, 'Appearance', 'Customize the interface theme for day or night use')}

        <div className="theme-selector" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', maxWidth: '440px' }}>
          <button
            type="button"
            onClick={() => setTheme('light')}
            style={{
              padding: '1rem',
              borderRadius: '10px',
              border: `2px solid ${theme === 'light' ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
              backgroundColor: theme === 'light' ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              cursor: 'pointer',
              textAlign: 'left',
              transition: 'all 0.15s ease',
            }}
          >
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: '#ffffff',
                border: '1px solid #e2e8f0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#0f172a',
              }}
            >
              <Sun size={18} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-main)' }}>Light Mode</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Clean, crisp layout</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setTheme('dark')}
            style={{
              padding: '1rem',
              borderRadius: '10px',
              border: `2px solid ${theme === 'dark' ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
              backgroundColor: theme === 'dark' ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              cursor: 'pointer',
              textAlign: 'left',
              transition: 'all 0.15s ease',
            }}
          >
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: '#0f172a',
                border: '1px solid #334155',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#f8fafc',
              }}
            >
              <Moon size={18} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-main)' }}>Dark Mode</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Reduced eye strain</div>
            </div>
          </button>
        </div>
      </div>

      {/* Account Info */}
      <div className="card" style={{ padding: '1.5rem' }}>
        {sectionHeader(<User size={18} />, 'Account Information', 'Your active session profile')}
        {user ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(180px, 100%), 1fr))', gap: '1rem' }}>
            {[
              { label: 'Full Name', value: user.name },
              { label: 'Email Address', value: user.email },
              { label: 'Role', value: user.role || 'User' },
            ].map(({ label, value }) => (
              <div key={label} style={{ padding: '0.75rem 1rem', background: 'var(--bg-subtle)', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>{label}</div>
                <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-main)' }}>{value}</div>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Loading account details…</div>
        )}
      </div>

      {/* Discovery Defaults */}
      <div className="card" style={{ padding: '1.5rem' }}>
        {sectionHeader(<Sliders size={18} />, 'Discovery Defaults', 'Default configuration applied when creating new discovery tasks')}

        <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem', marginBottom: '1rem' }}>
          <div>
            <label className="form-label">Default Maximum Results</label>
            <select
              className="form-select"
              value={settings.defaultMaxResults}
              onChange={(e) => set('defaultMaxResults', Number(e.target.value))}
            >
              <option value={10}>10 Business Candidates</option>
              <option value={25}>25 Business Candidates (Standard)</option>
              <option value={50}>50 Business Candidates</option>
              <option value={100}>100 Business Candidates</option>
            </select>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Target number of businesses discovered per task.</div>
          </div>

          <div>
            <label className="form-label">Default Maximum Pages per Site</label>
            <select
              className="form-select"
              value={settings.defaultMaxPagesPerSite}
              onChange={(e) => set('defaultMaxPagesPerSite', Number(e.target.value))}
            >
              <option value={1}>1 Page – Homepage only</option>
              <option value={3}>3 Pages – Contact &amp; About</option>
              <option value={5}>5 Pages – Recommended</option>
              <option value={10}>10 Pages – Full Crawl</option>
            </select>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Internal page depth per website (Home, Contact, About, Team).</div>
          </div>
        </div>

        <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem' }}>
          <div>
            <label className="form-label">Default Search Radius</label>
            <select
              className="form-select"
              value={settings.defaultSearchRadiusKm}
              onChange={(e) => set('defaultSearchRadiusKm', e.target.value)}
            >
              <option value="">Any Distance (No Radius)</option>
              <option value="5">5 km radius</option>
              <option value="10">10 km radius</option>
              <option value="25">25 km radius</option>
              <option value="50">50 km radius</option>
              <option value="100">100 km radius</option>
            </select>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Geographical radius filter around the target city.</div>
          </div>

          <div>
            <label className="form-label">Default Required Contact Fields</label>
            <input
              className="form-input"
              placeholder="EMAIL, PHONE, SOCIAL"
              value={settings.defaultRequiredFields}
              onChange={(e) => set('defaultRequiredFields', e.target.value)}
            />
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>Comma-separated: EMAIL, PHONE, WHATSAPP, SOCIAL, ADDRESS.</div>
          </div>
        </div>
      </div>

      {/* Crawling Safety & Compliance */}
      <div className="card" style={{ padding: '1.5rem' }}>
        {sectionHeader(<Shield size={18} />, 'Crawling &amp; Compliance', 'Ethical scraping limits and rate control defaults')}

        <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.25rem', marginBottom: '1rem' }}>
          <div>
            <label className="form-label">Domain Rate Limit Delay</label>
            <input
              className="form-input"
              disabled
              value={`${settings.domainRateLimitMs} ms`}
              style={{ backgroundColor: 'var(--bg-subtle)', color: 'var(--text-secondary)' }}
            />
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Minimum polite delay between requests to the same domain.
            </div>
          </div>

          <div>
            <label className="form-label">Transient Error Retries</label>
            <input
              className="form-input"
              disabled
              value={`${settings.maxRetries} retries`}
              style={{ backgroundColor: 'var(--bg-subtle)', color: 'var(--text-secondary)' }}
            />
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
              Maximum retry attempts on network timeouts.
            </div>
          </div>
        </div>

        <div style={{ padding: '0.75rem 1rem', background: 'var(--accent-emerald-subtle)', border: '1px solid rgba(16, 185, 129, 0.2)', borderRadius: '8px', fontSize: '0.8125rem', color: 'var(--accent-emerald)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <CheckCircle2 size={16} style={{ flexShrink: 0 }} />
          <span>
            <strong>robots.txt compliance is active</strong> — the crawler automatically respects robots.txt rules for every discovered website.
          </span>
        </div>
      </div>
    </div>
  );
};
