// Thin fetch wrapper. Deliberately not axios - one more dependency isn't worth it
// for a handful of endpoints, and fetch + a small error-parsing layer covers
// everything this backend actually returns.

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const TOKEN_KEY = "cadence_token";

export class ApiError extends Error {
  status: number;
  // Populated only for 400 validation errors (MethodArgumentNotValidException on
  // the backend), which come back as a field-name -> message map. Everything
  // else (401 JSON {error}, 409/404 plain text) just uses `message`.
  fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

async function parseErrorBody(res: Response): Promise<ApiError> {
  const text = await res.text();

  if (!text) {
    return new ApiError(res.statusText, res.status);
  }

  try {
    const json = JSON.parse(text);

    // 401 shape from SecurityConfig's authenticationEntryPoint: { "error": "..." }
    if (typeof json === "object" && json !== null && "error" in json) {
      return new ApiError(String(json.error), res.status);
    }

    // 400 shape from GlobalExceptionHandler's validation handler: { field: message, ... }
    if (typeof json === "object" && json !== null) {
      const messages = Object.values(json as Record<string, string>);
      return new ApiError(messages.join(" "), res.status, json as Record<string, string>);
    }

    return new ApiError(String(json), res.status);
  } catch {
    // 409/404 come back as plain text (ResponseEntity<String>), not JSON.
    return new ApiError(text, res.status);
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
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
