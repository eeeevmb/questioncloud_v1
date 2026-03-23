import apiClient from './client';
import type {
  PaperBasicVO,
  PaperCreatedResponse,
  PaperDetailVO,
  PaperItemRef,
  PaperItemSavePayload,
  PaperPageQuery,
  PaperPageResult,
  PaperRandomBuildReq,
  PaperSavePayload
} from '../types/paper';

export function fetchPaperPage(query: PaperPageQuery) {
  return apiClient.get<PaperPageResult>('/api/v1/paper', {
    params: query
  });
}

export function createPaper(payload: PaperSavePayload) {
  return apiClient.post<PaperCreatedResponse>('/api/v1/paper', payload);
}

export function fetchPaperDetail(paperId: string) {
  return apiClient.get<PaperDetailVO>(`/api/v1/paper/${paperId}`);
}

export function updatePaper(paperId: string, payload: PaperSavePayload) {
  return apiClient.put<PaperBasicVO>(`/api/v1/paper/${paperId}`, payload);
}

export function deletePaper(paperId: string) {
  return apiClient.delete<void>(`/api/v1/paper/${paperId}`);
}

export function clearPaperItems(paperId: string) {
  return apiClient.delete<void>(`/api/v1/paper/${paperId}/items`);
}

export function savePaperItems(paperId: string, payload: PaperItemSavePayload[]) {
  return apiClient.post<PaperItemRef[]>(`/api/v1/paper/${paperId}/items`, payload);
}

export function previewRandomBuild(paperId: string, payload: PaperRandomBuildReq) {
  return apiClient.post<PaperItemRef[]>(`/api/v1/paper/${paperId}/random-preview`, payload);
}
