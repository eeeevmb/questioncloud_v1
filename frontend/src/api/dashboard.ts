import apiClient from './client';
import type { DashboardOverviewVO } from '../types/dashboard';

export function fetchDashboardOverview() {
  return apiClient.get<DashboardOverviewVO>('/api/v1/dashboard/overview');
}
