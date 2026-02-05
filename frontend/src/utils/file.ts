export function buildFileViewUrl(fileId: string | number | null | undefined) {
  if (fileId === null || fileId === undefined) {
    return '';
  }
  const id = typeof fileId === 'number' ? String(fileId) : fileId.trim();
  if (!id) {
    return '';
  }
  return `/api/v1/common/file/${id}/view`;
}
