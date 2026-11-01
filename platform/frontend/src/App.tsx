import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "@/context/AuthContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { AppLayout } from "@/components/AppLayout";
import { LoginPage } from "@/pages/LoginPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { ResetPasswordPage } from "@/pages/ResetPasswordPage";
import { AcceptInvitePage } from "@/pages/AcceptInvitePage";
import { AdminUsersPage } from "@/pages/AdminUsersPage";
import { AdminInvitationsPage } from "@/pages/AdminInvitationsPage";
import { AdminAuditLogPage } from "@/pages/AdminAuditLogPage";
import { ReportHistoryPage } from "@/pages/ReportHistoryPage";
import { ReportFormPage } from "@/pages/ReportFormPage";
import { ReportDetailPage } from "@/pages/ReportDetailPage";
import { TeamDashboardPage } from "@/pages/TeamDashboardPage";
import { AllReportsPage } from "@/pages/AllReportsPage";
import { TeamMembersPage } from "@/pages/TeamMembersPage";
import { TeamMemberProfilePage } from "@/pages/TeamMemberProfilePage";
import { ProjectsPage } from "@/pages/ProjectsPage";

// Landing page differs by role - a team member's home is their own report
// history, a manager's is the team dashboard, an administrator's is the user list.
function HomeRedirect() {
  const { user } = useAuth();
  const home =
    user?.role === "ADMIN" ? "/admin/users" : user?.role === "MANAGER" ? "/dashboard" : "/reports";
  return <Navigate to={home} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/accept-invite" element={<AcceptInvitePage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<HomeRedirect />} />
            <Route
              path="/reports"
              element={
                <ProtectedRoute allowedRoles={["TEAM_MEMBER"]}>
                  <ReportHistoryPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reports/new"
              element={
                <ProtectedRoute allowedRoles={["TEAM_MEMBER"]}>
                  <ReportFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reports/:reportId"
              element={
                <ProtectedRoute allowedRoles={["TEAM_MEMBER", "MANAGER"]}>
                  <ReportDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reports/:reportId/edit"
              element={
                <ProtectedRoute allowedRoles={["TEAM_MEMBER"]}>
                  <ReportFormPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/users"
              element={
                <ProtectedRoute allowedRoles={["ADMIN"]}>
                  <AdminUsersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/invitations"
              element={
                <ProtectedRoute allowedRoles={["ADMIN"]}>
                  <AdminInvitationsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/audit-log"
              element={
                <ProtectedRoute allowedRoles={["ADMIN"]}>
                  <AdminAuditLogPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute allowedRoles={["MANAGER"]}>
                  <TeamDashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reports/all"
              element={
                <ProtectedRoute allowedRoles={["MANAGER"]}>
                  <AllReportsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/team-members"
              element={
                <ProtectedRoute allowedRoles={["MANAGER"]}>
                  <TeamMembersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/team-members/:userId"
              element={
                <ProtectedRoute allowedRoles={["MANAGER"]}>
                  <TeamMemberProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/projects"
              element={
                <ProtectedRoute allowedRoles={["MANAGER"]}>
                  <ProjectsPage />
                </ProtectedRoute>
              }
            />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
