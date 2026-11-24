# OrderTaker Project Documentation

**Repository:** rvmchua/ordertaker  
**Type:** Backend REST API and capstone portfolio project  
**Runtime:** Java 21 / Spring Boot 4.1.1  
**Status:** Implemented MVP with documented deferred enhancements

## 1. Executive Summary

OrderTaker is a backend for a restaurant pickup food-ordering workflow. It
supports account registration and login, restaurant and menu discovery,
customer cart management, transactional checkout, order history, cancellation,
and restaurant-controlled order fulfillment.

The project demonstrates practical full-stack backend concerns: REST API
design, role-based authorization, password hashing, signed access tokens,
relational modeling, versioned database migrations, server-side price
calculation, optimistic concurrency, centralized error handling, container
deployment, and test coverage across multiple Spring test layers.

## 2. Problem and Product Goals

Small restaurants need a clear workflow for publishing menu items and receiving
orders, while customers need a way to build an order without trusting the
browser with prices or totals. OrderTaker separates HTTP concerns from domain
and persistence concerns, and the server is responsible for:

- Verifying that requested menu items belong to the selected restaurant.
- Calculating checkout totals from database values.
- Saving the price paid for each item at the time of checkout.
- Enforcing the allowed order-state sequence.
- Deriving customer and restaurant identity from the authenticated JWT principal.
- Preventing two active carts for one customer.

## 3. Architecture

The application uses a conventional Spring layered architecture:

```text
HTTP Client
    |
    v
REST Controller
    |
    v
Service / Domain Rules
    |
    v
Spring Data JPA Repository
    |
    v
JPA Entities
    |
    v
PostgreSQL
```

### Layer Responsibilities

- **Controller:** maps HTTP requests, performs Bean Validation, and reads the
  authenticated principal.
- **Service:** coordinates repositories and enforces workflows.
- **Domain entity:** owns cart replacement and order creation, and exposes
  order-transition rules.
- **Repository:** provides Spring Data CRUD and derived queries.
- **Mapper:** translates entities and Java records using MapStruct.
- **Security:** validates JWT bearer tokens and establishes Spring Security
  authorities.
- **Exception handler:** converts domain and framework failures into deliberate
  HTTP responses.

### Why This Structure

The structure keeps HTTP details out of domain and persistence logic. Controllers
remain thin, services are directly testable with Mockito, and JPA entities
consolidate invariants that should not be duplicated across endpoints.

## 4. Core Actors and Roles

| Role | Access |
|---|---|
| `CUSTOMER` | Own profile, cart, and orders |
| `RESTAURANT_OWNER` | Own restaurant menu and restaurant orders |
| `RESTAURANT` | Compatibility role accepted by restaurant authorization |
| `ADMIN` | Reserved for future administrative workflows |

The role is stored on `User`, encoded in the JWT claim and authority, and
mapped to `ROLE_<name>` for Spring Security.

## 5. Authentication and Authorization

Registration normalizes email addresses, rejects duplicates, and stores a
BCrypt password hash. Login compares a password to the hash and returns a
signed JJWT access token.

The access token contains:

- Email as its subject.
- A `role` claim.
- Issued-at and expiry timestamps.
- A 60-minute default expiration.

`JwtAuthenticationFilter` reads bearer tokens, validates their signature and
expiry, resolves the user, and installs a stateless Spring Security context.
CSRF, form login, HTTP Basic, and logout are disabled for this REST API.

Customer and restaurant IDs come from `Authentication` and the
`AuthenticatedUserService`, rather than the ID in a request body or route.
Restaurant menu mutations and order updates recheck the authenticated owner's
relationship to the target resource.

## 6. Domain and Data Model

The system contains users and optional customer/restaurant profiles. A
restaurant owns menu items. A customer owns an active cart and orders.

### Main Entities

| Entity | Responsibility |
|---|---|
| `User` | Login identity, password hash, role |
| `Customer` | Customer profile and ownership |
| `Restaurant` | Restaurant profile and ownership |
| `MenuItem` | Priced menu entry with availability/deletion flags |
| `Cart` | Customer's active in-progress restaurant selection |
| `CartItem` | Menu item and quantity in a cart |
| `Order` | Committed order, status, total, optimistic version |
| `OrderItem` | Purchased quantity and historical item price |

Soft deletion keeps historical menu references while hiding deleted items from
active menus. The cart aggregate replaces items and uses orphan removal.
`@Version` enables optimistic locking for order updates.

Full schema details are in [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md), and the
diagram is in [ERD.puml](uml/ERD.puml).

## 7. Checkout Workflow

1. The authenticated customer is resolved.
2. The customer's active cart is loaded.
3. The cart must contain at least one item.
4. The domain creates an order in `PENDING`.
5. Every cart item is checked for availability and a non-null price.
6. Current menu prices are multiplied by quantities to calculate the total.
7. Each `OrderItem` stores `priceAtOrderTime`.
8. The order and order items are saved in one transaction.
9. Only after a successful save, the cart is marked inactive.

This approach prevents historical order totals or item prices from changing
when a menu item is later edited. Persistence failures do not deactivate a cart.

## 8. Order State Model

```text
PENDING -> CONFIRMED -> PREPARING -> FOR_PICKUP -> RECEIVED
   |             |
   +-------------+-> CANCELLED
```

`PENDING` and `CONFIRMED` are customer-cancellable. A customer may also move
`FOR_PICKUP` to `RECEIVED`. A restaurant may confirm or cancel pending orders,
move confirmed work to preparing, and tag preparing work for pickup.
`RECEIVED` and `CANCELLED` are terminal states.

The enum centralizes transition rules, while the service and repository verify
the customer or restaurant owns the order. The state diagram source is in
[State Diagram.puml](uml/State%20Diagram.puml).

## 9. API Summary

The full route contract is maintained in [API.md](API.md). Important route
groups are:

- `/api/v1/auth` for registration and login.
- `/api/v1/restaurants` for public browsing and restaurant-owner operations.
- `/api/v1/customers` for customer profile, cart, and order operations.
- `/actuator/health` for deployment health.

Springdoc exposes `/swagger-ui.html` and `/v3/api-docs` when the application is
running.

## 10. Error Handling

The global exception handler maps:

- Missing resources to `404 Not Found`.
- Duplicate email to `409 Conflict`.
- Invalid credentials to `401 Unauthorized`.
- Ownership violations to `403 Forbidden`.
- Invalid status transitions and deleted resources to `409 Conflict`.
- Validation, malformed JSON, missing parameters, and invalid arguments to
  `400 Bad Request`.
- Optimistic-lock conflicts to `409 Conflict`.

JSON error bodies make API failures clear to a frontend or a client test.

## 11. Testing Strategy

[TESTING.md](TESTING.md) documents the 130-test suite. The suite combines:

- JUnit 5 and Mockito service/domain tests.
- `@WebMvcTest` and MockMvc controller tests.
- `@DataJpaTest` and H2 repository tests.
- MapStruct conversion tests.
- Seeded checkout and status API integration tests.
- Application-context startup tests.

H2 keeps repository and context tests independent of a developer's PostgreSQL
service.

## 12. Deployment

The Dockerfile performs a Maven build in a Java 21 build stage and creates a
slim Java 21 runtime image. Docker Compose provides PostgreSQL 17 and the API,
waits for database health, and maps the API port.

Database credentials and JWT signing secrets are supplied outside Git. The
default JWT secret in local `application.yaml` is explicitly for development;
production deployments must use a strong secret.

## 13. Engineering Tradeoffs

### Why JWT

A signed bearer token avoids server-side session state and fits a stateless API
deployed as a container. Refresh and revocation are intentionally deferred
until the MVP security lifecycle requires them.

### Why PostgreSQL

PostgreSQL provides relational constraints and a mature Spring/Flyway ecosystem.
It is appropriate for identities, aggregates, foreign keys, and transactional
checkout.

### Why Flyway

Schema changes are ordered and repeatable. Docker startup migrates the database,
allowing application and schema changes to be coordinated.

### Why MapStruct

MapStruct removes repetitive conversion code and generates implementations at
compile time, preserving type safety and a clear DTO boundary.

### Why H2 Tests

H2 gives fast repository/context feedback without requiring PostgreSQL for unit
and integration test execution. PostgreSQL smoke testing remains necessary for
deployment-specific verification.

## 14. Known Limitations

- Customer profile editing is implemented but requires stronger uniqueness and
  validation coverage for email changes.
- Restaurant profile editing and deletion are not implemented.
- Cart mutation is full-list replacement rather than item-specific routes.
- No JWT refresh or revocation workflow exists.
- No pickup timeout scheduler or ban policy exists.
- Payments and real-time notifications are outside scope.
- Pagination and advanced order/menu filtering are not implemented.

The prioritized state of all known work is maintained in [ROADMAP.md](ROADMAP.md).

## 15. Interview Defense Points

Useful talking points include:

- **Consistency:** checkout writes the order and snapshots prices in one
  transaction and deactivates the cart only after persistence succeeds.
- **Security:** bearer JWT validation is combined with role checks and
  authenticated customer/restaurant ownership resolution.
- **Domain design:** status transitions are in the domain enum, with role-specific
  customer and restaurant rules.
- **Persistence:** Flyway migrations, JPA relationships, soft deletion, and
  optimistic locking preserve data integrity and historical records.
- **Testing:** the project verifies behavior at unit, MVC, repository, context,
  and workflow integration levels.
- **Operations:** Docker Compose and health checks make local deployment portable,
  while secrets remain outside source control.
- **Tradeoffs:** H2 accelerates tests, PostgreSQL supplies production semantics,
  and refresh/revocation or payments are deliberately deferred as separate
  product scopes.

## 16. Related Documentation

- [Architecture and codebase map](CODEBASE_MAP.md)
- [API contract](API.md)
- [Database schema](DATABASE_SCHEMA.md)
- [Testing strategy](TESTING.md)
- [Roadmap and known gaps](ROADMAP.md)
