import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CadenceMark } from "../components/CadenceMark";
import { useAuth } from "../context/AuthContext";
import { ApiError } from "../lib/api";
import type { RoleName } from "../types/auth";

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<RoleName>("TEAM_MEMBER");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    setIsSubmitting(true);

    try {
      await register({ name, email, password, role });
      navigate("/");
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.fieldErrors) {
          setFieldErrors(err.fieldErrors);
        } else {
          setError(err.message);
        }
      } else {
        setError("Something went wrong. Try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <CadenceMark />

        <div className="border border-hairline bg-white p-8">
          <h2 className="font-display text-lg font-semibold text-ink">Create your account</h2>
          <p className="mt-1 text-sm text-muted">
            Team members log their weeks; managers review them.
          </p>

          <form onSubmit={handleSubmit} className="mt-6 space-y-5">
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-ink">
                Name
              </label>
              <input
                id="name"
                type="text"
                required
                autoComplete="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="mt-1.5 w-full border border-hairline bg-white px-3 py-2 text-sm text-ink outline-none focus:border-teal"
              />
              {fieldErrors.name && <p className="mt-1 text-xs text-ochre">{fieldErrors.name}</p>}
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-ink">
                Email
              </label>
              <input
                id="email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1.5 w-full border border-hairline bg-white px-3 py-2 text-sm text-ink outline-none focus:border-teal"
              />
              {fieldErrors.email && <p className="mt-1 text-xs text-ochre">{fieldErrors.email}</p>}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-ink">
                Password
              </label>
              <input
                id="password"
                type="password"
                required
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1.5 w-full border border-hairline bg-white px-3 py-2 text-sm text-ink outline-none focus:border-teal"
              />
              {fieldErrors.password && (
                <p className="mt-1 text-xs text-ochre">{fieldErrors.password}</p>
              )}
            </div>

            <div>
              <span className="block text-sm font-medium text-ink">Role</span>
              {/* Self-service role choice at signup - there's no admin-invite flow in
                  this build, so this is how a person becomes a team member or manager. */}
              <div className="mt-1.5 grid grid-cols-2 gap-2">
                {(["TEAM_MEMBER", "MANAGER"] as const).map((option) => (
                  <button
                    type="button"
                    key={option}
                    onClick={() => setRole(option)}
                    className={`border px-3 py-2 text-sm font-medium transition-colors ${
                      role === option
                        ? "border-teal bg-teal/10 text-teal"
                        : "border-hairline text-muted hover:border-ink/30"
                    }`}
                  >
                    {option === "TEAM_MEMBER" ? "Team member" : "Manager"}
                  </button>
                ))}
              </div>
            </div>

            {error && (
              <p role="alert" className="border border-ochre/40 bg-ochre/10 px-3 py-2 text-sm text-ochre">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full bg-teal py-2.5 text-sm font-medium text-white transition-colors hover:bg-teal/90 disabled:opacity-60"
            >
              {isSubmitting ? "Creating account..." : "Create account"}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-muted">
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-teal hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
