import { useState } from "react";
import { Input, Select, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import { useAuditLog } from "@/hooks/useAdmin";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { AUDIT_ACTIONS, type AuditActionName, type AuditLogEntry } from "@/types/admin";

const ACTION_OPTIONS = AUDIT_ACTIONS.map((action) => ({ value: action, label: action.replace(/_/g, " ").toLowerCase() }));

// Failures and lockouts stand out; everything else reads as routine.
function actionColor(action: AuditActionName): string {
  if (action === "LOGIN_FAILED" || action === "LOGIN_BLOCKED") return "red";
  if (action === "USER_DEACTIVATED" || action === "INVITE_REVOKED") return "orange";
  return "default";
}

export function AdminAuditLogPage() {
  const [page, setPage] = useState(0);
  const size = 25;
  const [action, setAction] = useState<AuditActionName | undefined>();
  const [actor, setActor] = useState("");
  const debouncedActor = useDebouncedValue(actor, 300);

  const { entries, totalElements, isAuditFetching } = useAuditLog(
    { action, actor: debouncedActor || undefined },
    page,
    size
  );

  const columns: ColumnsType<AuditLogEntry> = [
    {
      title: "When",
      dataIndex: "createdAt",
      render: (value: string) => dayjs(value).format("D MMM YYYY, HH:mm:ss"),
    },
    { title: "Actor", dataIndex: "actorEmail", render: (value: string | null) => value ?? "-" },
    {
      title: "Action",
      dataIndex: "action",
      render: (value: AuditActionName) => <Tag color={actionColor(value)}>{value.replace(/_/g, " ").toLowerCase()}</Tag>,
    },
    { title: "Target", dataIndex: "target", render: (value: string | null) => value ?? "-" },
    { title: "Details", dataIndex: "details", render: (value: string | null) => value ?? "-" },
    { title: "IP", dataIndex: "ipAddress", render: (value: string | null) => value ?? "-" },
  ];

  return (
    <div>
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        Audit log
      </Typography.Title>
      <Typography.Paragraph type="secondary">
        Sign-ins, invitations, account changes, password resets and report review decisions, newest first.
      </Typography.Paragraph>

      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="All actions"
          style={{ width: 260 }}
          options={ACTION_OPTIONS}
          value={action}
          onChange={(value) => {
            setAction(value);
            setPage(0);
          }}
        />
        <Input.Search
          allowClear
          placeholder="Filter by actor email"
          style={{ width: 260 }}
          value={actor}
          onChange={(e) => {
            setActor(e.target.value);
            setPage(0);
          }}
        />
      </Space>

      <Table<AuditLogEntry>
        rowKey="auditId"
        loading={isAuditFetching}
        columns={columns}
        dataSource={entries}
        scroll={{ x: "max-content" }}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: totalElements,
          showSizeChanger: false,
          onChange: (nextPage) => setPage(nextPage - 1),
        }}
      />
    </div>
  );
}
