import { useQuery } from "@tanstack/react-query";
import {
  getDashboardSummary,
  getTasksTrend,
  getStatusByMember,
  getWorkloadByProject,
  getTimeByTaskType,
  getRecentActivity,
  getMemberStats,
  getTeamMemberOverview,
} from "@/api/dashboard";
import type { TeamMemberFilterCriteria } from "@/types/dashboard";

export const useDashboardSummary = () => {
  const { data, isFetching, error } = useQuery({
    queryKey: ["dashboardSummary"],
    queryFn: getDashboardSummary,
  });
  return { summary: data, isSummaryFetching: isFetching, summaryError: error };
};

export const useTasksTrend = () => {
  const { data, isFetching } = useQuery({
    queryKey: ["tasksTrend"],
    queryFn: getTasksTrend,
  });
  return { tasksTrend: data ?? [], isTasksTrendFetching: isFetching };
};

export const useStatusByMember = () => {
  const { data, isFetching } = useQuery({
    queryKey: ["statusByMember"],
    queryFn: getStatusByMember,
  });
  return { statusByMember: data ?? [], isStatusByMemberFetching: isFetching };
};

export const useWorkloadByProject = () => {
  const { data, isFetching } = useQuery({
    queryKey: ["workloadByProject"],
    queryFn: getWorkloadByProject,
  });
  return { workloadByProject: data ?? [], isWorkloadFetching: isFetching };
};

export const useTimeByTaskType = () => {
  const { data, isFetching } = useQuery({
    queryKey: ["timeByTaskType"],
    queryFn: getTimeByTaskType,
  });
  return { timeByTaskType: data ?? [], isTimeByTaskTypeFetching: isFetching };
};

export const useRecentActivity = (limit = 15) => {
  const { data, isFetching } = useQuery({
    queryKey: ["recentActivity", limit],
    queryFn: () => getRecentActivity(limit),
  });
  return { activity: data ?? [], isActivityFetching: isFetching };
};

export const useMemberStats = (userId?: string) => {
  const { data, isFetching } = useQuery({
    queryKey: ["memberStats", userId],
    queryFn: () => getMemberStats(userId as string),
    enabled: !!userId,
  });
  return { memberStats: data, isMemberStatsFetching: isFetching };
};

export const useTeamMemberOverview = (filters: TeamMemberFilterCriteria = {}, page = 0, size = 10) => {
  const { data, isFetching } = useQuery({
    queryKey: ["teamMemberOverview", filters, page, size],
    queryFn: () => getTeamMemberOverview(filters, page, size),
  });
  return {
    teamMemberOverview: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    isTeamMemberOverviewFetching: isFetching,
  };
};
