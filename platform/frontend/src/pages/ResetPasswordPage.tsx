import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { resetPassword } from "@/api/auth";
import { AuthCard, ErrorNotice, inputClass, primaryButtonClass } from "@/components/AuthCard";
import { ApiError } from "@/lib/axios";

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [isDone, setIsDone] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setFieldError(null);

    if (password !== confirm) {
      setFieldError("The two passwords don't match.");
      return;
    }

    setIsSubmitting(true);
    try {
      await resetPassword({ token, password });
      setIsDone(true);
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors?.password) {
        setFieldError(err.fieldErrors.password);
      } else {
        setError(err instanceof ApiError ? err.message : "Something went wrong. Try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  const toLogin = (
    <Link to="/login" className="font-medium text-teal hover:underline">
      Go to log in
    </Link>
  );

  if (!token) {
    return (
      <AuthCard title="Link not valid" footer={<Link to="/forgot-password" className="font-medium text-teal hover:underline">Request a new link</Link>}>
        <p className="text-sm text-ink">This password reset link is incomplete. Request a new one.</p>
      </AuthCard>
    );
  }

  if (isDone) {
    return (
      <AuthCard title="Password updated" footer={toLogin}>
        <p className="text-sm text-ink">Your password has been changed. Log in with the new password.</p>
      </AuthCard>
    );
  }

  return (
    <AuthCard title="Choose a new password" subtitle="At least 10 characters. A long phrase works well.">
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="password" className="block text-sm font-medium text-ink">
            New password
          </label>
          <input
            id="password"
            type="password"
            required
            minLength={10}
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
          {fieldError && <p className="mt-1 text-xs text-ochre">{fieldError}</p>}
        </div>

        <div>
          <label htmlFor="confirm" className="block text-sm font-medium text-ink">
            Confirm new password
          </label>
          <input
            id="confirm"
            type="password"
            required
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            className={inputClass}
          />
        </div>

        {error && <ErrorNotice message={error} />}

        <button type="submit" disabled={isSubmitting} className={primaryButtonClass}>
          {isSubmitting ? "Saving..." : "Update password"}
        </button>
      </form>
    </AuthCard>
  );
}
