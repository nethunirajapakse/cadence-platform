# Cadence Frontend

React single-page app for Cadence. Team members write and submit weekly reports, managers review them and view team analytics, and administrators manage people.

**Stack:** React 19 · TypeScript · Vite · Ant Design · Tailwind CSS · React Router · TanStack Query · Axios · Recharts

## Run

```bash
npm ci
npm run dev        # http://localhost:5173, expects the backend on :8080
```

Set `VITE_API_BASE_URL` in `.env.local` (see `.env.example`) to point at another API. Other scripts are `npm run build` (type-check and production build), `npm run lint` and `npm run preview`.

## Structure

```text
src/
├── api/          One module per backend area
├── hooks/        React Query hooks wrapping the api modules
├── pages/        One component per screen
├── components/   Shared UI (layout, route guard, status tags, filters)
├── context/      AuthContext, the current user and sign-in state
├── lib/          Configured Axios instance
└── types/        TypeScript types matching the backend DTOs
```

A page calls a hook, the hook calls an `api/` function, and that function uses the shared Axios instance.

## Screens by role

| Role | Screens |
| --- | --- |
| Team member | Report history, create and edit a report, report detail |
| Manager | Team dashboard, all team reports and review, team members and profiles, projects |
| Administrator | Users, invitations, audit log |
| Everyone | Login, accept invitation, forgot and reset password |

`ProtectedRoute` sends signed-out users to the login page and redirects anyone outside a route's allowed roles to their own home page.

## How it works

- **Auth.** The backend sets an HTTP-only cookie, so the app never handles the token. Axios sends credentials and the CSRF header, and retries once if the CSRF cookie was stale.
- **Data.** Reads use `useQuery` with filters and page numbers in the query key. Writes use `useMutation` and invalidate related queries so lists and dashboards refresh. Lists are paginated, filtered and sorted on the server, with debounced text search.
- **Errors.** An Axios interceptor turns every failure into an `ApiError`, and validation errors map to inline form messages.
- **Styling.** Ant Design components themed in `src/theme/antdTheme.ts`, with Tailwind for layout.

## Deploy

`npm run build` outputs static files to `dist/`. Set `VITE_API_BASE_URL` at build time, make sure the backend allows this origin in CORS and uses `APP_JWT_COOKIE_SECURE=true`, and configure the host to fall back to `index.html` so reloading a deep link does not return a 404.
