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

OrderTaker is an in-progress food-ordering API. The current implementation supports restaurant lookup, menu-item management, customer profile lookup, and direct order creation and status updates. Cart checkout and authenticated customer/restaurant workflows are not implemented. Menu items use soft deletion, orders snapshot item prices, and order statuses have sequential transition rules.

## What’s Implemented

- Project scaffolded as a Java 21 / Spring Boot application using Maven.
- Initial JPA entities and repositories for users, customers, restaurants, menu items, carts, and orders.
- Restaurant listing, name search, and profile lookup.
- Menu-item creation, update, availability toggle, and soft deletion.
- Customer profile lookup and initial order lookup, creation, and cancellation endpoints.
- Cart service operations for creating, retrieving, and replacing a customer's active cart; these are not exposed through HTTP routes yet.
- Direct order creation with server-calculated totals, item price snapshots, and checks for missing items and restaurant ownership.
- An order status enum with sequential transitions and terminal `RECEIVED` and `CANCELLED` states.
- A global exception handler for not-found, invalid arguments, already-deleted, terminal-order, validation, and optimistic-locking errors.
- Initial product scope, ERD, order lifecycle diagram, and endpoint contract in [`docs/`](./docs/).

This is not yet a complete or secured ordering service. Cart HTTP endpoints and checkout, authentication/authorization, menu browsing over HTTP, and cart-specific automated coverage remain incomplete. See [Next Steps](#next-steps) for the prioritized work list.

## Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot 4.1.1 (Spring MVC and Spring Data JPA)
- **Mapping:** MapStruct 1.6.3; Lombok
- **API documentation:** Springdoc OpenAPI dependency is included; endpoint generation and UI still need to be verified.
- **Auth:** JJWT 0.13.0 libraries are included, but authentication and authorization are not yet implemented.
- **Database:** PostgreSQL connection settings are read from environment variables; Flyway applies the initial schema migration on startup.
- **Build tool:** Maven
- **Tests:** JUnit Jupiter and Mockito

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

The intended pickup flow is documented as an explicit state machine. The enum currently supports sequential transitions and cancellation from any non-terminal state; role-specific permissions and timeout behavior are not implemented yet.

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
| `PATCH` | `/api/v1/customers/profile` | Profile edit route is present but its service is unimplemented |
| `GET` | `/api/v1/customers/order` | List customer orders; currently reads the customer ID from the request body |
| `GET` | `/api/v1/customers/order/{orderId}` | Get a customer order; currently reads the customer ID from the request body |
| `POST` | `/api/v1/customers/order` | Create an order directly from item IDs and quantities |
| `PATCH` | `/api/v1/customers/order/{orderId}` | Request cancellation |

`MenuService` has menu lookup methods, but there is currently no HTTP route to retrieve a restaurant's menu. `CartService` supports cart retrieval, creation, and full item-list replacement, but there are no cart controller routes. Authentication routes are also absent.

See the [endpoint contract](./docs/endpoint/Endpoint%20Documentation.pdf) for the planned routes and request/response details. Cart workflows, authentication, and customer-facing menu browsing remain future implementation work.

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

To run the service unit tests without the PostgreSQL-backed context test:

```sh
JAVA_HOME="$HOME/.jdks/temurin-21.0.12.1" \
PATH="$HOME/.jdks/temurin-21.0.12.1/bin:$PATH" \
./mvnw -Dtest=OrderServiceTest,MenuServiceTest,RestaurantServiceTest test
```

The unit tests use JUnit and Mockito. The `contextLoads` test starts the application context and therefore also needs the PostgreSQL environment variables above and a reachable database. Order-service unit tests cover order creation, lookup, and status updates; cart service tests and API integration tests have not been added yet. See [`docs/next-steps.md`](./docs/next-steps.md).

## Future Enhancements

- **Android client** — explicitly out of scope for this project; planned as a separate portfolio piece after OrderTaker ships.

## Author

**Royce Vincent Chua**
royce.chua@rvmchua.com