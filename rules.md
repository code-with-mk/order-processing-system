# Coding Guidelines

These guidelines apply to all code added to this repository. Keep changes focused on the order processing assignment and its approved design.

## Java and project structure

- Target Java 21 and follow the repository's Spring Boot and Maven conventions.
- Use descriptive `PascalCase` type names and `camelCase` methods and fields. Name constants in `UPPER_SNAKE_CASE`.
- Keep one primary type per file. Use familiar packages by layer: `controller`, `service`, `dto`, and `dao`, with supporting `entity`, `exception`, and `scheduler` packages.
- Keep controllers focused on HTTP concerns. Put business rules in the application/domain layer and database access behind repositories.
- Prefer small cohesive classes and methods. Avoid speculative abstractions, duplicated logic, and unrelated refactoring.
- Use constructor injection; do not use field injection.
- Use immutable request/response DTOs where practical. Do not expose persistence entities as API responses.

## Formatting and readability

- Use four spaces for indentation and follow standard Java formatting conventions.
- Use braces for all control-flow blocks. Avoid deeply nested conditionals; use guard clauses where they improve clarity.
- Name variables for their meaning. Avoid abbreviations except common domain terms.
- Add comments only when they explain a non-obvious decision or constraint. Do not narrate straightforward code.
- Keep public APIs and configuration documented in the README or concise Javadoc when their contract is not otherwise clear.

## Domain and business rules

- Represent order states with an enum: `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
- Keep state transition decisions in one domain/service location, not spread across controllers or persistence code.
- New orders start as `PENDING`. Cancellation is allowed only while pending. The scheduler advances pending orders to processing. Explicit updates may advance processing to shipped and shipped to delivered. Delivered and cancelled orders are terminal.
- Reject invalid transitions with a clear client-facing error. Do not silently coerce requested statuses.
- Validate order input: at least one item, non-blank product details, positive quantities, and non-negative prices.

## HTTP API and errors

- Keep endpoints under `/api/v1/orders` and use DTOs for input and output.
- Use appropriate HTTP semantics: `201 Created` for creation, `404 Not Found` for unknown IDs, `400 Bad Request` for malformed or invalid input/transitions, and successful 2xx responses for valid operations.
- Return a consistent JSON error structure. Never return stack traces, internal SQL details, or secrets to clients.
- Validate at the request boundary and retain domain checks in the service so non-HTTP callers cannot bypass business rules.
- Do not add authentication, pagination, payment, inventory, or other out-of-scope features without updating the approved design.

## Persistence and transactions

- Use `BigDecimal` for prices and totals; never use `float` or `double` for monetary values.
- Persist order data using JPA mappings with explicit relationships, sensible column constraints, and stable enum storage as strings.
- Keep transaction boundaries in the application/service layer. Make operations that update related order state atomic.
- Avoid loading full order graphs for bulk scheduler work. Bulk status updates must include `status = PENDING` in the write condition so concurrent changes are not overwritten.
- Do not delete order history when cancelling an order.
- Keep credentials out of source control. Use environment-based configuration and safe local defaults only for non-sensitive settings.

## Scheduling and concurrency

- Schedule pending-to-processing work at a five-minute interval.
- Keep the scheduled component thin; delegate processing to a transactional service/repository operation.
- Make the transition safe to repeat. A run should only update orders that remain pending at write time.
- Do not introduce distributed locks or queues unless the deployment assumptions change and the design is revised.

## Testing

- Write behavior-focused tests for business rules, especially invalid transitions, pending-only cancellation, and the scheduled transition.
- Test API status codes, validation responses, filtering, and not-found behavior at the HTTP boundary.
- Prefer real application components and an isolated test database configuration. Mock only external boundaries that cannot reasonably be exercised in-process.
- Keep test names explicit about the behavior they protect. Keep test fixtures minimal and deterministic.
- Tests must run without a developer-managed PostgreSQL instance; use the repository's isolated test database setup.
- Do not claim tests pass unless they were run and their output was checked.

## Configuration, dependencies, and security

- Keep dependency versions controlled by the Spring Boot parent/BOM where possible. Add only dependencies needed by the approved design.
- Keep application configuration externalizable through environment variables and document required variables.
- Do not commit real secrets, personal data, database dumps, or machine-specific IDE files.
- Use parameterized repository APIs and let JPA bind values; never build SQL from untrusted input.
- Log operationally useful identifiers and outcomes, but not credentials or unnecessary customer data.

## Documentation and collaboration

- Keep the README accurate for startup, configuration, endpoints, scheduler behavior, and test commands.
- In the AI assistance section, state what tools were used, what issues were identified, and what was changed to address them. Describe only work that actually occurred.
- Keep code changes reviewable and focused. Update or add tests and documentation when behavior or public API contracts change.
