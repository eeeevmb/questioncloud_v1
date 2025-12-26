import apiClient from './client';
import type { CollectionPayload, CollectionQuestionQuery, CollectionView } from '../types/collection';
import type { QuestionSummaryPage } from '../types/question';

export function fetchCollections() {
  return apiClient.get<CollectionView[]>('/api/v1/collection');
}

export function createCollection(payload: CollectionPayload) {
  return apiClient.post<CollectionView>('/api/v1/collection', payload);
}

export function updateCollection(collectionId: string, payload: CollectionPayload) {
  return apiClient.put<CollectionView>(`/api/v1/collection/${collectionId}`, payload);
}

export function deleteCollection(collectionId: string) {
  return apiClient.delete<void>(`/api/v1/collection/${collectionId}`);
}

export function fetchCollectionQuestions(collectionId: string, query: CollectionQuestionQuery) {
  return apiClient.get<QuestionSummaryPage>(`/api/v1/collection/${collectionId}/questions`, {
    params: query
  });
}
