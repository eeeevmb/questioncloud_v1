import apiClient from './client';
import type {
  ImportCommitReq,
  ImportCreateVO,
  ImportItemBatchUpdateReq,
  ImportItemPageQuery,
  ImportItemPageResult,
  ImportItemVO,
  ImportSessionCreateReq,
  ImportSessionVO
} from '../types/import';

export function createImportSession(payload: ImportSessionCreateReq) {
  return apiClient.post<ImportCreateVO>('/api/v1/imports', payload);
}

export function fetchImportSessions() {
  return apiClient.get<ImportSessionVO[]>('/api/v1/imports');
}

export function fetchImportSession(importId: string) {
  return apiClient.get<ImportSessionVO>(`/api/v1/imports/${importId}`);
}

export function fetchImportItems(importId: string, query: ImportItemPageQuery) {
  return apiClient.get<ImportItemPageResult>(`/api/v1/imports/${importId}/items`, {
    params: query
  });
}

export function updateImportItems(importId: string, payload: ImportItemBatchUpdateReq) {
  return apiClient.put<ImportItemVO[]>(`/api/v1/imports/${importId}/items`, payload);
}

export function commitImport(importId: string, payload?: ImportCommitReq) {
  return apiClient.post<void>(`/api/v1/imports/${importId}/commit`, payload);
}

export function cancelImport(importId: string) {
  return apiClient.post<void>(`/api/v1/imports/${importId}/cancel`);
}
