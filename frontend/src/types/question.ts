import type { PageResult } from './common';

export interface QuestionAsset {
  fileId: string;
  section: 'PRO' | 'SOLU';
  ordinal: number;
  assetType?: 'IMAGE' | 'VIDEO' | 'FILE';
  slotId?: string | number | null;
}

export interface QuestionSummary {
  id: string;
  currentVersionId: string;
  versionNo: number;
  typeCode: string;
  title: string;
  difficulty: number;
  correctRate: number;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionDetail {
  id: string;
  currentVersionId: string;
  versionNo: number;
  typeCode: string;
  title: string;
  stem: string;
  answer: string | null;
  answerKey?: string | null;
  options?: QuestionOption[];
  correctOptions?: string[];
  judgeAnswer?: string | null;
  solution: string | null;
  assets: QuestionAsset[];
  attempts: number;
  correctCount: number;
  correctRate: number;
  difficulty: number;
  exposureFactor: number;
  lastExposedAt: string | null;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionFormModel {
  typeCode: string;
  title: string;
  stem: string;
  options?: QuestionOption[];
  answer: string | null;
  correctOptions?: string[];
  judgeAnswer?: 'T' | 'F' | '' | null;
  solution: string | null;
  difficulty: number | null;
  collectionId: string | null;
  assets: QuestionAsset[];
}

export interface QuestionOption {
  key: string;
  content: string;
}

export type QuestionSummaryPage = PageResult<QuestionSummary>;
