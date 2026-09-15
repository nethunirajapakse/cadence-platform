import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createReport,
  updateReport,
  submitReport,
  getReportDetail,
  getReportVersions,
  getMyReports,
  getDashboardReports,
  reviewReport,
  editReportComment,
  type DashboardFilters,
} from "@/api/reports";
import type { ReportRequest, ReviewRequest } from "@/types/report";

export const useMyReports = (filters: DashboardFilters = {}, page = 0, size = 10) => {
  const {
    data,
    isFetching: isReportsFetching,
    error: reportsError,
  } = useQuery({
    queryKey: ["myReports", filters, page, size],
    queryFn: () => getMyReports(filters, page, size),
  });

  return {
    reports: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    isReportsFetching,
    reportsError,
  };
};

export const useDashboardReports = (filters: DashboardFilters, page = 0, size = 10) => {
  const {
    data,
    isFetching: isDashboardFetching,
    error: dashboardError,
  } = useQuery({
    queryKey: ["dashboardReports", filters, page, size],
    queryFn: () => getDashboardReports(filters, page, size),
  });

  return {
    reports: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
    isDashboardFetching,
    dashboardError,
  };
};

export const useReportDetail = (reportId?: string) => {
  const {
    data: report,
    isFetching: isReportFetching,
    error: reportError,
  } = useQuery({
    queryKey: ["report", reportId],
    queryFn: () => getReportDetail(reportId as string),
    enabled: !!reportId,
  });

  return { report, isReportFetching, reportError };
};

export const useReportVersions = (reportId?: string) => {
  const {
    data: versions,
    isFetching: isVersionsFetching,
    error: versionsError,
  } = useQuery({
    queryKey: ["reportVersions", reportId],
    queryFn: () => getReportVersions(reportId as string),
    enabled: !!reportId,
  });

  return { versions: versions ?? [], isVersionsFetching, versionsError };
};

export const useCreateReport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ReportRequest) => createReport(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["myReports"] }),
  });
};

export const useUpdateReport = (reportId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ReportRequest) => updateReport(reportId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myReports"] });
      queryClient.invalidateQueries({ queryKey: ["report", reportId] });
    },
  });
};

export const useSubmitReport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reportId: string) => submitReport(reportId),
    onSuccess: (_data, reportId) => {
      queryClient.invalidateQueries({ queryKey: ["myReports"] });
      queryClient.invalidateQueries({ queryKey: ["report", reportId] });
      queryClient.invalidateQueries({ queryKey: ["reportVersions", reportId] });
    },
  });
};

export const useReviewReport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reportId, payload }: { reportId: string; payload: ReviewRequest }) =>
      reviewReport(reportId, payload),
    onSuccess: (_data, { reportId }) => {
      queryClient.invalidateQueries({ queryKey: ["dashboardReports"] });
      queryClient.invalidateQueries({ queryKey: ["report", reportId] });
      queryClient.invalidateQueries({ queryKey: ["reportVersions", reportId] });
    },
  });
};

export const useEditReportComment = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reportId, comment }: { reportId: string; comment: string }) =>
      editReportComment(reportId, comment),
    onSuccess: (_data, { reportId }) => {
      queryClient.invalidateQueries({ queryKey: ["report", reportId] });
      queryClient.invalidateQueries({ queryKey: ["reportVersions", reportId] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReports"] });
    },
  });
};
