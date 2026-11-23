# OrderTaker MVP Roadmap

## MVP target

Deliver one coherent, deployable food-ordering API with:

- Customer and restaurant-owner registration and authentication.
- A restaurant menu that owners can manage and customers can browse.
- A cart that supports adding, updating, and removing items through its item-list update operation.
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
- [x] Add a Spring application-context test (`contextLoads`).
- [x] Isolate the context test with H2 so it runs independently of local PostgreSQL.
- [ ] Run the context test against a reachable PostgreSQL database and verify application startup.
- [x] Add service, domain, mapper, `@WebMvcTest`/`MockMvc`, and `@DataJpaTest` coverage.

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
- [x] Expose cart retrieval, creation, and update through controller routes with request validation.
- [x] Add cart service tests for missing customer/cart, restaurant mismatch, duplicate quantities, overflow, and clearing.
- [x] Verify the customer cannot create a second active cart with a test.

Progress note: `GET`, `POST`, and `PUT /api/v1/customers/cart` expose active-cart retrieval, creation, and full item-list replacement. Item-specific add/update/remove routes remain out of scope; identity-based ownership is still part of the authentication slice.

### 3. Checkout from cart

- [x] Implement checkout from the active cart.
- [x] Reject checkout when the cart is empty.
- [x] Recheck that every cart item is still available.
- [x] Calculate the total from current database prices.
- [x] Save each item's checkout price as `priceAtOrderTime`.
- [x] Persist the order and order items in one transaction.
- [x] Deactivate the active cart only after a successful order save.
- [x] Test that failed order persistence does not deactivate the cart.
- [x] Return the created order response from the checkout service and endpoint.
- [x] Add focused controller edge-case coverage with `@WebMvcTest` and `MockMvc`.
- [x] Verify successful checkout and cart deactivation through the checkout API.
- [x] Verify unavailable-item, missing-customer, missing-cart, and empty-cart checkout edge cases.

Progress note: checkout builds the order from the active cart, saves current menu prices, and deactivates the cart within the transactional service only after the order save succeeds. Successful checkout and unavailable, empty, missing-cart, and missing-customer cases now have seeded API integration coverage.

### 4. Order processing

- [x] Implement sequential `OrderStatus` transitions and reject changes from terminal states.
- [x] Cover a valid and an invalid transition in `OrderServiceTest`.
- [x] Add focused coverage for the full transition matrix, including cancellation and terminal-state edges.
- [x] Customers may cancel only `PENDING` or `CONFIRMED`; the customer route only accepts `CANCELLED`.
- [x] Document role-specific cancellation rules and restaurant status-update permissions; add the restaurant update route.
- [x] Return `409 Conflict` for an invalid transition.
- [x] Test one successful and one rejected status update at the service layer.
- [x] Add controller success and rejection edge-case coverage with `@WebMvcTest` and `MockMvc`.
- [x] Test a successful restaurant status update through the API using seeded records.
- [x] Test a rejected restaurant status update through the API using seeded records.

Progress note: the transition matrix, role-specific cancellation rules, restaurant update route, `409 Conflict` mapping, and seeded successful/rejected restaurant status API integration tests are implemented.

### 5. Identity and access control

- [ ] Add Spring Security and a minimal stateless API configuration.
- [x] Store the password hash field on `User`.
- [ ] Encode passwords with BCrypt and add a focused password-encoding test.
- [x] Provide the `User` email field and repository.
- [ ] Implement customer registration and explicitly reject duplicate email addresses.
- [x] Define the `RESTAURANT` role and the `User`-to-restaurant owner relationship.
- [ ] Implement restaurant-owner registration with the agreed role name (`RESTAURANT_OWNER` in scope documentation).
- [x] Include JJWT dependencies for token support.
- [ ] Implement email/password login and return a signed JWT.
- [ ] Validate bearer tokens on protected requests.
- [ ] Derive the acting customer or restaurant from the authenticated principal, not request IDs.
- [ ] Restrict menu changes to the owning restaurant.
- [ ] Restrict cart and order access to their owning customer; restrict restaurant order updates to the owning restaurant.
- [ ] Add tests proving unauthenticated requests are rejected and cross-owner access is denied.

### 6. Release readiness

- [x] Map Bean Validation and `IllegalArgumentException` failures to `400 Bad Request` JSON responses.
- [ ] Use a consistent JSON error shape and cover other invalid-request/binding failures.
- [x] Include the Springdoc OpenAPI UI dependency.
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
