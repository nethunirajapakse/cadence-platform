import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getProjects, createProject, updateProject, deleteProject } from "@/api/projects";
import type { ProjectRequest, ProjectFilterCriteria } from "@/types/project";

export const useProjects = (filters: ProjectFilterCriteria = {}) => {
  const {
    data,
    isFetching: isProjectsFetching,
    error: projectsError,
  } = useQuery({
    queryKey: ["projects", filters],
    queryFn: () => getProjects(filters),
  });

  return {
    projects: data ?? [],
    isProjectsFetching,
    projectsError,
  };
};

export const useCreateProject = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProjectRequest) => createProject(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["projects"] }),
  });
};

export const useUpdateProject = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, payload }: { projectId: string; payload: ProjectRequest }) =>
      updateProject(projectId, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["projects"] }),
  });
};

export const useDeleteProject = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (projectId: string) => deleteProject(projectId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["projects"] }),
  });
};
