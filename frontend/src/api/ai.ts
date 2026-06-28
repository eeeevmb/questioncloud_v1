import apiClient from './client';
import type { AgentPaperDraftVO, GeneratePaperDraftReq, GenerateQuestionDraftReq, QuestionDraft } from '../types/ai';

export function generateQuestionDraft(payload: GenerateQuestionDraftReq) {
  return apiClient.post<QuestionDraft>('/api/v1/agent/question-draft/generate', payload);
}

export function generateAgentPaperDraft(payload: GeneratePaperDraftReq) {
  return apiClient.post<AgentPaperDraftVO>('/api/v1/agent/paper-draft/generate', payload);
}
