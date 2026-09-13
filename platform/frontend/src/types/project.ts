export interface Project {
  projectId: string;
  name: string;
  description?: string;
}

export interface ProjectRequest {
  name: string;
  description?: string;
}
