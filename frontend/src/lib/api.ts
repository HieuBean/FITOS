import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('fitos_access_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for handling 401 unauthorized
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      if (typeof window !== 'undefined') {
        const refreshToken = localStorage.getItem('fitos_refresh_token');
        if (refreshToken) {
          try {
            const res = await axios.post(
              (process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080/api') + '/auth/refresh',
              { refreshToken }
            );
            const { accessToken } = res.data;
            localStorage.setItem('fitos_access_token', accessToken);
            document.cookie = `fitos_token=${accessToken}; path=/; max-age=604800; SameSite=Lax`;
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return api(originalRequest);
          } catch (refreshErr) {
            localStorage.removeItem('fitos_access_token');
            localStorage.removeItem('fitos_refresh_token');
            localStorage.removeItem('fitos_user');
            document.cookie = 'fitos_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
            if (!window.location.pathname.startsWith('/login')) {
              window.location.href = '/login';
            }
          }
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
