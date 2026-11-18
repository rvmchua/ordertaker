# OrderTaker MVP Roadmap

## MVP target

Deliver one coherent, deployable food-ordering API with:

- Customer and restaurant-owner registration and authentication.
- A restaurant menu that owners can manage and customers can browse.
- A cart that supports adding, updating, and removing items.
- Checkout from the cart with server-calculated totals and saved order prices.
- Order status updates with rules for customer and restaurant actions.
- PostgreSQL persistence, request validation, focused tests, API documentation, and setup instructions.

Payments, real-time notifications, Android, microservices, event-driven infrastructure, and Kubernetes are outside the MVP.

## Definition of done

A feature is complete when its main request works end-to-end, invalid input has a deliberate response, important behavior has a focused test, and the API documentation is updated when the contract changes. Once those criteria are met, defer unrelated improvements to **Future enhancements**.

## Current foundation

- [x] Java 21 Spring Boot project with Maven.
- [x] Initial JPA entities, repositories, DTOs, and mappers.
- [x] PostgreSQL environment configuration and initial Flyway migration.
- [x] Local build and database setup instructions.
- [ ] Verify application startup and context tests against PostgreSQL.

## Implementation slices

Build and test the core workflows with seeded local/test customer and restaurant records. Do not expose the API outside a trusted local environment until the identity and access-control slice is complete.

### 1. Restaurant and menu catalog

- [x] Implement restaurant profile lookup and expose a GET endpoint.
- [x] Expose the existing restaurant menu lookup through a GET endpoint.
- [x] Add a menu-item creation endpoint with request validation.
- [x] Add a menu-item update endpoint and test one successful update.
- [x] Add a separate availability-toggle endpoint.
- [x] Add a soft-delete endpoint and verify deleted items are hidden from active menus.

### 2. Customer cart

- [x] Add repository queries to find and check for a customer's active cart.
- [x] Implement active-cart retrieval and creation with menu-item/restaurant validation.
- [x] Implement replacing the active cart's item list, including clearing it with an empty list.
- [ ] Expose cart retrieval, creation, and update through controller routes with request validation.
- [ ] Add cart service tests for missing customer/cart, restaurant mismatch, duplicate quantities, overflow, and clearing.
- [ ] Verify the customer cannot create a second active cart with a test.
- [ ] Add item-level add/update/remove operations and test missing-item behavior if those operations remain part of the desired API.

### 3. Checkout from cart

- [ ] Implement checkout from the active cart; the current order endpoint still creates orders directly from request item IDs.
- [ ] Reject checkout when the cart is empty.
- [ ] Recheck that every cart item is still available.
- [ ] Calculate the total from current database prices.
- [ ] Save each item's checkout price as `priceAtOrderTime`.
- [ ] Persist the order and order items in one transaction.
- [ ] Clear the cart only after a successful order save.
- [ ] Test that failed checkout leaves the cart unchanged.
- [ ] Return the created order response and verify it in an API integration test.

### 4. Order processing

- [ ] Define and test the allowed `OrderStatus` transitions.
- [ ] Define which states permit customer cancellation and restaurant status updates.
- [ ] Return `409 Conflict` for an invalid transition.
- [ ] Test one successful and one rejected status update through the API using seeded records.

### 5. Identity and access control

- [ ] Add Spring Security and a minimal stateless API configuration.
- [ ] Add BCrypt password encoding and a focused password-encoding test.
- [ ] Implement customer registration and reject duplicate email addresses.
- [ ] Implement restaurant-owner registration with the restaurant role.
- [ ] Implement email/password login and return a signed JWT.
- [ ] Validate bearer tokens on protected requests.
- [ ] Derive the acting customer or restaurant from the authenticated principal, not request IDs.
- [ ] Restrict menu changes to the owning restaurant.
- [ ] Restrict cart and order access to their owning customer; restrict restaurant order updates to the owning restaurant.
- [ ] Add tests proving unauthenticated requests are rejected and cross-owner access is denied.

### 6. Release readiness

- [ ] Map invalid request data to `400 Bad Request` with a consistent JSON body.
- [ ] Verify the OpenAPI UI and document its local URL.
- [ ] Align the endpoint contract and [scope.md](./scope/scope.md) with the pickup workflow.
- [ ] Follow the setup instructions from a clean checkout.
- [ ] Add a Java 21 CI build that runs the tests.
- [ ] Confirm authentication and ownership checks are in place before exposing the API beyond a trusted local environment.
- [ ] Choose a deployment target and configure production secrets outside the repository.
- [ ] Deploy and verify the health endpoint.
- [ ] Smoke-test registration, menu browsing, checkout, and order status on the deployed API.

## Decisions to finalize

- Whether restaurant deletion needs an admin role.
- JWT expiration; defer token refresh/revocation unless needed for the MVP.
- Whether a pickup timeout and customer ban are in scope; default to deferring them.
- Which order states allow customer cancellation and restaurant updates.

## Future enhancements

- Pickup timeout scheduling and customer-ban policy.
- Token refresh/revocation and administrative workflows.
- Payment processing, notifications, and an Android client.
- Infrastructure beyond a single deployable service.
