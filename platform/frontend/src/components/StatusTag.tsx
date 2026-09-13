import { Tag } from "antd";
import type { ReportStatus } from "@/types/report";

const COLORS: Record<ReportStatus, string> = {
  DRAFT: "default",
  SUBMITTED: "blue",
  NEEDS_CORRECTION: "orange",
  APPROVED: "green",
};

const LABELS: Record<ReportStatus, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  NEEDS_CORRECTION: "Needs correction",
  APPROVED: "Approved",
};

export function StatusTag({ status }: { status: ReportStatus }) {
  return <Tag color={COLORS[status]}>{LABELS[status]}</Tag>;
}
