# Cadence Platform

Cadence is a weekly-report management platform for team members and managers. Team members can create and submit weekly reports; managers can review submissions, manage projects, and view team analytics.

The application consists of a React single-page application, a Spring Boot REST API, and a PostgreSQL database.

## Technology

| Area | Technology |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, Ant Design, React Query |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway |
| Database | PostgreSQL |
| Authentication | JWT stored in an HTTP-only cookie, with CSRF protection |

## Repository layout

```text
.
├── README.md
├── architecture-diagram.md       # Architecture and request-flow diagram
└── platform/
    ├── frontend/                 # React/Vite client
    │   ├── src/api/              # API-specific clients
    │   ├── src/pages/            # Application screens
    │   ├── src/components/       # Reusable UI
    │   └── src/context/          # Authentication state
    └── backend/                  # Spring Boot API
        ├── src/main/java/com/cadence/
        │   ├── controller/       # HTTP endpoints
        │   ├── service/          # Business logic
        │   ├── repository/       # JPA and custom queries
        │   ├── entity/           # Persistence model
        │   └── security/         # JWT, cookies, CSRF, and CORS
        └── src/main/resources/
            ├── db/migration/     # Flyway database migrations
            └── seed-data/        # Development/demo data
```

## Prerequisites

- Node.js 20 or later and npm
- JDK 21
- PostgreSQL (a current supported release; PostgreSQL 17 was used to produce the included schema dump)
- Optional: Docker, if you prefer to run PostgreSQL in a container

Check the installed tools:

```powershell
node --version
npm --version
java --version
psql --version
```

## Quick start

From the repository root, complete these steps in order:

1. Start PostgreSQL and create the `weekly_report_db` database.
2. Configure the backend's local secrets file with that database connection and a JWT secret.
3. Start the backend. Flyway creates the schema and the application inserts demo data where missing.
4. Start the frontend and open `http://localhost:5173`.

The detailed instructions below use PowerShell. Equivalent commands work in another shell.

## 1. Install dependencies

### Frontend

```powershell
Set-Location platform/frontend
npm ci
```

Use `npm install` instead of `npm ci` only when intentionally changing the dependency lockfile.

### Backend

No separate package installation is required. The Maven Wrapper downloads the Maven distribution and Java dependencies the first time it runs. From `platform/backend`, use `./mvnw.cmd` on Windows or `./mvnw` on macOS/Linux.

## 2. Run the database

### Option A: local PostgreSQL installation

Start your PostgreSQL service, then create the development database once:

```powershell
psql -U postgres -d postgres -c "CREATE DATABASE weekly_report_db;"
```

If the database already exists, PostgreSQL will report that fact; continue with the backend setup.

### Option B: PostgreSQL with Docker

```powershell
docker run --name cadence-postgres `
  -e POSTGRES_DB=weekly_report_db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=change-me `
  -p 5432:5432 `
  -d postgres:17
```

Later, restart that container with:

```powershell
docker start cadence-postgres
```

Do not apply `platform/backend/schema.sql` during a normal local setup. The backend uses Flyway and automatically applies `src/main/resources/db/migration/V1__initial_schema.sql` to an empty database. Applying both would create conflicting schema setup paths. The SQL dump is useful for inspection or a controlled restore only.

## 3. Configure and run the backend

Change to the backend directory and create the ignored local secrets file from the template:

```powershell
Set-Location platform/backend
Copy-Item src/main/resources/application-secrets.properties.example src/main/resources/application-secrets.properties
```

Set the following values in `src/main/resources/application-secrets.properties` for your local database:

```properties
DB_URL=jdbc:postgresql://localhost:5432/weekly_report_db
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password
JWT_SECRET=your_long_random_base64_secret
```

`application-secrets.properties` is intentionally ignored by Git. Keep real database credentials and JWT secrets out of source control. Generate an appropriate random secret for every non-development environment.

Start the API:

```powershell
./mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8080` by default. On its first connection to an empty database, Flyway creates the schema; the startup data seeder then creates the `TEAM_MEMBER` and `MANAGER` roles and loads the JSON data in `src/main/resources/seed-data/` only when those records are absent.

Useful backend commands:

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

### Backend configuration notes

- Database settings are read from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- `JWT_SECRET` is required to sign and validate login tokens.
- The frontend development origin `http://localhost:5173` is permitted by the backend CORS configuration and requests include cookies.
- If the frontend is hosted elsewhere, use `VITE_API_BASE_URL` for its API address and add its exact origin to the backend CORS allowlist before using cookie-based authentication.

## 4. Run the frontend

Open a second terminal:

```powershell
Set-Location platform/frontend
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
