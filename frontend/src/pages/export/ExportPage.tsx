import React, { useEffect, useState, useMemo } from 'react';
import {
  Download,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  Search,
  Table,
} from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { leadsApi } from '../../api/client';
import type { CategoryStat, VerificationStatus } from '../../types';

export const ExportPage: React.FC = () => {
  const [categories, setCategories] = useState<CategoryStat[]>([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [categorySearch, setCategorySearch] = useState('');

  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [city, setCity] = useState('');
  const [verificationStatus, setVerificationStatus] = useState<VerificationStatus | ''>('');
  const [search, setSearch] = useState('');

  const [isExporting, setIsExporting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [exportFormat, setExportFormat] = useState<'csv' | 'excel'>('csv');

  const fetchCategories = async () => {
    try {
      setIsLoadingCategories(true);
      const data = await leadsApi.getCategories();
      setCategories(data || []);
    } catch (err: any) {
      console.error('Failed to load categories:', err);
    } finally {
      setIsLoadingCategories(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const totalLeads = useMemo(() => {
    return categories.reduce((sum, c) => sum + (c.leadCount || 0), 0);
  }, [categories]);

  const filteredCategories = useMemo(() => {
    if (!categorySearch.trim()) return categories;
    return categories.filter((c) =>
      c.category.toLowerCase().includes(categorySearch.toLowerCase().trim())
    );
  }, [categories, categorySearch]);

  const handleExport = async (categoryOverride?: string) => {
    setErrorMessage(null);
    setIsSuccess(false);

    const targetCategory = categoryOverride !== undefined ? categoryOverride : selectedCategory;

    const params = {
      city: city.trim() || undefined,
      category: targetCategory.trim() || undefined,
      verificationStatus: verificationStatus || undefined,
      search: search.trim() || undefined,
    };

    try {
      setIsExporting(true);
      if (exportFormat === 'excel') {
        await leadsApi.downloadExcel(params);
      } else {
        await leadsApi.downloadCsv(params);
      }
      setIsSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.message || `Failed to download ${exportFormat === 'excel' ? 'Excel' : 'CSV'} export`);
    } finally {
      setIsExporting(false);
    }
  };

  const handleCategorySelect = (catName: string) => {
    if (selectedCategory === catName) {
      setSelectedCategory('');
    } else {
      setSelectedCategory(catName);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem', maxWidth: '1080px', margin: '0 auto', paddingBottom: '2rem' }}>
      {/* Header Banner */}
      <div
        style={{
          background: 'var(--hero-bg)',
          borderRadius: '16px',
          padding: '2rem 2.25rem',
          color: 'var(--hero-text)',
          boxShadow: 'var(--shadow-md)',
          border: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '1.5rem',
        }}
      >
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.025em', marginBottom: '0.5rem', color: '#ffffff' }}>
            Export Leads
          </h2>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8', lineHeight: 1.6, maxWidth: '640px', margin: 0 }}>
            Download complete spreadsheets of your discovered business leads in CSV or Excel format.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', background: 'rgba(255, 255, 255, 0.05)', padding: '0.875rem 1.25rem', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
          <div>
            <div style={{ fontSize: '0.6875rem', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Total Leads</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#38bdf8' }}>{totalLeads}</div>
          </div>
          <div style={{ height: '30px', width: '1px', background: 'rgba(255, 255, 255, 0.1)' }} />
          <div>
            <div style={{ fontSize: '0.6875rem', color: '#94a3b8', textTransform: 'uppercase', fontWeight: 600 }}>Categories</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#a855f7' }}>{categories.length}</div>
          </div>
        </div>
      </div>

      {/* Notifications */}
      {isSuccess && (
        <div
          style={{
            padding: '1rem',
            backgroundColor: 'var(--accent-emerald-subtle)',
            border: '1px solid rgba(16, 185, 129, 0.25)',
            borderRadius: '10px',
            color: 'var(--accent-emerald)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.625rem',
          }}
        >
          <CheckCircle2 size={18} />
          <span style={{ fontSize: '0.875rem', fontWeight: 600 }}>
            Spreadsheet download started successfully! Check your browser downloads folder.
          </span>
        </div>
      )}

      {errorMessage && (
        <div
          style={{
            padding: '1rem',
            backgroundColor: 'var(--accent-rose-subtle)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            borderRadius: '10px',
            color: 'var(--accent-rose)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.625rem',
          }}
        >
          <AlertCircle size={18} />
          <span style={{ fontSize: '0.875rem', fontWeight: 600 }}>{errorMessage}</span>
        </div>
      )}

      {/* Export Configuration Card */}
      <div className="card" style={{ padding: '1.75rem' }}>
        <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '1.25rem' }}>
          Export Settings
        </h3>

        {/* Format Selector */}
        <div style={{ marginBottom: '1.5rem' }}>
          <label className="form-label" style={{ marginBottom: '0.625rem' }}>File Format</label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', maxWidth: '440px' }}>
            <button
              type="button"
              onClick={() => setExportFormat('csv')}
              style={{
                padding: '0.875rem',
                borderRadius: '10px',
                border: `2px solid ${exportFormat === 'csv' ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
                backgroundColor: exportFormat === 'csv' ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
                display: 'flex',
                alignItems: 'center',
                gap: '0.625rem',
                cursor: 'pointer',
                textAlign: 'left',
              }}
            >
              <Table size={20} color={exportFormat === 'csv' ? 'var(--accent-indigo)' : 'var(--text-muted)'} />
              <div>
                <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-main)' }}>CSV (.csv)</div>
                <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>Universal tabular format</div>
              </div>
            </button>

            <button
              type="button"
              onClick={() => setExportFormat('excel')}
              style={{
                padding: '0.875rem',
                borderRadius: '10px',
                border: `2px solid ${exportFormat === 'excel' ? 'var(--accent-emerald)' : 'var(--border-subtle)'}`,
                backgroundColor: exportFormat === 'excel' ? 'var(--accent-emerald-subtle)' : 'var(--bg-surface)',
                display: 'flex',
                alignItems: 'center',
                gap: '0.625rem',
                cursor: 'pointer',
                textAlign: 'left',
              }}
            >
              <FileSpreadsheet size={20} color={exportFormat === 'excel' ? 'var(--accent-emerald)' : 'var(--text-muted)'} />
              <div>
                <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-main)' }}>Excel (.xlsx)</div>
                <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>Formatted spreadsheet</div>
              </div>
            </button>
          </div>
        </div>

        {/* Optional Filter Parameters */}
        <div className="two-col-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">City Filter (Optional)</label>
            <input
              className="form-input"
              placeholder="e.g. Austin"
              value={city}
              onChange={(e) => setCity(e.target.value)}
            />
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Search Keyword (Optional)</label>
            <input
              className="form-input"
              placeholder="e.g. Dental"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Status Filter (Optional)</label>
            <select
              className="form-select"
              value={verificationStatus}
              onChange={(e) => setVerificationStatus(e.target.value as VerificationStatus | '')}
            >
              <option value="">All Leads</option>
              <option value="VERIFIED">Verified Only</option>
              <option value="PARTIALLY_VERIFIED">Partially Verified</option>
              <option value="UNVERIFIED">Unverified</option>
            </select>
          </div>
        </div>

        {/* Category Selection Grid */}
        <div style={{ marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <label className="form-label" style={{ margin: 0 }}>Select Category</label>
            <div className="input-wrapper" style={{ maxWidth: '240px' }}>
              <span className="input-icon-left"><Search size={14} /></span>
              <input
                className="form-input input-with-icon-left"
                style={{ padding: '0.4rem 0.75rem 0.4rem 2rem', fontSize: '0.75rem' }}
                placeholder="Search categories..."
                value={categorySearch}
                onChange={(e) => setCategorySearch(e.target.value)}
              />
            </div>
          </div>

          {isLoadingCategories ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
              <span className="spinner" style={{ width: '20px', height: '20px' }} />
              <div style={{ marginTop: '0.5rem', fontSize: '0.8125rem' }}>Loading categories...</div>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '0.625rem', maxHeight: '220px', overflowY: 'auto', padding: '0.25rem' }}>
              {/* All Categories Pill */}
              <button
                type="button"
                onClick={() => setSelectedCategory('')}
                style={{
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: `1px solid ${selectedCategory === '' ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
                  backgroundColor: selectedCategory === '' ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
                  color: selectedCategory === '' ? 'var(--accent-indigo)' : 'var(--text-main)',
                  fontWeight: 600,
                  fontSize: '0.8125rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <span>All Categories</span>
                <span style={{ fontSize: '0.75rem', opacity: 0.7 }}>{totalLeads}</span>
              </button>

              {filteredCategories.map((c) => {
                const isSelected = selectedCategory === c.category;
                return (
                  <button
                    key={c.category}
                    type="button"
                    onClick={() => handleCategorySelect(c.category)}
                    style={{
                      padding: '0.75rem',
                      borderRadius: '8px',
                      border: `1px solid ${isSelected ? 'var(--accent-indigo)' : 'var(--border-subtle)'}`,
                      backgroundColor: isSelected ? 'var(--accent-indigo-subtle)' : 'var(--bg-surface)',
                      color: isSelected ? 'var(--accent-indigo)' : 'var(--text-main)',
                      fontWeight: 600,
                      fontSize: '0.8125rem',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      cursor: 'pointer',
                      textAlign: 'left',
                      overflow: 'hidden',
                    }}
                  >
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.category}</span>
                    <span style={{ fontSize: '0.75rem', opacity: 0.7, marginLeft: '4px' }}>{c.leadCount}</span>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Download Action Bar */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.75rem', borderTop: '1px solid var(--border-subtle)', paddingTop: '1.25rem' }}>
          <Button
            type="button"
            variant="indigo"
            size="lg"
            isLoading={isExporting}
            icon={<Download size={16} />}
            onClick={() => handleExport()}
          >
            Download {exportFormat.toUpperCase()}
          </Button>
        </div>
      </div>
    </div>
  );
};
