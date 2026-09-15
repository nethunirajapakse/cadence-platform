import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { Card, Col, Row, Statistic, Typography, Table, Space, Tag, Select, DatePicker, Button, Switch } from "antd";
import { FilterFilled, ClearOutlined } from "@ant-design/icons";
import type { ColumnType } from "antd/es/table";
import type { Dayjs } from "dayjs";
import { useUserProfile } from "@/hooks/useUsers";
import { useMemberStats } from "@/hooks/useDashboard";
import { useDashboardReports } from "@/hooks/useReports";
import { useProjects } from "@/hooks/useProjects";
import { StatusTag } from "@/components/StatusTag";
import { makeFilterDropdown } from "@/components/ColumnFilterDropdown";
import { MANAGER_VISIBLE_STATUS_OPTIONS } from "@/constants/reportOptions";
import type { ReportStatus, ReportSummary } from "@/types/report";

const { RangePicker } = DatePicker;

// Manager-only view: clicking a team member's name elsewhere in the app
// lands here - their basic info, a few summary stats, and their full report
// history, filterable the same way as the other report tables in the app.
export function TeamMemberProfilePage() {
  const { userId } = useParams<{ userId: string }>();
  const { profile, isProfileFetching } = useUserProfile(userId);
  const { memberStats, isMemberStatsFetching } = useMemberStats(userId);
  const { projects } = useProjects();

  const [page, setPage] = useState(0);
  const [statuses, setStatuses] = useState<ReportStatus[]>([]);
  const [projectIds, setProjectIds] = useState<string[]>([]);
  const [excludeProjects, setExcludeProjects] = useState(false);
  const [weekRange, setWeekRange] = useState<[Dayjs, Dayjs] | null>(null);
  const size = 10;

  const { reports, totalElements, isDashboardFetching } = useDashboardReports(
    {
      userId, // fixed to this profile - not one of the user-editable filters
      statuses: statuses.length ? statuses : undefined,
      projectIds: projectIds.length ? projectIds : undefined,
      excludeProjects,
      weekStart: weekRange?.[0]?.format("YYYY-MM-DD"),
      weekEnd: weekRange?.[1]?.format("YYYY-MM-DD"),
    },
    page,
    size
  );

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
          // MANAGER_VISIBLE_STATUS_OPTIONS (no DRAFT) - this is a manager's
          // view of someone else's reports, same draft-hiding rule as everywhere else.
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
        <Button size="small" icon={<ClearOutlined />} disabled={!hasActiveFilters} onClick={resetAllFilters}>
          Reset filters
        </Button>
      ),
      key: "actions",
      render: (_: unknown, r: ReportSummary) => <Link to={`/reports/${r.reportId}`}>View</Link>,
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Card loading={isProfileFetching}>
        <Space direction="vertical" size={0}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {profile?.name}
          </Typography.Title>
          <Typography.Text type="secondary">{profile?.email}</Typography.Text>
          {profile?.role && (
            <Tag style={{ marginTop: 8 }} color={profile.role === "MANAGER" ? "purple" : "blue"}>
              {profile.role === "MANAGER" ? "Manager" : "Team member"}
            </Tag>
          )}
        </Space>
      </Card>

      <Row gutter={16}>
        <Col xs={24} sm={12} lg={5}>
          <Card loading={isMemberStatsFetching}>
            <Statistic title="Total reports" value={memberStats?.totalReports ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={5}>
          <Card loading={isMemberStatsFetching}>
            <Statistic title="Approved" value={memberStats?.approvedCount ?? 0} valueStyle={{ color: "#2F6F63" }} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={5}>
          <Card loading={isMemberStatsFetching}>
            <Statistic
              title="Needs correction"
              value={memberStats?.needsCorrectionCount ?? 0}
              valueStyle={{ color: "#B8802E" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={5}>
          <Card loading={isMemberStatsFetching}>
            <Statistic title="Tasks completed" value={memberStats?.tasksCompletedCount ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={4}>
          <Card loading={isMemberStatsFetching}>
            <Statistic
              title="Open blockers"
              value={memberStats?.openBlockersCount ?? 0}
              valueStyle={{ color: "#B8802E" }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Report history">
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
      </Card>
    </Space>
  );
}
