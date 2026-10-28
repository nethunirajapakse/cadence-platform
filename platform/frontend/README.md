# Cadence Frontend

React single-page application for the Cadence platform. Team members create and submit weekly reports. Managers review reports, manage projects, and view team analytics.

## Contents

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Scripts](#scripts)
- [Project structure](#project-structure)
- [Routes and access](#routes-and-access)
- [Authentication](#authentication)
- [Data fetching](#data-fetching)
- [Error handling](#error-handling)
- [Styling](#styling)
- [Building and deploying](#building-and-deploying)
- [Troubleshooting](#troubleshooting)

## Tech stack

| Concern | Technology |
| --- | --- |
| Framework | React 19, TypeScript |
| Build tool | Vite |
| UI components | Ant Design 6, Ant Design Icons |
| Styling | Ant Design theme tokens, Tailwind CSS 3 |
| Routing | React Router 7 |
| Server state | TanStack React Query 5 |
| HTTP | Axios |
| Charts | Recharts |
| Dates | Day.js |
| Linting | ESLint, typescript-eslint |

## Getting started

Requirements: Node.js 20 or later, and the [backend](../backend/README.md) running on `http://localhost:8080`.

```bash
npm ci
npm run dev
```

Open `http://localhost:5173`. Sign in with a demo account, for example `dilani.wickramasinghe@cadence.dev` (manager) or `amara.silva@cadence.dev` (team member). The password is `Password123!`.

Use `npm install` instead of `npm ci` only when you intend to change `package-lock.json`.

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Base URL of the backend API |

Copy `.env.example` to `.env.local` to override it, and restart Vite after any change. For a deployed frontend, the backend must also list this app's exact origin in its CORS allowlist, otherwise cookie login will not work.

## Scripts

| Command | Description |
| --- | --- |
| `npm run dev` | Start the dev server with hot reload |
| `npm run build` | Type-check (`tsc -b`) and create a production build in `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |

## Project structure

```text
src/
├── api/          One module per backend area (auth, reports, projects, users, dashboard)
├── components/   Shared UI: AppLayout, ProtectedRoute, StatusTag, ColumnFilterDropdown, CadenceMark
├── constants/    Dropdown options for priority, task status and task type
├── context/      AuthContext (current user and sign-in state)
├── hooks/        React Query hooks wrapping the api modules
├── lib/          Configured Axios instance and API helpers
├── pages/        One component per screen
├── theme/        Ant Design theme
├── types/        TypeScript types matching backend DTOs
├── App.tsx       Route table
└── main.tsx      Entry point and providers
```

The `@` alias maps to `src/`, so imports look like `import { useAuth } from "@/context/AuthContext"`.

**How a feature is wired.** A page calls a hook from `hooks/`, the hook calls a function in `api/`, and that function uses the shared Axios instance from `lib/axios.ts`. Types in `types/` describe the data at each step.

## Routes and access

| Path | Page | Access |
| --- | --- | --- |
| `/login`, `/register` | Sign in and sign up | Public |
| `/reports` | Own report history | Signed in |
| `/reports/new` | Create a report | Signed in |
| `/reports/:reportId` | Report detail, versions and review | Signed in |
| `/reports/:reportId/edit` | Edit a draft or a report needing correction | Signed in |
| `/dashboard` | Team dashboard | Manager |
| `/reports/all` | All team reports | Manager |
| `/team-members` | Team members list | Manager |
| `/team-members/:userId` | Member profile and stats | Manager |
| `/projects` | Project management | Manager |

Unknown paths redirect to `/`, which sends each user to their home page by role. The route guard only controls what the UI shows. The backend enforces the real permissions on every request.

## Authentication

- The backend sets an HTTP-only `auth_token` cookie on login. The app never reads or stores the token itself.
- Axios is created with `withCredentials: true` so the cookie is sent with every request.
- For CSRF, Axios reads the `XSRF-TOKEN` cookie and sends it as the `X-XSRF-TOKEN` header on state-changing requests. `withXSRFToken` is enabled because the frontend and API run on different origins.
- `AuthContext` loads the current user from `/api/auth/me` on start and exposes it through `useAuth()`.
- `ProtectedRoute` redirects signed-out users to `/login`. With `allowedRoles={["MANAGER"]}` it sends other users to `/`.

## Data fetching

React Query handles caching and refetching. The hooks follow one pattern.

- Reads use `useQuery`, with the filters and page numbers in the query key, so changing them refetches automatically.
- Writes use `useMutation` and invalidate the related queries, so lists and dashboards refresh after a report is created, submitted, or reviewed.
- Lists are paginated on the server. Filtering and sorting are passed as query parameters, with a debounced input (`useDebouncedValue`) for text search.

## Error handling

The Axios response interceptor converts every failure into an `ApiError` with a message, HTTP status, and, for validation errors, a `fieldErrors` map that forms use to show inline messages.

| Response | Result |
| --- | --- |
| No response | `ApiError` with status 0 |
| 401 with `{ "error": ... }` | `ApiError` with that message |
| 400 with a field-to-message map | `ApiError` with `fieldErrors` |
| 404 or 409 with plain text | `ApiError` with the text as message |

If a request fails with 401 because the CSRF cookie was stale, the interceptor retries it once with the refreshed cookie, so the user does not have to click twice.

## Styling

Ant Design supplies the components and is themed in `src/theme/antdTheme.ts`. Tailwind handles layout and spacing utilities, and global styles are in `src/index.css`. Change colors and typography in the theme file so components stay consistent.

## Building and deploying

```bash
npm run build
```

This outputs static files to `dist/`, which any static host can serve. Before deploying:

1. Set `VITE_API_BASE_URL` to the public API URL at build time. Vite embeds it into the bundle.
2. Make sure the backend allows this site's origin in CORS and runs with `APP_JWT_COOKIE_SECURE=true` over HTTPS.
3. Configure the host to fall back to `index.html` for unknown paths, because routing happens in the browser. Without this, reloading a page like `/reports/123` returns a 404.

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| Network error on every request | Backend is not running, or `VITE_API_BASE_URL` is wrong |
| CORS error in the console | The frontend origin is missing from the backend's allowed origins |
| Signed in, then logged out on the next request | The cookie was not stored. Check that the backend's secure-cookie setting matches HTTP or HTTPS |
| Changes to `.env.local` have no effect | Restart `npm run dev` |
| 404 after reloading a deployed page | Host is missing the fallback to `index.html` |
