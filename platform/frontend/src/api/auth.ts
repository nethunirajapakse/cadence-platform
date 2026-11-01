import api from "@/lib/axios";
import type {
  AcceptInvitePayload,
  AuthResponse,
  InvitationPreview,
  LoginPayload,
  ResetPasswordPayload,
} from "@/types/auth";

export const loginUser = async (payload: LoginPayload): Promise<AuthResponse> => {
  const response = await api.post("/api/auth/login", payload);
  return response.data;
};

export const logoutUser = async (): Promise<void> => {
  await api.post("/api/auth/logout");
};

// Asks the backend "is my cookie still valid" - the frontend has no other way
// to check, since the token itself is httpOnly.
export const getCurrentUser = async (): Promise<AuthResponse> => {
  const response = await api.get("/api/auth/me");
  return response.data;
};

export const previewInvitation = async (token: string): Promise<InvitationPreview> => {
  const response = await api.get("/api/auth/invitations/preview", { params: { token } });
  return response.data;
};

// Creates the account from an invitation link and signs the new user in.
export const acceptInvitation = async (payload: AcceptInvitePayload): Promise<AuthResponse> => {
  const response = await api.post("/api/auth/accept-invite", payload);
  return response.data;
};

export const requestPasswordReset = async (email: string): Promise<void> => {
  await api.post("/api/auth/forgot-password", { email });
};

export const resetPassword = async (payload: ResetPasswordPayload): Promise<void> => {
  await api.post("/api/auth/reset-password", payload);
};
