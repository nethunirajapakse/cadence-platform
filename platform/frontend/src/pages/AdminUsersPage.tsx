import { useMemo, useState } from "react";
import { Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useAdminUsers, useSetUserActive, useUpdateAdminUser } from "@/hooks/useAdmin";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/axios";
import { ROLE_LABELS, type RoleName } from "@/types/auth";
import type { AdminUser } from "@/types/admin";

const ROLE_OPTIONS = (Object.keys(ROLE_LABELS) as RoleName[]).map((role) => ({
  value: role,
  label: ROLE_LABELS[role],
}));

const ROLE_COLORS: Record<RoleName, string> = {
  ADMIN: "purple",
  MANAGER: "blue",
  TEAM_MEMBER: "default",
};

interface EditFormValues {
  name: string;
  role: RoleName;
  managerId?: string;
}

export function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const { users, isUsersFetching } = useAdminUsers();
  const updateMutation = useUpdateAdminUser();
  const activeMutation = useSetUserActive();

  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleName | undefined>();
  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [form] = Form.useForm<EditFormValues>();
  const watchedRole = Form.useWatch("role", form);

  const managerOptions = useMemo(
    () =>
      users
        .filter((u) => u.role === "MANAGER" && u.active)
        .map((u) => ({ value: u.userId, label: `${u.name} (${u.email})` })),
    [users]
  );

  const visibleUsers = useMemo(() => {
    const term = search.trim().toLowerCase();
    return users.filter(
      (u) =>
        (!roleFilter || u.role === roleFilter) &&
        (!term || u.name.toLowerCase().includes(term) || u.email.toLowerCase().includes(term))
    );
  }, [users, search, roleFilter]);

  function openEdit(target: AdminUser) {
    setEditing(target);
    form.setFieldsValue({
      name: target.name,
      role: target.role,
      managerId: target.managerId ?? undefined,
    });
  }

  async function handleSave() {
    if (!editing) return;
    try {
      const values = await form.validateFields();
      await updateMutation.mutateAsync({
        userId: editing.userId,
        payload: {
          name: values.name,
          role: values.role,
          managerId: values.role === "TEAM_MEMBER" ? values.managerId : undefined,
        },
      });
      message.success("User updated");
      setEditing(null);
    } catch (err) {
      if (err instanceof ApiError) message.error(err.message);
    }
  }

  async function handleSetActive(target: AdminUser, active: boolean) {
    try {
      await activeMutation.mutateAsync({ userId: target.userId, active });
      message.success(active ? "User reactivated" : "User deactivated");
    } catch (err) {
      message.error(err instanceof ApiError ? err.message : "Could not update the user");
    }
  }

  const columns: ColumnsType<AdminUser> = [
    {
      title: "Name",
      dataIndex: "name",
      sorter: (a, b) => a.name.localeCompare(b.name),
    },
    { title: "Email", dataIndex: "email" },
    {
      title: "Role",
      dataIndex: "role",
      render: (role: RoleName) => <Tag color={ROLE_COLORS[role]}>{ROLE_LABELS[role]}</Tag>,
    },
    {
      title: "Reports to",
      dataIndex: "managerName",
      render: (name: string | null, row) => (row.role === "TEAM_MEMBER" ? (name ?? "Not assigned") : "-"),
    },
    {
      title: "Team size",
      dataIndex: "directReports",
      render: (count: number, row) => (row.role === "MANAGER" ? count : "-"),
    },
    {
      title: "Status",
      dataIndex: "active",
      render: (active: boolean) => (active ? <Tag color="green">Active</Tag> : <Tag>Deactivated</Tag>),
    },
    {
      title: "",
      key: "actions",
      align: "right",
      render: (_, row) => {
        const isSelf = row.userId === currentUser?.userId;
        return (
          <Space>
            <Button size="small" onClick={() => openEdit(row)}>
              Edit
            </Button>
            {row.active ? (
              <Popconfirm
                title="Deactivate this user?"
                description="They are signed out immediately and can't log in. Their reports are kept."
                okText="Deactivate"
                disabled={isSelf}
                onConfirm={() => handleSetActive(row, false)}
              >
                <Button size="small" danger disabled={isSelf}>
                  Deactivate
                </Button>
              </Popconfirm>
            ) : (
              <Button size="small" onClick={() => handleSetActive(row, true)}>
                Reactivate
              </Button>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Users
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Every account, its role and who it reports to. New people join from the Invitations page.
      </Typography.Paragraph>

      <Space wrap style={{ marginBottom: 16 }}>
        <Input.Search
          allowClear
          placeholder="Search name or email"
          style={{ width: 260 }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Select
          allowClear
          placeholder="All roles"
          style={{ width: 180 }}
          options={ROLE_OPTIONS}
          value={roleFilter}
          onChange={(value) => setRoleFilter(value)}
        />
      </Space>

      <Table<AdminUser>
        rowKey="userId"
        loading={isUsersFetching}
        columns={columns}
        dataSource={visibleUsers}
        pagination={{ pageSize: 15, hideOnSinglePage: true }}
        scroll={{ x: "max-content" }}
      />

      <Modal
        title={editing ? `Edit ${editing.name}` : ""}
        open={!!editing}
        onCancel={() => setEditing(null)}
        onOk={handleSave}
        okText="Save"
        confirmLoading={updateMutation.isPending}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true, message: "Name is required" }]}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="role" label="Role" rules={[{ required: true }]}>
            <Select options={ROLE_OPTIONS} disabled={editing?.userId === currentUser?.userId} />
          </Form.Item>
          {watchedRole === "TEAM_MEMBER" && (
            <Form.Item
              name="managerId"
              label="Reports to"
              rules={[{ required: true, message: "Pick a manager for this team member" }]}
            >
              <Select showSearch optionFilterProp="label" options={managerOptions} placeholder="Select a manager" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
