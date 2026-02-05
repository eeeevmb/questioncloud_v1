export interface CollectionPayload {
  name: string;
  description: string;
}

export interface CollectionView {
  id: string;
  name: string;
  description: string | null;
}

export interface CollectionQuestionQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  sortField?: string;
  sortDirection?: 'ASC' | 'DESC';
  typeCode?: string;
  levelMin?: number;
  levelMax?: number;
}
