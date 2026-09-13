import api from "@/lib/axios";
import type {
  DashboardSummary,
  TasksTrendPoint,
  MemberStatusBreakdown,
  ProjectWorkload,
  TaskTypeHours,
  ActivityItem,
} from "@/types/dashboard";

export const getDashboardSummary = async (): Promise<DashboardSummary> => {
  const response = await api.get("/api/dashboard/summary");
  return response.data;
};

export const getTasksTrend = async (): Promise<TasksTrendPoint[]> => {
  const response = await api.get("/api/dashboard/tasks-trend");
  return response.data;
};

export const getStatusByMember = async (): Promise<MemberStatusBreakdown[]> => {
  const response = await api.get("/api/dashboard/status-by-member");
  return response.data;
};

export const getWorkloadByProject = async (): Promise<ProjectWorkload[]> => {
  const response = await api.get("/api/dashboard/workload-by-project");
  return response.data;
};

export const getTimeByTaskType = async (): Promise<TaskTypeHours[]> => {
  const response = await api.get("/api/dashboard/time-by-task-type");
  return response.data;
};

export const getRecentActivity = async (limit = 15): Promise<ActivityItem[]> => {
  const response = await api.get("/api/dashboard/recent-activity", { params: { limit } });
  return response.data;
};
