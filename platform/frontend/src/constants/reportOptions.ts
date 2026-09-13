export const PRIORITY_OPTIONS = [
  { value: "HIGH", label: "High" },
  { value: "MEDIUM", label: "Medium" },
  { value: "LOW", label: "Low" },
];

export const TASK_STATUS_OPTIONS = [
  { value: "NOT_STARTED", label: "Not started" },
  { value: "IN_PROGRESS", label: "In progress" },
  { value: "DONE", label: "Done" },
];

export const TASK_TYPE_OPTIONS = [
  { value: "DEVELOPMENT", label: "Development" },
  { value: "TESTING", label: "Testing" },
  { value: "MEETINGS", label: "Meetings" },
  { value: "DOCUMENTATION", label: "Documentation" },
  { value: "OTHER", label: "Other" },
];

export const NOTE_LINK_TYPE_OPTIONS = [
  { value: "NOTE", label: "Note" },
  { value: "LINK", label: "Link" },
];

export const REPORT_STATUS_OPTIONS = [
  { value: "DRAFT", label: "Draft" },
  { value: "SUBMITTED", label: "Submitted" },
  { value: "NEEDS_CORRECTION", label: "Needs correction" },
  { value: "APPROVED", label: "Approved" },
];

export const MANAGER_VISIBLE_STATUS_OPTIONS = REPORT_STATUS_OPTIONS.filter(
  (option) => option.value !== "DRAFT"
);
