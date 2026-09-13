import api from "@/lib/axios";
import type { AuthResponse, LoginPayload, RegisterPayload } from "@/types/auth";

export const registerUser = async (payload: RegisterPayload): Promise<AuthResponse> => {
  const response = await api.post("/api/auth/register", payload);
  return response.data;
};

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
