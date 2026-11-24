# Task Manager API

A Spring Boot REST API for managing tasks, users, and categories. It includes JWT login, role-based access, task ownership, an audit history, pagination/search, and Swagger UI.

## Requirements

- Java 25 or newer (the project compiles with Java 25; Java 27 was used for local testing)
- Git, if you are cloning the repository
- Internet access the first time you build, so Maven can download dependencies

The Maven wrapper is included, so a separate Maven installation is not needed.

## Run locally

1. Open a terminal in the folder containing this README.
2. Check Java is available:

   ```powershell
   java -version
   ```

3. Start the API:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   If `java` is not Java 25 or newer, install a supported JDK and set `JAVA_HOME` to its installation folder before starting the command.

4. Open Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

The default database is an in-memory H2 database. Its data is erased when the app stops.

## Run tests

```powershell
.\mvnw.cmd test
```

Tests use the project's Java 25 target. If Maven reports `release version 25 not supported`, select Java 25 or newer in `JAVA_HOME`.

## Authentication and roles

### Register a regular user

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "student1",
  "password": "password123"
}
```

Public registration always creates a `USER`. Passwords are stored as BCrypt hashes and are not included in user API responses.

### Log in

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "student1",
  "password": "password123"
}
```

The response contains a `jwt` token. For protected requests, send it in this header:

```http
Authorization: Bearer <jwt>
```

In Swagger UI, choose **Authorize**, paste the token, and confirm. To switch accounts, clear the current authorization and log in as the other account.

### Roles

- `USER`: can create tasks and manage only their own tasks.
- `ADMIN`: can manage any user's tasks and perform user and category management.
- User and category deletion is rejected if tasks still reference the record.

Registration cannot create an administrator. For local development only, you can promote a registered account using the H2 Console:

1. Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console).
2. Connect with JDBC URL `jdbc:h2:mem:taskdb`, username `kr`, and password `password`.
3. Run this query, replacing the username:

   ```sql
   UPDATE "USER DETAILS" SET ROLE = 'ADMIN' WHERE USERNAME = 'student1';
   ```

4. Log in again as that account and use the new token.

The H2 Console is enabled and publicly accessible in this development configuration. **Do not deploy this configuration publicly.** Use a secured database, a strong external `JWT_SECRET`, and production-specific security settings before deployment. The local database and promoted role are lost when the app stops.

## API guide

All endpoints use `http://localhost:8080` as the base URL. Protected endpoints need a Bearer token.

### Tasks — `/api/v1/tasks`

Available to authenticated `USER` and `ADMIN` accounts; regular users are limited to their own tasks.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/tasks` | List visible tasks with pagination and sorting |
| `GET` | `/api/v1/tasks/{id}` | Get a task by ID, if visible to the caller |
| `POST` | `/api/v1/tasks` | Create a task owned by the logged-in user |
| `PUT` | `/api/v1/tasks/{id}` | Update an accessible task |
| `DELETE` | `/api/v1/tasks/{id}` | Delete an accessible task |
| `GET` | `/api/v1/tasks/search` | Search by optional title and/or status, with pagination |
| `GET` | `/api/v1/tasks/search-title?title=report` | Search visible tasks by title |
| `GET` | `/api/v1/tasks/priority/{priority}` | Find a visible task by priority |
| `GET` | `/api/v1/tasks/completed` | List visible completed tasks |
| `GET` | `/api/v1/tasks/{id}/history` | Read a task's audit history |

Example task request:

```json
{
  "title": "Prepare demo",
  "description": "Review the API before the presentation",
  "priority": "HIGH",
  "deadline": "2026-12-01",
  "status": "TODO",
  "categoryId": null
}
```

Valid status values are `TODO`, `In_Progress`, and `COMPLETED`. The current request validates the title and description; priority and deadline are database-required fields.

Pagination and sort options for the task list/search endpoints:

```text
GET /api/v1/tasks?page=0&size=10&sortBy=id&sortDir=DESC
GET /api/v1/tasks/search?title=demo&status=TODO&page=0&size=10&sortBy=title&sortDir=ASC
```

The response includes the task results plus `currentPage`, `totalItems`, `totalPages`, `hasNext`, and `hasPrevious`.

### Categories — `/api/v1/categories`

Admin only.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/categories` | List categories |
| `GET` | `/api/v1/categories/{id}` | Get one category |
| `POST` | `/api/v1/categories` | Create a category |
| `PUT` | `/api/v1/categories/{id}` | Update a category |
| `DELETE` | `/api/v1/categories/{id}` | Delete an unused category |

Example request:

```json
{
  "name": "School",
  "description": "Course and assignment tasks"
}
```

### Users — `/api/v1/users`

Admin only. Public registration is `POST /api/auth/register`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/users` | List users |
| `GET` | `/api/v1/users/{id}` | Get one user |
| `PUT` | `/api/v1/users/{id}` | Update username and role; password is optional |
| `DELETE` | `/api/v1/users/{id}` | Delete a user who owns no tasks |

User responses include the ID, username, and role; they never include the password hash.

### Audit history

The API records a snapshot when a task is created, updated, or deleted. Each entry contains the actor, action, timestamp, and task fields at that moment. History remains available after the task is deleted. The owner and admins can read the history.

```http
GET /api/v1/tasks/{id}/history
Authorization: Bearer <jwt>
```

## Swagger and OpenAPI

- Interactive API documentation: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Swagger UI is generated from the Spring controllers at runtime; it is not a separate HTML file in the source tree.

## Project structure

Java source is under `src/main/java/com/kraiven/taskmanager`:

| Folder/file | Purpose |
|---|---|
| `TaskmanagerApplication.java` | Spring Boot entry point |
| `controller/` | HTTP routes and request/response handling |
| `Service/` | Business logic, ownership checks, and task audit recording |
| `entity/` | JPA database models: `User`, `Task`, `Category`, roles, and audit entries |
| `repository/` | Spring Data database operations and searches |
| `dto/` | Validated request objects and safe response shapes |
| `security/` | BCrypt setup, JWT creation/validation, and endpoint authorization |
| `config/OpenApiConfig.java` | Swagger API metadata and JWT authorization scheme |
| `exception/` | Application exceptions and centralized HTTP error responses |
| `src/main/resources/application.properties` | Local server, database, JWT, and Swagger settings |
| `src/test/java/` | Integration tests for startup, login, roles, CRUD, audit history, and Swagger |

Typical request flow: **controller → service → repository → database**. The service enforces business rules; DTOs shape API data so database entities do not need to be exposed directly.

## Configuration and deployment notes

- Set `JWT_SECRET` to a strong private value before deployment; never publish a production signing key.
- The default H2 database is in-memory and intended for local demos. Configure a persistent database for real deployments.
- The H2 Console and its development credentials are not production-safe.
- This project currently uses `spring.jpa.hibernate.ddl-auto=update`; use reviewed database migrations for production.
- Refresh tokens are not implemented; the rubric described them as recommended, not mandatory.
