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

// No token field - it lives only in the httpOnly cookie now, the frontend
// never sees or handles it directly.
export interface AuthResponse {
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
