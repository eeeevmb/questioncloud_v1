import type { QuestionOption } from './question';

export interface GenerateQuestionDraftReq {
  topic: string;
  typeCode: string;
  scene?: string;
  extraRequirements?: string;
}

export interface QuestionDraft {
  typeCode?: string | null;
  title?: string | null;
  stem?: string | null;
  options?: QuestionOption[] | null;
  answer?: string | null;
  correctOptions?: string[] | null;
  judgeAnswer?: string | null;
  solution?: string | null;
  difficulty?: number | null;
  assumptions?: string | null;
}
