import { useState } from "react";
import { Link } from "react-router-dom";
import { Table, Input, Button, Space, Typography, Tag } from "antd";
import { SearchOutlined, ClearOutlined } from "@ant-design/icons";
import type { ColumnType } from "antd/es/table";
import { useTeamMemberOverview } from "@/hooks/useDashboard";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { makeFilterDropdown } from "@/components/ColumnFilterDropdown";
import type { TeamMemberOverview } from "@/types/dashboard";

// Manager-only roster page - filtered and paginated server-side via
// TeamMemberFilterCriteria, same pattern as the Projects and Reports tables.
export function TeamMembersPage() {
  const [page, setPage] = useState(0);
  const size = 10;

  const [nameFilter, setNameFilter] = useState("");
  const [emailFilter, setEmailFilter] = useState("");
  const debouncedName = useDebouncedValue(nameFilter, 300);
  const debouncedEmail = useDebouncedValue(emailFilter, 300);

  const { teamMemberOverview, totalElements, isTeamMemberOverviewFetching } = useTeamMemberOverview(
    { name: debouncedName, email: debouncedEmail },
    page,
    size
  );

  const hasActiveFilters = !!nameFilter || !!emailFilter;

  function resetAllFilters() {
    setNameFilter("");
    setEmailFilter("");
    setPage(0);
  }

  const activeFilterColor = (active: boolean) => (active ? "#2F6F63" : undefined);

  const columns: ColumnType<TeamMemberOverview>[] = [
    {
      title: "Name",
      dataIndex: "name",
      render: (_: unknown, m: TeamMemberOverview) => <Link to={`/team-members/${m.userId}`}>{m.name}</Link>,
      filterIcon: () => <SearchOutlined style={{ color: activeFilterColor(!!nameFilter) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <Input
            placeholder="Search by name..."
            value={nameFilter}
            onChange={(e) => {
              setNameFilter(e.target.value);
              setPage(0);
            }}
            allowClear
          />
        ),
        () => setNameFilter("")
      ),
    },
    {
      title: "Email",
      dataIndex: "email",
      filterIcon: () => <SearchOutlined style={{ color: activeFilterColor(!!emailFilter) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <Input
            placeholder="Search by email..."
            value={emailFilter}
            onChange={(e) => {
              setEmailFilter(e.target.value);
              setPage(0);
            }}
            allowClear
          />
        ),
        () => setEmailFilter("")
      ),
    },
    {
      title: "Role",
      dataIndex: "role",
      render: (role: string) => (
        <Tag color={role === "MANAGER" ? "purple" : "blue"}>{role === "MANAGER" ? "Manager" : "Team member"}</Tag>
      ),
    },
    { title: "Total reports", dataIndex: "totalReports" },
    { title: "Approved", dataIndex: "approvedCount" },
    {
      title: (
        <Button size="small" icon={<ClearOutlined />} disabled={!hasActiveFilters} onClick={resetAllFilters}>
          Reset filters
        </Button>
      ),
      key: "needsCorrection",
      render: (_: unknown, m: TeamMemberOverview) =>
        m.needsCorrectionCount > 0 ? (
          <Tag color="orange">{m.needsCorrectionCount} needs correction</Tag>
        ) : (
          <Typography.Text type="secondary"> </Typography.Text>
        ),
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        Team members
      </Typography.Title>

      <Table
        rowKey="userId"
        columns={columns}
        dataSource={teamMemberOverview}
        loading={isTeamMemberOverviewFetching}
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
