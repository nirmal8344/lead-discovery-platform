import React from 'react';
import type { TaskStatus, VerificationStatus } from '../../types';

interface BadgeProps {
  status?: TaskStatus | VerificationStatus | string;
  variant?: 'emerald' | 'indigo' | 'amber' | 'rose' | 'sky' | 'gray';
  children?: React.ReactNode;
  showDot?: boolean;
}

export const Badge: React.FC<BadgeProps> = ({
  status,
  variant,
  children,
  showDot = true,
}) => {
  let selectedVariant: 'emerald' | 'indigo' | 'amber' | 'rose' | 'sky' | 'gray' = variant || 'gray';

  if (!variant && status) {
    switch (status) {
      case 'VERIFIED':
      case 'COMPLETED':
        selectedVariant = 'emerald';
        break;
      case 'RUNNING':
      case 'PARTIALLY_VERIFIED':
      case 'PARTIALLY_COMPLETED':
        selectedVariant = 'indigo';
        break;
      case 'QUEUED':
      case 'PENDING':
      case 'COMPLETED_WITH_NO_RESULTS':
        selectedVariant = 'amber';
        break;
      case 'FAILED':
      case 'CANCELLED':
        selectedVariant = 'rose';
        break;
      case 'CREATED':
        selectedVariant = 'sky';
        break;
      default:
        selectedVariant = 'gray';
    }
  }

  const label = children || (status ? status.replace(/_/g, ' ') : '');

  return (
    <span className={`badge badge-${selectedVariant}`}>
      {showDot && <span className="badge-dot" />}
      {label}
    </span>
  );
};
