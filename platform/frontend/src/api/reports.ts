import api from "@/lib/axios";
import type { PageResponse } from "@/types/page";
import type {
  ReportRequest,
  ReportResponse,
  ReportSummary,
  ReportVersion,
  ReviewRequest,
  ReportStatus,
} from "@/types/report";

export const createReport = async (payload: ReportRequest): Promise<ReportResponse> => {
  const response = await api.post("/api/reports", payload);
  return response.data;
};

export const updateReport = async (
  reportId: string,
  payload: ReportRequest
): Promise<ReportResponse> => {
  const response = await api.put(`/api/reports/${reportId}`, payload);
  return response.data;
};

export const submitReport = async (reportId: string): Promise<ReportResponse> => {
  const response = await api.post(`/api/reports/${reportId}/submit`);
  return response.data;
};

export const getReportDetail = async (reportId: string): Promise<ReportResponse> => {
  const response = await api.get(`/api/reports/${reportId}`);
  return response.data;
};

export const getReportVersions = async (reportId: string): Promise<ReportVersion[]> => {
  const response = await api.get(`/api/reports/${reportId}/versions`);
  return response.data;
};

export const getMyReports = async (
  filters: DashboardFilters = {},
  page = 0,
  size = 10
): Promise<PageResponse<ReportSummary>> => {
  const response = await api.get("/api/reports/mine", {
    params: { ...filters, page, size, sort: "weekStartDate,desc" },
  });
  return response.data;
};

export interface DashboardFilters {
  userId?: string;
  projectIds?: string[];
  excludeProjects?: boolean;
  statuses?: ReportStatus[];
  weekStart?: string;
  weekEnd?: string;
}

export const getDashboardReports = async (
  filters: DashboardFilters,
  page = 0,
  size = 10
): Promise<PageResponse<ReportSummary>> => {
  const response = await api.get("/api/reports", {
    params: { ...filters, page, size, sort: "weekStartDate,desc" },
  });
  return response.data;
};

export const reviewReport = async (
  reportId: string,
  payload: ReviewRequest
): Promise<ReportResponse> => {
  const response = await api.post(`/api/reports/${reportId}/review`, payload);
  return response.data;
};

// Typo-fix path: rewrites the wording of an existing manager comment without
// changing the report's status. Backend only allows this while the report is
// still NEEDS_CORRECTION.
export const editReportComment = async (reportId: string, comment: string): Promise<ReportResponse> => {
  const response = await api.patch(`/api/reports/${reportId}/comment`, { comment });
  return response.data;
};
