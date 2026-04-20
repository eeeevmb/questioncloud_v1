import apiClient from './client';
import type { AgentPaperDraftVO, GeneratePaperDraftReq, GenerateQuestionDraftReq, QuestionDraft } from '../types/ai';

const AI_GENERATE_TIMEOUT_MS = 30000;

export function generateQuestionDraft(payload: GenerateQuestionDraftReq) {
  return apiClient.post<QuestionDraft>('/api/v1/agent/question-draft/generate', payload, {
    timeout: AI_GENERATE_TIMEOUT_MS
  });
}

export function generateAgentPaperDraft(payload: GeneratePaperDraftReq) {
  return apiClient.post<AgentPaperDraftVO>('/api/v1/agent/paper-draft/generate', payload, {
    timeout: AI_GENERATE_TIMEOUT_MS
  });
}
