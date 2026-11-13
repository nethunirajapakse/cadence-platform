import axios, { type AxiosError } from "axios";

export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? "http://localhost:8080" : "/"),
  withCredentials: true, // send/receive the httpOnly auth cookie
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (!error.response) {
      return Promise.reject(new ApiError(error.message || "Network error", 0));
    }

    const { status, data } = error.response;
    const config = error.config as (typeof error.config & { _retried?: boolean }) | undefined;
    const isStaleCsrfAuthError =
      status === 401 &&
      typeof data === "object" &&
      data !== null &&
      "error" in data &&
      String((data as { error: unknown }).error).includes("Full authentication is required");

    if (isStaleCsrfAuthError && config && !config._retried) {
      config._retried = true;
      return api.request(config);
    }

    // 401 shape from SecurityConfig's authenticationEntryPoint: { "error": "..." }
    if (typeof data === "object" && data !== null && "error" in data) {
      return Promise.reject(new ApiError(String((data as { error: unknown }).error), status));
    }

    // 400 shape from GlobalExceptionHandler's validation handler: { field: message, ... }
    if (typeof data === "object" && data !== null) {
      const fieldErrors = data as Record<string, string>;
      const message = Object.values(fieldErrors).join(" ");
      return Promise.reject(new ApiError(message, status, fieldErrors));
    }

    // 409/404 come back as plain text (ResponseEntity<String>), not JSON.
    return Promise.reject(new ApiError(String(data), status));
  }
);

export default api;
