import apiClient from './client';
import type {
  PaperBasicVO,
  PaperCreatedResponse,
  PaperDetailVO,
  PaperItemRef,
  PaperItemDetailRef,
  PaperItemSavePayload,
  PaperPageQuery,
  PaperPageResult,
  PaperRandomBuildReq,
  PaperSavePayload,
  RandomReplaceReq
} from '../types/paper';

export function fetchPaperPage(query: PaperPageQuery) {
  return apiClient.get<PaperPageResult>('/api/v1/paper', {
    params: query
  });
}

export function fetchPaperDetail(paperId: string) {
  return apiClient.get<PaperDetailVO>(`/api/v1/paper/${paperId}`);
}

export function createPaper(payload: PaperSavePayload) {
  return apiClient.post<PaperCreatedResponse>('/api/v1/paper', payload);
}

export function updatePaper(paperId: string, payload: PaperSavePayload) {
  return apiClient.put<void>(`/api/v1/paper/${paperId}`, payload);
}

export function deletePaper(paperId: string) {
  return apiClient.delete<void>(`/api/v1/paper/${paperId}`);
}

export function clearPaperItems(paperId: string) {
  return apiClient.delete<void>(`/api/v1/paper/${paperId}/items`);
}

export function savePaperItems(paperId: string, payload: PaperItemSavePayload[]) {
  return apiClient.post<PaperItemRef[]>(`/api/v1/paper/${paperId}/items`, { items: payload });
}

export function previewRandomBuild(paperId: string, payload: PaperRandomBuildReq) {
  return apiClient.post<PaperItemDetailRef[]>('/api/v1/paper/actions/random-preview', payload);
}

export function randomReplaceItem(paperId: string, payload: RandomReplaceReq) {
  return apiClient.post<PaperItemDetailRef>('/api/v1/paper/actions/replace-item', payload);
}