import { Card, Col, Row, Statistic, Typography, Progress, Space, List, Tag } from "antd";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import {
  useDashboardSummary,
  useTasksTrend,
  useStatusByMember,
  useWorkloadByProject,
  useTimeByTaskType,
  useRecentActivity,
} from "@/hooks/useDashboard";

// Cadence palette, reused for chart series instead of Recharts' default colors.
const COLORS = {
  teal: "#2F6F63",
  ochre: "#B8802E",
  ink: "#1B2521",
  muted: "#5B665F",
  hairline: "#D8DCD6",
};
const PIE_COLORS = ["#2F6F63", "#B8802E", "#5B665F", "#8FA998", "#D8B26A"];

export function TeamDashboardPage() {
  const { summary, isSummaryFetching } = useDashboardSummary();
  const { tasksTrend } = useTasksTrend();
  const { statusByMember } = useStatusByMember();
  const { workloadByProject } = useWorkloadByProject();
  const { timeByTaskType } = useTimeByTaskType();
  const { activity } = useRecentActivity(10);

  const compliance = summary?.compliance;
  const complianceTotal = compliance?.totalExpected ?? 0;
  const compliancePct = complianceTotal > 0 ? Math.round(((compliance?.submitted ?? 0) / complianceTotal) * 100) : 0;

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        Team dashboard
      </Typography.Title>

      {/* ---- Summary metrics ---- */}
      <Row gutter={16}>
        <Col span={6}>
          <Card loading={isSummaryFetching}>
            <Statistic title="Submitted this week" value={summary?.totalReportsSubmittedThisWeek ?? 0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={isSummaryFetching}>
            <Statistic title="Needs correction" value={summary?.needsCorrectionCount ?? 0} valueStyle={{ color: COLORS.ochre }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={isSummaryFetching}>
            <Statistic title="Open blockers" value={summary?.openBlockersCount ?? 0} valueStyle={{ color: COLORS.ochre }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={isSummaryFetching}>
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              Compliance this week
            </Typography.Text>
            <Progress
              percent={compliancePct}
              strokeColor={COLORS.teal}
              format={() => `${compliance?.submitted ?? 0}/${complianceTotal}`}
            />
            <Space size="small">
              <Tag color="blue">{compliance?.pending ?? 0} pending</Tag>
              <Tag color="orange">{compliance?.late ?? 0} late</Tag>
            </Space>
          </Card>
        </Col>
      </Row>

      {/* ---- Tasks completed trend ---- */}
      <Card title="Tasks completed trend (team-wide)">
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={tasksTrend}>
            <CartesianGrid stroke={COLORS.hairline} strokeDasharray="3 3" />
            <XAxis dataKey="weekStartDate" tick={{ fontSize: 12 }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
            <Tooltip />
            <Line type="monotone" dataKey="tasksCompleted" name="Tasks completed" stroke={COLORS.teal} strokeWidth={2} />
          </LineChart>
        </ResponsiveContainer>
      </Card>

      {/* ---- Status by member ---- */}
      <Card title="Report status by team member">
        <ResponsiveContainer width="100%" height={Math.max(280, statusByMember.length * 32)}>
          <BarChart data={statusByMember} layout="vertical" margin={{ left: 40 }}>
            <CartesianGrid stroke={COLORS.hairline} strokeDasharray="3 3" />
            <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12 }} />
            <YAxis type="category" dataKey="userName" width={140} tick={{ fontSize: 12 }} />
            <Tooltip />
            <Legend />
            <Bar dataKey="approved" name="Approved" stackId="a" fill={COLORS.teal} />
            <Bar dataKey="submitted" name="Submitted" stackId="a" fill={COLORS.muted} />
            <Bar dataKey="needsCorrection" name="Needs correction" stackId="a" fill={COLORS.ochre} />
          </BarChart>
        </ResponsiveContainer>
      </Card>

      <Row gutter={16}>
        {/* ---- Workload by project ---- */}
        <Col span={12}>
          <Card title="Workload by project (task count)">
            <ResponsiveContainer width="100%" height={320}>
              <BarChart data={workloadByProject} layout="vertical" margin={{ left: 20 }}>
                <CartesianGrid stroke={COLORS.hairline} strokeDasharray="3 3" />
                <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12 }} />
                <YAxis type="category" dataKey="projectName" width={160} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="taskCount" name="Tasks" fill={COLORS.teal} />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>

        {/* ---- Time by task type ---- */}
        <Col span={12}>
          <Card title="Time spent by task type (team-wide)">
            <ResponsiveContainer width="100%" height={320}>
              <PieChart>
                <Pie
                  data={timeByTaskType}
                  dataKey="totalHours"
                  nameKey="taskType"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  label={(entry) => `${entry.name}: ${entry.value}h`}
                >
                  {timeByTaskType.map((_, index) => (
                    <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      {/* ---- Recent activity feed ---- */}
      <Card title="Recent activity">
        <List
          dataSource={activity}
          renderItem={(item) => (
            <List.Item>
              <Space direction="vertical" size={0}>
                <span>{item.description}</span>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {item.projectName} · {new Date(item.actionAt).toLocaleString()}
                </Typography.Text>
              </Space>
            </List.Item>
          )}
        />
      </Card>
    </Space>
  );
}
