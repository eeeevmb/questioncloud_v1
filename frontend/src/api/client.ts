import axios from 'axios';
import { showError } from '../utils/messages';
import { triggerUnauthorized } from '../utils/auth-events';

interface ResultVO<T> {
  code: string;
  message: string;
  data: T;
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true,
  timeout: 15000
});

const handledErrorCodes = new Set([
  '401000',
  '403000',
  '404000',
  'Q00001',
  'Q10001',
  '400000',
  '400001',
  'Q00002',
  'Q00005',
  '500000',
  '500001'
]);

apiClient.interceptors.response.use(
  (response) => {
    const payload = response.data as ResultVO<unknown>;
    if (!payload) {
      return response.data;
    }

    if (payload.code === '200000') {
      return payload.data;
    }

    const message = payload.message || '请求失败';
    if (payload.code === '401000') {
      triggerUnauthorized(message);
    }
    if (handledErrorCodes.has(payload.code)) {
      showError(message);
    } else {
      showError(message);
    }

    return Promise.reject(new Error(message));
  },
  (error) => {
    if (error.response?.status === 401) {
      triggerUnauthorized('用户未登录');
      showError('用户未登录');
    } else {
      showError(error.message || '网络异常');
    }
    return Promise.reject(error);
  }
);

export default apiClient;
