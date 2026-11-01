import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { requestPasswordReset } from "@/api/auth";
import { AuthCard, ErrorNotice, inputClass, primaryButtonClass } from "@/components/AuthCard";
import { ApiError } from "@/lib/axios";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [isSent, setIsSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await requestPasswordReset(email);
      setIsSent(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  const backToLogin = (
    <Link to="/login" className="font-medium text-teal hover:underline">
      Back to log in
    </Link>
  );

  if (isSent) {
    return (
      <AuthCard title="Check your email" footer={backToLogin}>
        <p className="text-sm text-ink">
          If an account exists for <strong>{email}</strong>, we've sent a link to reset the password. It works once
          and expires in 30 minutes.
        </p>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title="Reset your password"
      subtitle="Enter your work email and we'll send you a reset link."
      footer={backToLogin}
    >
      <form onSubmit={handleSubmit} className="space-y-5">
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
            className={inputClass}
          />
        </div>

        {error && <ErrorNotice message={error} />}

        <button type="submit" disabled={isSubmitting} className={primaryButtonClass}>
          {isSubmitting ? "Sending..." : "Send reset link"}
        </button>
      </form>
    </AuthCard>
  );
}
