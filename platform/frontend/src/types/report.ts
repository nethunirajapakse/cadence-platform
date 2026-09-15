export type Priority = "HIGH" | "MEDIUM" | "LOW";
export type TaskStatus = "NOT_STARTED" | "IN_PROGRESS" | "DONE";
export type TaskType = "DEVELOPMENT" | "TESTING" | "MEETINGS" | "DOCUMENTATION" | "OTHER";
export type NoteLinkType = "NOTE" | "LINK";
export type ReportStatus = "DRAFT" | "SUBMITTED" | "NEEDS_CORRECTION" | "APPROVED";
export type ReviewDecision = "APPROVE" | "REQUEST_CHANGES";

export interface ReportTaskDto {
  taskName: string;
  priority: Priority;
  plannedPct?: number;
  actualPct?: number;
  status: TaskStatus;
  timePlanned?: number;
  timeSpent?: number;
  deliverable?: string;
}

export interface NextWeekTaskDto {
  taskDescription: string;
  priority?: Priority;
}

export interface BlockerDto {
  description: string;
  keyIssue: boolean;
}

export interface AchievementDto {
  description: string;
  keyAchievement: boolean;
}

export interface TimeLogDto {
  taskType: TaskType;
  hours: number;
}

export interface NoteLinkDto {
  type: NoteLinkType;
  content: string;
}

// Body shape for both create (draft) and update (edit while
// DRAFT/NEEDS_CORRECTION) - matches the backend's ReportRequest exactly.
export interface ReportRequest {
  projectId: string;
  weekStartDate: string; // ISO date, e.g. "2026-09-08"
  weekEndDate: string;
  notes?: string;
  tasks: ReportTaskDto[];
  nextWeekTasks?: NextWeekTaskDto[];
  blockers?: BlockerDto[];
  achievements?: AchievementDto[];
  timeLogs?: TimeLogDto[];
  noteLinks?: NoteLinkDto[];
}

// Full detail shape returned by create/update/submit/review/getDetail.
export interface ReportResponse {
  reportId: string;
  userId: string;
  userName: string;
  projectId: string;
  projectName: string;
  weekStartDate: string;
  weekEndDate: string;
  status: ReportStatus;
  managerComment?: string;
  notes?: string;
  submittedAt?: string;
  approvedAt?: string;
  // Used to compute the 15-minute comment edit window client-side, and to
  // show an "(edited)" label once the comment has been corrected.
  managerCommentPostedAt?: string;
  managerCommentEdited: boolean;
  tasks: ReportTaskDto[];
  nextWeekTasks: NextWeekTaskDto[];
  blockers: BlockerDto[];
  achievements: AchievementDto[];
  timeLogs: TimeLogDto[];
  noteLinks: NoteLinkDto[];
}

// Lighter shape used in list views (own history, manager dashboard).
export interface ReportSummary {
  reportId: string;
  userId: string;
  userName: string;
  projectName: string;
  weekStartDate: string;
  weekEndDate: string;
  status: ReportStatus;
  submittedAt?: string;
}

export interface ReviewRequest {
  decision: ReviewDecision;
  comment?: string;
}

export interface ReportVersion {
  versionId: string;
  versionNumber: number;
  submittedAt: string;
  comment?: string;
  current: boolean;
}
