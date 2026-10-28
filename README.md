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

On first start, Flyway creates the schema and demo data is loaded. Sign in with `dilani.wickramasinghe@cadence.dev` (manager) or `amara.silva@cadence.dev` (team member). Both use the password `Password123!`. These are demo accounts only.

## Roles

| Role | Can do |
| --- | --- |
| Team member | Create, edit, and submit their own reports; view their history |
| Manager | Review all reports, manage projects, view team dashboard and member profiles |

## Documentation

- [Backend guide](platform/backend/README.md): configuration, security, API, database, testing, deployment
- [Frontend guide](platform/frontend/README.md): configuration, structure, routes, data fetching, build

## License

See [LICENSE](LICENSE).
