import { useQuery } from "@tanstack/react-query";
import { getTeamMembers, getUserProfile } from "@/api/users";

export const useTeamMembers = () => {
  const { data, isFetching } = useQuery({
    queryKey: ["teamMembers"],
    queryFn: getTeamMembers,
  });
  return { teamMembers: data ?? [], isTeamMembersFetching: isFetching };
};

export const useUserProfile = (userId?: string) => {
  const { data, isFetching } = useQuery({
    queryKey: ["userProfile", userId],
    queryFn: () => getUserProfile(userId as string),
    enabled: !!userId,
  });
  return { profile: data, isProfileFetching: isFetching };
};
