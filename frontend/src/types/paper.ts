import type { PageResult } from './common';

export interface PaperSavePayload {
  title: string;
  description?: string | null;
}

export interface PaperCreatedResponse {
  paperId: string;
}

export interface PaperBasicVO {
  id: string;
  title: string;
  description: string | null;
  status: number;
  totalItems: number;
  totalScore: number;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaperListItemVO {
  id: string;
  title: string;
  totalItems: number;
  totalScore: number;
  updatedAt: string;
  createdAt: string;
}

export interface PaperItemVO {
  paperId: string;
  questionId: string;
  questionVersionId: string;
  score: number;
  seq: number;
  questionTitle: string;
  stem: string;
  typeCode: string;
  versionNo: number;
  difficulty: number;
  correctRate: number;
}

export interface PaperItemRef {
  questionId: string;
  questionVersionId: string;
}

export interface PaperItemDetailRef {
  questionId: string;
  questionVersionId: string;
  difficulty?: number;
  questionTitle?: string;
  stem?: string;
  typeCode?: string;
}

export interface PaperItemDetailVO {
  questionId: string;
  questionVersionId: string;
  difficulty?: number;
  questionTitle?: string;
  stem?: string;
  typeCode?: string;
}

export interface PaperDetailVO {
  id: string;
  title: string;
  description: string | null;
  status: number;
  totalItems: number;
  totalScore: number;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
  items: PaperItemVO[];
}

export interface PaperDraftItemRow extends PaperItemRef {
  key: string;
  score: number;
  typeCode?: string;
  difficulty?: number;
  source?: 'preview' | 'manual' | 'paper';
  questionTitle?: string;
  stem?: string;
  replacing?: boolean;
}

export interface PaperPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
}

export interface PaperRandomBuildRule {
  typeCode: string;
  count: number;
  expectedDifficulty?: number;
}

export interface PaperRandomBuildReq {
  collectionIds: string[];
  rules: PaperRandomBuildRule[];
}

export interface RandomReplaceReq {
  collectionIds: string[];
  excludedQuestionIds?: string[];
  typeCode: string;
  expectedDifficulty?: number;
}

export interface PaperItemSavePayload {
  questionId: string;
  questionVersionId: string;
  score: number;
}

export type PaperPageResult = PageResult<PaperListItemVO>;
