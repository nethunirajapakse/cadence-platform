import { useState } from "react";
import { Link } from "react-router-dom";
import { Table, Select, DatePicker, Button, Space, Switch, Typography } from "antd";
import { SearchOutlined, FilterFilled, ClearOutlined } from "@ant-design/icons";
import type { ColumnType } from "antd/es/table";
import type { Dayjs } from "dayjs";
import { useProjects } from "@/hooks/useProjects";
import { useTeamMembers } from "@/hooks/useUsers";
import { useDashboardReports } from "@/hooks/useReports";
import { StatusTag } from "@/components/StatusTag";
import { makeFilterDropdown } from "@/components/ColumnFilterDropdown";
import { MANAGER_VISIBLE_STATUS_OPTIONS } from "@/constants/reportOptions";
import type { ReportStatus, ReportSummary } from "@/types/report";

const { RangePicker } = DatePicker;

export function AllReportsPage() {
  const [page, setPage] = useState(0);
  const [statuses, setStatuses] = useState<ReportStatus[]>([]);
  const [projectIds, setProjectIds] = useState<string[]>([]);
  const [excludeProjects, setExcludeProjects] = useState(false);
  const [userId, setUserId] = useState<string | undefined>();
  const [weekRange, setWeekRange] = useState<[Dayjs, Dayjs] | null>(null);
  const size = 10;

  const { projects } = useProjects();
  const { teamMembers } = useTeamMembers();
  const { reports, totalElements, isDashboardFetching } = useDashboardReports(
    {
      statuses: statuses.length ? statuses : undefined,
      projectIds: projectIds.length ? projectIds : undefined,
      excludeProjects,
      userId,
      weekStart: weekRange?.[0]?.format("YYYY-MM-DD"),
      weekEnd: weekRange?.[1]?.format("YYYY-MM-DD"),
    },
    page,
    size
  );

  const activeFilterColor = (active: boolean) => (active ? "#2F6F63" : undefined);

  const hasActiveFilters =
    !!userId || projectIds.length > 0 || excludeProjects || statuses.length > 0 || !!weekRange;

  function resetAllFilters() {
    setUserId(undefined);
    setProjectIds([]);
    setExcludeProjects(false);
    setStatuses([]);
    setWeekRange(null);
    setPage(0);
  }

  const columns: ColumnType<ReportSummary>[] = [
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
      title: "Team member",
      dataIndex: "userName",
      render: (_: unknown, r: ReportSummary) => (
        <Link to={`/team-members/${r.userId}`}>{r.userName}</Link>
      ),
      filterIcon: () => <SearchOutlined style={{ color: activeFilterColor(!!userId) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <Select
            showSearch
            allowClear
            placeholder="Type a name..."
            style={{ width: "100%" }}
            value={userId}
            optionFilterProp="label"
            options={teamMembers.map((m) => ({ value: m.userId, label: m.name }))}
            onChange={(v) => {
              setUserId(v);
              setPage(0);
            }}
          />
        ),
        () => setUserId(undefined)
      ),
    },
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
      title: "Status",
      key: "status",
      render: (_: unknown, r: ReportSummary) => <StatusTag status={r.status} />,
      filterIcon: () => <FilterFilled style={{ color: activeFilterColor(statuses.length > 0) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <Select
            mode="multiple"
            showSearch={false}
            allowClear
            placeholder="Select statuses..."
            style={{ width: "100%" }}
            value={statuses}
            options={MANAGER_VISIBLE_STATUS_OPTIONS}
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
        <Button
          size="small"
          icon={<ClearOutlined />}
          disabled={!hasActiveFilters}
          onClick={resetAllFilters}
        >
          Reset filters
        </Button>
      ),
      key: "actions",
      render: (_: unknown, r: ReportSummary) => <Link to={`/reports/${r.reportId}`}>Review</Link>,
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        All reports
      </Typography.Title>

      <Table
        rowKey="reportId"
        columns={columns}
        dataSource={reports}
        loading={isDashboardFetching}
        scroll={{ x: "max-content" }}
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
