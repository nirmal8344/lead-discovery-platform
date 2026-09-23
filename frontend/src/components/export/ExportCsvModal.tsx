import React, { useEffect, useState, useMemo } from 'react';
import {
  Download,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  Table,
} from 'lucide-react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { leadsApi } from '../../api/client';
import type { CategoryStat, VerificationStatus } from '../../types';

export type ExportFormat = 'csv' | 'excel';

interface ExportCsvModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialCategory?: string;
  initialFormat?: ExportFormat;
  filterParams?: {
    city?: string;
    search?: string;
    verificationStatus?: VerificationStatus;
  };
  onExportSuccess?: (count: number, category: string, format: ExportFormat) => void;
}

export const ExportCsvModal: React.FC<ExportCsvModalProps> = ({
  isOpen,
  onClose,
  initialCategory,
  initialFormat = 'csv',
  filterParams,
  onExportSuccess,
}) => {
  const [exportFormat, setExportFormat] = useState<ExportFormat>('csv');
  const [categories, setCategories] = useState<CategoryStat[]>([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchFilter, setSearchFilter] = useState('');
  const [isDownloading, setIsDownloading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [successInfo, setSuccessInfo] = useState<{ count: number; categoryName: string; format: ExportFormat } | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchCategories = async () => {
    try {
      setIsLoadingCategories(true);
      setErrorMessage(null);
      const data = await leadsApi.getCategories();
      setCategories(data || []);
    } catch (err: any) {
      console.error('Failed to load categories for export modal:', err);
      setErrorMessage(err.message || 'Failed to fetch categories.');
    } finally {
      setIsLoadingCategories(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      setIsSuccess(false);
      setSuccessInfo(null);
      setErrorMessage(null);
      setSearchFilter('');
      setExportFormat(initialFormat);
      setSelectedCategory(initialCategory !== undefined ? initialCategory : 'ALL');
      fetchCategories();
    } else {
      setSelectedCategory(null);
    }
  }, [isOpen, initialCategory, initialFormat]);

  const totalLeads = useMemo(() => {
    return categories.reduce((sum, c) => sum + (c.leadCount || 0), 0);
  }, [categories]);

  const filteredCategories = useMemo(() => {
    if (!searchFilter.trim()) return categories;
    return categories.filter((c) =>
      c.category.toLowerCase().includes(searchFilter.toLowerCase().trim())
    );
  }, [categories, searchFilter]);

  const selectedCount = useMemo(() => {
    if (selectedCategory === null) return 0;
    if (selectedCategory === 'ALL' || selectedCategory === '') return totalLeads;
    const match = categories.find((c) => c.category === selectedCategory);
    return match ? match.leadCount : 0;
  }, [selectedCategory, categories, totalLeads]);

  const selectedDisplayName = useMemo(() => {
    if (selectedCategory === null) return '';
    if (selectedCategory === 'ALL' || selectedCategory === '') return 'All Categories';
    return selectedCategory;
  }, [selectedCategory]);

  const handleDownload = async () => {
    if (selectedCategory === null || isDownloading) return;
    setErrorMessage(null);
    setIsSuccess(false);

    const categoryParam =
      selectedCategory === 'ALL' || selectedCategory === '' ? undefined : selectedCategory;

    try {
      setIsDownloading(true);
      if (exportFormat === 'excel') {
        await leadsApi.downloadExcel({
          category: categoryParam,
          city: filterParams?.city,
          search: filterParams?.search,
          verificationStatus: filterParams?.verificationStatus,
        });
      } else {
        await leadsApi.downloadCsv({
          category: categoryParam,
          city: filterParams?.city,
          search: filterParams?.search,
          verificationStatus: filterParams?.verificationStatus,
        });
      }

      const count = selectedCount;
      const name = selectedDisplayName;
      setIsSuccess(true);
      setSuccessInfo({ count, categoryName: name, format: exportFormat });

      if (onExportSuccess) {
        onExportSuccess(count, name, exportFormat);
      }
    } catch (err: any) {
      console.error(`Failed to download ${exportFormat}:`, err);
      setErrorMessage(err.message || `Failed to download ${exportFormat.toUpperCase()} file.`);
    } finally {
      setIsDownloading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Export Discovered Leads"
      subtitle="Select file format and category scope"
      maxWidth="600px"
    >
      {/* Success Notification */}
      {isSuccess && successInfo && (
        <div
          style={{
            padding: '1rem',
            backgroundColor: 'var(--accent-emerald-subtle)',
            border: '1px solid rgba(16, 185, 129, 0.25)',
            borderRadius: '10px',
            color: 'var(--accent-emerald)',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.625rem',
          }}
        >
          <CheckCircle2 size={20} style={{ flexShrink: 0 }} />
          <div style={{ fontSize: '0.8125rem' }}>
            <span style={{ fontWeight: 700 }}>
              {successInfo.format.toUpperCase()} export started ({successInfo.count} leads)
            </span>
          </div>
        </div>
      )}

      {/* Error Message */}
      {errorMessage && (
        <div
          style={{
            padding: '1rem',
            backgroundColor: 'var(--accent-rose-subtle)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            borderRadius: '10px',
            color: 'var(--accent-rose)',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.625rem',
          }}
        >
          <AlertCircle size={20} style={{ flexShrink: 0 }} />
          <span style={{ fontSize: '0.8125rem', fontWeight: 600 }}>{errorMessage}</span>
        </div>
      )}

      {/* Format Selector */}
      <div style={{ marginBottom: '1.25rem' }}>
        <label className="form-label" style={{ marginBottom: '0.5rem' }}>Choose Format</label>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
          <button
            type="button"
            onClick={() => setExportFormat('csv')}
            style={{
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              border: `2px solid ${exportFormat === 'csv' ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
              backgroundColor: exportFormat === 'csv' ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              cursor: 'pointer',
              textAlign: 'left',
            }}
          >
            <Table size={18} color={exportFormat === 'csv' ? 'var(--accent-indigo)' : 'var(--text-muted)'} />
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.8125rem', color: 'var(--text-main)' }}>CSV (.csv)</div>
              <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>Raw tabular data</div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setExportFormat('excel')}
            style={{
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              border: `2px solid ${exportFormat === 'excel' ? 'var(--accent-emerald)' : 'var(--border-subtle)'}`,
              backgroundColor: exportFormat === 'excel' ? 'var(--accent-emerald-subtle)' : 'var(--bg-surface)',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              cursor: 'pointer',
              textAlign: 'left',
            }}
          >
            <FileSpreadsheet size={18} color={exportFormat === 'excel' ? 'var(--accent-emerald)' : 'var(--text-muted)'} />
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.8125rem', color: 'var(--text-main)' }}>Excel (.xlsx)</div>
              <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>Spreadsheet file</div>
            </div>
          </button>
        </div>
      </div>

      {/* Category List */}
      <div style={{ marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
          <label className="form-label" style={{ margin: 0 }}>Category</label>
          <input
            style={{ padding: '0.3rem 0.6rem', fontSize: '0.75rem', borderRadius: '6px', border: '1px solid var(--border-subtle)', background: 'var(--bg-surface)', color: 'var(--text-main)' }}
            placeholder="Search category..."
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
          />
        </div>

        {isLoadingCategories ? (
          <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <span className="spinner" style={{ width: '18px', height: '18px' }} />
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', maxHeight: '180px', overflowY: 'auto', padding: '0.25rem' }}>
            <button
              type="button"
              onClick={() => setSelectedCategory('ALL')}
              style={{
                padding: '0.625rem 0.75rem',
                borderRadius: '6px',
                border: `1px solid ${selectedCategory === 'ALL' || selectedCategory === '' ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
                backgroundColor: selectedCategory === 'ALL' || selectedCategory === '' ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
                color: selectedCategory === 'ALL' || selectedCategory === '' ? 'var(--accent-indigo)' : 'var(--text-main)',
                fontWeight: 600,
                fontSize: '0.75rem',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                cursor: 'pointer',
              }}
            >
              <span>All Categories</span>
              <span style={{ opacity: 0.7 }}>{totalLeads}</span>
            </button>

            {filteredCategories.map((c) => {
              const isSelected = selectedCategory === c.category;
              return (
                <button
                  key={c.category}
                  type="button"
                  onClick={() => setSelectedCategory(c.category)}
                  style={{
                    padding: '0.625rem 0.75rem',
                    borderRadius: '6px',
                    border: `1px solid ${isSelected ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
                    backgroundColor: isSelected ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
                    color: isSelected ? 'var(--accent-indigo)' : 'var(--text-main)',
                    fontWeight: 600,
                    fontSize: '0.75rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    cursor: 'pointer',
                    overflow: 'hidden',
                  }}
                >
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.category}</span>
                  <span style={{ opacity: 0.7, marginLeft: '4px' }}>{c.leadCount}</span>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Footer Buttons */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem', borderTop: '1px solid var(--border-subtle)', paddingTop: '1.25rem' }}>
        <Button variant="secondary" size="md" onClick={onClose} disabled={isDownloading}>
          Cancel
        </Button>

        <Button
          variant="indigo"
          size="md"
          isLoading={isDownloading}
          disabled={selectedCategory === null}
          icon={<Download size={16} />}
          onClick={handleDownload}
        >
          Download {exportFormat.toUpperCase()} ({selectedCount})
        </Button>
      </div>
    </Modal>
  );
};
