import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Form,
  Input,
  InputNumber,
  Select,
  DatePicker,
  Button,
  Card,
  Space,
  Divider,
  Checkbox,
  message,
} from "antd";
import { PlusOutlined, MinusCircleOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { useProjects } from "@/hooks/useProjects";
import { useReportDetail, useCreateReport, useUpdateReport } from "@/hooks/useReports";
import {
  PRIORITY_OPTIONS,
  TASK_STATUS_OPTIONS,
  TASK_TYPE_OPTIONS,
  NOTE_LINK_TYPE_OPTIONS,
} from "@/constants/reportOptions";
import type { ReportRequest } from "@/types/report";
import { ApiError } from "@/lib/axios";

const { RangePicker } = DatePicker;
const { TextArea } = Input;

// The report structure must be fixed and identical for every user - this
// form is the one place that structure is defined, matching ReportRequest exactly.
export function ReportFormPage() {
  const { reportId } = useParams<{ reportId: string }>();
  const isEdit = !!reportId;
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const { projects } = useProjects();
  const { report, isReportFetching } = useReportDetail(reportId);
  const createMutation = useCreateReport();
  const updateMutation = useUpdateReport(reportId ?? "");

  useEffect(() => {
    if (isEdit && report) {
      form.setFieldsValue({
        projectId: report.projectId,
        weekRange: [dayjs(report.weekStartDate), dayjs(report.weekEndDate)],
        notes: report.notes,
        tasks: report.tasks,
        nextWeekTasks: report.nextWeekTasks,
        blockers: report.blockers,
        achievements: report.achievements,
        timeLogs: report.timeLogs,
        noteLinks: report.noteLinks,
      });
    }
  }, [isEdit, report, form]);

  // Enforces "flag ONE as the key issue/achievement" - clicking a row's flag
  // sets it true and every other row in that same list false, rather than
  // letting each row's checkbox be independent.
  function setSingleFlag(
    listName: "blockers" | "achievements",
    flagName: "keyIssue" | "keyAchievement",
    index: number
  ) {
    const list = form.getFieldValue(listName) || [];
    form.setFieldValue(
      listName,
      list.map((item: Record<string, unknown>, i: number) => ({ ...item, [flagName]: i === index }))
    );
  }

  async function handleSubmit(values: any) {
    const payload: ReportRequest = {
      projectId: values.projectId,
      weekStartDate: values.weekRange[0].format("YYYY-MM-DD"),
      weekEndDate: values.weekRange[1].format("YYYY-MM-DD"),
      notes: values.notes,
      tasks: values.tasks ?? [],
      nextWeekTasks: values.nextWeekTasks ?? [],
      blockers: values.blockers ?? [],
      achievements: values.achievements ?? [],
      timeLogs: values.timeLogs ?? [],
      noteLinks: values.noteLinks ?? [],
    };

    try {
      if (isEdit && reportId) {
        await updateMutation.mutateAsync(payload);
        message.success("Report updated");
        navigate(`/reports/${reportId}`);
      } else {
        const created = await createMutation.mutateAsync(payload);
        message.success("Draft created");
        navigate(`/reports/${created.reportId}`);
      }
    } catch (err) {
      message.error(err instanceof ApiError ? err.message : "Something went wrong");
    }
  }

  if (isEdit && isReportFetching) {
    return <p>Loading report...</p>;
  }

  if (isEdit && report && report.status !== "DRAFT" && report.status !== "NEEDS_CORRECTION") {
    return <p>This report can no longer be edited (status: {report.status}).</p>;
  }

  const isSaving = createMutation.isPending || updateMutation.isPending;

  return (
    <Card title={isEdit ? "Edit weekly report" : "New weekly report"} style={{ maxWidth: 960 }}>
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Space size="large" align="start" wrap>
          <Form.Item
            name="projectId"
            label="Project"
            rules={[{ required: true, message: "Pick a project" }]}
            style={{ minWidth: 240 }}
          >
            <Select
              placeholder="Select project"
              options={projects.map((p) => ({ value: p.projectId, label: p.name }))}
            />
          </Form.Item>
          <Form.Item name="weekRange" label="Week" rules={[{ required: true, message: "Pick the week" }]}>
            <RangePicker />
          </Form.Item>
        </Space>

        <Form.Item name="notes" label="Notes">
          <TextArea rows={2} maxLength={2000} showCount placeholder="Optional notes for this week" />
        </Form.Item>

        <Divider orientation="left">Tasks completed</Divider>
        <Form.List name="tasks">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name, ...rest }) => (
                <Card
                  key={key}
                  size="small"
                  style={{ marginBottom: 12 }}
                  extra={<MinusCircleOutlined onClick={() => remove(name)} />}
                >
                  <Space wrap align="start">
                    <Form.Item {...rest} name={[name, "taskName"]} label="Task" rules={[{ required: true }]}>
                      <TextArea maxLength={200} showCount autoSize={{ minRows: 1, maxRows: 1 }} style={{ width: 220 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, "priority"]} label="Priority" rules={[{ required: true }]}>
                      <Select options={PRIORITY_OPTIONS} style={{ width: 120 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, "status"]} label="Status" rules={[{ required: true }]}>
                      <Select options={TASK_STATUS_OPTIONS} style={{ width: 150 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, "plannedPct"]} label="Planned %">
                      <InputNumber min={0} max={100} style={{ width: 100 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, "actualPct"]} label="Actual %">
                      <InputNumber min={0} max={100} style={{ width: 100 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, "timePlanned"]} label="Time planned (h)">
                      <InputNumber min={0} step={0.5} style={{ width: 130 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, "timeSpent"]} label="Time spent (h)">
                      <InputNumber min={0} step={0.5} style={{ width: 130 }} />
                    </Form.Item>
                    <Form.Item {...rest} name={[name, "deliverable"]} label="Deliverable">
                      <TextArea maxLength={300} showCount autoSize={{ minRows: 1, maxRows: 1 }} style={{ width: 220 }} />
                    </Form.Item>
                  </Space>
                </Card>
              ))}
              <Button
                type="dashed"
                onClick={() => add({ priority: "MEDIUM", status: "NOT_STARTED" })}
                icon={<PlusOutlined />}
              >
                Add task
              </Button>
            </>
          )}
        </Form.List>

        <Divider orientation="left">Tasks planned for next week</Divider>
        <Form.List name="nextWeekTasks">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name, ...rest }) => (
                <Space key={key} align="baseline" style={{ display: "flex", marginBottom: 8 }}>
                  <Form.Item {...rest} name={[name, "taskDescription"]} rules={[{ required: true }]}>
                    <TextArea
                      maxLength={300}
                      showCount
                      autoSize={{ minRows: 1, maxRows: 1 }}
                      placeholder="Task"
                      style={{ width: 320 }}
                    />
                  </Form.Item>
                  <Form.Item {...rest} name={[name, "priority"]}>
                    <Select options={PRIORITY_OPTIONS} placeholder="Priority" style={{ width: 120 }} />
                  </Form.Item>
                  <MinusCircleOutlined onClick={() => remove(name)} />
                </Space>
              ))}
              <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />}>
                Add task
              </Button>
            </>
          )}
        </Form.List>

        <Divider orientation="left">Blockers / challenges</Divider>
        <Form.Item shouldUpdate noStyle>
          {() => (
            <Form.List name="blockers">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...rest }) => {
                    const isKey = form.getFieldValue(["blockers", name, "keyIssue"]);
                    return (
                      <Space key={key} align="baseline" style={{ display: "flex", marginBottom: 8 }}>
                        <Form.Item {...rest} name={[name, "description"]} rules={[{ required: true }]}>
                          <TextArea
                            maxLength={500}
                            showCount
                            autoSize={{ minRows: 1, maxRows: 1 }}
                            placeholder="Blocker"
                            style={{ width: 360 }}
                          />
                        </Form.Item>
                        <Button
                          size="small"
                          type={isKey ? "primary" : "default"}
                          onClick={() => setSingleFlag("blockers", "keyIssue", name)}
                        >
                          Key issue
                        </Button>
                        <Form.Item
                          {...rest}
                          name={[name, "keyIssue"]}
                          valuePropName="checked"
                          initialValue={false}
                          style={{ display: "none" }}
                        >
                          <Checkbox />
                        </Form.Item>
                        <MinusCircleOutlined onClick={() => remove(name)} />
                      </Space>
                    );
                  })}
                  <Button type="dashed" onClick={() => add({ keyIssue: false })} icon={<PlusOutlined />}>
                    Add blocker
                  </Button>
                </>
              )}
            </Form.List>
          )}
        </Form.Item>

        <Divider orientation="left">Achievements / highlights</Divider>
        <Form.Item shouldUpdate noStyle>
          {() => (
            <Form.List name="achievements">
              {(fields, { add, remove }) => (
                <>
                  {fields.map(({ key, name, ...rest }) => {
                    const isKey = form.getFieldValue(["achievements", name, "keyAchievement"]);
                    return (
                      <Space key={key} align="baseline" style={{ display: "flex", marginBottom: 8 }}>
                        <Form.Item {...rest} name={[name, "description"]} rules={[{ required: true }]}>
                          <TextArea
                            maxLength={500}
                            showCount
                            autoSize={{ minRows: 1, maxRows: 1 }}
                            placeholder="Achievement"
                            style={{ width: 360 }}
                          />
                        </Form.Item>
                        <Button
                          size="small"
                          type={isKey ? "primary" : "default"}
                          onClick={() => setSingleFlag("achievements", "keyAchievement", name)}
                        >
                          Key achievement
                        </Button>
                        <Form.Item
                          {...rest}
                          name={[name, "keyAchievement"]}
                          valuePropName="checked"
                          initialValue={false}
                          style={{ display: "none" }}
                        >
                          <Checkbox />
                        </Form.Item>
                        <MinusCircleOutlined onClick={() => remove(name)} />
                      </Space>
                    );
                  })}
                  <Button type="dashed" onClick={() => add({ keyAchievement: false })} icon={<PlusOutlined />}>
                    Add achievement
                  </Button>
                </>
              )}
            </Form.List>
          )}
        </Form.Item>

        <Divider orientation="left">Hours by task type (optional)</Divider>
        <Form.List name="timeLogs">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name, ...rest }) => (
                <Space key={key} align="baseline" style={{ display: "flex", marginBottom: 8 }}>
                  <Form.Item {...rest} name={[name, "taskType"]} rules={[{ required: true }]}>
                    <Select options={TASK_TYPE_OPTIONS} placeholder="Type" style={{ width: 180 }} />
                  </Form.Item>
                  <Form.Item {...rest} name={[name, "hours"]} rules={[{ required: true }]}>
                    <InputNumber min={0} step={0.5} placeholder="Hours" style={{ width: 120 }} />
                  </Form.Item>
                  <MinusCircleOutlined onClick={() => remove(name)} />
                </Space>
              ))}
              <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />}>
                Add time log
              </Button>
            </>
          )}
        </Form.List>

        <Divider orientation="left">Notes &amp; links</Divider>
        <Form.List name="noteLinks">
          {(fields, { add, remove }) => (
            <>
              {fields.map(({ key, name, ...rest }) => (
                <Space key={key} align="baseline" style={{ display: "flex", marginBottom: 8 }}>
                  <Form.Item {...rest} name={[name, "type"]} rules={[{ required: true }]}>
                    <Select options={NOTE_LINK_TYPE_OPTIONS} style={{ width: 120 }} />
                  </Form.Item>
                  <Form.Item {...rest} name={[name, "content"]} rules={[{ required: true }]}>
                    <TextArea
                      maxLength={1000}
                      showCount
                      autoSize={{ minRows: 1, maxRows: 1 }}
                      placeholder="Content or URL"
                      style={{ width: 360 }}
                    />
                  </Form.Item>
                  <MinusCircleOutlined onClick={() => remove(name)} />
                </Space>
              ))}
              <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />}>
                Add note/link
              </Button>
            </>
          )}
        </Form.List>

        <Divider />
        <Space>
          <Button type="primary" htmlType="submit" loading={isSaving}>
            {isEdit ? "Save changes" : "Save draft"}
          </Button>
          <Button onClick={() => navigate(-1)}>Cancel</Button>
        </Space>
      </Form>
    </Card>
  );
}
