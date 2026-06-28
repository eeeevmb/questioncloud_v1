import apiClient from './client';
import type { UserBasicInfo, LoginResponse } from '../types/user';

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  verificationCode: string;
}

export interface LoginPayload {
  account: string;
  password: string;
}

export interface SendVerificationCodePayload {
  email: string;
}

export interface ResetPasswordPayload {
  email: string;
  verificationCode: string;
  newPassword: string;
}

export interface AvatarResponse {
  url: string;
}

export function registerUser(payload: RegisterPayload) {
  return apiClient.post<string>('/api/v1/user/register', payload);
}

export function sendRegisterCode(payload: SendVerificationCodePayload) {
  return apiClient.post<void>('/api/v1/user/send-register-code', payload);
}

export function sendResetPasswordCode(payload: SendVerificationCodePayload) {
  return apiClient.post<void>('/api/v1/user/send-reset-password-code', payload);
}

export function resetPassword(payload: ResetPasswordPayload) {
  return apiClient.post<void>('/api/v1/user/reset-password', payload);
}

export function loginUser(payload: LoginPayload) {
  return apiClient.post<LoginResponse>('/api/v1/user/login', payload);
}

export function logoutUser() {
  return apiClient.post<void>('/api/v1/user/logout');
}

export function fetchBasicInfo() {
  return apiClient.get<UserBasicInfo>('/api/v1/user/basicInfo');
}

export function uploadAvatar(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return apiClient.post<AvatarResponse>('/api/v1/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}
