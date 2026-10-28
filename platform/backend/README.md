# Cadence Backend

Spring Boot REST API for the Cadence platform. Handles authentication, the weekly-report workflow, projects, and dashboard analytics.

## Contents

- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Project structure](#project-structure)
- [Authentication and security](#authentication-and-security)
- [Report workflow](#report-workflow)
- [API reference](#api-reference)
- [Database](#database)
- [Demo data](#demo-data)
- [Testing and building](#testing-and-building)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

## Tech stack

| Concern | Technology |
| --- | --- |
| Language / framework | Java 21, Spring Boot 4.1 (Web MVC, Validation) |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL, Querydsl for filtered queries |
| Migrations | Flyway |
| Security | Spring Security, JWT (jjwt 0.12), BCrypt |
| Utilities | Lombok |
| Build | Maven (wrapper included) |

## Getting started

Requirements: JDK 21 and a running PostgreSQL database. No separate Maven install is needed.

1. Create the database, for example with Docker:

   ```bash
   docker run --name cadence-postgres -e POSTGRES_DB=weekly_report_db \
     -e POSTGRES_PASSWORD=change-me -p 5432:5432 -d postgres:17
   ```

   Or with a local install: `psql -U postgres -c "CREATE DATABASE weekly_report_db;"`

2. Create the local secrets file from the template (it is git-ignored):

   ```bash
   cp src/main/resources/application-secrets.properties.example \
      src/main/resources/application-secrets.properties
   ```

3. Fill in the values (see [Configuration](#configuration)), then start the API:

   ```bash
   ./mvnw spring-boot:run        # use mvnw.cmd on Windows
   ```

The API is available at `http://localhost:8080`. A quick check is `GET /api/health`.

## Configuration

Settings live in `src/main/resources/application.properties` and read their secrets from environment variables or from `application-secrets.properties`.

| Variable | Required | Description |
| --- | --- | --- |
| `DB_URL` | Yes | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/weekly_report_db` |
| `DB_USERNAME` | No | Database user (defaults to `postgres`) |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | Base64 secret used to sign login tokens. Use a long random value |
| `APP_JWT_COOKIE_SECURE` | Yes | `false` for local HTTP, `true` for HTTPS deployments (see below) |

Other fixed settings in `application.properties`:

- Login tokens last 24 hours (`app.jwt.expiration-ms=86400000`).
- `spring.jpa.hibernate.ddl-auto=validate`, so the schema is owned entirely by Flyway.
- `spring.jpa.open-in-view=false`.

Generate a secret with `openssl rand -base64 48`.

## Project structure

```text
src/main/java/com/cadence/
├── controller/    HTTP endpoints and GlobalExceptionHandler
├── service/       Business logic (AuthService, ReportService, ReportAccessService,
│                  ProjectService, DashboardService)
├── repository/    Spring Data repositories, plus Querydsl implementations for
│                  filtered and paginated queries
├── entity/        JPA entities and enums
├── dto/           Request and response objects, grouped by feature
├── security/      SecurityConfig, JWT filter and service, cookie and CSRF handling
└── config/        DataSeeder, Querydsl config, seed-data models
src/main/resources/
├── db/migration/  Flyway migrations
└── seed-data/     Demo users, projects and reports (JSON)
```

Requests flow controller → service → repository. Controllers deal in DTOs only; entities do not leave the service layer.

## Authentication and security

**Login.** `POST /api/auth/login` validates credentials and sets an `auth_token` cookie containing a signed JWT. The cookie is `HttpOnly`, so JavaScript cannot read it. The API is stateless and keeps no server-side session.

**Cookie attributes depend on `APP_JWT_COOKIE_SECURE`.**

| Setting | `Secure` | `SameSite` | Use |
| --- | --- | --- | --- |
| `false` | No | `Lax` | Local development over HTTP |
| `true` | Yes | `None` | Deployed over HTTPS, with frontend and API on different domains |

Browsers reject `SameSite=None` without `Secure`, which is why both are controlled by one flag.

**CSRF.** Spring sets an `XSRF-TOKEN` cookie that the frontend echoes back in the `X-XSRF-TOKEN` header on state-changing requests. Register, login, and logout are exempt.

**CORS.** Allowed origins are an explicit list in `SecurityConfig.corsConfigurationSource()`: `http://localhost:5173` and the deployed frontend URL. Credentials are enabled, so `*` cannot be used. Add any new frontend origin there.

**Authorization.** Public endpoints are `/api/auth/**` and `/api/health`. All other `/api/**` routes require authentication, and everything else is denied. Method-level rules use `@PreAuthorize`:

- `hasRole('MANAGER')` or `hasRole('TEAM_MEMBER')` for role checks.
- `ReportAccessService.isOwner` limits edit and submit to the report's author.
- `ReportAccessService.canView` allows the author or any manager to read a report.

Passwords are stored as BCrypt hashes.

## Report workflow

```text
            submit                    approve
  DRAFT ──────────────► SUBMITTED ──────────────► APPROVED
    ▲                       │
    │   edit + resubmit     │ request changes (comment required)
    └──── NEEDS_CORRECTION ◄┘
```

- A report can be edited or submitted only while it is `DRAFT` or `NEEDS_CORRECTION`.
- Each submission stores a **snapshot version** in `report_versions`, so the full revision history is kept.
- Only `SUBMITTED` reports can be reviewed. A manager either approves or requests changes. Requesting changes requires a comment.
- A manager can edit their comment for 15 minutes after posting it, and only while the report is `NEEDS_CORRECTION`. Edited comments are flagged.
- Invalid state changes are rejected and returned as errors by `GlobalExceptionHandler`. Validation failures come back as a field-to-message map.

## API reference

All routes are under `/api`. "Manager" and "Member" show the role required.

### Auth

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| POST | `/auth/register` | Public | Create an account |
| POST | `/auth/login` | Public | Sign in and set the auth cookie |
| POST | `/auth/logout` | Public | Clear the auth cookie |
| GET | `/auth/me` | Signed in | Current user |

### Reports

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| POST | `/reports` | Member | Create a draft |
| PUT | `/reports/{id}` | Owner | Update a draft or a report needing correction |
| POST | `/reports/{id}/submit` | Owner | Submit and snapshot a version |
| GET | `/reports/mine` | Member | Own report history (filter, sort, paginate) |
| GET | `/reports/{id}` | Owner or manager | Report detail |
| GET | `/reports/{id}/versions` | Owner or manager | Version history |
| GET | `/reports` | Manager | All reports (filter, sort, paginate) |
| POST | `/reports/{id}/review` | Manager | Body `decision`: `APPROVE` or `REQUEST_CHANGES`, plus `comment` |
| PATCH | `/reports/{id}/comment` | Manager | Edit a comment within the edit window |

### Projects

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| GET | `/projects` | Signed in | List projects |
| POST | `/projects` | Manager | Create a project |
| PUT | `/projects/{id}` | Manager | Update a project |
| DELETE | `/projects/{id}` | Manager | Delete a project |

### Users

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| GET | `/users/team-members` | Manager | Team members list (filterable) |
| GET | `/users/{id}` | Manager | Member profile |

### Dashboard (manager only)

| Method | Path | Description |
| --- | --- | --- |
| GET | `/dashboard/summary` | Headline totals |
| GET | `/dashboard/tasks-trend` | Task counts over time |
| GET | `/dashboard/status-by-member` | Report status breakdown per member |
| GET | `/dashboard/workload-by-project` | Workload per project |
| GET | `/dashboard/time-by-task-type` | Hours by task type |
| GET | `/dashboard/recent-activity` | Latest activity feed |
| GET | `/dashboard/team-overview` | Per-member overview |
| GET | `/dashboard/member-stats/{userId}` | Stats for one member |

### Health

`GET /api/health` is public and returns the service status.

## Database

PostgreSQL is the system of record and all IDs are UUIDs. The schema is created by Flyway from `src/main/resources/db/migration/V1__initial_schema.sql` the first time the app connects to an empty database. Do not apply it by hand.

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

| Table | Purpose |
| --- | --- |
| `roles` | `TEAM_MEMBER` and `MANAGER` |
| `users` | Profile, unique email, BCrypt hash, active flag, role |
| `projects` | Name and optional description |
| `weekly_reports` | Reporting week, notes, status, submit and review timestamps, manager comment |
| `report_tasks` | Tasks with priority, status, progress, planned and spent time |
| `time_logs` | Hours by task type |
| `achievements` | Achievements, with a key-achievement flag |
| `blockers` | Blockers, with a key-issue flag |
| `next_week_tasks` | Planned work for next week, with priority |
| `report_notes_links` | Extra notes or links |
| `report_versions` | Submitted snapshots, version number, review comment, current-version flag |

Allowed values:

| Field | Values |
| --- | --- |
| Report status | `DRAFT`, `SUBMITTED`, `NEEDS_CORRECTION`, `APPROVED` |
| Task status | `NOT_STARTED`, `IN_PROGRESS`, `DONE` |
| Priority | `HIGH`, `MEDIUM`, `LOW` |
| Time-log type | `DEVELOPMENT`, `TESTING`, `MEETINGS`, `DOCUMENTATION`, `OTHER` |
| Note type | `NOTE`, `LINK` |

Foreign keys have no cascade deletes, so related rows must be removed in order during manual cleanup.

**Changing the schema.** Add a new `V2__description.sql` file. Never edit an applied migration, because Flyway checks its checksum.

## Demo data

On startup `DataSeeder` runs and:

- Creates the two roles if missing.
- Adds users and projects from `seed-data/*.json` if they do not exist.
- Adds the demo reports only when the database has no reports at all.

Report dates are relative to the Monday the seeder runs on, so the data always looks current. All seeded accounts use the password `Password123!`. Seeded managers include `dilani.wickramasinghe@cadence.dev`, `buddhika.kularatne@cadence.dev`, and `imesha.ekanayake@cadence.dev`. Team members include `amara.silva@cadence.dev`.

Never deploy these accounts to a shared or production environment.

## Testing and building

```bash
./mvnw test             # run tests
./mvnw package          # build target/*.jar
./mvnw package -DskipTests
```

Tests live in `src/test/java/com/cadence`. They include `ReportAccessServiceTest` for ownership and view rules, and `ApplicationTests`, a context-load test that needs a reachable database.

## Deployment

A multi-stage `Dockerfile` is included. It builds with Maven on JDK 21 and runs the jar on a JRE 21 image, exposing port 8080.

```bash
docker build -t cadence-backend .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/weekly_report_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=... \
  -e JWT_SECRET=... \
  -e APP_JWT_COOKIE_SECURE=true \
  cadence-backend
```

Production checklist:

- Use a unique, random `JWT_SECRET` and strong database credentials.
- Set `APP_JWT_COOKIE_SECURE=true` and serve over HTTPS.
- Add the frontend's exact origin to the CORS list in `SecurityConfig`.
- Do not load the demo users into a real environment.
- Let Flyway apply migrations on startup, or run them in your deployment pipeline.

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| Fails at startup with a placeholder error for `DB_URL` or `JWT_SECRET` | `application-secrets.properties` is missing or incomplete |
| Schema validation error | The database has tables from outside Flyway. Use an empty database |
| Login works but the next request is 401 | Cookie not stored. Check `APP_JWT_COOKIE_SECURE` matches HTTP or HTTPS, and that the origin is in the CORS list |
| 403 on POST, PUT, PATCH, DELETE | Missing CSRF header. The frontend sends `X-XSRF-TOKEN` automatically |
| Browser reports a CORS error | Frontend origin is not in the allowed list |
