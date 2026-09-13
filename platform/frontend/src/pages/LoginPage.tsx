import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CadenceMark } from "../components/CadenceMark";
import { useAuth } from "../context/AuthContext";
import { ApiError } from "../lib/api";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await login({ email, password });
      navigate("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <CadenceMark />

        <div className="border border-hairline bg-white p-8">
          <h2 className="font-display text-lg font-semibold text-ink">Log in</h2>
          <p className="mt-1 text-sm text-muted">Pick up your team's weekly reports.</p>

          <form onSubmit={handleSubmit} className="mt-6 space-y-5">
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
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-ink">
                Password
              </label>
              <input
                id="password"
                type="password"
                required
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1.5 w-full border border-hairline bg-white px-3 py-2 text-sm text-ink outline-none focus:border-teal"
              />
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
              {isSubmitting ? "Logging in..." : "Log in"}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-muted">
          New here?{" "}
          <Link to="/register" className="font-medium text-teal hover:underline">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
}
