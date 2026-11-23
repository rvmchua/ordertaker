# OrderTaker — Food Ordering REST API

A Spring Boot backend for a food ordering platform connecting customers and restaurants, built as a portfolio project to demonstrate REST API design, relational data modeling, and order-processing fundamentals. Authentication and authorization are not implemented yet.

## Table of Contents
- [Overview](#overview)
- [What’s Implemented](#whats-implemented)
- [Tech Stack](#tech-stack)
- [Data Model](#data-model)
- [Planned Order Status Lifecycle](#planned-order-status-lifecycle)
- [API Design and Current Implementation](#api-design-and-current-implementation)
- [Next Steps](#next-steps)
- [Getting Started](#getting-started)
- [Future Enhancements](#future-enhancements)
- [Author](#author)

## Overview

OrderTaker is an in-progress food-ordering API. The current implementation supports restaurant lookup, menu-item management, customer profile lookup, active-cart management, checkout from a cart, and order status updates. Authentication and authenticated customer/restaurant workflows are not implemented. Menu items use soft deletion, orders snapshot item prices, and order statuses have sequential transition rules.

## What’s Implemented

- Project scaffolded as a Java 21 / Spring Boot application using Maven.
- Initial JPA entities and repositories for users, customers, restaurants, menu items, carts, and orders.
- Restaurant listing, name search, and profile lookup.
- Menu-item creation, update, availability toggle, and soft deletion.
- Customer profile lookup and order lookup, checkout, and cancellation endpoints.
- Customer cart retrieval, creation, and full item-list replacement through HTTP routes.
- Checkout from the active cart with server-calculated totals, item price snapshots, availability checks, and cart deactivation after a successful order save.
- An order status enum with sequential transitions and terminal `RECEIVED` and `CANCELLED` states.
- A global exception handler for not-found, invalid arguments, already-deleted, terminal-order, validation, and optimistic-locking errors.
- JUnit coverage for service workflows, domain factories and status transitions, MapStruct mappings, MVC controller routes, and JPA repository derived queries.
- Initial product scope, ERD, order lifecycle diagram, and endpoint contract in [`docs/`](./docs/).

This is not yet a complete or secured ordering service. Authentication/authorization and menu browsing over HTTP remain incomplete. Checkout and restaurant status updates have seeded end-to-end API integration coverage, while cart mutation and deployed-API smoke tests remain future work. See [Next Steps](#next-steps) for the prioritized work list.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 4.1.1 (Spring MVC and Spring Data JPA)
- **Mapping:** MapStruct 1.6.3; Lombok
- **API documentation:** Springdoc OpenAPI dependency is included; endpoint generation and UI still need to be verified.
- **Auth:** JJWT 0.13.0 libraries are included, but authentication and authorization are not yet implemented.
- **Database:** PostgreSQL connection settings are read from environment variables; Flyway applies the initial schema migration on startup.
- **Build tool:** Maven
- **Tests:** JUnit Jupiter, Mockito, MockMvc, `@WebMvcTest`, `@DataJpaTest`, H2, and Spring Boot test starters

## Data Model

The model has a shared `User` identity with customer and restaurant profiles; a restaurant owns its menu items; a customer can have a cart made up of cart items; and an order contains order items with a price snapshot.

| Entity | Role |
|---|---|
| `Users` | Shared identity table for both account types (email, password, role) |
| `Customers` | Customer-specific profile data, linked to a `User` |
| `Restaurants` | Restaurant profile and business info, linked to an owning `User` |
| `MenuItems` | A restaurant's menu, with independent availability and deletion flags |
| `Carts` / `CartItems` | A customer's in-progress order — live pricing, not yet committed |
| `Orders` / `OrderItems` | A committed order — snapshotted pricing (`priceAtOrderTime`), drives the status lifecycle below |

Full entity-relationship diagram: [`ERD.puml`](./docs/uml/ERD.puml).

## Planned Order Status Lifecycle

The implemented pickup flow is an explicit state machine. Customers can cancel `PENDING` or `CONFIRMED` orders, restaurants can confirm or cancel `PENDING` orders and advance confirmed/preparing orders, and `FOR_PICKUP` orders are received by the customer. Timeout behavior is not implemented.

```mermaid
stateDiagram-v2
    [*] --> Pending
    state choice_point <<choice>>
    Pending --> choice_point: Restaurant reviews
    choice_point --> Confirmed: [in stock]
    choice_point --> Cancelled: [out of stock]
    Pending --> Cancelled: Customer cancels
    Confirmed --> Preparing: Restaurant confirms preparing
    Confirmed --> Cancelled: Customer cancels
    Preparing --> Cancelled: Customer cancels
    Preparing --> For_Pickup: Restaurant tags for pickup
    For_Pickup --> Cancelled: Timeout (1hr) / banCustomer()
    For_Pickup --> Received: Customer confirms handoff
    Received --> [*]
    Cancelled --> [*]
```

## API Design and Current Implementation

The endpoint document describes a planned contract; it does not mean all documented endpoints exist. The currently mapped routes are:

| Method | Route | Behavior |
|---|---|---|
| `GET` | `/api/v1/restaurants` | List restaurants; optional `?restaurant=` name search |
| `GET` | `/api/v1/restaurants/{restaurantId}` | Get a restaurant profile |
| `GET` | `/api/v1/restaurants/order/{restaurantId}` | List restaurant orders |
| `POST` | `/api/v1/restaurants/menu` | Create a menu item |
| `PUT` | `/api/v1/restaurants/menu/{menuItemId}` | Update a menu item |
| `PATCH` | `/api/v1/restaurants/menu/{menuItemId}` | Toggle availability |
| `DELETE` | `/api/v1/restaurants/menu/{menuItemId}` | Soft-delete a menu item |
| `GET` | `/api/v1/customers/profile?customerId={id}` | Get a customer profile |
| `PUT` | `/api/v1/customers/profile` | Profile edit route is present but its service is unimplemented |
| `GET` | `/api/v1/customers/order` | List customer orders; currently reads the customer ID from the request body |
| `GET` | `/api/v1/customers/order/{orderId}` | Get a customer order; currently reads the customer ID from the request body |
| `POST` | `/api/v1/customers/order` | Check out the customer's active cart; request body contains `customerId` |
| `PATCH` | `/api/v1/customers/order/{orderId}` | Request cancellation |
| `PATCH` | `/api/v1/restaurants/order/{orderId}?restaurantId={id}` | Restaurant status update |
| `GET` | `/api/v1/customers/cart` | Get the customer's active cart; request body contains `customerId` |
| `POST` | `/api/v1/customers/cart` | Create an active cart from `CreateCartRequest` |
| `PUT` | `/api/v1/customers/cart` | Replace the active cart's item list; an empty list clears it |

`MenuService` has menu lookup methods, but there is currently no HTTP route to retrieve a restaurant's menu. Cart updates replace the full item list; item-specific add/update/remove routes and authentication routes are absent. Customer and restaurant ownership is not yet derived from an authenticated principal.

See the [endpoint contract](./docs/endpoint/Endpoint%20Documentation.pdf) for the planned routes and request/response details. Authentication and customer-facing menu browsing remain future implementation work. MVC route coverage, repository query coverage, and seeded checkout/status API integration coverage are implemented.

## Next Steps

The prioritized implementation checklist and design concerns are maintained in [`docs/next-steps.md`](./docs/next-steps.md).

## Getting Started

### Prerequisites

- JDK 21
- PostgreSQL 17 or newer
- Maven wrapper (included)

### Configure PostgreSQL

As a PostgreSQL administrator, create a local database named `ordertaker` and a login role that owns it. For a new local setup, open `psql -d postgres` and run:

```sql
CREATE ROLE ordertaker LOGIN;
\password ordertaker
CREATE DATABASE ordertaker OWNER ordertaker;
```

`\password` prompts for the role's password without placing it in shell history. If the role and database already exist, reuse them instead of recreating them.

Set the connection values in the shell where Maven will run:

```sh
export POSTGRES_URL="jdbc:postgresql://localhost:5432/ordertaker"
export POSTGRES_USER="ordertaker"
read -r -s -p "PostgreSQL password: " POSTGRES_PASS
printf '\n'
export POSTGRES_PASS
```

In an IDE, configure the same three environment variables in the run configuration. Flyway applies the initial schema migration automatically on application startup.

### Build and run

Ensure Maven uses a JDK 21 installation (including `javac`); selecting only a JRE or using a Java executable without its matching compiler can fail compilation.

On the current development machine, IntelliJ's JDK 21 is installed at
`$HOME/.jdks/temurin-21.0.12.1`. Select it explicitly when running Maven from a
fresh shell; replace the path if your JDK 21 is installed elsewhere:

```sh
JAVA_HOME="$HOME/.jdks/temurin-21.0.12.1" \
PATH="$HOME/.jdks/temurin-21.0.12.1/bin:$PATH" \
./mvnw -DskipTests package
```

For `spring-boot:run`, use the same `JAVA_HOME` and `PATH` assignments before
`./mvnw spring-boot:run`. The inline assignments apply to that invocation and do
not persist into later shell sessions.

### Tests

```sh
JAVA_HOME="$HOME/.jdks/temurin-21.0.12.1" \
PATH="$HOME/.jdks/temurin-21.0.12.1/bin:$PATH" \
./mvnw test
```

To run the service unit tests only:

```sh
JAVA_HOME="$HOME/.jdks/temurin-21.0.12.1" \
PATH="$HOME/.jdks/temurin-21.0.12.1/bin:$PATH" \
./mvnw -Dtest=CartServiceTest,OrderServiceTest,MenuServiceTest,RestaurantServiceTest test
```

The suite currently contains 106 tests covering services, domain factories, status transitions, mappers, MVC controllers, JPA repositories, checkout and status API integration, and application context startup.

Test layers are:

- JUnit 5 and Mockito for service and domain unit tests.
- `@WebMvcTest` plus `MockMvc` for controller routing, validation, JSON responses, and exception handling.
- `@DataJpaTest` with H2 for derived-query repository integration tests.
- MapStruct unit tests for entity/DTO mappings.
- `contextLoads` for Spring application-context startup with an isolated H2 datasource.
- `CheckoutApiIntegrationTest` for end-to-end checkout through `MockMvc`, persistence, price snapshots, cart deactivation, and checkout errors.
- `OrderStatusApiIntegrationTest` for seeded successful and rejected restaurant status updates through `MockMvc`.

Cart tests cover retrieval, creation, replacement, quantity overflow, clearing, and cart deactivation. Order tests cover cart checkout, lookup, persistence failures, cancellation, restaurant updates, and transition rules. The H2 test configuration keeps repository and context tests independent of a local PostgreSQL instance. See [`docs/next-steps.md`](./docs/next-steps.md).

## Future Enhancements

- **Android client** — explicitly out of scope for this project; planned as a separate portfolio piece after OrderTaker ships.

## Author

**Royce Vincent Chua**
royce.chua@rvmchua.com