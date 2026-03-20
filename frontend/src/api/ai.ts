import apiClient from './client';
import type { GenerateQuestionDraftReq, QuestionDraft } from '../types/ai';

export function generateQuestionDraft(payload: GenerateQuestionDraftReq) {
  return apiClient.post<QuestionDraft>('/api/v1/agent/question-draft/generate', payload);
}
