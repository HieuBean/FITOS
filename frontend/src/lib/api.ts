import axios, { InternalAxiosRequestConfig } from 'axios';

declare const process: {
  env: {
    NEXT_PUBLIC_BACKEND_URL?: string;
  };
};

const API_BASE_URL =
  (typeof process !== 'undefined' && process.env?.NEXT_PUBLIC_BACKEND_URL) ||
  'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('fitos_token');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error: unknown) => Promise.reject(error)
);

export default apiClient;
