import { useState } from "react";
import { Table, Button, Space, Modal, Form, Input, message, Popconfirm, Typography } from "antd";
import { SearchOutlined, ClearOutlined } from "@ant-design/icons";
import type { ColumnType } from "antd/es/table";
import { useProjects, useCreateProject, useUpdateProject, useDeleteProject } from "@/hooks/useProjects";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { makeFilterDropdown } from "@/components/ColumnFilterDropdown";
import type { Project } from "@/types/project";
import { ApiError } from "@/lib/axios";

const { TextArea } = Input;

export function ProjectsPage() {
  // Immediate input state (so typing feels instant) vs. debounced state (what
  // actually gets sent to the backend) - avoids firing a request per keystroke.
  const [nameFilter, setNameFilter] = useState("");
  const [descriptionFilter, setDescriptionFilter] = useState("");
  const debouncedName = useDebouncedValue(nameFilter, 300);
  const debouncedDescription = useDebouncedValue(descriptionFilter, 300);

  const { projects, isProjectsFetching } = useProjects({
    name: debouncedName,
    description: debouncedDescription,
  });
  const createMutation = useCreateProject();
  const updateMutation = useUpdateProject();
  const deleteMutation = useDeleteProject();

  const [isModalOpen, setModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [form] = Form.useForm();

  const hasActiveFilters = !!nameFilter || !!descriptionFilter;

  function resetAllFilters() {
    setNameFilter("");
    setDescriptionFilter("");
  }

  function openCreateModal() {
    setEditingProject(null);
    form.resetFields();
    setModalOpen(true);
  }

  function openEditModal(project: Project) {
    setEditingProject(project);
    form.setFieldsValue(project);
    setModalOpen(true);
  }

  async function handleSave() {
    try {
      const values = await form.validateFields();
      if (editingProject) {
        await updateMutation.mutateAsync({ projectId: editingProject.projectId, payload: values });
        message.success("Project updated");
      } else {
        await createMutation.mutateAsync(values);
        message.success("Project created");
      }
      setModalOpen(false);
    } catch (err) {
      if (err instanceof ApiError) message.error(err.message);
    }
  }

  async function handleDelete(projectId: string) {
    try {
      await deleteMutation.mutateAsync(projectId);
      message.success("Project deleted");
    } catch (err) {
      // 409 expected when reports still reference this project - the backend
      // deliberately blocks the delete rather than cascading it away.
      message.error(err instanceof ApiError ? err.message : "Could not delete project");
    }
  }

  const activeFilterColor = (active: boolean) => (active ? "#2F6F63" : undefined);

  const columns: ColumnType<Project>[] = [
    {
      title: "Name",
      dataIndex: "name",
      filterIcon: () => <SearchOutlined style={{ color: activeFilterColor(!!nameFilter) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <Input
            placeholder="Search by name..."
            value={nameFilter}
            onChange={(e) => setNameFilter(e.target.value)}
            allowClear
          />
        ),
        () => setNameFilter("")
      ),
    },
    {
      title: "Description",
      dataIndex: "description",
      filterIcon: () => <SearchOutlined style={{ color: activeFilterColor(!!descriptionFilter) }} />,
      filterDropdown: makeFilterDropdown(
        () => (
          <Input
            placeholder="Search by description..."
            value={descriptionFilter}
            onChange={(e) => setDescriptionFilter(e.target.value)}
            allowClear
          />
        ),
        () => setDescriptionFilter("")
      ),
    },
    {
      title: (
        <Button size="small" icon={<ClearOutlined />} disabled={!hasActiveFilters} onClick={resetAllFilters}>
          Reset filters
        </Button>
      ),
      key: "actions",
      render: (_: unknown, project: Project) => (
        <Space>
          <Button size="small" onClick={() => openEditModal(project)}>
            Edit
          </Button>
          <Popconfirm title="Delete this project?" onConfirm={() => handleDelete(project.projectId)}>
            <Button size="small" danger>
              Delete
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Space style={{ display: "flex", justifyContent: "space-between" }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          Projects
        </Typography.Title>
        <Button type="primary" onClick={openCreateModal}>
          New project
        </Button>
      </Space>

      <Table
        rowKey="projectId"
        loading={isProjectsFetching}
        dataSource={projects}
        pagination={false}
        columns={columns}
      />

      <Modal
        title={editingProject ? "Edit project" : "New project"}
        open={isModalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        confirmLoading={createMutation.isPending || updateMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input maxLength={120} showCount />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
