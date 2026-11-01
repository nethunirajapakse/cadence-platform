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

On first start, Flyway creates the schema and demo data is loaded. Sign in with `admin@cadence.dev` (administrator), `dilani.wickramasinghe@cadence.dev` (manager) or `amara.silva@cadence.dev` (team member, reports to Dilani). All use the password `Password123!`. These are demo accounts only.

There is no public sign-up. New people join through an invitation from an administrator. In development no email is sent: the invitation link is printed in the backend log (see the [backend guide](platform/backend/README.md#email)).

## Roles

| Role | Can do |
| --- | --- |
| Team member | Create, edit, and submit their own reports; view their history. Reports to one manager |
| Manager | Review reports from their own direct reports, manage projects, view the dashboard and profiles for their own team |
| Administrator | Invite people, set roles and managers, deactivate accounts, read the audit log. Cannot read report content |

## Documentation

- [Backend guide](platform/backend/README.md): configuration, security, API, database, testing, deployment
- [Frontend guide](platform/frontend/README.md): configuration, structure, routes, data fetching, build

## License

See [LICENSE](LICENSE).
