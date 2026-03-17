import type { PageResult } from './common';
import type { QuestionOption } from './question';

export type ImportSessionStatus = 0 | 1 | 2 | 3 | 4 | 5;
export type ImportItemStatus = 0 | 1 | 2;
export type ImportItemQueryStatus = 'ALL' | 'VALID' | 'INVALID';

export interface ImportSessionVO {
  importId: string;
  status: ImportSessionStatus;
  total: number;
  validCnt: number;
  invalidCnt: number;
  progress: number;
}

export interface ImportCreateVO {
  importId: string;
  parseJobId: string;
}

export interface ImportErrorReport {
  field: string;
  code: string;
  message: string;
}

export interface QuestionDraft {
  typeCode: string;
  title: string;
  stem: string;
  options?: QuestionOption[];
  answer?: string | null;
  correctOptions?: string[];
  judgeAnswer?: string | null;
  solution?: string | null;
  difficulty?: number | null;
}

export interface ImportItemVO {
  itemId: string;
  indexNo: number;
  collectionName: string;
  draft: QuestionDraft;
  status: ImportItemStatus;
  errors: ImportErrorReport[];
  updatedAt: string;
}

export interface ImportSessionCreateReq {
  fileId: string;
  format: string;
}

export interface ImportItemBatchUpdateItem {
  itemId: string;
  draft: QuestionDraft;
}

export interface ImportItemBatchUpdateReq {
  items: ImportItemBatchUpdateItem[];
}

export interface ImportCommitReq {
  ignoreInvalidDraft?: boolean;
}

export interface ImportItemPageQuery {
  pageNum?: number;
  pageSize?: number;
  status?: ImportItemQueryStatus;
  keyword?: string;
  sortField?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export type ImportItemPageResult = PageResult<ImportItemVO>;
