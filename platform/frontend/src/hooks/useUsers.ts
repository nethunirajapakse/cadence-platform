import { useQuery } from "@tanstack/react-query";
import { getTeamMembers } from "@/api/users";

export const useTeamMembers = () => {
  const { data, isFetching } = useQuery({
    queryKey: ["teamMembers"],
    queryFn: getTeamMembers,
  });
  return { teamMembers: data ?? [], isTeamMembersFetching: isFetching };
};
