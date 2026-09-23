import React, { useState } from 'react';
import { Mail, Lock, User as UserIcon, AlertCircle, Search, CheckCircle2, Shield } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';

interface RegisterPageProps {
  onSwitchToLogin: () => void;
}

export const RegisterPage: React.FC<RegisterPageProps> = ({ onSwitchToLogin }) => {
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!name.trim() || !email.trim() || !password) {
      setErrorMessage('Please fill in all required fields');
      return;
    }

    if (password.length < 8) {
      setErrorMessage('Password must be at least 8 characters long');
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match');
      return;
    }

    try {
      setIsLoading(true);
      await register({
        name: name.trim(),
        email: email.trim(),
        password,
      });
    } catch (err: any) {
      setErrorMessage(err.message || 'Registration failed. Please try again.');
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
          maxHeight: 'min(94vh, 660px)',
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
            padding: '2.25rem 2.5rem',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            overflowY: 'auto',
          }}
          className="auth-form-column"
        >
          <div>
            {/* Logo */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '1.25rem' }}>
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
              Create Account
            </h1>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
              Sign up to start discovering business leads and official contacts.
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
                label="Full Name"
                type="text"
                placeholder="Alex Johnson"
                value={name}
                onChange={(e) => setName(e.target.value)}
                icon={<UserIcon size={16} />}
                required
              />

              <Input
                label="Work Email Address"
                type="email"
                placeholder="alex@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                icon={<Mail size={16} />}
                required
              />

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <Input
                  label="Password (min 8 chars)"
                  isPassword
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  icon={<Lock size={16} />}
                  required
                />

                <Input
                  label="Confirm Password"
                  isPassword
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  icon={<Lock size={16} />}
                  required
                />
              </div>

              <Button
                type="submit"
                variant="indigo"
                size="lg"
                isLoading={isLoading}
                style={{ width: '100%', height: '44px', borderRadius: '10px', marginTop: '0.5rem' }}
              >
                <span>Create Account</span>
              </Button>
            </form>
          </div>

          <div style={{ marginTop: '1.25rem', textAlign: 'center' }}>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
              Already have an account?{' '}
              <button
                type="button"
                onClick={onSwitchToLogin}
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
                Sign In
              </button>
            </p>
          </div>
        </div>

        {/* Right Hero Column */}
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
              Structured Business Intelligence
            </h2>

            <p style={{ fontSize: '0.875rem', color: '#94a3b8', lineHeight: 1.6, marginBottom: '1.75rem' }}>
              Automate multi-page business research, extract verified emails & phone numbers, and export structured spreadsheets.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <CheckCircle2 size={16} color="#10b981" />
                <span style={{ fontSize: '0.8125rem', color: '#e2e8f0' }}>Reliable web crawling with rate limiting</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <CheckCircle2 size={16} color="#10b981" />
                <span style={{ fontSize: '0.8125rem', color: '#e2e8f0' }}>Multi-field contact extraction & verification</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <CheckCircle2 size={16} color="#10b981" />
                <span style={{ fontSize: '0.8125rem', color: '#e2e8f0' }}>Live task progress & instant spreadsheet downloads</span>
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
              Encrypted session security with multi-tenant isolation
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
