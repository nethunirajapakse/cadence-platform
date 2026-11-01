export type RoleName = "TEAM_MEMBER" | "MANAGER" | "ADMIN";

export const ROLE_LABELS: Record<RoleName, string> = {
  TEAM_MEMBER: "Team member",
  MANAGER: "Manager",
  ADMIN: "Administrator",
};

export interface LoginPayload {
  email: string;
  password: string;
}

// No token field - it lives only in the httpOnly cookie, the frontend never
// sees or handles it directly.
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

// What the accept-invitation page may show before the invitee has done anything.
export interface InvitationPreview {
  email: string;
  role: RoleName;
  managerName: string | null;
  expiresAt: string;
}

export interface AcceptInvitePayload {
  token: string;
  name: string;
  password: string;
}

export interface ResetPasswordPayload {
  token: string;
  password: string;
}
