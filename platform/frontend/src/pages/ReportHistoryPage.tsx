import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Table, Select, DatePicker, Button, Space, Switch, Typography } from "antd";
import { FilterFilled, ClearOutlined } from "@ant-design/icons";
import type { ColumnType } from "antd/es/table";
import type { Dayjs } from "dayjs";
import { useProjects } from "@/hooks/useProjects";
import { useMyReports } from "@/hooks/useReports";
import { StatusTag } from "@/components/StatusTag";
import { makeFilterDropdown } from "@/components/ColumnFilterDropdown";
import { REPORT_STATUS_OPTIONS } from "@/constants/reportOptions";
import type { ReportStatus, ReportSummary } from "@/types/report";

const { RangePicker } = DatePicker;

export function ReportHistoryPage() {
  const [page, setPage] = useState(0);
  const [statuses, setStatuses] = useState<ReportStatus[]>([]);
  const [projectIds, setProjectIds] = useState<string[]>([]);
  const [excludeProjects, setExcludeProjects] = useState(false);
  const [weekRange, setWeekRange] = useState<[Dayjs, Dayjs] | null>(null);
  const size = 10;

  const { projects } = useProjects();
  const { reports, totalElements, isReportsFetching } = useMyReports(
    {
      statuses: statuses.length ? statuses : undefined,
      projectIds: projectIds.length ? projectIds : undefined,
      excludeProjects,
      weekStart: weekRange?.[0]?.format("YYYY-MM-DD"),
      weekEnd: weekRange?.[1]?.format("YYYY-MM-DD"),
    },
    page,
    size
  );
  const navigate = useNavigate();

  const activeFilterColor = (active: boolean) => (active ? "#2F6F63" : undefined);

  const hasActiveFilters = projectIds.length > 0 || excludeProjects || statuses.length > 0 || !!weekRange;

  function resetAllFilters() {
    setProjectIds([]);
    setExcludeProjects(false);
    setStatuses([]);
    setWeekRange(null);
    setPage(0);
  }

  const columns: ColumnType<ReportSummary>[] = [
    {
      title: "Project",
      dataIndex: "projectName",
      filterIcon: () => <FilterFilled style={{ color: activeFilterColor(projectIds.length > 0) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <Space direction="vertical" style={{ width: "100%" }}>
            <Select
              mode="multiple"
              showSearch
              allowClear
              placeholder="Type to search projects..."
              style={{ width: "100%" }}
              value={projectIds}
              optionFilterProp="label"
              maxTagCount={2}
              options={projects.map((p) => ({ value: p.projectId, label: p.name }))}
              onChange={(v) => {
                setProjectIds(v);
                setPage(0);
              }}
            />
            <Space align="center">
              <Switch
                size="small"
                checked={excludeProjects}
                onChange={(checked) => {
                  setExcludeProjects(checked);
                  setPage(0);
                }}
              />
              <Typography.Text style={{ fontSize: 12 }}>
                Exclude selected (show all except these)
              </Typography.Text>
            </Space>
          </Space>
        ),
        () => {
          setProjectIds([]);
          setExcludeProjects(false);
        }
      ),
    },
    {
      title: "Week",
      key: "week",
      render: (_: unknown, r: ReportSummary) => `${r.weekStartDate} – ${r.weekEndDate}`,
      filterIcon: () => <FilterFilled style={{ color: activeFilterColor(!!weekRange) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <RangePicker
            style={{ width: "100%" }}
            value={weekRange}
            onChange={(v) => {
              setWeekRange(v as [Dayjs, Dayjs] | null);
              setPage(0);
            }}
          />
        ),
        () => setWeekRange(null)
      ),
    },
    {
      title: "Status",
      key: "status",
      render: (_: unknown, r: ReportSummary) => <StatusTag status={r.status} />,
      filterIcon: () => <FilterFilled style={{ color: activeFilterColor(statuses.length > 0) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          // Includes DRAFT deliberately - this is the team member's own
          // history, unlike the manager's views, so their own drafts must be
          // filterable here too.
          <Select
            mode="multiple"
            showSearch={false}
            allowClear
            placeholder="Select statuses..."
            style={{ width: "100%" }}
            value={statuses}
            options={REPORT_STATUS_OPTIONS}
            onChange={(v) => {
              setStatuses(v);
              setPage(0);
            }}
          />
        ),
        () => setStatuses([])
      ),
    },
    {
      title: (
        <Button size="small" icon={<ClearOutlined />} disabled={!hasActiveFilters} onClick={resetAllFilters}>
          Reset filters
        </Button>
      ),
      key: "actions",
      render: (_: unknown, r: ReportSummary) => <Link to={`/reports/${r.reportId}`}>View</Link>,
    },
  ];

  return (
    <div>
      <Space wrap style={{ marginBottom: 16, display: "flex", justifyContent: "space-between", gap: 12 }}>
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
        scroll={{ x: "max-content" }}
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
