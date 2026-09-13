export type RoleName = "TEAM_MEMBER" | "MANAGER";

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
  role: RoleName;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  name: string;
  email: string;
  role: RoleName;
}

export interface AuthUser {
  userId: string;
  name: string;
  email: string;
  role: RoleName;
}
