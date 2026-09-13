import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { RoleName } from "../types/auth";

interface ProtectedRouteProps {
  children: ReactNode;
  // Optional role gate for later - e.g. <ProtectedRoute allowedRoles={["MANAGER"]}>
  // wrapping the team dashboard route once that page exists.
  allowedRoles?: RoleName[];
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return null;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
