# Order Processing System Design

**Date:** 2026-10-05
**Status:** Approved in chat

## Goal

Build a submission-ready Java backend for the take-home order processing assignment. Customers can create orders with multiple items, retrieve one order, list orders with an optional status filter, update an order's status, and cancel an order while it is pending. Pending orders are moved to processing by a background job every five minutes. The project should be easy to run locally and straightforward to explain in a coding walkthrough and design-pattern round.

## Selected approach

Use Java 21, Spring Boot, Maven, Spring Web, Spring Data JPA, Bean Validation, and PostgreSQL. PostgreSQL runs as a locally installed Windows service and is managed through pgAdmin. Application database settings are supplied through environment variables; do not require Docker or commit credentials. Document how to create a dedicated database and login role in pgAdmin.

Use the familiar package-by-layer structure, with a small number of supporting packages:

- **`controller`:** HTTP routes and status codes.
- **`service`:** transaction boundaries, business rules, and order operations.
- **`dto`:** request and response models; never expose JPA entities.
- **`dao`:** Spring Data JPA repositories and efficient bulk processing of due orders.
- **`entity`:** `OrderEntity`, `OrderItemEntity`, and `OrderStatus` persisted by JPA.
- **`exception`:** domain and application errors mapped to HTTP responses.
- **`scheduler`:** a five-minute scheduled task that delegates to the service.

Controllers must not own business rules. Use DTOs at the API boundary rather than exposing JPA entities directly.

## Order behavior and data

An order has a generated ID, creation timestamp, status, and one or more line items. Each line item records a product identifier/name, positive quantity, and non-negative unit price. Store monetary values as decimal values (`BigDecimal`), never floating point. The database stores order and item records with a parent-child relationship; deleting/cancelling an order does not erase its history.

Statuses are `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, and `CANCELLED`. New orders begin as `PENDING`. Cancellation is permitted only from `PENDING` and changes status to `CANCELLED`. Explicit status updates allow only forward transitions `PROCESSING -> SHIPPED -> DELIVERED`; the scheduled transition is `PENDING -> PROCESSING`. Reject unsupported transitions with a client error. `CANCELLED` and `DELIVERED` are terminal. This makes the API's manually managed and automatically managed transitions consistent.

The scheduler runs every five minutes and processes pending orders transactionally. It should update all eligible pending orders, not load an unbounded collection of complete order graphs. Concurrent scheduler runs should not cause a terminal or otherwise changed order to be overwritten; the update condition must still require `PENDING` at write time.

## HTTP API

Use a versioned base path `/api/v1/orders`:

| Method and path | Behavior |
| --- | --- |
| `POST /api/v1/orders` | Create an order; return `201 Created` and its representation. |
| `GET /api/v1/orders/{id}` | Retrieve one order; return `404` if absent. |
| `GET /api/v1/orders?status=PENDING` | List orders, optionally filtered by a valid status. |
| `PATCH /api/v1/orders/{id}/status` | Apply an allowed forward status transition. |
| `POST /api/v1/orders/{id}/cancel` | Cancel only a pending order. |

Validate required fields, non-empty item lists, positive quantities, and non-negative prices. Return a consistent JSON error shape for validation failures, unknown orders, malformed statuses, and invalid transitions. Listing is unpaged for the assignment's basic scope; document that limitation.

## Verification

Add automated tests for order creation and validation, retrieval/not-found, status filtering, permitted and rejected transitions, pending-only cancellation, and the scheduled pending-to-processing behavior. Use service-level tests for business rules and Spring integration tests for HTTP/persistence behavior where useful. Tests should use an isolated test database configuration and must not require an external PostgreSQL server to run.

## Documentation and delivery

The README will describe prerequisites, starting the local PostgreSQL service and application, configuration, API examples, status rules, scheduler behavior, and tests. Include a short section describing AI assistance (where it helped), issues found during review/testing, and how they were corrected; keep it factual and update it to match the actual implementation and validation performed. Do not include a Docker Compose file. Provide an environment-variable example without secrets.

## Out of scope

Authentication/authorization, inventory reservation, payment processing, customer accounts, messaging/queues, retries/dead-letter handling, pagination, and deployment infrastructure are not required by the assignment and will not be added unless implementation reveals a direct need.
