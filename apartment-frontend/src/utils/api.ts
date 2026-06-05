import axios from 'axios';
import { i18n } from '../main';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

// Request interceptor for token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for auth errors and i18n error codes
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('roles');
      const currentPath = window.location.pathname;
      if (currentPath.startsWith('/m')) {
        window.location.href = '/m/auth';
      } else {
        window.location.href = '/login';
      }
    }

    // Translate error codes from backend
    const data = error.response?.data;
    if (data && data.code) {
      const i18nKey = 'errors.' + data.code;
      const args = Array.isArray(data.args) ? data.args : [];
      try {
        const translated = i18n.global.t(i18nKey, args);
        // If the key exists (translated !== key itself), use it
        if (translated !== i18nKey) {
          error.response.data = { message: translated, code: data.code };
        }
      } catch (e) {
        // Fallback: use raw code
      }
    }

    return Promise.reject(error);
  }
);

export default api;
