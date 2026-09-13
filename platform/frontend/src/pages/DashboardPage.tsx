import { useAuth } from "../context/AuthContext";

// Placeholder landing page - just enough to prove the full auth loop works
// end to end. The real personal-report / team-dashboard views replace this.
export function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-paper px-4 py-10">
      <div className="mx-auto max-w-2xl">
        <div className="flex items-center justify-between border border-hairline bg-white p-6">
          <div>
            <p className="text-sm text-muted">Logged in as</p>
            <p className="font-display text-lg font-semibold text-ink">{user?.name}</p>
            <p className="text-sm text-muted">
              {user?.email} · {user?.role === "MANAGER" ? "Manager" : "Team member"}
            </p>
          </div>
          <button
            onClick={logout}
            className="border border-hairline px-4 py-2 text-sm font-medium text-ink transition-colors hover:border-ink/30"
          >
            Log out
          </button>
        </div>
      </div>
    </div>
  );
}
