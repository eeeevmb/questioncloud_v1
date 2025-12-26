import apiClient from './client';

export interface FileUploadResponse {
  fileId: string;
}

export function uploadAssetFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient.post<FileUploadResponse>('/api/v1/common/file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}
