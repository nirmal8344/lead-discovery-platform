export type Role = 'USER' | 'ADMIN';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export type TaskStatus =
  | 'CREATED'
  | 'QUEUED'
  | 'RUNNING'
  | 'COMPLETED'
  | 'COMPLETED_WITH_NO_RESULTS'
  | 'PARTIALLY_COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type VerificationStatus = 'UNVERIFIED' | 'PARTIALLY_VERIFIED' | 'VERIFIED' | 'FAILED';

export type ContactVerificationStatus = 'VERIFIED' | 'EXTERNAL' | 'UNVERIFIED';

export interface CategoryStat {
  category: string;
  leadCount: number;
}


export interface ScrapingTask {
  id: number;
  location: string;
  keyword: string;
  maxResults: number;
  maxPagesPerSite: number;
  searchRadiusKm?: number;
  requiredFields?: string;
  optionalFilters?: string;
  status: TaskStatus;
  progressPercentage: number;
  currentStage?: string;
  discoveredBusinesses: number;
  processedWebsites: number;
  leadsSaved: number;
  failedRecords: number;
  startTime?: string;
  endTime?: string;
  errorMessage?: string;
  createdAt: string;
}

export interface TaskProgress {
  taskId: number;
  status: TaskStatus;
  progressPercentage: number;
  currentStage: string;
  totalDiscovered: number;
  totalCrawled: number;
  totalSaved: number;
  totalFailed: number;
  startTime?: string;
  endTime?: string;
  errorMessage?: string;
  lastUpdated: string;
}

export interface TaskStartResponse {
  taskId: number;
  status: TaskStatus;
  message: string;
}

export interface CreateTaskPayload {
  location: string;
  keyword: string;
  maxResults?: number;
  maxPagesPerSite?: number;
  searchRadiusKm?: number;
  requiredFields?: string;
  optionalFilters?: string;
}

export interface TaskError {
  id: number;
  taskId: number;
  url: string;
  domain: string;
  errorType: string;
  errorMessage: string;
  stage: string;
  createdAt: string;
}

export interface LeadListItem {
  id: number;
  leadId?: number;
  taskId?: number;
  businessName: string;
  category?: string;
  city?: string;
  state?: string;
  country?: string;
  officialWebsite?: string;
  websiteUrl?: string;
  primaryEmail?: string;
  primaryPhone?: string;
  verificationStatus: VerificationStatus;
  confidenceScore: number;
  createdAt: string;
}

export interface WebsiteDto {
  id: number;
  url: string;
  normalizedUrl: string;
  isOfficial: boolean;
  status: string;
  lastCrawledAt?: string;
}

export interface EmailDto {
  id: number;
  rawValue: string;
  normalizedValue: string;
  sourcePageUrl?: string;
  sourceDomain?: string;
  verificationStatus?: ContactVerificationStatus;
  extractedAt?: string;
}

export interface PhoneDto {
  id: number;
  rawValue: string;
  normalizedValue: string;
  phoneType: string;
  sourcePageUrl?: string;
  sourceDomain?: string;
  verificationStatus?: ContactVerificationStatus;
  extractedAt?: string;
}

export interface SocialLinkDto {
  id: number;
  platform: string;
  url: string;
  sourcePageUrl?: string;
  sourceDomain?: string;
  verificationStatus?: ContactVerificationStatus;
  extractedAt?: string;
}

export interface ContactDto {
  id: number;
  contactPerson: string;
  role?: string;
  sourcePageUrl?: string;
}

export interface SourcePageDto {
  id: number;
  pageUrl: string;
  pageType: string;
  httpStatus: number;
  fetchedAt?: string;
}

export interface LeadDetail {
  id: number;
  taskId: number;
  businessName: string;
  normalizedName: string;
  category?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  sourceUrl?: string;
  officialDomain?: string;
  verificationStatus: VerificationStatus;
  confidenceScore: number;
  missingFields?: string;
  processingStatus: string;
  createdAt: string;
  websites: WebsiteDto[];
  sourcePages?: SourcePageDto[];
  emailAddresses: EmailDto[];
  phoneNumbers: PhoneDto[];
  socialLinks: SocialLinkDto[];
  contacts: ContactDto[];
}

export interface LeadFilterParams {
  city?: string;
  category?: string;
  verificationStatus?: VerificationStatus;
  search?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
  timestamp?: string;
}
