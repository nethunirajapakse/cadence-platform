import { useState } from "react";
import { Table, Button, Space, Modal, Form, Input, message, Popconfirm, Typography } from "antd";
import { useProjects, useCreateProject, useUpdateProject, useDeleteProject } from "@/hooks/useProjects";
import type { Project } from "@/types/project";
import { ApiError } from "@/lib/axios";

const { TextArea } = Input;

export function ProjectsPage() {
  const { projects, isProjectsFetching } = useProjects();
  const createMutation = useCreateProject();
  const updateMutation = useUpdateProject();
  const deleteMutation = useDeleteProject();

  const [isModalOpen, setModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [form] = Form.useForm();

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
        columns={[
          { title: "Name", dataIndex: "name" },
          { title: "Description", dataIndex: "description" },
          {
            title: "",
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
        ]}
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
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
