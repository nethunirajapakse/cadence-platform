import axios, { type AxiosError } from "axios";

// Populated only for 400 validation errors, which come back as a
// field-name -> message map from the backend's GlobalExceptionHandler -
// lets forms show inline errors per field instead of one generic message.
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
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
  withCredentials: true, // send/receive the httpOnly auth cookie
  // Axios's built-in CSRF echo: reads the XSRF-TOKEN cookie Spring's
  // CsrfCookieFilter sets and sends it back as X-XSRF-TOKEN on state-changing
  // requests - these are Spring Security's own default names, so no custom
  // interceptor is needed. withXSRFToken must be explicit here because the
  // frontend (5173) and backend (8080) are different origins - axios only
  // does this automatically for same-origin requests otherwise.
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

    // Known CSRF-cookie race: Spring's CsrfFilter rejects a request whose
    // X-XSRF-TOKEN header no longer matches its expected value, but that
    // same rejection also refreshes the cookie in its response. Retrying
    // once - now that axios will read the freshly-set cookie - resolves it
    // transparently instead of making the user click twice.
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
