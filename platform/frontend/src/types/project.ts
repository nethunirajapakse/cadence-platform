export interface Project {
  projectId: string;
  name: string;
  description?: string;
}

export interface ProjectRequest {
  name: string;
  description?: string;
}

export interface ProjectFilterCriteria {
  name?: string;
  description?: string;
}
