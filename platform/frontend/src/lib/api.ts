// Thin fetch wrapper. The auth token itself is an httpOnly cookie the browser
// attaches automatically - there's no header for this code to set for that.
// What this layer DOES still need to handle: sending credentials on every
// request, and echoing the CSRF cookie back as a header on state-changing ones.

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  // Populated only for 400 validation errors, which come back as a
  // field-name -> message map from the backend's GlobalExceptionHandler.
  fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp("(?:^|; )" + name + "=([^;]*)"));
  return match ? decodeURIComponent(match[1]) : null;
}

async function parseErrorBody(res: Response): Promise<ApiError> {
  const text = await res.text();

  if (!text) {
    return new ApiError(res.statusText, res.status);
  }

  try {
    const json = JSON.parse(text);

    if (typeof json === "object" && json !== null && "error" in json) {
      return new ApiError(String(json.error), res.status);
    }

    if (typeof json === "object" && json !== null) {
      const messages = Object.values(json as Record<string, string>);
      return new ApiError(messages.join(" "), res.status, json as Record<string, string>);
    }

    return new ApiError(String(json), res.status);
  } catch {
    return new ApiError(text, res.status);
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? "GET").toUpperCase();

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> | undefined),
  };

  // Spring Security's double-submit CSRF check: the server set a (non-httpOnly)
  // XSRF-TOKEN cookie via CsrfCookieFilter; echoing its value back as a header
  // proves the request came from a page that could actually read that cookie -
  // i.e. same-origin JS, not a cross-site form/script. Only needed for methods
  // that change state; GET/HEAD are never CSRF-checked.
  if (method !== "GET" && method !== "HEAD") {
    const csrfToken = getCookie("XSRF-TOKEN");
    if (csrfToken) {
      headers["X-XSRF-TOKEN"] = csrfToken;
    }
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include",
    headers,
  });

  if (!res.ok) {
    throw await parseErrorBody(res);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return (await res.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "POST", body: JSON.stringify(body) }),
};
