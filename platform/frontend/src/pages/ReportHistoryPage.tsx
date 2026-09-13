import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Table, Button, Space, Typography } from "antd";
import { useMyReports } from "@/hooks/useReports";
import { StatusTag } from "@/components/StatusTag";
import type { ReportSummary } from "@/types/report";

export function ReportHistoryPage() {
  const [page, setPage] = useState(0);
  const size = 10;
  const { reports, totalElements, isReportsFetching } = useMyReports(page, size);
  const navigate = useNavigate();

  const columns = [
    {
      title: "Week",
      key: "week",
      render: (_: unknown, r: ReportSummary) => `${r.weekStartDate} – ${r.weekEndDate}`,
    },
    { title: "Project", dataIndex: "projectName" },
    {
      title: "Status",
      key: "status",
      render: (_: unknown, r: ReportSummary) => <StatusTag status={r.status} />,
    },
    {
      title: "",
      key: "actions",
      render: (_: unknown, r: ReportSummary) => <Link to={`/reports/${r.reportId}`}>View</Link>,
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, display: "flex", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          My reports
        </Typography.Title>
        <Button type="primary" onClick={() => navigate("/reports/new")}>
          New report
        </Button>
      </Space>
      <Table
        rowKey="reportId"
        columns={columns}
        dataSource={reports}
        loading={isReportsFetching}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: totalElements,
          onChange: (p) => setPage(p - 1),
        }}
      />
    </div>
  );
}
