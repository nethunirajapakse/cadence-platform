import api from "@/lib/axios";
import type { PageResponse } from "@/types/page";
import type {
  AdminUser,
  AdminUserUpdate,
  AuditLogEntry,
  AuditLogFilters,
  Invitation,
  InvitationRequest,
} from "@/types/admin";

export const getAdminUsers = async (): Promise<AdminUser[]> => {
  const response = await api.get("/api/admin/users");
  return response.data;
};

export const updateAdminUser = async (userId: string, payload: AdminUserUpdate): Promise<AdminUser> => {
  const response = await api.patch(`/api/admin/users/${userId}`, payload);
  return response.data;
};

export const deactivateAdminUser = async (userId: string): Promise<AdminUser> => {
  const response = await api.post(`/api/admin/users/${userId}/deactivate`);
  return response.data;
};

export const reactivateAdminUser = async (userId: string): Promise<AdminUser> => {
  const response = await api.post(`/api/admin/users/${userId}/reactivate`);
  return response.data;
};

export const getInvitations = async (): Promise<Invitation[]> => {
  const response = await api.get("/api/admin/invitations");
  return response.data;
};

export const createInvitation = async (payload: InvitationRequest): Promise<Invitation> => {
  const response = await api.post("/api/admin/invitations", payload);
  return response.data;
};

export const resendInvitation = async (invitationId: string): Promise<Invitation> => {
  const response = await api.post(`/api/admin/invitations/${invitationId}/resend`);
  return response.data;
};

export const revokeInvitation = async (invitationId: string): Promise<void> => {
  await api.delete(`/api/admin/invitations/${invitationId}`);
};

export const getAuditLog = async (
  filters: AuditLogFilters = {},
  page = 0,
  size = 25
): Promise<PageResponse<AuditLogEntry>> => {
  const response = await api.get("/api/admin/audit-log", {
    params: { ...filters, page, size },
  });
  return response.data;
};
