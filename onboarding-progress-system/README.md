# onboarding-progress-system

Onboarding progress management system for new employees and mid-career hires.

## Overview

This project is a learning-focused Spring Boot application for managing onboarding progress.

## Stack

- Java 21
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Thymeleaf
- Flyway
- Docker Compose

## Features (MVP)

- Manage onboarding tasks
- Assign tasks to users
- Track progress and status
- View task completion status

## Local Development

### Prerequisites

- Java 21
- Docker
- Docker Compose

### Run with Docker

```bash
export POSTGRES_PASSWORD=<any password you like>
docker compose up --build
```

Then access:

- Dashboard (top page): http://localhost:8080/
- Task list page (reorder with ↑ / ↓): http://localhost:8080/tasks
- User list and progress page: http://localhost:8080/users
- API: http://localhost:8080/api/tasks
- PostgreSQL: localhost:5432

### Run on GitHub Codespaces

The repository has a dev container configuration (`.devcontainer/`), so in a
codespace **the DB and the app start automatically**:

1. Create a codespace (Code → Codespaces → Create codespace on master).
   For an existing codespace, run "Codespaces: Rebuild Container" once from the command palette.
2. Wait 2-3 minutes for the first build. Port 8080 is forwarded automatically.
3. Open port 8080 from the **Ports** tab. The dashboard is shown.

What happens on start (`.devcontainer/start-app.sh`):

- A random DB password is generated once and saved to `onboarding-progress-system/.env`
  (ignored by git). Docker Compose reads it automatically.
- `docker compose -f docker-compose.yml -f docker-compose.codespaces.yml up -d --build` is run.
  In Codespaces the app container cannot reach the `db` container over the compose network,
  so the override file runs the app on the host network and connects via `localhost:5432`.

Useful commands (in `onboarding-progress-system`):

```bash
bash ../.devcontainer/start-app.sh                                        # start again
docker compose logs -f app                                                # app logs
docker compose -f docker-compose.yml -f docker-compose.codespaces.yml down      # stop
docker compose -f docker-compose.yml -f docker-compose.codespaces.yml down -v   # stop and delete DB data
```

## Tests

```bash
cd onboarding-progress-system
mvn test
```

- Docker is required: the tests start a temporary PostgreSQL 15 container with
  [Testcontainers](https://testcontainers.com/), and Flyway creates the tables and sample data.
- Each test runs in a transaction that is rolled back, so tests do not affect each other.
- The tests also run on GitHub Actions on every push (`test` job).

| Test class | What it checks |
|---|---|
| `controller/TaskControllerTest` | REST API `/api/tasks` (list, get, create, update, delete, 400/404) |
| `controller/TaskPageControllerTest` | Screens (`/`, list, create/edit/delete forms, validation messages, 404) |
| `controller/UserPageControllerTest` | User screens (list with progress, create/edit/delete, validation, duplicate email, 404) |
| `controller/UserAssignmentPageTest` | Assigning tasks, changing progress, unassigning, cascade delete, 400/404 |
| `controller/TaskOrderTest` | Task display order (↑ / ↓, new tasks at the end, order of assignments) |
| `controller/DashboardTest` | Dashboard numbers, follow-up users, tasks due within 7 days |
| `form/TaskFormTest` | `TaskForm` conversion (trimming the title, blank description to null) |
| `entity/TaskAssignmentTest` | Progress rules (completion time, overdue check) |
| `service/UserProgressTest` | Completion rate calculation |

## Database

The database schema is managed by Flyway (`src/main/resources/db/migration`).

| Table | Contents |
|---|---|
| `tasks` | Onboarding tasks shared by everyone (the template list) |
| `users` | New employees / mid-career hires |
| `task_assignments` | Which task is assigned to which user, its status (未着手 / 対応中 / 完了), due date and completion time |

Sample data (5 tasks, 3 users and their assignments) is inserted on the first start.
Deleting a user or a task also deletes its assignments.
