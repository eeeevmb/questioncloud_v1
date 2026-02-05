export interface UserBasicInfo {
  userId: string;
  username: string;
  email: string;
  phone: string | null;
  status: number;
}

export interface LoginResponse {
  userId: string;
  username: string;
  tokenName: string;
  tokenValue: string;
}
