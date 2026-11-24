# OrderTaker Codebase Map

This is the implementation map for developers navigating the repository.
The narrative source of truth is
[PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md).

## Architecture

```text
Controller -> Service -> Repository -> JPA Entity -> PostgreSQL
                         |
                         +-> MapStruct DTO mapper
```

Cross-cutting components handle Spring Security, JWT authentication, Bean
Validation, centralized exception mapping, and Flyway migrations.

## Package Map

| Package/path | Responsibility |
|---|---|
| `controller/` | REST routes, HTTP status, request validation, authenticated principal |
| `service/` | Business workflows and repository orchestration |
| `repository/` | Spring Data JPA CRUD and derived queries |
| `entity/` | JPA persistence model and domain rules |
| `entity/enums/` | Roles and order states |
| `dto/` | API request and response records |
| `mapper/` | MapStruct entity/DTO conversion |
| `security/` | JWT signing/parsing, filter, security chain, principal resolution |
| `exception/` | Domain exceptions and global HTTP mappings |
| `src/main/resources/db/migration/` | Flyway schema migrations |
| `src/test/java/com/rvmc/ordertaker/` | Unit, MVC, repository, mapper, and integration tests |

## Controllers

### AuthController

- `POST /api/v1/auth/register/customer`
- `POST /api/v1/auth/register/restaurant-owner`
- `POST /api/v1/auth/login`

Registration and login use `AuthService` and return `AuthResponse`.

### CustomerController

- Own profile lookup and update.
- Own active cart retrieval, creation, and full-list replacement.
- Own order listing/detail, checkout, and cancellation.

The controller resolves the customer through
`AuthenticatedUserService`; it does not trust the customer ID as a cross-owner
identity source.

### RestaurantController

- Public restaurant listing/search/detail/menu lookup.
- Authenticated owner profile lookup, partial update, and soft deletion.
- Authenticated owner menu creation, update, availability toggle, and
  soft deletion.
- Authenticated owner order listing and status update.

Menu and order mutations are rechecked against the authenticated restaurant.

### AdminController

- Administrative restaurant reactivation and hard deletion.
- Requires `ROLE_ADMIN`; restaurant owner accounts cannot hard-delete or reactivate
  restaurants.

## Services

| Service | Key behavior |
|---|---|
| `AuthService` | Email normalization, BCrypt, registration, login, JWT response |
| `AuthenticatedUserService` | Resolves customer or restaurant from Spring authentication |
| `CustomerService` | Profile lookup and mutable profile updates |
| `RestaurantService` | Restaurant listing, search, lookup, profile updates, owner soft deletion, admin reactivation, and admin hard deletion |
| `MenuService` | Menu lookup, CRUD, availability, ownership checks, soft deletion |
| `CartService` | Active-cart operations, quantity merging, item/restaurant validation |
| `OrderService` | Order lookup, checkout, cancellation, role-specific status updates |

## Entities and Relationships

```text
User 1 ---- 0..1 Customer
User 1 ---- 0..1 Restaurant
Restaurant 1 ---- 0..* MenuItem
Customer 1 ---- 0..1 active Cart
Cart 1 ---- 0..* CartItem ---- 1 MenuItem
Customer 1 ---- 0..* Order
Restaurant 1 ---- 0..* Order
Order 1 ---- 1..* OrderItem ---- 1 MenuItem
```

### Domain Rules

- `Cart.replaceCartItems(...)` replaces the aggregate collection and relies on
  JPA orphan removal.
- `Order.create(cart)` rejects invalid carts, snapshots menu prices, and creates
  a `PENDING` order.
- `OrderStatus` defines customer, restaurant, and general transition rules.
- `@Version` on `Order` enables optimistic locking.

## Security

`SecurityConfiguration` is stateless:

- Authentication routes are public.
- Health and public restaurant/menu `GET` routes are public.
- Customer routes require `ROLE_CUSTOMER`.
- Restaurant mutation and order routes require `ROLE_RESTAURANT`.

`JwtAuthenticationFilter` reads `Authorization: Bearer <token>`, validates the
token, resolves the user, and installs its role authority.

## Persistence

Spring Data JPA repositories expose CRUD and derived queries for users,
customers, restaurants, menu items, carts, and orders.

Flyway migrations:

1. `V1__create_initial_schema.sql`
2. `V2__add_active_cart_state.sql`
3. `V3__add_unique_user_email.sql`
4. `V4__store_order_status_as_name.sql`

See [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md).

## Test Map

| Path | Coverage |
|---|---|
| `service/` | Workflow success/failure and Mockito dependencies |
| `entity/` | Cart/order factories and status transitions |
| `controller/` | Routes, validation, JSON, ownership, checkout, status API |
| `repository/` | Derived queries with `@DataJpaTest` and H2 |
| `mapper/` | MapStruct conversions |
| `security/` | JWT, password encoding, principal resolution |
| `OrdertakerApplicationTests` | Spring context startup |

The latest verified suite result is 144 tests, 0 failures, 0 errors.

## Where to Change Things

| Task | Start here |
|---|---|
| Add an HTTP route | Matching controller and `service/` |
| Add business behavior | Matching service and relevant domain entity |
| Add a query | Matching repository and service test |
| Change persisted fields | JPA entity plus a new Flyway migration |
| Change request/response shape | `dto/` plus matching MapStruct mapper |
| Change access rules | `security/SecurityConfiguration.java` |
| Change JWT behavior | `security/JwtService.java` and filter |
| Change order lifecycle | `entity/enums/OrderStatus.java` and `OrderService` |
| Change cart behavior | `Cart`, `CartItem`, `CartService`, and cart mapper |
| Change checkout | `CartService`, `Order.create(...)`, and `OrderService` |
| Change HTTP errors | `exception/GlobalExceptionHandler.java` |
| Add tests | Adjacent package under `src/test/java/com/rvmc/ordertaker/` |

## Documentation Index

- [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md) — project and capstone
  reference
- [API.md](API.md) — live API contract
- [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) — schema and migrations
- [TESTING.md](TESTING.md) — test strategy and result
- [ROADMAP.md](ROADMAP.md) — scope and known gaps
