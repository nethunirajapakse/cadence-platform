import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "@/context/AuthContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { AppLayout } from "@/components/AppLayout";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { ReportHistoryPage } from "@/pages/ReportHistoryPage";
import { ReportFormPage } from "@/pages/ReportFormPage";
import { ReportDetailPage } from "@/pages/ReportDetailPage";
import { TeamDashboardPage } from "@/pages/TeamDashboardPage";
import { AllReportsPage } from "@/pages/AllReportsPage";
import { ProjectsPage } from "@/pages/ProjectsPage";

// Landing page differs by role - a team member's home is their own report
// history, a manager's is the team dashboard.
function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={user?.role === "MANAGER" ? "/dashboard" : "/reports"} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<HomeRedirect />} />
            <Route path="/reports" element={<ReportHistoryPage />} />
            <Route path="/reports/new" element={<ReportFormPage />} />
            <Route path="/reports/:reportId" element={<ReportDetailPage />} />
            <Route path="/reports/:reportId/edit" element={<ReportFormPage />} />
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
