export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
  hasPrevious: boolean;
  hasNext: boolean;
}
