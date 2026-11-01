import { useMemo, useState } from "react";
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import {
  useAdminUsers,
  useCreateInvitation,
  useInvitations,
  useResendInvitation,
  useRevokeInvitation,
} from "@/hooks/useAdmin";
import { ApiError } from "@/lib/axios";
import { ROLE_LABELS, type RoleName } from "@/types/auth";
import type { Invitation, InvitationStatus } from "@/types/admin";

const ROLE_OPTIONS = (Object.keys(ROLE_LABELS) as RoleName[]).map((role) => ({
  value: role,
  label: ROLE_LABELS[role],
}));

const STATUS_COLORS: Record<InvitationStatus, string> = {
  PENDING: "gold",
  ACCEPTED: "green",
  REVOKED: "default",
  EXPIRED: "red",
};

interface InviteFormValues {
  email: string;
  role: RoleName;
  managerId?: string;
}

export function AdminInvitationsPage() {
  const { invitations, isInvitationsFetching } = useInvitations();
  const { users } = useAdminUsers();
  const createMutation = useCreateInvitation();
  const resendMutation = useResendInvitation();
  const revokeMutation = useRevokeInvitation();

  const [isModalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<InviteFormValues>();
  const watchedRole = Form.useWatch("role", form);

  const managerOptions = useMemo(
    () =>
      users
        .filter((u) => u.role === "MANAGER" && u.active)
        .map((u) => ({ value: u.userId, label: `${u.name} (${u.email})` })),
    [users]
  );

  function openModal() {
    form.resetFields();
    form.setFieldsValue({ role: "TEAM_MEMBER" });
    setModalOpen(true);
  }

  async function handleInvite() {
    try {
      const values = await form.validateFields();
      await createMutation.mutateAsync({
        email: values.email.trim(),
        role: values.role,
        managerId: values.role === "TEAM_MEMBER" ? values.managerId : undefined,
      });
      message.success(`Invitation sent to ${values.email.trim()}`);
      setModalOpen(false);
    } catch (err) {
      if (err instanceof ApiError) message.error(err.message);
    }
  }

  async function handleResend(invitation: Invitation) {
    try {
      await resendMutation.mutateAsync(invitation.invitationId);
      message.success(`Invitation resent to ${invitation.email}`);
    } catch (err) {
      message.error(err instanceof ApiError ? err.message : "Could not resend the invitation");
    }
  }

  async function handleRevoke(invitation: Invitation) {
    try {
      await revokeMutation.mutateAsync(invitation.invitationId);
      message.success("Invitation revoked");
    } catch (err) {
      message.error(err instanceof ApiError ? err.message : "Could not revoke the invitation");
    }
  }

  const columns: ColumnsType<Invitation> = [
    { title: "Email", dataIndex: "email" },
    {
      title: "Role",
      dataIndex: "role",
      render: (role: RoleName) => ROLE_LABELS[role],
    },
    {
      title: "Reports to",
      dataIndex: "managerName",
      render: (name: string | null) => name ?? "-",
    },
    { title: "Invited by", dataIndex: "invitedByName" },
    {
      title: "Status",
      dataIndex: "status",
      render: (status: InvitationStatus) => <Tag color={STATUS_COLORS[status]}>{status}</Tag>,
    },
    {
      title: "Expires",
      dataIndex: "expiresAt",
      render: (value: string, row) => (row.status === "PENDING" ? dayjs(value).format("D MMM YYYY, HH:mm") : "-"),
    },
    {
      title: "",
      key: "actions",
      align: "right",
      render: (_, row) =>
        row.status === "PENDING" || row.status === "EXPIRED" ? (
          <Space>
            <Button size="small" onClick={() => handleResend(row)} loading={resendMutation.isPending}>
              Resend
            </Button>
            <Popconfirm
              title="Revoke this invitation?"
              description="The link stops working immediately."
              okText="Revoke"
              onConfirm={() => handleRevoke(row)}
            >
              <Button size="small" danger>
                Revoke
              </Button>
            </Popconfirm>
          </Space>
        ) : null,
    },
  ];

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 12 }}>
        <div>
          <Typography.Title level={3} style={{ marginTop: 0 }}>
            Invitations
          </Typography.Title>
          <Typography.Paragraph type="secondary">
            People can only join through an invitation. Each link works once and expires after 48 hours.
          </Typography.Paragraph>
        </div>
        <Button type="primary" onClick={openModal}>
          Invite user
        </Button>
      </div>

      <Table<Invitation>
        rowKey="invitationId"
        loading={isInvitationsFetching}
        columns={columns}
        dataSource={invitations}
        pagination={{ pageSize: 15, hideOnSinglePage: true }}
        scroll={{ x: "max-content" }}
      />

      <Modal
        title="Invite a user"
        open={isModalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleInvite}
        okText="Send invitation"
        confirmLoading={createMutation.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="email"
            label="Work email"
            rules={[
              { required: true, message: "Email is required" },
              { type: "email", message: "Enter a valid email address" },
            ]}
          >
            <Input autoComplete="off" />
          </Form.Item>
          <Form.Item name="role" label="Role" rules={[{ required: true }]}>
            <Select options={ROLE_OPTIONS} />
          </Form.Item>
          {watchedRole === "TEAM_MEMBER" && (
            <Form.Item
              name="managerId"
              label="Reports to"
              rules={[{ required: true, message: "Pick the manager this person reports to" }]}
              extra={managerOptions.length === 0 ? "There are no active managers yet. Invite a manager first." : undefined}
            >
              <Select showSearch optionFilterProp="label" options={managerOptions} placeholder="Select a manager" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
