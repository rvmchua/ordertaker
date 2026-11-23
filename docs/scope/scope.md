# Ordertaker - Scope Documentation
## Purpose
A Spring Boot REST API for browsing restaurants, placing food orders, and tracking them through fulfillment. Built as a portfolio project demonstrating CRUD design, business logic, order-state management, and JWT-secured authentication/authorization.

## Implementation Status
- Restaurant lookup, menu-item CRUD, cart management, checkout, and order status transitions are implemented.
- Checkout and restaurant status updates have seeded H2 API integration coverage.
- Service, domain, MapStruct, `@WebMvcTest`/`MockMvc`, and `@DataJpaTest` coverage is included.
- Authentication, authorization, registration, login, JWT validation, menu browsing, and ownership derivation remain future work.
## Entities
- **User:** id, name, email, passwordHash, role, createdAt
- **Customer:** id, address, userId(FK -> User), isActive
- **Restaurant:** id, name, address, ownerId(FK -> User), isActive
- **MenuItem:** id, restaurantId(FK -> Restaurant), name, description, price, isAvailable
- **Order:** id, customerId(FK -> User), restaurantId(FK -> Restaurant), status, totalAmount, createdAt
- **OrderItem:** id, orderId(FK -> Order), menuItemId(FK -> MenuItem), quantity, priceAtOrderTime
- **Cart:** id, customerId(FK -> User), restaurantId(FK -> Restaurant), status, totalAmount
- **CartItem:** id, cartId(FK -> Order), menuItemId(FK -> MenuItem), quantity, priceAtOrderTime

(priceAtOrderTime is stored on the OrderItem, not read live from MenuItem, so past orders keep their original price even if the menu changes later.)

## Roles
- **CUSTOMER:** browse restaurants and menus, place orders, view own order history
- **RESTAURANT_OWNER:** manage menu items for their own restaurant(s), view and update the status of orders placed at their restaurant
- **ADMIN:** manage all users and restaurants, view all orders (read-only oversight), deactivate accounts
## Core User Flows
- **Customer:** register/log in → browse restaurants → view a menu → build an order → place order → track status
- **Restaurant owner:** log in → manage menu items (CRUD) → view incoming orders → move each order through its status lifecycle 
- **Admin:** log in → view/manage all users and restaurants → deactivate problematic accounts
## Order Status Lifecycle
`PENDING → CONFIRMED → PREPARING → FOR_PICKUP → RECEIVED`
- Restaurant: `PENDING → CONFIRMED` or `CANCELLED`; `CONFIRMED → PREPARING`; `PREPARING → FOR_PICKUP`.
- Customer: cancel `PENDING` or `CONFIRMED`; confirm handoff at `FOR_PICKUP`.
- `RECEIVED` and `CANCELLED` are terminal states.

## Out of Scope (for this version)
- **Payment processing:** orders are assumed cash-on-delivery
- Real-time push/SMS notifications
- **Android client:** separate project after this one ships