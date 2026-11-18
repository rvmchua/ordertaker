# OrderTaker Codebase Map

## 1. Project Overview

OrderTaker is an in-progress Java REST API for a local food-ordering and restaurant pickup workflow. The implemented foundation covers restaurant lookup, menu-item CRUD/soft-delete behavior, customer profile lookup, direct order creation/status/listing, and service-level active-cart creation/retrieval/replacement. Authentication, authorization, cart HTTP endpoints and checkout, complete profile editing, and controller coverage for menu browsing are still incomplete.

The project is intended to demonstrate:

- Spring MVC REST API design.
- Spring Data JPA and relational modeling.
- Flyway-managed PostgreSQL schema setup.
- MapStruct DTO/entity mapping.
- Bean validation and centralized exception handling.
- A server-calculated order total with `priceAtOrderTime` snapshots.
- An explicit order status transition model.

### Technology stack

| Area | Technology |
|---|---|
| Language/runtime | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring MVC / `spring-boot-starter-webmvc` |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL |
| Migrations | Flyway via `spring-boot-starter-flyway` and `flyway-database-postgresql` |
| API documentation | Springdoc OpenAPI `springdoc-openapi-starter-webmvc-ui` 3.1.0 |
| Mapping | MapStruct 1.6.3 |
| Boilerplate reduction | Lombok |
| JWT dependencies | JJWT 0.13.0 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) |
| Build | Maven 3.9.16 wrapper, Java 21 release |
| Tests | JUnit Jupiter, Mockito, Spring Boot test starters |
| Operations | Spring Boot Actuator |

Relevant files: `pom.xml`, `src/main/resources/application.yaml`, `src/main/java/com/rvmc/ordertaker/OrdertakerApplication.java`.

### Architecture

The application follows a conventional layered REST architecture:

`Controller -> Service -> Spring Data Repository -> JPA Entity -> PostgreSQL`

Response/request mapping is handled through MapStruct interfaces. Domain creation logic for orders is in `Order.create(...)`; services own lookup, validation, and mutation logic.

There is no authentication/authorization implementation in source code. JWT libraries are present, but no Spring Security dependency or security configuration is implemented.

## 2. Directory Structure

| Path | Responsibility |
|---|---|
| `src/main/java/com/rvmc/ordertaker/` | Main application source |
| `src/main/java/com/rvmc/ordertaker/controller/` | REST controllers and HTTP route definitions |
| `src/main/java/com/rvmc/ordertaker/service/` | Business logic and orchestration |
| `src/main/java/com/rvmc/ordertaker/repository/` | Spring Data JPA repository interfaces |
| `src/main/java/com/rvmc/ordertaker/entity/` | JPA entities |
| `src/main/java/com/rvmc/ordertaker/entity/enums/` | `Role` and `OrderStatus` enums |
| `src/main/java/com/rvmc/ordertaker/dto/` | Java record request/response models |
| `src/main/java/com/rvmc/ordertaker/mapper/` | MapStruct mapping interfaces |
| `src/main/java/com/rvmc/ordertaker/exception/` | Domain exceptions and global HTTP exception handling |
| `src/main/resources/application.yaml` | Datasource configuration |
| `src/main/resources/db/migration/` | Flyway SQL migrations |
| `src/test/java/com/rvmc/ordertaker/` | JUnit/Mockito tests |
| `docs/` | Scope, ERD, state diagram, roadmap, and endpoint documentation |
| `docs/next-steps.md` | Prioritized implementation checklist |
| `docs/uml/ERD.puml` | Intended entity relationship diagram |
| `docs/uml/State Diagram.puml` | Intended order lifecycle diagram |
| `docs/scope/scope.md` | Product scope and planned roles |
| `pom.xml` | Maven dependencies and Java compiler configuration |
| `README.md` | Setup and implementation summary |
| `mvnw`, `mvnw.cmd` | Maven wrapper commands |

### Where to implement new features

- Add HTTP routes in `src/main/java/com/rvmc/ordertaker/controller/`.
- Put validation and cross-repository orchestration in `service/`.
- Add derived queries or standard JPA operations in `repository/`.
- Add persistent fields or relationships in `entity/`.
- Add API payload records in `dto/`.
- Add entity/DTO conversions in `mapper/`.
- Add HTTP mappings for new exceptions in `exception/GlobalExceptionHandler.java`.
- Add schema changes as a new Flyway migration under `src/main/resources/db/migration/`.
- Add focused service tests in `src/test/java/com/rvmc/ordertaker/service/`; add context/controller tests alongside the existing test structure.

## 3. Architecture and Component Relationships

### Controllers and endpoints

`CustomerController` (`/api/v1/customers`)

| Method | Route | Implemented behavior |
|---|---|---|
| `GET` | `/profile` | Finds a customer profile by the `customerId` method parameter |
| `PATCH` | `/profile` | Calls `CustomerService.editProfile(...)`; currently unimplemented |
| `GET` | `/order` | Lists a customer's orders; accepts the customer ID in the request body |
| `GET` | `/order/{orderId}` | Finds one customer's order; accepts the customer ID in the request body |
| `POST` | `/order` | Creates an order from `OrderRequest` |
| `PATCH` | `/order/{orderId}` | Rejects status values other than `CANCELLED`, then calls order status update |

`RestaurantController` (`/api/v1/restaurants`)

| Method | Route | Implemented behavior |
|---|---|---|
| `GET` | `/` | Lists all restaurants or filters with `?restaurant=` |
| `GET` | `/{restaurantId}` | Finds one restaurant |
| `GET` | `/order/{restaurantId}` | Lists orders for a restaurant |
| `POST` | `/menu` | Creates a menu item |
| `PUT` | `/menu/{menuItemId}` | Replaces mapped mutable menu fields |
| `PATCH` | `/menu/{menuItemId}` | Toggles menu-item availability |
| `DELETE` | `/menu/{menuItemId}` | Soft-deletes a menu item |

There is no controller route that currently exposes `MenuService.getMenuFromRestaurant(...)`; menu browsing must be added to a controller or that method must be wired to an existing route.

### Services

- `RestaurantService`
  - Lists restaurants.
  - Gets a restaurant by ID.
  - Case-insensitive, contains-style name search.
- `MenuService`
  - Gets active menu items for a restaurant or one non-deleted item.
  - Creates, updates, toggles availability, and soft-deletes menu items.
- `CustomerService`
  - Gets a customer profile.
  - `editProfile(...)` is currently `return null`.
- `OrderService`
  - Loads customer/restaurant orders.
  - Creates orders and delegates total calculation and price snapshots to `Order.create(...)`.
  - Loads distinct requested menu-item IDs and validates existence and restaurant ownership.
  - Updates status through the entity transition rules.
- `CartService`
  - Retrieves a customer's active cart.
  - Creates an active cart from a non-empty item list, merging duplicate menu-item IDs and validating quantities and restaurant ownership.
  - Replaces the active cart's item list; an empty list clears it, and changing the cart's restaurant is rejected.
  - Has no controller routes or service tests yet; checkout is not implemented.

### Repositories

- `CustomerRepository`: standard `JpaRepository<Customer, Long>`.
- `UserRepository`: standard `JpaRepository<User, Long>`.
- `RestaurantRepository`: name contains-ignore-case search.
- `MenuItemRepository`: active/deleted queries, menu lookup, and a derived bulk lookup by `menuItemId`.
- `CartRepository`: find/check active carts by customer.
- `OrderRepository`: customer/restaurant/order-status and collection queries.

`MenuItemRepository.findAllByMenuItemIdInAndIsDeleted(...)` is used by both order and cart creation/update. There is no `CartItemRepository`; cart items are managed through the `Cart` aggregate.

### Entities and relationships

- `User` owns optional `Customer` and `Restaurant` profiles via one-to-one inverse mappings.
- `Customer` belongs to one `User`, owns an order list, and optionally has one active `Cart`.
- `Restaurant` belongs to one owner `User`, owns menu items, carts, and orders.
- `MenuItem` belongs to one `Restaurant` and has cart-item and order-item lists.
- `Cart` belongs to one `Customer` and one `Restaurant` and has many `CartItem`s.
- `CartItem` belongs to one `Cart` and one `MenuItem`.
- Active-cart state is stored in `Cart.isActive`; `Cart.replaceCartItems(...)` maintains both sides of the relationship, and orphan removal deletes replaced cart-item rows.
- `Order` belongs to one `Customer` and one `Restaurant`, has a status and version, and contains many `OrderItem`s.
- `OrderItem` belongs to one `Order` and one `MenuItem`, with quantity and `priceAtOrderTime`.

### DTOs and mapping

- `CustomerProfileRequest/Response`
- `RestaurantProfileResponse`
- `MenuItemRequest/Response`
- `OrderRequest/OrderResponse`
- `OrderItemRequest/OrderItemResponse`
- `UpdateOrderStatus`
- `CreateCartRequest`, `UpdateCartRequest`, `CartItemRequest`, `CartItemUpdateRequest`, `CartResponse`, and `CartItemResponse`

MapStruct interfaces:

- `CustomerMapper.toProfileResponse(...)`
- `RestaurantMapper.toRestaurantProfileResponse(...)`
- `MenuItemMapper.fromRequestToEntity(...)`, `toResponse(...)`, and `updateEntityFromRequest(...)`
- `OrderMapper.toResponse(...)`
- `CartMapper.toResponse(...)`

`MenuItemMapper` forces new items to `isAvailable=true` and `isDeleted=false`, ignores ID/restaurant for updates, and ignores null request properties during partial mapping.

### Validation and exception handling

Validation is provided by Jakarta Bean Validation:

- Menu item name is nonblank; price is required and at least `1.00`.
- Order customer/restaurant IDs and order item list are required.
- The item list must be nonempty and nested items are validated.
- Quantity must be at least 1.
- Cart creation requires customer/restaurant IDs and a non-empty list with non-null items; cart updates require a customer ID and a non-null item list, while an empty list clears the cart.
- `CartService` validates positive quantities, merges duplicate menu-item IDs with overflow checks, and verifies that all requested active menu items belong to the cart's restaurant.

`@RestControllerAdvice` handles:

- `ResourceNotFoundException` -> 404 with `{"error":"..."}`.
- `ResourceAlreadyTaggedAsDeletedException` -> 409.
- `OrderTaggedAsReceivedOrCancelledException` -> 409.
- `IllegalArgumentException` -> 400 with `{"error":"..."}`.
- `MethodArgumentNotValidException` -> 400 field/message map.
- `ObjectOptimisticLockingFailureException` -> 409 text response.

`IllegalStateException` and the cancellation guard's plain `RuntimeException` are not mapped by the global handler.

## 4. Application Data Flow

### Restaurant/menu request

1. A request reaches `RestaurantController`.
2. The controller calls `RestaurantService` or `MenuService`.
3. The service loads entities through a repository.
4. Spring Data JPA queries PostgreSQL.
5. MapStruct converts entities to Java-record DTO responses.
6. Spring MVC serializes the DTO as JSON.

### Order creation

1. `POST /api/v1/customers/order` binds `OrderRequest`.
2. `CustomerController.createOrder(...)` calls `OrderService.createOrder(...)`.
3. The service derives menu-item IDs from `OrderItemRequest`.
4. It calls the menu-item repository and validates result count, IDs, non-deleted state, and restaurant ownership.
5. It loads the restaurant and customer.
6. `Order.create(...)` sets status to `PENDING`, builds `OrderItem` rows, snapshots each current menu price, and calculates the total.
7. `OrderMapper` maps the saved order to `OrderResponse`.
8. `OrderRepository.save(...)` persists the aggregate; cascade behavior persists order items.

The method saves the order aggregate, but cart checkout, inventory recheck, cart clearing, and rollback assertions are not implemented.

### Cart service operations

1. `CartService.createActiveCart(...)` validates and merges request quantities, loads the customer and restaurant, and rejects an existing active cart.
2. It loads non-deleted menu items and verifies all IDs and restaurant ownership.
3. It builds `CartItem`s, associates them with a new active `Cart`, and saves the aggregate with cascade.
4. `getActiveCart(...)` loads the customer's active cart; `updateActiveCart(...)` replaces its entire item collection and allows an empty list to clear it.

These are service methods only; no cart HTTP endpoints or cart-to-order checkout flow exists yet.

### Order status update

1. The client sends `UpdateOrderStatus` and an order ID.
2. The customer controller blocks values other than `CANCELLED`.
3. `OrderService.updateOrderStatus(...)` verifies the order exists and is not terminal.
4. `OrderStatus.canTransitionTo(...)` validates the transition.
5. JPA saves the changed status and `@Version` supplies optimistic locking.

`@Enumerated` is not used on `Order.status`, so JPA currently maps the enum as its ordinal. The migration stores `status INTEGER`. This ordering must be preserved if enum values change.

## 5. Database Structure

Schema source: `src/main/resources/db/migration/V1__create_initial_schema.sql` and subsequent versioned migrations.

| Table | Primary key | Important foreign keys / constraints |
|---|---|---|
| `users` | `user_id` identity | `role` and other values currently unconstrained |
| `customers` | `customer_id` identity | Unique/non-null `user_id` -> `users.user_id` |
| `restaurants` | `restaurant_id` identity | Unique/non-null `owner_id` -> `users.user_id` |
| `menu_items` | `menu_item_id` identity | Non-null `restaurant_id` -> `restaurants.restaurant_id` |
| `carts` | `cart_id` identity | Unique/non-null `customer_id`; non-null `restaurant_id` |
| `cart_items` | `cart_item_id` identity | Non-null cart/menu foreign keys |
| `orders` | `order_id` identity | Customer/restaurant foreign keys; non-null version |
| `order_items` | `order_item_id` identity | Non-null order/menu foreign keys |

Indexes exist for restaurant, customer, cart, menu-item, and order-item foreign keys.

Important constraints and behavior:

- A customer has at most one customer record per user and one cart due to unique foreign keys.
- A restaurant has at most one owner user due to unique `owner_id`.
- Menu soft deletion is represented by `is_deleted`; carts and order items link to menu items.
- `order_items.price_at_order_time` stores the snapshot, preserving historical prices.
- `orders.version` supports optimistic locking.
- `carts.total_amount`, `orders.total_amount`, and menu/order-item prices use `NUMERIC(38,2)`.
- Migration V2 adds `carts.is_active` and renames `cart_items.menu_id` to `menu_item_id` to match the entity mappings.
- No explicit check constraints enforce positive quantity, positive price, enum validity, or role validity.
- Cart and order tables are created by the initial migration; later migrations keep evolving columns aligned with entity mappings.

Flyway runs migrations automatically on Spring startup. Datasource values come from:

- `POSTGRES_URL`
- `POSTGRES_USER`
- `POSTGRES_PASS`

## 6. Existing Implementations and Incomplete Work

### Implemented

- Spring Boot/Maven application scaffold.
- Flyway initial PostgreSQL schema.
- JPA entities and basic derived repositories.
- MapStruct request/entity/response mapping for customers, restaurants, menu items, and orders.
- Cart request/response DTOs, mapping, repository queries, active-cart service operations, and a migration aligning cart columns with entity mappings.
- Global exceptions for not found, invalid arguments, already deleted, terminal orders, bean validation, and optimistic locking.
- Restaurant listing/search/by-ID.
- Menu lookup service, menu creation/update/toggle/soft-delete.
- Customer profile lookup.
- Direct order creation with server-calculated totals and price snapshots.
- Customer/restaurant order listing and customer/restaurant single-order lookup methods.
- Order status transition validation and optimistic locking.
- Unit tests for menu, restaurant, and order services.

### Incomplete or planned

- `CustomerService.editProfile(...)` returns `null`.
- `CartService` supports active-cart retrieval, creation, and full item-list replacement, but has no HTTP routes or tests.
- No cart-item repository; cart items are managed through the cart aggregate.
- No cart HTTP endpoints or item-specific add/update/remove operations.
- No cart-to-order checkout flow.
- No authentication, authorization, login/registration, password hashing, or JWT validation.
- No menu browsing controller endpoint, despite `MenuService.getMenuFromRestaurant(...)`.
- No public endpoint for `MenuService.getMenuItemById(...)`.
- Customer and restaurant ownership is not derived from an authenticated principal.
- Customer and restaurant controller payloads accept IDs directly instead of enforcing ownership.
- Cart request DTOs declare validation constraints, but no controller currently triggers request-body validation for them.
- The state enum does not implement role-based permission rules or timeout behavior.
- `IllegalStateException` has no explicit global HTTP mapping.
- The customer cancellation guard throws plain `RuntimeException`; only cancellation is checked at controller level.
- Endpoint coverage for menu and order state integration is not present.
- The `contextLoads` test may require PostgreSQL because JPA/Flyway context startup is enabled.
- The OpenAPI UI location and contract have not been verified.

The implementation checklist and future scope are in `docs/next-steps.md` and `docs/scope/scope.md`.

## 7. Build and Testing

### Prerequisites

- JDK 21.
- PostgreSQL 17 or newer for datasource-backed startup.
- `POSTGRES_URL`, `POSTGRES_USER`, and `POSTGRES_PASS`.

Example local datasource:

```sh
export POSTGRES_URL="jdbc:postgresql://localhost:5432/ordertaker"
export POSTGRES_USER="ordertaker"
read -r -s -p "PostgreSQL password: " POSTGRES_PASS
export POSTGRES_PASS
```

### Build and run

```sh
./mvnw clean package
./mvnw spring-boot:run
```

For an existing setup, build without tests with:

```sh
./mvnw -DskipTests package
```

No health endpoint mapping is custom-defined; Spring Boot Actuator is present.

### Tests

```sh
./mvnw test
```

Existing tests:

- `src/test/java/com/rvmc/ordertaker/OrdertakerApplicationTests.java`: Spring context load smoke test.
- `src/test/java/com/rvmc/ordertaker/service/MenuServiceTest.java`: Mockito unit tests for menu lookup/create/update/toggle/soft-delete.
- `src/test/java/com/rvmc/ordertaker/service/OrderServiceTest.java`: Mockito unit tests for order lookup/creation/status updates.
- `src/test/java/com/rvmc/ordertaker/service/RestaurantServiceTest.java`: Mockito unit tests for restaurant lookup, search, and listing.

No existing tests cover controllers, cart behavior, customer profile editing, JWT/authentication, or Flyway migrations.

For Maven builds in the shell, use a JDK 21 installation (including `javac`) and set
`JAVA_HOME` and `PATH` accordingly. The Spring `contextLoads` test additionally
requires the PostgreSQL environment variables to refer to a reachable database
because JPA and Flyway start in the test application context.

## 8. Navigation Reference

| Development task | Relevant files |
|---|---|
| Add an API endpoint | `controller/CustomerController.java`, `controller/RestaurantController.java`, then add service/repository methods |
| Implement business logic | `service/RestaurantService.java`, `service/MenuService.java`, `service/OrderService.java`, `service/CustomerService.java`, `service/CartService.java` |
| Add a database entity | `entity/`, `src/main/resources/db/migration/V1__create_initial_schema.sql`, and a new Flyway migration |
| Add a repository/query | `repository/CustomerRepository.java`, `repository/RestaurantRepository.java`, `repository/MenuItemRepository.java`, `repository/OrderRepository.java`, `repository/UserRepository.java` |
| Create a request/response DTO | `dto/CustomerProfile*.java`, `dto/MenuItem*.java`, `dto/Order*.java`, `dto/RestaurantProfileResponse.java`, `dto/UpdateOrderStatus.java` |
| Implement object mapping | `mapper/CustomerMapper.java`, `mapper/RestaurantMapper.java`, `mapper/MenuItemMapper.java`, `mapper/OrderMapper.java` |
| Add validation | Jakarta constraints in `dto/` records and `@Valid` parameters in `controller/` |
| Handle exceptions | `exception/GlobalExceptionHandler.java` plus exception classes in `exception/` |
| Add or change order status rules | `entity/enums/OrderStatus.java` and `service/OrderService.java` |
| Add cart behavior | `dto/Cart*.java`, `entity/Cart.java`, `entity/CartItem.java`, `repository/CartRepository.java`, `mapper/CartMapper.java`, and `service/CartService.java`; expose through a controller and add tests |
| Change order creation/calculation | `service/OrderService.java`, `entity/Order.java`, `dto/OrderRequest.java`, `dto/OrderItemRequest.java` |
| Change database schema | `src/main/resources/db/migration/` and matching JPA entities |
| Add tests | `src/test/java/com/rvmc/ordertaker/service/MenuServiceTest.java`, `src/test/java/com/rvmc/ordertaker/service/RestaurantServiceTest.java`, `src/test/java/com/rvmc/ordertaker/OrdertakerApplicationTests.java` |
| Review product scope or roadmap | `docs/scope/scope.md`, `docs/next-steps.md`, `README.md` |
