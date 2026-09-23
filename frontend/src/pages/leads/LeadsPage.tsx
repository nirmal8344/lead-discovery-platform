import React, { useEffect, useState } from 'react';
import {
  Users,
  Search,
  Download,
  Trash2,
  Eye,
  RotateCcw,
  CheckCircle,
  AlertCircle,
  X,
  FileSpreadsheet,
} from 'lucide-react';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Pagination } from '../../components/ui/Pagination';
import { leadsApi } from '../../api/client';
import type { LeadListItem, VerificationStatus } from '../../types';
import { LeadDetailsModal } from './LeadDetailsModal';
import { ExportCsvModal } from '../../components/export/ExportCsvModal';

interface LeadsPageProps {
  initialTaskId?: number | null;
}

export const LeadsPage: React.FC<LeadsPageProps> = ({ initialTaskId: _initialTaskId }) => {
  const [leads, setLeads] = useState<LeadListItem[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize] = useState(15);
  const [isExportModalOpen, setIsExportModalOpen] = useState(false);
  const [exportFormat, setExportFormat] = useState<'csv' | 'excel'>('csv');

  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [cityFilter, setCityFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<VerificationStatus | ''>('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('DESC');
  const [isLoading, setIsLoading] = useState(false);

  // Lead detail modal & Delete confirmation & Toast
  const [selectedLeadId, setSelectedLeadId] = useState<number | null>(null);
  const [leadToDelete, setLeadToDelete] = useState<LeadListItem | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Keyboard escape for delete confirmation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && leadToDelete && !isDeleting) {
        setLeadToDelete(null);
      }
    };
    if (leadToDelete) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [leadToDelete, isDeleting]);

  const fetchLeads = async (page = 0) => {
    try {
      setIsLoading(true);
      const res = await leadsApi.listLeads({
        search: searchTerm.trim() || undefined,
        city: cityFilter.trim() || undefined,
        category: categoryFilter.trim() || undefined,
        verificationStatus: statusFilter || undefined,
        page,
        size: pageSize,
        sortBy,
        sortDirection,
      });

      setLeads(res.content || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
      setCurrentPage(res.number || 0);
    } catch (err) {
      console.error('Failed to load leads', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLeads(currentPage);
  }, [currentPage, statusFilter, sortBy, sortDirection]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    fetchLeads(0);
  };

  const handleResetFilters = () => {
    setSearchTerm('');
    setCityFilter('');
    setCategoryFilter('');
    setStatusFilter('');
    setSortBy('createdAt');
    setSortDirection('DESC');
    setCurrentPage(0);
    leadsApi.listLeads({ page: 0, size: pageSize }).then((res) => {
      setLeads(res.content || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
      setCurrentPage(0);
    });
  };

  const handleDeleteLead = async () => {
    if (!leadToDelete || isDeleting) return;
    const deletedId = leadToDelete.id ?? leadToDelete.leadId;
    if (!deletedId) {
      setToast({ type: 'error', message: 'Unable to resolve valid lead ID for deletion.' });
      return;
    }
    const deletedName = leadToDelete.businessName;
    try {
      setIsDeleting(true);
      setToast(null);
      await leadsApi.deleteLead(deletedId);
      setLeadToDelete(null);
      setToast({ type: 'success', message: `Lead "${deletedName}" was deleted successfully.` });
      await fetchLeads(currentPage);
    } catch (err: any) {
      console.error('Failed to delete lead', err);
      let errorText = 'Failed to delete lead. Please try again.';
      if (err.status === 403) {
        errorText = 'You do not have permission to delete this lead.';
      } else if (err.status === 404) {
        errorText = 'This lead no longer exists or was already deleted.';
      }
      setToast({ type: 'error', message: errorText });
    } finally {
      setIsDeleting(false);
    }
  };

  const openExportModal = (format: 'csv' | 'excel') => {
    setExportFormat(format);
    setIsExportModalOpen(true);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Toast Notification */}
      {toast && (
        <div
          style={{
            padding: '0.75rem 1rem',
            borderRadius: '8px',
            backgroundColor: toast.type === 'success' ? 'var(--accent-emerald-subtle)' : 'var(--accent-rose-subtle)',
            border: `1px solid ${toast.type === 'success' ? 'rgba(16, 185, 129, 0.25)' : 'rgba(239, 68, 68, 0.25)'}`,
            color: toast.type === 'success' ? 'var(--accent-emerald)' : 'var(--accent-rose)',
            fontSize: '0.8125rem',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {toast.type === 'success' ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
            <span>{toast.message}</span>
          </div>
          <button
            onClick={() => setToast(null)}
            style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', padding: '0.25rem' }}
            aria-label="Dismiss message"
          >
            <X size={14} />
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '1.25rem' }}>
        <form onSubmit={handleSearchSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem', alignItems: 'flex-end' }}>
            {/* Search Input */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Search Leads</label>
              <div className="input-wrapper">
                <span className="input-icon-left"><Search size={15} /></span>
                <input
                  className="form-input input-with-icon-left"
                  placeholder="Business name or keyword..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            {/* City Filter */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">City / Location</label>
              <input
                className="form-input"
                placeholder="e.g. Austin, London"
                value={cityFilter}
                onChange={(e) => setCityFilter(e.target.value)}
              />
            </div>

            {/* Category Filter */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Category</label>
              <input
                className="form-input"
                placeholder="e.g. Software, Clinic"
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
              />
            </div>

            {/* Verification Status */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Verification Status</label>
              <select
                className="form-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as VerificationStatus | '')}
              >
                <option value="">All Statuses</option>
                <option value="VERIFIED">Verified</option>
                <option value="PARTIALLY_VERIFIED">Partially Verified</option>
                <option value="UNVERIFIED">Unverified</option>
              </select>
            </div>

            {/* Sort Order */}
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Sort By</label>
              <select
                className="form-select"
                value={`${sortBy}:${sortDirection}`}
                onChange={(e) => {
                  const [field, dir] = e.target.value.split(':');
                  setSortBy(field);
                  setSortDirection(dir as 'ASC' | 'DESC');
                }}
              >
                <option value="createdAt:DESC">Newest First</option>
                <option value="createdAt:ASC">Oldest First</option>
                <option value="confidenceScore:DESC">Confidence (High to Low)</option>
                <option value="businessName:ASC">Name (A-Z)</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Button type="submit" variant="indigo" size="sm" icon={<Search size={14} />}>
                Filter
              </Button>

              <Button
                type="button"
                variant="secondary"
                size="sm"
                icon={<RotateCcw size={14} />}
                onClick={handleResetFilters}
              >
                Reset
              </Button>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                icon={<Download size={14} />}
                onClick={() => openExportModal('csv')}
              >
                Export CSV
              </Button>

              <Button
                type="button"
                variant="secondary"
                size="sm"
                icon={<FileSpreadsheet size={14} />}
                onClick={() => openExportModal('excel')}
              >
                Export Excel
              </Button>
            </div>
          </div>
        </form>
      </div>

      {/* Leads Table */}
      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>Business Name</th>
              <th>Category</th>
              <th>Location</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Confidence</th>
              <th>Status</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {leads.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '3.5rem 1rem', color: 'var(--text-dim)' }}>
                  <Users size={32} style={{ margin: '0 auto 0.5rem auto', opacity: 0.4 }} />
                  <div style={{ fontWeight: 600, color: 'var(--text-muted)' }}>
                    {isLoading ? 'Loading leads...' : 'No leads found matching current criteria'}
                  </div>
                  {!isLoading && (
                    <p style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>
                      Try adjusting your search filters or start a new discovery task
                    </p>
                  )}
                </td>
              </tr>
            ) : (
              leads.map((lead) => {
                const leadId = lead.id ?? lead.leadId;
                return (
                  <tr key={leadId}>
                    <td>
                      <div style={{ fontWeight: 700, color: 'var(--text-main)' }}>
                        {lead.businessName}
                      </div>
                      {lead.officialWebsite && (
                        <a
                          href={lead.officialWebsite}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{ fontSize: '0.75rem', color: 'var(--accent-indigo)', display: 'inline-block', marginTop: '2px' }}
                        >
                          {lead.officialWebsite.replace(/^https?:\/\//, '').replace(/\/$/, '')}
                        </a>
                      )}
                    </td>

                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                      {lead.category || 'General'}
                    </td>

                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                      {lead.city ? `${lead.city}${lead.state ? `, ${lead.state}` : ''}` : 'Not available'}
                    </td>

                    <td style={{ fontSize: '0.8125rem' }}>
                      {lead.primaryEmail ? (
                        <a href={`mailto:${lead.primaryEmail}`} style={{ color: 'var(--accent-indigo)', fontWeight: 500 }}>
                          {lead.primaryEmail}
                        </a>
                      ) : (
                        <span style={{ color: 'var(--text-dim)' }}>Not available</span>
                      )}
                    </td>

                    <td style={{ fontSize: '0.8125rem' }}>
                      {lead.primaryPhone ? (
                        <a href={`tel:${lead.primaryPhone}`} style={{ color: 'var(--text-main)', fontWeight: 500 }}>
                          {lead.primaryPhone}
                        </a>
                      ) : (
                        <span style={{ color: 'var(--text-dim)' }}>Not available</span>
                      )}
                    </td>

                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
                        <span
                          style={{
                            fontWeight: 700,
                            fontSize: '0.8125rem',
                            color: (lead.confidenceScore || 0) >= 80 ? 'var(--accent-emerald)' : (lead.confidenceScore || 0) >= 50 ? 'var(--accent-amber)' : 'var(--text-muted)',
                          }}
                        >
                          {lead.confidenceScore || 0}%
                        </span>
                      </div>
                    </td>

                    <td>
                      <Badge status={lead.verificationStatus} />
                    </td>

                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.375rem' }}>
                        <Button
                          variant="secondary"
                          size="sm"
                          icon={<Eye size={13} />}
                          onClick={() => setSelectedLeadId(leadId)}
                          title="View lead details"
                        >
                          Details
                        </Button>

                        <button
                          onClick={() => setLeadToDelete(lead)}
                          title="Delete lead"
                          style={{
                            background: 'none',
                            border: '1px solid var(--border-subtle)',
                            color: 'var(--text-muted)',
                            borderRadius: '6px',
                            padding: '0.375rem',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            cursor: 'pointer',
                            transition: 'all 0.15s ease',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.color = 'var(--accent-rose)';
                            e.currentTarget.style.borderColor = 'var(--accent-rose)';
                            e.currentTarget.style.backgroundColor = 'var(--accent-rose-subtle)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.color = 'var(--text-muted)';
                            e.currentTarget.style.borderColor = 'var(--border-subtle)';
                            e.currentTarget.style.backgroundColor = 'transparent';
                          }}
                          aria-label="Delete lead"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>

        {/* Pagination */}
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={pageSize}
          onPageChange={(page) => setCurrentPage(page)}
        />
      </div>

      {/* Lead Details Modal */}
      <LeadDetailsModal
        leadId={selectedLeadId}
        isOpen={!!selectedLeadId}
        onClose={() => setSelectedLeadId(null)}
      />

      {/* Export Modal */}
      <ExportCsvModal
        isOpen={isExportModalOpen}
        onClose={() => setIsExportModalOpen(false)}
        initialFormat={exportFormat}
        filterParams={{
          city: cityFilter.trim() || undefined,
          search: searchTerm.trim() || undefined,
          verificationStatus: statusFilter || undefined,
        }}
      />

      {/* Delete Confirmation Modal */}
      {leadToDelete && (
        <div
          className="modal-overlay"
          onClick={() => {
            if (!isDeleting) setLeadToDelete(null);
          }}
        >
          <div
            className="modal-content"
            style={{ maxWidth: '440px' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <h3 style={{ fontSize: '1.125rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
                Delete Lead
              </h3>
              <button
                className="modal-close"
                onClick={() => {
                  if (!isDeleting) setLeadToDelete(null);
                }}
                disabled={isDeleting}
                aria-label="Close"
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '1.5rem' }}>
              Are you sure you want to delete <strong>"{leadToDelete.businessName}"</strong>? This will permanently remove this lead and its associated contact information.
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <Button
                variant="secondary"
                size="md"
                disabled={isDeleting}
                onClick={() => setLeadToDelete(null)}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                size="md"
                isLoading={isDeleting}
                onClick={handleDeleteLead}
              >
                Delete Lead
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
