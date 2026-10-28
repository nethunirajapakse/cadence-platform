# Cadence Platform

A weekly-report platform for teams. Team members write and submit weekly reports; managers review them, manage projects, and track team analytics.

**Stack:** React 19 + TypeScript (Vite, Ant Design) · Spring Boot (Java 21) · PostgreSQL · JWT in an HTTP-only cookie with CSRF protection

## Structure

```text
platform/
├── frontend/   React single-page app      → see platform/frontend/README.md
└── backend/    Spring Boot REST API       → see platform/backend/README.md
```

## Quick start

**Prerequisites:** Node.js 20+, JDK 21, PostgreSQL (or Docker)

**1. Database**

```bash
docker run --name cadence-postgres -e POSTGRES_DB=weekly_report_db \
  -e POSTGRES_PASSWORD=change-me -p 5432:5432 -d postgres:17
```

**2. Backend** (runs on `http://localhost:8080`)

```bash
cd platform/backend
cp src/main/resources/application-secrets.properties.example \
   src/main/resources/application-secrets.properties
# edit it: DB_URL, DB_PASSWORD, JWT_SECRET
./mvnw spring-boot:run
```

**3. Frontend** (runs on `http://localhost:5173`)

```bash
cd platform/frontend
npm ci
npm run dev
```

Vite serves the application at `http://localhost:5173` by default. It calls `http://localhost:8080` unless `VITE_API_BASE_URL` is set.

To point the frontend at a different backend, create `platform/frontend/.env.local`:

```properties
VITE_API_BASE_URL=https://api.example.com
```

Restart Vite after changing environment files.

Useful frontend commands:

```powershell
npm run lint
npm run build
npm run preview
```

## Database structure

PostgreSQL is the system of record. All identifiers are UUIDs. The canonical schema migration is `platform/backend/src/main/resources/db/migration/V1__initial_schema.sql`.

```text
roles 1 ─── * users 1 ─── * weekly_reports * ─── 1 projects
                              │
                              ├── * report_tasks
                              ├── * time_logs
                              ├── * achievements
                              ├── * blockers
                              ├── * next_week_tasks
                              ├── * report_notes_links
                              └── * report_versions
```

| Table | Purpose | Key relationships |
| --- | --- | --- |
| `roles` | Defines `TEAM_MEMBER` and `MANAGER` authorization roles. | One role has many users. |
| `users` | User profile, unique email, bcrypt password hash, active state. | Belongs to `roles`; owns weekly reports. |
| `projects` | Project name and optional description. | Referenced by weekly reports. |
| `weekly_reports` | Main report record: reporting week, notes, lifecycle status, submission/review timestamps, and manager comment. | Belongs to one user and one project. |
| `report_tasks` | Work tasks, priority, status, planned/actual progress, and planned/spent time. | Belongs to a weekly report. |
| `time_logs` | Hours recorded by task category. | Belongs to a weekly report. |
| `achievements` | Achievement descriptions and a key-achievement flag. | Belongs to a weekly report. |
| `blockers` | Blocker descriptions and a key-issue flag. | Belongs to a weekly report. |
| `next_week_tasks` | Planned work for the following week with priority. | Belongs to a weekly report. |
| `report_notes_links` | Supplemental text notes or links. | Belongs to a weekly report. |
| `report_versions` | Submitted report snapshots, version numbers, review comments, and current-version state. | Belongs to a weekly report. |

The database constrains these values:

- Report status: `DRAFT`, `SUBMITTED`, `NEEDS_CORRECTION`, or `APPROVED`
- Task status: `NOT_STARTED`, `IN_PROGRESS`, or `DONE`
- Priority: `HIGH`, `MEDIUM`, or `LOW`
- Time-log task type: `DEVELOPMENT`, `TESTING`, `MEETINGS`, `DOCUMENTATION`, or `OTHER`
- Note/link type: `NOTE` or `LINK`

Foreign keys protect report-owned records by referencing `weekly_reports`. The current migration does not specify cascade deletion, so retain related records when planning any manual database maintenance.

## API overview

All API endpoints are under `/api` and protected endpoints require the JWT cookie established by login.

| Area | Base path | Responsibilities |
| --- | --- | --- |
| Authentication | `/api/auth` | Register, login, logout, current user |
| Reports | `/api/reports` | Create, update, submit, review, version history, and comments |
| Projects | `/api/projects` | List and manager-controlled project management |
| Users | `/api/users` | Team-member list and member profile |
| Dashboard | `/api/dashboard` | Team and member summaries, trends, workload, and activity |

## Demo data

On a fresh database, startup seeding reads the JSON fixtures in `platform/backend/src/main/resources/seed-data/`.

- Roles and users are added only if they do not already exist.
- Projects are added only when the project name is missing.
- Reports are seeded only when the database contains no reports at all.
- Fixture credentials are development/demo data only; do not deploy them or reuse their passwords in a shared environment.

For a clean demo dataset, create a new empty database (or deliberately reset a development-only database), then start the backend again. Never use a destructive database reset against a shared or production database.

## Production considerations

- Set a strong, unique `JWT_SECRET`, secure database credentials, and HTTPS-only cookie settings.
- Use a production PostgreSQL instance and apply Flyway migrations through your deployment process.
- Set `VITE_API_BASE_URL` to the public API URL.
- Update the backend CORS allowlist with the exact frontend origin. Cookie authentication requires an explicit origin and credentials-enabled CORS.
- Build the frontend with `npm run build` and run/package the backend with Maven according to your hosting environment.

For the component and request-flow diagram, see [architecture-diagram.md](architecture-diagram.md).
