import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { previewInvitation } from "@/api/auth";
import { AuthCard, ErrorNotice, inputClass, primaryButtonClass } from "@/components/AuthCard";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/lib/axios";
import { ROLE_LABELS, type InvitationPreview } from "@/types/auth";

type PreviewState =
  | { status: "loading" }
  | { status: "invalid"; message: string }
  | { status: "ready"; invitation: InvitationPreview };

export function AcceptInvitePage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const { acceptInvite } = useAuth();
  const navigate = useNavigate();

  const [preview, setPreview] = useState<PreviewState>({ status: "loading" });
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!token) return;
    previewInvitation(token)
      .then((invitation) => setPreview({ status: "ready", invitation }))
      .catch((err) =>
        setPreview({
          status: "invalid",
          message:
            err instanceof ApiError
              ? err.message
              : "We couldn't check this invitation. Try again in a moment.",
        })
      );
  }, [token]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});

    if (password !== confirm) {
      setFieldErrors({ password: "The two passwords don't match." });
      return;
    }

    setIsSubmitting(true);
    try {
      await acceptInvite({ token, name, password });
      navigate("/");
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) {
        setFieldErrors(err.fieldErrors);
      } else {
        setError(err instanceof ApiError ? err.message : "Something went wrong. Try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <AuthCard
        title="Invitation not valid"
        footer={
          <Link to="/login" className="font-medium text-teal hover:underline">
            Go to log in
          </Link>
        }
      >
        <p className="text-sm text-ink">This invitation link is incomplete. Ask your administrator for a new one.</p>
      </AuthCard>
    );
  }

  if (preview.status === "loading") {
    return (
      <AuthCard title="Checking your invitation...">
        <p className="text-sm text-muted">One moment.</p>
      </AuthCard>
    );
  }

  if (preview.status === "invalid") {
    return (
      <AuthCard
        title="Invitation not valid"
        footer={
          <Link to="/login" className="font-medium text-teal hover:underline">
            Go to log in
          </Link>
        }
      >
        <p className="text-sm text-ink">{preview.message}</p>
      </AuthCard>
    );
  }

  const { invitation } = preview;
  const role = ROLE_LABELS[invitation.role];

  return (
    <AuthCard
      title="Set up your account"
      subtitle={
        invitation.managerName
          ? `You're joining as ${role.toLowerCase()}, reporting to ${invitation.managerName}.`
          : `You're joining as ${role.toLowerCase()}.`
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-ink">
            Email
          </label>
          <input id="email" type="email" value={invitation.email} readOnly className={`${inputClass} bg-paper`} />
        </div>

        <div>
          <label htmlFor="name" className="block text-sm font-medium text-ink">
            Your name
          </label>
          <input
            id="name"
            type="text"
            required
            maxLength={120}
            autoComplete="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
          {fieldErrors.name && <p className="mt-1 text-xs text-ochre">{fieldErrors.name}</p>}
        </div>

        <div>
          <label htmlFor="password" className="block text-sm font-medium text-ink">
            Password
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
          <p className="mt-1 text-xs text-muted">At least 10 characters. A long phrase works well.</p>
          {fieldErrors.password && <p className="mt-1 text-xs text-ochre">{fieldErrors.password}</p>}
        </div>

        <div>
          <label htmlFor="confirm" className="block text-sm font-medium text-ink">
            Confirm password
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
          {isSubmitting ? "Creating account..." : "Create account"}
        </button>
      </form>
    </AuthCard>
  );
}
