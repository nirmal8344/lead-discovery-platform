import React, { useState } from 'react';
import { Mail, Lock, AlertCircle, Search, Globe, CheckCircle2, Shield } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';

interface LoginPageProps {
  onSwitchToRegister: () => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onSwitchToRegister }) => {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!email.trim() || !password) {
      setErrorMessage('Please enter both email and password');
      return;
    }

    try {
      setIsLoading(true);
      await login({ email: email.trim(), password });
    } catch (err: any) {
      setErrorMessage(err.message || 'Invalid email or password. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      style={{
        height: '100vh',
        width: '100vw',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--bg-canvas)',
        padding: '1rem',
        overflow: 'hidden',
        boxSizing: 'border-box',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '960px',
          maxHeight: 'min(92vh, 620px)',
          backgroundColor: 'var(--bg-surface)',
          borderRadius: '20px',
          border: '1px solid var(--border-subtle)',
          boxShadow: 'var(--shadow-xl)',
          display: 'grid',
          gridTemplateColumns: '1.1fr 1fr',
          overflow: 'hidden',
        }}
        className="auth-grid-container"
      >
        {/* Left Form Column */}
        <div
          style={{
            padding: '2.5rem',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            overflowY: 'auto',
          }}
          className="auth-form-column"
        >
          <div>
            {/* App Logo */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '1.5rem' }}>
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
              <span style={{ fontWeight: 800, fontSize: '1.125rem', letterSpacing: '-0.02em', color: 'var(--text-main)' }}>
                Lead Discovery
              </span>
            </div>

            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', letterSpacing: '-0.025em', marginBottom: '0.25rem' }}>
              Sign In
            </h1>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
              Enter your credentials to access your discovery tasks and leads.
            </p>

            {errorMessage && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.625rem 0.875rem',
                  borderRadius: '8px',
                  backgroundColor: 'var(--accent-rose-subtle)',
                  border: '1px solid rgba(239, 68, 68, 0.25)',
                  color: 'var(--accent-rose)',
                  fontSize: '0.8125rem',
                  marginBottom: '1rem',
                }}
              >
                <AlertCircle size={16} style={{ flexShrink: 0 }} />
                <span>{errorMessage}</span>
              </div>
            )}

            <form onSubmit={handleSubmit}>
              <Input
                label="Email Address"
                type="email"
                placeholder="name@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                icon={<Mail size={16} />}
                required
              />

              <Input
                label="Password"
                isPassword
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                icon={<Lock size={16} />}
                required
              />

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  margin: '0.75rem 0 1.25rem 0',
                  fontSize: '0.8125rem',
                }}
              >
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    style={{ accentColor: 'var(--accent-indigo)', width: '15px', height: '15px', borderRadius: '4px' }}
                  />
                  <span>Remember me</span>
                </label>
              </div>

              <Button
                type="submit"
                variant="indigo"
                size="lg"
                isLoading={isLoading}
                style={{ width: '100%', height: '44px', borderRadius: '10px' }}
              >
                <span>Sign In</span>
              </Button>
            </form>
          </div>

          <div style={{ marginTop: '1.5rem', textAlign: 'center' }}>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Don't have an account?{' '}
              <button
                type="button"
                onClick={onSwitchToRegister}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--accent-indigo)',
                  fontWeight: 700,
                  cursor: 'pointer',
                  padding: 0,
                  textDecoration: 'underline',
                }}
              >
                Create Account
              </button>
            </p>
          </div>
        </div>

        {/* Right Product Intro Column */}
        <div
          style={{
            background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
            padding: '2.5rem',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            position: 'relative',
            overflow: 'hidden',
            color: '#ffffff',
          }}
          className="auth-hero-column"
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <span style={{ display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#10b981' }} />
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Lead Discovery Platform
              </span>
            </div>

            <h2 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.025em', lineHeight: 1.3, marginBottom: '0.75rem', color: '#ffffff' }}>
              Discover Business Leads
            </h2>

            <p style={{ fontSize: '0.875rem', color: '#94a3b8', lineHeight: 1.6, marginBottom: '1.75rem' }}>
              Find relevant businesses, discover their official websites, and collect verified public contact information from one place.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ width: '28px', height: '28px', borderRadius: '8px', backgroundColor: 'rgba(99, 102, 241, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#818cf8', flexShrink: 0 }}>
                  <Search size={15} />
                </div>
                <div>
                  <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#f8fafc' }}>Targeted Search</div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Search by location, industry, and search radius</div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ width: '28px', height: '28px', borderRadius: '8px', backgroundColor: 'rgba(16, 185, 129, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#34d399', flexShrink: 0 }}>
                  <Globe size={15} />
                </div>
                <div>
                  <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#f8fafc' }}>Website Research</div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Crawl official websites and extract verified contact points</div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ width: '28px', height: '28px', borderRadius: '8px', backgroundColor: 'rgba(56, 189, 248, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#38bdf8', flexShrink: 0 }}>
                  <CheckCircle2 size={15} />
                </div>
                <div>
                  <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: '#f8fafc' }}>Structured Exports</div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Download clean CSV and Excel spreadsheets anytime</div>
                </div>
              </div>
            </div>
          </div>

          <div
            style={{
              padding: '0.875rem 1rem',
              backgroundColor: 'rgba(255, 255, 255, 0.04)',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              borderRadius: '12px',
              display: 'flex',
              alignItems: 'center',
              gap: '0.625rem',
            }}
          >
            <Shield size={16} color="#94a3b8" />
            <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
              Secure authentication & dedicated workspace isolation
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
