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
export APP_ADMIN_PASSWORD=<password of the first administrator>
docker compose up --build
```

Then open http://localhost:8080/ and log in as `admin@example.com` with `APP_ADMIN_PASSWORD`
(see [Login](#login)).

- Dashboard (top page, administrators): http://localhost:8080/
- Task list page (reorder with ↑ / ↓): http://localhost:8080/tasks
- User list and progress page: http://localhost:8080/users
- My page (new employees, opened with their personal link): http://localhost:8080/me
- API (administrators, HTTP Basic): `curl -u admin@example.com:<password> http://localhost:8080/api/tasks`
- Health check (no login): http://localhost:8080/actuator/health → `{"status":"UP"}`
  (`DOWN` with HTTP 503 when, for example, the database cannot be reached)
- PostgreSQL: localhost:5432 (published on localhost only, not to the network)
- To print the SQL the app runs (for learning or debugging), start with `SPRING_JPA_SHOW_SQL=true`

### Run on GitHub Codespaces

The repository has a dev container configuration (`.devcontainer/`), so in a
codespace **the DB and the app start automatically**:

1. Create a codespace (Code → Codespaces → Create codespace on master).
   For an existing codespace, run "Codespaces: Rebuild Container" once from the command palette.
2. Wait for the first build (a few minutes: the codespace uses a Java 21 image with Maven and
   Docker added, not the large "universal" image). When the app is ready, port 8080 is
   forwarded and the dashboard opens in the browser automatically.
3. If the browser does not open, open port 8080 from the **Ports** tab
   (the globe icon). If you see 401 or 502, wait a moment and open it again from the Ports tab.

What happens on start (`.devcontainer/start-app.sh`):

- A random DB password and a random administrator password are generated once and saved to
  `onboarding-progress-system/.env` (ignored by git). Docker Compose reads it automatically.
  The administrator login is printed at the end (`cat onboarding-progress-system/.env` shows it again).
- `docker compose -f docker-compose.yml -f docker-compose.codespaces.yml up -d --build` is run,
  then the script waits until the app answers (up to 5 minutes) and prints "Ready".
- `APP_BASE_URL` is set to the codespace address, so personal links point to the codespace.

Port 8080 of a codespace is **private** by default: only you (logged in to GitHub) can open it.
To let new employees open their personal links, change the port visibility to **Public**
in the Ports tab (anyone with the address can then reach the login page; administrator pages
still need the password and personal links cannot be guessed).
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
| `controller/SecurityTest` | Login, logout, who can open which page, my page, passwords and roles |
| `controller/FirstLoginPasswordChangeTest` | Password set by an administrator must be changed first (redirects, API 403, unlock after change) |
| `controller/PasswordResetTest` | Temporary password: random, shown once, must be changed; not for yourself or for new employees with links |
| `security/AdminPasswordRecoveryTest` | Startup recovery with `APP_ADMIN_PASSWORD_RESET` (administrators only) |
| `web/RelativeRedirectTest` | Redirects contain only the path (needed behind proxies such as Codespaces) |
| `web/HealthCheckTest` | Health check is open and shows only UP / DOWN; other actuator endpoints are not published |
| `controller/AccessLinkTest` | Personal links: issuing (shown once, hash only), logging in, expiry, reissue, revoke, no password login for new employees |
| `controller/AccessLinkBaseUrlTest` | Personal links use `app.base-url` when it is set |
| `controller/PasswordChangeTest` | Changing one's own password (`/me/password`): checks, login with the new password |
| `form/TaskFormTest` | `TaskForm` conversion (trimming the title, blank description to null) |
| `entity/TaskAssignmentTest` | Progress rules (completion time, overdue check) |
| `service/UserProgressTest` | Completion rate calculation |

## Login

| Role | Can do | Home |
|---|---|---|
| 管理者 (`ADMIN`) | Everything: dashboard, tasks, users, assignments, REST API | `/` (dashboard) |
| 新入社員 (`MEMBER`) | See their own tasks and change their status | `/me` (my page) |

- **Administrators** log in on `/login` with the email address and password. Passwords are stored as BCrypt hashes.
- **First administrator**: created on startup when no administrator exists, from
  `APP_ADMIN_EMAIL` (default `admin@example.com`) and `APP_ADMIN_PASSWORD`. Nothing is created
  without `APP_ADMIN_PASSWORD`, and an existing administrator is never overwritten.
- **New employees** log in in one of two ways, chosen with `APP_MEMBER_LOGIN`:
  - `link` (default): no password. An administrator issues a **personal link** on the user
    detail page (専用URL → 発行) and gives it to the employee. Opening it logs them in and shows `/me`.
    The link is shown only once (only a hash is stored), expires after `APP_ACCESS_LINK_VALID_DAYS`
    days (default 60), and can be reissued (the old one stops working) or revoked at any time.
    A session that is already logged in stays until it times out or the employee logs out.
  - `password`: an administrator sets the initial password on the user registration / edit
    screen (8 characters or more), and the employee logs in on `/login`.
  Users without a password (or link) cannot log in; the sample users (山田, 佐藤, 鈴木) have neither at first.
- **First login**: a password that an administrator set for someone else must be changed by that
  person first: until then every page leads to `/me/password` (the API answers 403).
  Not required for a password you set for yourself or for the first administrator from `APP_ADMIN_PASSWORD`.
- **Forgotten password**:
  - Another administrator opens the user's detail page and presses "パスワードをリセット".
    A random temporary password (12 characters) is shown once; the user must change it after logging in.
  - If the only administrator forgot it: start once with `APP_ADMIN_PASSWORD_RESET=true`. The password of
    `APP_ADMIN_EMAIL` is set back to `APP_ADMIN_PASSWORD` (to be changed after logging in). Remove the setting
    afterwards; otherwise it is set back on every start. In a codespace, add the line to
    `onboarding-progress-system/.env` and run `bash .devcontainer/start-app.sh`.
  - New employees with personal links just get a new link.
- **Own password**: users who log in with a password can change it on `/me/password`
  (link "パスワード変更" in the menu). The current password is required.
- `APP_BASE_URL` (e.g. `https://onboarding.example.com`) sets the address shown in personal links;
  when empty, the address the administrator's browser used is shown.
- An administrator cannot remove their own administrator role or delete themselves.
- Forms are protected against CSRF. The REST API uses HTTP Basic and is for administrators only.

## Database

The database schema is managed by Flyway (`src/main/resources/db/migration`).

| Table | Contents |
|---|---|
| `tasks` | Onboarding tasks shared by everyone (the template list) |
| `users` | New employees / mid-career hires and administrators (role, password hash, personal link hash and expiry) |
| `task_assignments` | Which task is assigned to which user, its status (未着手 / 対応中 / 完了), due date and completion time |

Sample data (6 tasks, 3 users and their assignments) is inserted on the first start.
Deleting a user or a task also deletes its assignments.
E-mail addresses are unique regardless of case (`Taro@example.com` and `taro@example.com` are the same).

If the app is pointed by mistake at a database that has other tables but no Flyway history,
it stops with an error instead of changing that database (`baseline-on-migrate` is off).
