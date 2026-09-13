import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { Card, Descriptions, Table, Tag, Button, Space, Input, Modal, Typography, List, message } from "antd";
import { EditOutlined } from "@ant-design/icons";
import { useAuth } from "@/context/AuthContext";
import {
  useReportDetail,
  useReportVersions,
  useSubmitReport,
  useReviewReport,
  useEditReportComment,
} from "@/hooks/useReports";
import { StatusTag } from "@/components/StatusTag";
import { ApiError } from "@/lib/axios";
import type { ReviewDecision } from "@/types/report";

const { TextArea } = Input;

// Shared by both roles: a team member sees Edit/Submit on their own
// DRAFT/NEEDS_CORRECTION reports; a manager sees Approve/Request changes on
// SUBMITTED ones. Same page, different action set, driven by role + status.
export function ReportDetailPage() {
  const { reportId } = useParams<{ reportId: string }>();
  const { user } = useAuth();
  const { report, isReportFetching } = useReportDetail(reportId);
  const { versions } = useReportVersions(reportId);
  const submitMutation = useSubmitReport();
  const reviewMutation = useReviewReport();
  const editCommentMutation = useEditReportComment();

  const [isReviewModalOpen, setReviewModalOpen] = useState(false);
  const [pendingDecision, setPendingDecision] = useState<ReviewDecision | null>(null);
  const [reviewComment, setReviewComment] = useState("");

  const [isEditCommentModalOpen, setEditCommentModalOpen] = useState(false);
  const [editedComment, setEditedComment] = useState("");

  if (isReportFetching || !report) {
    return <p>Loading...</p>;
  }

  const isOwner = user?.userId === report.userId;
  const isManager = user?.role === "MANAGER";
  const canEdit = isOwner && (report.status === "DRAFT" || report.status === "NEEDS_CORRECTION");
  const canReview = isManager && report.status === "SUBMITTED";

  // 15-minute edit window, anchored to when the comment was first posted -
  // matches the backend's own check exactly, so the button simply won't be
  // there once the backend would refuse the request anyway.
  const COMMENT_EDIT_WINDOW_MS = 15 * 60 * 1000;
  const isWithinEditWindow = report.managerCommentPostedAt
    ? Date.now() - new Date(report.managerCommentPostedAt).getTime() < COMMENT_EDIT_WINDOW_MS
    : false;
  const canEditComment =
    isManager && report.status === "NEEDS_CORRECTION" && !!report.managerComment && isWithinEditWindow;

  async function handleSubmitReport() {
    try {
      await submitMutation.mutateAsync(reportId!);
      message.success("Report submitted");
    } catch (err) {
      message.error(err instanceof ApiError ? err.message : "Something went wrong");
    }
  }

  function openReviewModal(decision: ReviewDecision) {
    setPendingDecision(decision);
    setReviewComment("");
    setReviewModalOpen(true);
  }

  async function confirmReview() {
    if (!pendingDecision) return;
    if (pendingDecision === "REQUEST_CHANGES" && !reviewComment.trim()) {
      message.error("A comment is required when requesting changes");
      return;
    }
    try {
      await reviewMutation.mutateAsync({
        reportId: reportId!,
        payload: { decision: pendingDecision, comment: reviewComment || undefined },
      });
      message.success(pendingDecision === "APPROVE" ? "Report approved" : "Changes requested");
      setReviewModalOpen(false);
    } catch (err) {
      message.error(err instanceof ApiError ? err.message : "Something went wrong");
    }
  }

  function openEditCommentModal() {
    setEditedComment(report!.managerComment ?? "");
    setEditCommentModalOpen(true);
  }

  async function confirmEditComment() {
    if (!editedComment.trim()) {
      message.error("Comment cannot be empty");
      return;
    }
    try {
      await editCommentMutation.mutateAsync({ reportId: reportId!, comment: editedComment });
      message.success("Comment updated");
      setEditCommentModalOpen(false);
    } catch (err) {
      message.error(err instanceof ApiError ? err.message : "Something went wrong");
    }
  }

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Card
        title={`${report.projectName} — ${report.weekStartDate} to ${report.weekEndDate}`}
        extra={<StatusTag status={report.status} />}
      >
        <Descriptions column={2} size="small">
          <Descriptions.Item label="Team member">{report.userName}</Descriptions.Item>
          <Descriptions.Item label="Submitted">{report.submittedAt ?? "—"}</Descriptions.Item>
        </Descriptions>

        {report.managerComment && (
          <Card size="small" style={{ marginTop: 12, borderColor: "#B8802E" }}>
            <Space style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <div>
                <Typography.Text strong>Manager comment: </Typography.Text>
                {report.managerComment}
                {report.managerCommentEdited && (
                  <Typography.Text type="secondary" italic style={{ marginLeft: 6 }}>
                    (edited)
                  </Typography.Text>
                )}
              </div>
              {canEditComment && (
                <Button
                  size="small"
                  type="text"
                  icon={<EditOutlined />}
                  onClick={openEditCommentModal}
                  title="Fix a typo - does not change the report's status"
                />
              )}
            </Space>
          </Card>
        )}

        {report.notes && <p style={{ marginTop: 12 }}>{report.notes}</p>}

        <Space style={{ marginTop: 16 }}>
          {canEdit && (
            <Link to={`/reports/${reportId}/edit`}>
              <Button>Edit</Button>
            </Link>
          )}
          {canEdit && (
            <Button type="primary" loading={submitMutation.isPending} onClick={handleSubmitReport}>
              Submit
            </Button>
          )}
          {canReview && (
            <>
              <Button type="primary" onClick={() => openReviewModal("APPROVE")}>
                Approve
              </Button>
              <Button danger onClick={() => openReviewModal("REQUEST_CHANGES")}>
                Request changes
              </Button>
            </>
          )}
        </Space>
      </Card>

      <Card title="Tasks completed" size="small">
        <Table
          size="small"
          rowKey="taskName"
          pagination={false}
          dataSource={report.tasks}
          columns={[
            { title: "Task", dataIndex: "taskName" },
            { title: "Priority", dataIndex: "priority" },
            { title: "Status", dataIndex: "status" },
            { title: "Planned %", dataIndex: "plannedPct" },
            { title: "Actual %", dataIndex: "actualPct" },
            { title: "Time planned", dataIndex: "timePlanned" },
            { title: "Time spent", dataIndex: "timeSpent" },
            { title: "Deliverable", dataIndex: "deliverable" },
          ]}
        />
      </Card>

      <Card title="Next week" size="small">
        <List
          size="small"
          dataSource={report.nextWeekTasks}
          renderItem={(item) => (
            <List.Item>
              {item.taskDescription} {item.priority && <Tag>{item.priority}</Tag>}
            </List.Item>
          )}
        />
      </Card>

      <Card title="Blockers" size="small">
        <List
          size="small"
          dataSource={report.blockers}
          renderItem={(item) => (
            <List.Item>
              {item.description} {item.keyIssue && <Tag color="orange">Key issue</Tag>}
            </List.Item>
          )}
        />
      </Card>

      <Card title="Achievements" size="small">
        <List
          size="small"
          dataSource={report.achievements}
          renderItem={(item) => (
            <List.Item>
              {item.description} {item.keyAchievement && <Tag color="green">Key achievement</Tag>}
            </List.Item>
          )}
        />
      </Card>

      {report.timeLogs.length > 0 && (
        <Card title="Hours by task type" size="small">
          <List
            size="small"
            dataSource={report.timeLogs}
            renderItem={(item) => (
              <List.Item>
                {item.taskType}: {item.hours}h
              </List.Item>
            )}
          />
        </Card>
      )}

      {versions.length > 0 && (
        <Card title="Version history" size="small">
          <List
            size="small"
            dataSource={versions}
            renderItem={(v) => (
              <List.Item>
                <Space direction="vertical" size={0}>
                  <span>
                    Version {v.versionNumber} — {v.submittedAt}{" "}
                    {v.current && <Tag color="blue">Current</Tag>}
                  </span>
                  {v.comment && <span style={{ color: "#5B665F" }}>{v.comment}</span>}
                </Space>
              </List.Item>
            )}
          />
        </Card>
      )}

      <Modal
        title={pendingDecision === "APPROVE" ? "Approve report" : "Request changes"}
        open={isReviewModalOpen}
        onOk={confirmReview}
        onCancel={() => setReviewModalOpen(false)}
        confirmLoading={reviewMutation.isPending}
      >
        <TextArea
          rows={3}
          maxLength={1000}
          showCount
          placeholder={pendingDecision === "APPROVE" ? "Optional note" : "Explain what needs to change"}
          value={reviewComment}
          onChange={(e) => setReviewComment(e.target.value)}
        />
      </Modal>

      <Modal
        title="Edit comment"
        open={isEditCommentModalOpen}
        onOk={confirmEditComment}
        onCancel={() => setEditCommentModalOpen(false)}
        confirmLoading={editCommentMutation.isPending}
      >
        <TextArea rows={3} maxLength={1000} showCount value={editedComment} onChange={(e) => setEditedComment(e.target.value)} />
      </Modal>
    </Space>
  );
}
