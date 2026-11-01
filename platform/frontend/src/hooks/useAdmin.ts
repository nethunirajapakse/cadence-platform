import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createInvitation,
  deactivateAdminUser,
  getAdminUsers,
  getAuditLog,
  getInvitations,
  reactivateAdminUser,
  resendInvitation,
  revokeInvitation,
  updateAdminUser,
} from "@/api/admin";
import type { AdminUserUpdate, AuditLogFilters, InvitationRequest } from "@/types/admin";

export const useAdminUsers = () => {
  const { data, isFetching } = useQuery({ queryKey: ["adminUsers"], queryFn: getAdminUsers });
  return { users: data ?? [], isUsersFetching: isFetching };
};

export const useUpdateAdminUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, payload }: { userId: string; payload: AdminUserUpdate }) =>
      updateAdminUser(userId, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["adminUsers"] }),
  });
};

export const useSetUserActive = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, active }: { userId: string; active: boolean }) =>
      active ? reactivateAdminUser(userId) : deactivateAdminUser(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["adminUsers"] }),
  });
};

export const useInvitations = () => {
  const { data, isFetching } = useQuery({ queryKey: ["invitations"], queryFn: getInvitations });
  return { invitations: data ?? [], isInvitationsFetching: isFetching };
};

export const useCreateInvitation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: InvitationRequest) => createInvitation(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["invitations"] }),
  });
};

export const useResendInvitation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (invitationId: string) => resendInvitation(invitationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["invitations"] }),
  });
};

export const useRevokeInvitation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (invitationId: string) => revokeInvitation(invitationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["invitations"] }),
  });
};

export const useAuditLog = (filters: AuditLogFilters, page: number, size: number) => {
  const { data, isFetching } = useQuery({
    queryKey: ["auditLog", filters, page, size],
    queryFn: () => getAuditLog(filters, page, size),
  });
  return {
    entries: data?.content ?? [],
    totalElements: data?.totalElements ?? 0,
    isAuditFetching: isFetching,
  };
};
