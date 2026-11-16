# Next Steps and Open Concerns

This checklist reflects the current repository state. The endpoint PDF, diagrams, and scope document describe intended behavior; they are not proof that the corresponding code is complete.

## 1. Make the project build and test reliably

- [x] Run Maven with a JDK that supports the configured Java 21 release. Maven compilation now succeeds with the installed Java 21 JDK; this also exposed and fixed a missing Lombok all-arguments constructor on `CartItem`. VS Code's shell defaults to the system Java 25 runtime, so select a JDK 21 runtime in VS Code or set `JAVA_HOME` to a JDK 21 before running Maven.
- [x] Configure PostgreSQL connection properties through `POSTGRES_URL`, `POSTGRES_USER`, and `POSTGRES_PASS`; credentials are not stored in the repository.
- [x] Add and verify the initial PostgreSQL Flyway migration against a clean PostgreSQL 17 database. Add future schema changes as new versioned migrations.
- [x] Add a local setup guide with PostgreSQL environment variables and build/run instructions. Docker Compose remains optional.

## 2. Finish core service and API behavior

- [ ] Implement customer profile updates; `CustomerService.editProfile()` currently returns `null`.
- [ ] Implement restaurant profile and menu-management services/endpoints; `RestaurantService` is empty.
- [ ] Implement cart operations in `CartService`, including one active cart per customer, restaurant locking, and cart item validation.
- [ ] Complete checkout as one transaction: re-check item availability, calculate totals on the server, snapshot prices, create the order, and clear the cart.
- [ ] Add request validation for required fields, positive quantities, non-negative prices, and other domain constraints.
- [ ] Return appropriate HTTP status codes and use consistent, documented endpoint paths and parameter sources. In particular, avoid relying on request bodies for GET requests.
- [ ] Ensure controller routes describe the resource they return. The current restaurant route `GET /api/v1/restaurants/{restaurantId}` returns that restaurant's orders, despite looking like a restaurant resource lookup.
- [ ] Ensure resource ownership is checked for order reads, updates, and cancellation; do not rely on caller-supplied customer or restaurant IDs as authorization.
- [ ] Validate that requested menu items are available as well as not deleted, and handle duplicate menu-item IDs in order requests intentionally.

## 3. Complete authentication and order authorization

- [ ] Add Spring Security configuration, password hashing (BCrypt), signup/login flows, and JWT validation.
- [ ] Apply role and ownership checks to protected routes. Decide whether an admin role is required for the planned administrative actions.
- [ ] Decide token expiry and refresh/revocation behavior.
- [ ] Enforce order transitions by actor and state, including which states customers may cancel and which transitions restaurant owners may perform.
- [ ] Decide whether to implement the one-hour pickup timeout and customer ban in this version; if so, specify ban duration, recovery, and timeout scheduling behavior.
- [ ] Reconcile the pickup state flow across the README, `OrderStatus`, and [scope.md](./scope/scope.md), which currently describes a delivery-based flow.

## 4. Improve error handling and automated coverage

- [ ] Add focused service tests for order totals, price snapshots, missing/duplicate/unavailable menu items, ownership, and valid/invalid status transitions.
- [ ] Add API/integration tests for the main customer and restaurant flows using a reproducible test database.
- [ ] Fix the context-load failure caused by `MenuItemRepository.findAllByIdAndIsDeleted`; Spring Data reports that `MenuItem` has no `id` property.
- [ ] Map invalid transitions and invalid domain inputs to deliberate client responses (for example, `409 Conflict` or `400 Bad Request`) instead of allowing uncaught exceptions to become server errors.
- [ ] Expand the global exception handler to cover expected domain failures with a consistent error response.
- [ ] Keep the existing optimistic-locking conflict response and test concurrent order updates.

## 5. Align documentation and release readiness

- [ ] Keep the endpoint contract and scope document aligned with the actual controllers, roles, and pickup lifecycle.
- [ ] Verify the generated OpenAPI/Swagger UI and document its local URL.
- [ ] Add setup, configuration, migration, and test instructions once they work from a clean checkout.
- [ ] Choose a deployment target and database, configure production secrets safely, deploy, and verify key flows against the live service.

## Other concerns to decide

- **Current test coverage:** the existing test only checks that the Spring application context loads; business behavior is not covered yet.
- **IDE runtime mismatch:** IntelliJ uses the project JDK, while VS Code's shell defaults to Java 25. Prefer JDK 21 consistently for Maven and configure CI to use the same release.
- **Current test blocker:** `contextLoads` fails while initializing `MenuItemRepository.findAllByIdAndIsDeleted`; the derived query refers to a nonexistent `MenuItem.id` property. Tests also require the PostgreSQL environment variables described in the README.
- **Status transition mismatch:** `OrderStatus` permits cancellation from every non-terminal state, while the intended state diagram describes narrower cancellation rules and a timeout cancellation.
- **Database migration strategy:** the initial Flyway migration is now the schema source of truth; keep future entity changes paired with new migrations and clean-database verification.
- **API contract drift:** the README previously described four controllers, including authentication controllers that are not present in the source yet. Keep planned endpoints clearly labeled until implemented.
- **Security boundary:** JWT-related dependencies alone do not secure endpoints. Authentication, authorization, ownership validation, secret management, and token lifecycle all still need implementation.
