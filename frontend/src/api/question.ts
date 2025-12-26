import apiClient from './client';
import type { QuestionDetail, QuestionFormModel, QuestionSummaryPage, QuestionAsset, QuestionOption } from '../types/question';

export interface QuestionCreatedResponse {
  questionId: string;
  questionVersionId: string;
  collectionId: string;
}

export type QuestionCreatePayload = QuestionFormModel;

export interface QuestionUpdatePayload {
  title?: string;
  stem: string;
  options?: QuestionOption[];
  answer?: string | null;
  correctOptions?: string[];
  judgeAnswer?: string | null;
  solution?: string | null;
  assets?: QuestionAsset[];
}

export function createQuestion(payload: QuestionCreatePayload) {
  return apiClient.post<QuestionCreatedResponse>('/api/v1/question', payload);
}

export function updateQuestion(questionId: string, payload: QuestionUpdatePayload) {
  return apiClient.put<void>(`/api/v1/question/${questionId}`, payload);
}

export function deleteQuestion(questionId: string) {
  return apiClient.delete<void>(`/api/v1/question/${questionId}`);
}

export function fetchQuestionDetail(questionId: string) {
  return apiClient.get<QuestionDetail>(`/api/v1/question/${questionId}`);
}
