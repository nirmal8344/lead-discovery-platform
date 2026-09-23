import type {
  AuthResponse,
  CategoryStat,
  CreateTaskPayload,
  LeadDetail,
  LeadFilterParams,
  LeadListItem,
  Page,
  ScrapingTask,
  TaskError,
  TaskProgress,
  TaskStartResponse,
  User,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || '';

export class ApiRequestError extends Error {
  status: number;
  errorPayload?: any;

  constructor(message: string, status: number, errorPayload?: any) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.errorPayload = errorPayload;
  }
}

let onUnauthorizedCallback: (() => void) | null = null;

export const setOnUnauthorizedCallback = (callback: () => void) => {
  onUnauthorizedCallback = callback;
};

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401 && !endpoint.includes('/api/auth/login')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (onUnauthorizedCallback) {
        onUnauthorizedCallback();
      }
    }

    let errorData;
    try {
      errorData = await response.json();
    } catch {
      errorData = { message: response.statusText || 'An unexpected error occurred' };
    }

    const message = errorData.message || (errorData.details && errorData.details.join(', ')) || 'Request failed';
    throw new ApiRequestError(message, response.status, errorData);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

// ----------------------------------------------------
// Authentication API
// ----------------------------------------------------
export const authApi = {
  login: async (credentials: { email: string; password: string }): Promise<AuthResponse> => {
    return request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
  },

  register: async (payload: { name: string; email: string; password: string }): Promise<User> => {
    return request<User>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  getMe: async (): Promise<User> => {
    return request<User>('/api/auth/me');
  },
};

// ----------------------------------------------------
// Tasks API
// ----------------------------------------------------
export const tasksApi = {
  createTask: async (payload: CreateTaskPayload): Promise<ScrapingTask> => {
    return request<ScrapingTask>('/api/tasks', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  listTasks: async (page = 0, size = 20): Promise<Page<ScrapingTask>> => {
    return request<Page<ScrapingTask>>(`/api/tasks?page=${page}&size=${size}`);
  },

  getTask: async (taskId: number): Promise<ScrapingTask> => {
    return request<ScrapingTask>(`/api/tasks/${taskId}`);
  },

  startTask: async (taskId: number): Promise<TaskStartResponse> => {
    return request<TaskStartResponse>(`/api/tasks/${taskId}/start`, {
      method: 'POST',
    });
  },

  cancelTask: async (taskId: number): Promise<ScrapingTask> => {
    return request<ScrapingTask>(`/api/tasks/${taskId}/cancel`, {
      method: 'POST',
    });
  },

  getProgress: async (taskId: number): Promise<TaskProgress> => {
    return request<TaskProgress>(`/api/tasks/${taskId}/progress`);
  },

  getTaskErrors: async (taskId: number): Promise<TaskError[]> => {
    return request<TaskError[]>(`/api/tasks/${taskId}/errors`);
  },

  getTaskLeads: async (taskId: number, page = 0, size = 20): Promise<Page<LeadListItem>> => {
    return request<Page<LeadListItem>>(`/api/tasks/${taskId}/leads?page=${page}&size=${size}`);
  },
};

// ----------------------------------------------------
// Leads API
// ----------------------------------------------------
export const leadsApi = {
  listLeads: async (params: LeadFilterParams = {}): Promise<Page<LeadListItem>> => {
    const query = new URLSearchParams();
    if (params.city) query.append('city', params.city);
    if (params.category) query.append('category', params.category);
    if (params.verificationStatus) query.append('verificationStatus', params.verificationStatus);
    if (params.search) query.append('search', params.search);
    if (params.page !== undefined) query.append('page', params.page.toString());
    if (params.size !== undefined) query.append('size', params.size.toString());
    if (params.sortBy) query.append('sortBy', params.sortBy);
    if (params.sortDirection) query.append('sortDirection', params.sortDirection);

    const queryString = query.toString();
    return request<Page<LeadListItem>>(`/api/leads${queryString ? `?${queryString}` : ''}`);
  },

  getCategories: async (): Promise<CategoryStat[]> => {
    return request<CategoryStat[]>('/api/leads/categories');
  },

  getLeadDetails: async (leadId: number): Promise<LeadDetail> => {
    return request<LeadDetail>(`/api/leads/${leadId}`);
  },

  deleteLead: async (leadId: number): Promise<{ message: string; leadId: number }> => {
    return request<{ message: string; leadId: number }>(`/api/leads/${leadId}`, {
      method: 'DELETE',
    });
  },

  downloadCsv: async (params: LeadFilterParams = {}): Promise<void> => {
    const token = localStorage.getItem('token');
    const query = new URLSearchParams();
    if (params.city) query.append('city', params.city);
    if (params.category) query.append('category', params.category);
    if (params.verificationStatus) query.append('verificationStatus', params.verificationStatus);
    if (params.search) query.append('search', params.search);

    const queryString = query.toString();
    const url = `${API_BASE_URL}/api/leads/export${queryString ? `?${queryString}` : ''}`;

    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });

    if (!response.ok) {
      throw new Error('Failed to export leads CSV');
    }

    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    const date = new Date().toISOString().slice(0, 10);
    const cat = params.category ? `_${params.category.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase()}` : '';
    link.setAttribute('download', `leads_export${cat}_${date}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(downloadUrl);
  },

  downloadExcel: async (params: LeadFilterParams = {}): Promise<void> => {
    const token = localStorage.getItem('token');
    const query = new URLSearchParams();
    if (params.city) query.append('city', params.city);
    if (params.category) query.append('category', params.category);
    if (params.verificationStatus) query.append('verificationStatus', params.verificationStatus);
    if (params.search) query.append('search', params.search);

    const queryString = query.toString();
    const url = `${API_BASE_URL}/api/leads/export/excel${queryString ? `?${queryString}` : ''}`;

    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });

    if (!response.ok) {
      throw new Error('Failed to export leads as Excel');
    }

    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    const date = new Date().toISOString().slice(0, 10);
    const cat = params.category ? `_${params.category.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase()}` : '';
    link.setAttribute('download', `leads_export${cat}_${date}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(downloadUrl);
  },
};
