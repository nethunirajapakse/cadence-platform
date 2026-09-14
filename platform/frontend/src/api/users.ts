import api from "@/lib/axios";
import type { TeamMemberSummary } from "@/types/user";

export const getTeamMembers = async (): Promise<TeamMemberSummary[]> => {
  const response = await api.get("/api/users/team-members");
  return response.data;
};
