import api from "@/lib/axios";
import type { TeamMemberSummary, UserProfile } from "@/types/user";

export const getTeamMembers = async (): Promise<TeamMemberSummary[]> => {
  const response = await api.get("/api/users/team-members");
  return response.data;
};

export const getUserProfile = async (userId: string): Promise<UserProfile> => {
  const response = await api.get(`/api/users/${userId}`);
  return response.data;
};
