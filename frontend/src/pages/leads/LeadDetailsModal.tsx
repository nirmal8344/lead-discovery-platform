import React, { useEffect, useState } from 'react';
import {
  Globe,
  Mail,
  Phone,
  MapPin,
  ExternalLink,
  Copy,
  Check,
  AlertCircle,
  RefreshCw,
  Link as LinkIcon,
  MessageSquare,
  Users,
} from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { leadsApi } from '../../api/client';
import type { LeadDetail } from '../../types';

interface LeadDetailsModalProps {
  leadId: number | null;
  isOpen: boolean;
  onClose: () => void;
}

export const LeadDetailsModal: React.FC<LeadDetailsModalProps> = ({
  leadId,
  isOpen,
  onClose,
}) => {
  const [lead, setLead] = useState<LeadDetail | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [copiedItem, setCopiedItem] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchDetails = async () => {
    if (!leadId) return;
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const data = await leadsApi.getLeadDetails(leadId);
      setLead(data);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to load lead details');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!isOpen || !leadId) {
      setLead(null);
      setErrorMessage(null);
      return;
    }

    fetchDetails();
  }, [isOpen, leadId]);

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopiedItem(label);
    setTimeout(() => setCopiedItem(null), 2000);
  };

  if (!isOpen || !leadId) return null;

  const emails = lead?.emailAddresses || [];
  const phones = lead?.phoneNumbers || [];
  const sourcePages = lead?.sourcePages || [];
  const websites = lead?.websites || [];
  const socialLinks = lead?.socialLinks || [];
  const contacts = lead?.contacts || [];

  const officialWebsite = websites.find((w) => w.isOfficial)?.url || websites[0]?.url || lead?.officialDomain;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={lead?.businessName || `Lead Details #${leadId}`}
      subtitle={lead?.category ? `${lead.category} — Lead Profile` : 'Business Details & Contact Information'}
      maxWidth="780px"
    >
      {isLoading && (
        <div style={{ padding: '3.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <span className="spinner" style={{ width: '28px', height: '28px', marginBottom: '0.75rem' }} />
          <div style={{ fontWeight: 600, color: 'var(--text-main)' }}>Loading lead details...</div>
        </div>
      )}

      {errorMessage && !isLoading && (
        <div
          style={{
            padding: '1.25rem',
            backgroundColor: 'var(--accent-rose-subtle)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            borderRadius: '10px',
            color: 'var(--accent-rose)',
            marginBottom: '1rem',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.75rem',
          }}
        >
          <AlertCircle size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: '0.875rem' }}>Failed to Load Lead Details</div>
            <div style={{ fontSize: '0.8125rem', marginTop: '0.25rem' }}>{errorMessage}</div>
            <div style={{ marginTop: '0.75rem' }}>
              <Button variant="secondary" size="sm" icon={<RefreshCw size={14} />} onClick={fetchDetails}>
                Retry
              </Button>
            </div>
          </div>
        </div>
      )}

      {lead && !isLoading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Top Overview Bar */}
          <div
            style={{
              padding: '1.25rem',
              backgroundColor: 'var(--bg-subtle)',
              borderRadius: '12px',
              border: '1px solid var(--border-subtle)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '1rem',
            }}
          >
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '0.25rem' }}>
                <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)' }}>
                  {lead.businessName}
                </span>
                <Badge status={lead.verificationStatus} />
              </div>

              <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                <span>📁 {lead.category || 'General'}</span>
                {lead.createdAt && (
                  <span>📅 Discovered {new Date(lead.createdAt).toLocaleDateString()}</span>
                )}
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                Confidence Score
              </div>
              <div
                style={{
                  fontSize: '1.375rem',
                  fontWeight: 800,
                  color: (lead.confidenceScore || 0) >= 80 ? 'var(--accent-emerald)' : (lead.confidenceScore || 0) >= 50 ? 'var(--accent-amber)' : 'var(--text-muted)',
                }}
              >
                {lead.confidenceScore || 0}%
              </div>
            </div>
          </div>

          {/* Section 1: Contact Information */}
          <div className="card" style={{ padding: '1.25rem' }}>
            <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-main)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Mail size={16} color="var(--accent-indigo)" />
              Contact Information
            </h4>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
              {/* Emails */}
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                  Email Addresses
                </div>
                {emails.length === 0 ? (
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-dim)' }}>Not available</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
                    {emails.map((em, idx) => {
                      const emailVal = em.normalizedValue || em.rawValue;
                      return (
                        <div
                          key={idx}
                          style={{
                            padding: '0.5rem 0.75rem',
                            borderRadius: '6px',
                            border: '1px solid var(--border-subtle)',
                            backgroundColor: 'var(--bg-surface)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            fontSize: '0.8125rem',
                          }}
                        >
                          <a href={`mailto:${emailVal}`} style={{ color: 'var(--accent-indigo)', fontWeight: 500, wordBreak: 'break-all' }}>
                            {emailVal}
                          </a>
                          <button
                            onClick={() => copyToClipboard(emailVal, `email-${idx}`)}
                            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '2px', display: 'flex', alignItems: 'center' }}
                            title="Copy email"
                          >
                            {copiedItem === `email-${idx}` ? <Check size={14} color="var(--accent-emerald)" /> : <Copy size={14} />}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Phone Numbers */}
              <div>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                  Phone Numbers
                </div>
                {phones.length === 0 ? (
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-dim)' }}>Not available</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
                    {phones.map((ph, idx) => {
                      const phoneVal = ph.normalizedValue || ph.rawValue;
                      const isWhatsapp = ph.phoneType === 'WHATSAPP';
                      return (
                        <div
                          key={idx}
                          style={{
                            padding: '0.5rem 0.75rem',
                            borderRadius: '6px',
                            border: '1px solid var(--border-subtle)',
                            backgroundColor: 'var(--bg-surface)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            fontSize: '0.8125rem',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                            {isWhatsapp ? (
                              <MessageSquare size={13} color="var(--accent-emerald)" />
                            ) : (
                              <Phone size={13} color="var(--text-muted)" />
                            )}
                            <a href={`tel:${phoneVal}`} style={{ color: 'var(--text-main)', fontWeight: 500 }}>
                              {phoneVal}
                            </a>
                            {isWhatsapp && (
                              <span style={{ fontSize: '0.6875rem', color: 'var(--accent-emerald)', fontWeight: 600 }}>
                                (WhatsApp)
                              </span>
                            )}
                          </div>
                          <button
                            onClick={() => copyToClipboard(phoneVal, `phone-${idx}`)}
                            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '2px', display: 'flex', alignItems: 'center' }}
                            title="Copy phone"
                          >
                            {copiedItem === `phone-${idx}` ? <Check size={14} color="var(--accent-emerald)" /> : <Copy size={14} />}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>

            {/* Key Contacts / Decision Makers if present */}
            {contacts.length > 0 && (
              <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border-subtle)', paddingTop: '0.75rem' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                  <Users size={14} /> Key Contacts
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                  {contacts.map((c, idx) => (
                    <div
                      key={idx}
                      style={{
                        padding: '0.4rem 0.75rem',
                        borderRadius: '6px',
                        backgroundColor: 'var(--bg-subtle)',
                        border: '1px solid var(--border-subtle)',
                        fontSize: '0.8125rem',
                      }}
                    >
                      <strong style={{ color: 'var(--text-main)' }}>{c.contactPerson}</strong>
                      {c.role && <span style={{ color: 'var(--text-muted)', marginLeft: '4px' }}>— {c.role}</span>}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Section 2: Location & Official Website */}
          <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            {/* Location */}
            <div className="card" style={{ padding: '1.25rem' }}>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-main)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <MapPin size={16} color="var(--accent-indigo)" />
                Location
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem', fontSize: '0.8125rem' }}>
                <div>
                  <span style={{ color: 'var(--text-muted)' }}>Address: </span>
                  <span style={{ color: 'var(--text-main)', fontWeight: 500 }}>{lead.address || 'Not available'}</span>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)' }}>City: </span>
                  <span style={{ color: 'var(--text-main)', fontWeight: 500 }}>{lead.city || 'Not available'}</span>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)' }}>State / Region: </span>
                  <span style={{ color: 'var(--text-main)', fontWeight: 500 }}>{lead.state || 'Not available'}</span>
                </div>
                <div>
                  <span style={{ color: 'var(--text-muted)' }}>Country: </span>
                  <span style={{ color: 'var(--text-main)', fontWeight: 500 }}>{lead.country || 'Not available'}</span>
                </div>
              </div>
            </div>

            {/* Official Website & Social Links */}
            <div className="card" style={{ padding: '1.25rem' }}>
              <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-main)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Globe size={16} color="var(--accent-indigo)" />
                Website &amp; Social Links
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.8125rem' }}>
                <div>
                  <span style={{ color: 'var(--text-muted)' }}>Website: </span>
                  {officialWebsite ? (
                    <a
                      href={officialWebsite}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ color: 'var(--accent-indigo)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}
                    >
                      {officialWebsite}
                      <ExternalLink size={12} />
                    </a>
                  ) : (
                    <span style={{ color: 'var(--text-dim)' }}>Not available</span>
                  )}
                </div>

                {socialLinks.length > 0 ? (
                  <div style={{ marginTop: '0.25rem' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.375rem' }}>Social Profiles:</div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.375rem' }}>
                      {socialLinks.map((s, idx) => (
                        <a
                          key={idx}
                          href={s.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{
                            padding: '0.25rem 0.625rem',
                            borderRadius: '6px',
                            backgroundColor: 'var(--bg-subtle)',
                            border: '1px solid var(--border-subtle)',
                            fontSize: '0.75rem',
                            color: 'var(--text-main)',
                            fontWeight: 500,
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.25rem',
                          }}
                        >
                          <span style={{ textTransform: 'capitalize' }}>{s.platform || 'Link'}</span>
                          <ExternalLink size={10} />
                        </a>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div>
                    <span style={{ color: 'var(--text-muted)' }}>Social Links: </span>
                    <span style={{ color: 'var(--text-dim)' }}>Not available</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Section 3: Source Information & Provenance */}
          <div className="card" style={{ padding: '1.25rem' }}>
            <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--text-main)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <LinkIcon size={16} color="var(--accent-indigo)" />
              Source Information &amp; Crawl Provenance
            </h4>

            {sourcePages.length === 0 ? (
              <div style={{ fontSize: '0.8125rem', color: 'var(--text-dim)' }}>No internal source URLs recorded</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem', maxHeight: '180px', overflowY: 'auto' }}>
                {sourcePages.map((sp, idx) => (
                  <div
                    key={idx}
                    style={{
                      padding: '0.5rem 0.75rem',
                      borderRadius: '6px',
                      backgroundColor: 'var(--bg-subtle)',
                      border: '1px solid var(--border-subtle)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      fontSize: '0.75rem',
                    }}
                  >
                    <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '80%' }}>
                      <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>{sp.pageType || 'PAGE'}: </span>
                      <a href={sp.pageUrl} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--accent-indigo)' }}>
                        {sp.pageUrl}
                      </a>
                    </div>
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.6875rem' }}>
                      Status: {sp.httpStatus || 200}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer Action */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid var(--border-subtle)', paddingTop: '1rem' }}>
            <Button variant="secondary" size="md" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      )}
    </Modal>
  );
};
