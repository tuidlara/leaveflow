# LeaveFlow

REST API for employee leave management, built with Java and Spring Boot.

The project provides a structured way to manage employees, submit leave requests and handle approval workflows with role-based access control.

## Features

- Employee registration and management
- Leave request creation and tracking
- Leave request approval, rejection and cancellation
- Pagination and filtering by leave type and status
- JWT authentication
- Role-based authorization (`ADMIN`, `SUPERVISOR` and `EMPLOYEE`)
- Department-based supervisor access
- Prevention of overlapping approved vacation requests
- Concurrency control for leave request approvals
- Global exception handling
- Automated tests with JUnit 5
- Docker and Docker Compose
- Continuous Integration with GitHub Actions

## Business Rules

The API enforces access rules according to user roles:

- **ADMIN:** can access and manage all leave requests and register employees and users.
- **SUPERVISOR:** can access and manage leave requests from employees in their own department.
- **EMPLOYEE:** can create, view and cancel their own leave requests.

Supervisors and employees cannot approve or reject their own requests.

The system also prevents overlapping vacation periods when approving requests. Concurrency control ensures that simultaneous approval attempts do not bypass this validation.

## Technologies

- Java 17
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- H2
- JUnit 5
- Maven
- Docker
- Docker Compose
- GitHub Actions

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/login` | Authenticate and generate a JWT |
| `POST` | `/employees` | Register an employee and associated user |
| `GET` | `/employees` | List employees (paginated) |
| `GET` | `/employees/{id}` | Get an employee by ID |
| `POST` | `/leave-requests` | Create a leave request |
| `GET` | `/leave-requests` | List leave requests (paginated and filterable) |
| `GET` | `/leave-requests/{id}` | Get a leave request by ID |
| `PATCH` | `/leave-requests/{id}/cancel` | Cancel a leave request |
| `PATCH` | `/leave-requests/{id}/approve` | Approve a leave request |
| `PATCH` | `/leave-requests/{id}/reject` | Reject a leave request |

Leave request listing supports pagination and optional filters by `leaveType` and `status`.

Access to endpoints and resources is restricted according to the authenticated user's role and permissions.

## Authentication

Users authenticate through `POST /auth/login` using their email and password.

Protected endpoints require a JWT token in the request header:

```text
Authorization: Bearer <token>
```

## Running the Project

Make sure Docker and Docker Compose are installed.

Create a `.env` file in the project root:

```env
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password
JWT_SECRET=your-secret-key
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your-admin-password
```

Start the application and PostgreSQL with Docker Compose:

```bash
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080
```

### Initial Administrator

On the first application startup, LeaveFlow automatically creates an administrator account if no administrator already exists in the database.

The initial credentials are provided through the `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables.

The administrator is created only once. If an administrator already exists, the initializer does not create another one.

### Database Persistence

PostgreSQL data is stored in a Docker volume named `leaveflow_data`.

This ensures that database data is preserved when containers are stopped or removed with:

```bash
docker compose down
```

## Tests

Run the automated test suite with:

```bash
./mvnw test
```

## Continuous Integration

GitHub Actions automatically runs the test suite on pushes to `main` and pull requests targeting `main`.

Workflow:

```text
.github/workflows/ci.yml
```

## Author

Arthur de Lara Zilli - [LinkedIn](https://www.linkedin.com/in/arthur-de-lara-zilli-957512420/)
