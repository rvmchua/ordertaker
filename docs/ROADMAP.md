# Scope and Roadmap

## Implemented MVP

- Customer and restaurant-owner registration
- Password hashing with BCrypt
- JWT login and stateless bearer-token validation
- Customer and restaurant role authorization
- Restaurant listing and name search
- Restaurant owner profile lookup, partial editing, email uniqueness, and soft deletion
- Admin restaurant reactivation and hard deletion
- Public menu browsing
- Restaurant-owned menu CRUD and soft deletion
- Active-cart creation, retrieval, replacement, and clearing
- Transactional checkout with calculated totals and price snapshots
- Order and order-item persistence
- Customer cancellation rules
- Restaurant order status transitions
- Optimistic locking and centralized exception handling
- PostgreSQL/Flyway configuration and Docker Compose deployment
- OpenAPI/Swagger UI dependency
- Unit, MVC, repository, context, and integration tests

## Product Scope

The system focuses on a single pickup restaurant flow rather than delivery,
payments, or a distributed platform.

Roles:

- `CUSTOMER`: browse, maintain a cart, place and cancel eligible orders
- `RESTAURANT`: owner account that manages a restaurant menu and incoming orders
- `ADMIN`: performs backend administrative workflows, including restaurant deletion

## Order Lifecycle

```text
PENDING -> CONFIRMED -> PREPARING -> FOR_PICKUP -> RECEIVED
   \          \
    \          -> CANCELLED
     -> CANCELLED
```

Customers may cancel `PENDING` or `CONFIRMED`. They may confirm a handoff by
changing `FOR_PICKUP` to `RECEIVED`. Restaurants may:

- `PENDING -> CONFIRMED`
- `PENDING -> CANCELLED`
- `CONFIRMED -> PREPARING`
- `PREPARING -> FOR_PICKUP`

`RECEIVED` and `CANCELLED` are terminal.

## Known Gaps

- Item-specific cart add/update/remove routes are not exposed; the cart uses full
  item-list replacement.
- JWT refresh, logout/revocation, and token blacklisting are deferred.
- Pickup timeout scheduling and customer bans are out of scope.
- No payments, SMS, push notification, or Android client is included.

## Future Enhancements

1. Add more account lifecycle administration workflows.
2. Add pagination, filtering, and sorting for menus and orders.
3. Add refresh/revoke support and configurable token audiences.
4. Add scheduled pickup timeout and escalation policies.
5. Add payment-provider abstraction if the product requires payment.
6. Add an Android or web client.
7. Add observability, metrics, tracing, and production-grade secret management.
