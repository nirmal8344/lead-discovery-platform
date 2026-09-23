import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
}) => {
  if (totalElements === 0) return null;

  const startItem = currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0.875rem 1.25rem',
        borderTop: '1px solid var(--border-subtle)',
        backgroundColor: 'var(--bg-surface)',
        fontSize: '0.8125rem',
        color: 'var(--text-muted)',
      }}
    >
      <div>
        Showing <span className="font-semibold text-main">{startItem}</span> to{' '}
        <span className="font-semibold text-main">{endItem}</span> of{' '}
        <span className="font-semibold text-main">{totalElements}</span> entries
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}>
        <Button
          variant="secondary"
          size="sm"
          disabled={currentPage === 0}
          onClick={() => onPageChange(currentPage - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft size={16} />
          <span>Previous</span>
        </Button>
        <span style={{ padding: '0 0.5rem', fontWeight: 600, color: 'var(--text-main)' }}>
          Page {currentPage + 1} of {Math.max(1, totalPages)}
        </span>
        <Button
          variant="secondary"
          size="sm"
          disabled={currentPage >= totalPages - 1}
          onClick={() => onPageChange(currentPage + 1)}
          aria-label="Next page"
        >
          <span>Next</span>
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
};
