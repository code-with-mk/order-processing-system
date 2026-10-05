# Order Processing System

A Spring Boot REST API for creating and tracking e-commerce orders. This take-home implementation uses a locally installed PostgreSQL server; Docker is not required.

## Stack

- Java 21 language target and Spring Boot 4.1.1
- Maven Wrapper (no global Maven install required)
- Spring Web MVC, Spring Data JPA, Bean Validation
- PostgreSQL for local application data
- H2 in-memory database for automated tests

The workspace used to build this project has a JDK 25 installed. The Maven compiler targets Java 21, so a JDK 21 or later can build and run the application.

## Local setup

1. Install a JDK 21 or later and PostgreSQL. pgAdmin is optional but useful for managing the database. In PowerShell, check that the PostgreSQL service is running:

   ```powershell
   Get-Service postgresql*
   ```

   If your service is stopped, start it (the installed service on the development machine is `postgresql-x64-14`; your service name may differ):

   ```powershell
   Start-Service postgresql-x64-14
   ```
2. In pgAdmin, connect to the server and open **Tools → Query Tool**. Create a dedicated application role and database (replace the sample password with your own local password):

   ```sql
   CREATE ROLE order_app LOGIN PASSWORD 'choose-a-local-password';
   CREATE DATABASE order_processing OWNER order_app;
   ```

   Run the statements once, individually, against the server (the second statement creates a separate database). Keep the chosen password private and out of Git.
3. Create a local `.env` file from the example if you do not already have one, then enter the password used for `order_app`:

   ```powershell
   if (-not (Test-Path .env)) { Copy-Item .env.example .env }
   notepad .env
   ```

   Spring Boot reads `.env` automatically. This file is ignored by Git; keep the real password there and out of commits.
4. Start the API in development mode:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   The first run downloads Maven and dependencies. Hibernate creates or updates the local development tables. Keep this terminal open while using the API; stop the app with **Ctrl+C**. The database must be running before the app starts.

   You can also open the project in an IDE and run `OrderProcessingSystemApplication`; make sure the run configuration uses the repository root as its working directory so it can find `.env`.

   Confirm that the API is responding from another PowerShell window:

   ```powershell
   Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/orders"
   ```

### Build and run the packaged JAR

From the repository root, build the executable JAR:

```powershell
.\mvnw.cmd clean package
```

With the PostgreSQL service running, start the packaged app from the repository root so it can find `.env`:

```powershell
java -jar .\target\order-processing-system-0.0.1-SNAPSHOT.jar
```

Stop it with **Ctrl+C**.

PostgreSQL 14 works for this assignment and is supported today, but its community support ends November 12, 2026. For longer-term use, install a newer supported major version and update `ORDER_DB_URL` if its port differs. See the [PostgreSQL version support schedule](https://www.postgresql.org/support/versioning/).

## Automated tests

Run tests without a local PostgreSQL connection:

```powershell
.\mvnw.cmd test
```

The test profile uses an isolated in-memory H2 database. Tests cover order creation with multiple items, input validation, retrieval, status filtering, forward-only status changes, pending-only cancellation, and the pending-order processor.

## API

Base path: `http://localhost:8080/api/v1/orders`

| Method | Path | Behavior |
| --- | --- | --- |
| `POST` | `/api/v1/orders` | Create an order; new orders start as `PENDING`. Returns `201 Created` and a `Location` header. |
| `GET` | `/api/v1/orders/{id}` | Get order details, items, and calculated total. |
| `GET` | `/api/v1/orders?status=PENDING` | List all orders, optionally filtered by status. The list is unpaged for this assignment. |
| `PATCH` | `/api/v1/orders/{id}/status` | Advance `PROCESSING` to `SHIPPED` or `SHIPPED` to `DELIVERED`. |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel an order only while it is `PENDING`. |

Example create request:

```powershell
curl.exe -X POST "http://localhost:8080/api/v1/orders" `
  -H "Content-Type: application/json" `
  --data-raw '{"items":[{"productName":"Keyboard","quantity":2,"unitPrice":14.25},{"productName":"Cable","quantity":1,"unitPrice":3.00}]}'
```

Example status update:

```powershell
curl.exe -X PATCH "http://localhost:8080/api/v1/orders/1/status" `
  -H "Content-Type: application/json" `
  --data-raw '{"status":"SHIPPED"}'
```

An order moves through `PENDING → PROCESSING → SHIPPED → DELIVERED`; `CANCELLED` is terminal. The scheduler advances all orders that are still pending every five minutes. Invalid requests return a consistent JSON error body with a timestamp, HTTP status, message, and request path.

## Design notes

- Controllers handle HTTP concerns; the service owns order operations and state rules.
- DTOs keep the API contract separate from JPA entities.
- Prices use `BigDecimal` and PostgreSQL decimal columns.
- The scheduler performs one conditional bulk update, so it does not load every order item and will only update rows that are still `PENDING` when the update runs.
- Cancellation also uses a conditional `PENDING` update, protecting it from a concurrent scheduler run.
- JPA optimistic locking prevents stale explicit status changes from silently overwriting concurrent changes.

## AI assistance and review notes

ChatGPT was used to refine the design, scaffold the Spring Boot project, and help implement and review the API, persistence, scheduler, and tests. Build feedback caught a package-visibility issue in order construction and an obsolete Jackson configuration property for Spring Boot 4; both were corrected. A package refactor also required import updates, which were checked with a clean build. The API integration suite passed against H2.
