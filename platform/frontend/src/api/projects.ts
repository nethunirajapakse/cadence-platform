import api from "@/lib/axios";
import type { Project, ProjectRequest, ProjectFilterCriteria } from "@/types/project";

export const getProjects = async (filters: ProjectFilterCriteria = {}): Promise<Project[]> => {
  const response = await api.get("/api/projects", { params: filters });
  return response.data;
};

export const createProject = async (payload: ProjectRequest): Promise<Project> => {
  const response = await api.post("/api/projects", payload);
  return response.data;
};

export const updateProject = async (projectId: string, payload: ProjectRequest): Promise<Project> => {
  const response = await api.put(`/api/projects/${projectId}`, payload);
  return response.data;
};

export const deleteProject = async (projectId: string): Promise<void> => {
  await api.delete(`/api/projects/${projectId}`);
};
