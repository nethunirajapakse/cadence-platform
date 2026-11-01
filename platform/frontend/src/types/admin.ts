import type { RoleName } from "@/types/auth";

export interface AdminUser {
  userId: string;
  name: string;
  email: string;
  role: RoleName;
  managerId: string | null;
  managerName: string | null;
  active: boolean;
  directReports: number;
  createdAt: string;
}

export interface AdminUserUpdate {
  name?: string;
  role?: RoleName;
  managerId?: string;
}

export type InvitationStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

export interface Invitation {
  invitationId: string;
  email: string;
  role: RoleName;
  managerId: string | null;
  managerName: string | null;
  invitedByName: string;
  status: InvitationStatus;
  expiresAt: string;
  createdAt: string;
  acceptedAt: string | null;
}

export interface InvitationRequest {
  email: string;
  role: RoleName;
  managerId?: string;
}

export const AUDIT_ACTIONS = [
  "LOGIN_SUCCESS",
  "LOGIN_FAILED",
  "LOGIN_BLOCKED",
  "INVITE_CREATED",
  "INVITE_RESENT",
  "INVITE_REVOKED",
  "INVITE_ACCEPTED",
  "USER_UPDATED",
  "USER_DEACTIVATED",
  "USER_REACTIVATED",
  "PASSWORD_RESET_REQUESTED",
  "PASSWORD_RESET_COMPLETED",
  "REPORT_APPROVED",
  "REPORT_CHANGES_REQUESTED",
  "REPORT_COMMENT_EDITED",
  "ADMIN_BOOTSTRAPPED",
] as const;

export type AuditActionName = (typeof AUDIT_ACTIONS)[number];

export interface AuditLogEntry {
  auditId: string;
  actorEmail: string | null;
  action: AuditActionName;
  target: string | null;
  details: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface AuditLogFilters {
  action?: AuditActionName;
  actor?: string;
}
