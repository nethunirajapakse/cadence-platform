export interface SubmissionCompliance {
  submitted: number;
  pending: number;
  late: number;
  totalExpected: number;
}

export interface DashboardSummary {
  totalReportsSubmittedThisWeek: number;
  compliance: SubmissionCompliance;
  needsCorrectionCount: number;
  openBlockersCount: number;
}

export interface TasksTrendPoint {
  weekStartDate: string;
  tasksCompleted: number;
}

export interface MemberStatusBreakdown {
  userName: string;
  submitted: number;
  needsCorrection: number;
  approved: number;
}

export interface ProjectWorkload {
  projectName: string;
  taskCount: number;
}

export interface TaskTypeHours {
  taskType: string;
  totalHours: number;
}

export interface ActivityItem {
  reportId: string;
  userName: string;
  projectName: string;
  status: string;
  actionAt: string;
  description: string;
}

export interface MemberStats {
  totalReports: number;
  approvedCount: number;
  needsCorrectionCount: number;
  tasksCompletedCount: number;
  openBlockersCount: number;
}

export interface TeamMemberOverview {
  userId: string;
  name: string;
  email: string;
  role: string;
  totalReports: number;
  approvedCount: number;
  needsCorrectionCount: number;
}

export interface TeamMemberFilterCriteria {
  name?: string;
  email?: string;
}
