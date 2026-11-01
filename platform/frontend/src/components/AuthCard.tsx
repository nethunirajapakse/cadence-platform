import type { ReactNode } from "react";
import { CadenceMark } from "./CadenceMark";

interface AuthCardProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
}

// Shared frame for the signed-out screens (login, accept invitation, password reset).
export function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <CadenceMark />

        <div className="border border-hairline bg-white p-8">
          <h2 className="font-display text-lg font-semibold text-ink">{title}</h2>
          {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </div>

        {footer && <p className="mt-6 text-center text-sm text-muted">{footer}</p>}
      </div>
    </div>
  );
}

export const inputClass =
  "mt-1.5 w-full border border-hairline bg-white px-3 py-2 text-sm text-ink outline-none focus:border-teal";

export const primaryButtonClass =
  "w-full bg-teal py-2.5 text-sm font-medium text-white transition-colors hover:bg-teal/90 disabled:opacity-60";

export function ErrorNotice({ message }: { message: string }) {
  return (
    <p role="alert" className="border border-ochre/40 bg-ochre/10 px-3 py-2 text-sm text-ochre">
      {message}
    </p>
  );
}
