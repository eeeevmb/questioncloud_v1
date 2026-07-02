import apiClient from './client';
import type { QuestionKnowledgeTag } from '../types/knowledge';

export function fetchQuestionKnowledgeTags(questionVersionId: string) {
  return apiClient.get<QuestionKnowledgeTag[]>(`/api/v1/knowledge/questions/${questionVersionId}/tags`);
}
