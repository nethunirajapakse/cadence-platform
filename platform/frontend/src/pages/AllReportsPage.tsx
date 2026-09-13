import { useState } from "react";
import { Link } from "react-router-dom";
import { Table, Select, DatePicker, Space, Typography } from "antd";
import type { Dayjs } from "dayjs";
import { useProjects } from "@/hooks/useProjects";
import { useDashboardReports } from "@/hooks/useReports";
import { StatusTag } from "@/components/StatusTag";
import { MANAGER_VISIBLE_STATUS_OPTIONS } from "@/constants/reportOptions";
import type { ReportStatus, ReportSummary } from "@/types/report";

const { RangePicker } = DatePicker;

// The searchable/filterable record of every team report - separate from
// TeamDashboardPage, which is analytics-only (summary metrics + charts).
export function AllReportsPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<ReportStatus | undefined>();
  const [projectId, setProjectId] = useState<string | undefined>();
  const [weekRange, setWeekRange] = useState<[Dayjs, Dayjs] | null>(null);
  const size = 10;

  const { projects } = useProjects();
  const { reports, totalElements, isDashboardFetching } = useDashboardReports(
    {
      status,
      projectId,
      weekStart: weekRange?.[0]?.format("YYYY-MM-DD"),
      weekEnd: weekRange?.[1]?.format("YYYY-MM-DD"),
    },
    page,
    size
  );

  const columns = [
    { title: "Team member", dataIndex: "userName" },
    { title: "Project", dataIndex: "projectName" },
    {
      title: "Week",
      key: "week",
      render: (_: unknown, r: ReportSummary) => `${r.weekStartDate} – ${r.weekEndDate}`,
    },
    {
      title: "Status",
      key: "status",
      render: (_: unknown, r: ReportSummary) => <StatusTag status={r.status} />,
    },
    {
      title: "",
      key: "actions",
      render: (_: unknown, r: ReportSummary) => <Link to={`/reports/${r.reportId}`}>Review</Link>,
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Typography.Title level={4}>All reports</Typography.Title>

      <Space wrap>
        <Select
          allowClear
          placeholder="Filter by project"
          style={{ width: 200 }}
          options={projects.map((p) => ({ value: p.projectId, label: p.name }))}
          onChange={(v) => {
            setProjectId(v);
            setPage(0);
          }}
        />
        <Select
          allowClear
          placeholder="Filter by status"
          style={{ width: 180 }}
          options={MANAGER_VISIBLE_STATUS_OPTIONS}
          onChange={(v) => {
            setStatus(v);
            setPage(0);
          }}
        />
        <RangePicker
          onChange={(v) => {
            setWeekRange(v as [Dayjs, Dayjs] | null);
            setPage(0);
          }}
        />
      </Space>

      <Table
        rowKey="reportId"
        columns={columns}
        dataSource={reports}
        loading={isDashboardFetching}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: totalElements,
          onChange: (p) => setPage(p - 1),
        }}
      />
    </Space>
  );
}
