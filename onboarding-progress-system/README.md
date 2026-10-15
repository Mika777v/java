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

- Task list page: http://localhost:8080/ (redirects to http://localhost:8080/tasks)
- API: http://localhost:8080/api/tasks
- PostgreSQL: localhost:5432

### Run on GitHub Codespaces

In Codespaces, the app container cannot reach the `db` container over the
compose network, so use the Codespaces override file:

```bash
cd onboarding-progress-system
export POSTGRES_PASSWORD=<any password you like>
docker compose -f docker-compose.yml -f docker-compose.codespaces.yml up -d --build
```

Then open port 8080 from the **Ports** tab. The task list page is shown.

## Database

The database schema is managed by Flyway. The initial migration creates the `tasks` table.
