# Feature: Authentication and role-based access

**From build-plan:** feature 1
**Build attempt:** 1
**Branch:** feature/authentication-and-role-based-access
**Status:** verified

## Goal

Let customers and restaurant owners register and log in securely, issue a stateless JWT for authenticated API requests, and enforce the role policy in the existing endpoint contract. Passwords must only be stored as BCrypt hashes. Browser clients use an explicitly allowlisted cross-origin API and send the JWT as a Bearer token.

## In scope

- Customer registration at `POST /auth/signup` and restaurant-owner registration at `POST /auth/restaurants/signup`, using the request fields in `docs/endpoint/Endpoint Documentation.pdf`.
- Login at `POST /auth/login` and `POST /auth/restaurants/login`, using email and password.
- Logout at `POST /auth/logout` for either authenticated role.
- Normalize email by trimming and lowercasing it; enforce uniqueness so case variants cannot create separate accounts.
- Enforce passwords of at least 12 characters and no more than 72 UTF-8 bytes. Reject values outside those limits rather than allowing BCrypt to truncate.
- Hash with BCrypt. Never log or return submitted passwords, stored hashes, JWTs, or signing secrets.
- Assign roles on the server only: customer registration creates `CUSTOMER`, and restaurant-owner registration creates `RESTAURANT`. Both account types are active immediately after registration.
- Require a separate login after registration; registration returns `201` without an authenticated session.
- Issue a 15-minute JWT only after successful login. Return it in a JSON response with `accessToken`, `tokenType: "Bearer"`, and `expiresIn: 900`; successful login returns `200`. This intentionally changes the endpoint PDF's existing `204`/no-body login response because that contract cannot deliver a Bearer token.
- Use a required Base64-encoded `JWT_SECRET_BASE64` signing key with at least 32 decoded bytes and HMAC-SHA-256. Fail startup on a missing or invalid key; do not use a committed or generated fallback.
- Keep the JWT stateless: logout returns the documented `204` but does not revoke a copied token. The client must discard its token; a copied token remains valid until its 15-minute expiry. Do not add refresh tokens or server-side revocation.
- Support cross-site browser clients only through exact configured origins in `APP_CORS_ALLOWED_ORIGINS`. Do not enable credentialed CORS or wildcard origins. An empty allowlist permits no cross-origin browser access.
- Use `Authorization: Bearer <token>` for protected requests. Do not use cookies for authentication; because browsers do not automatically attach the Bearer header, this feature does not add CSRF cookies or CSRF tokens. Require JSON for auth request bodies and configure the CORS preflight for the explicit allowed origins and `Authorization`/`Content-Type` headers.
- Follow the documented method/path/role matrix in `docs/endpoint/Endpoint Documentation.pdf`: customer API operations require `ROLE_CUSTOMER`, restaurant-owner operations require `ROLE_RESTAURANT`, and only the restaurant list and restaurant detail GET routes are public. Do not infer ownership from a client-supplied user ID.
- Return `401` for invalid login credentials without revealing whether an email exists, `403` for insufficient role, `400` for malformed or invalid fields, and `409` for an already-registered normalized email.

## Out of scope

- Admin registration, admin account provisioning, and admin authorization. Do not add public admin self-registration; admin account provisioning must be designed with the admin oversight feature.
- Refresh tokens, server-side JWT revocation, token blacklists, or session persistence.
- Frontend implementation, browser automation, and deployment of a particular frontend origin. The API returns a JavaScript-readable token, so client-side token storage and XSS defenses remain the frontend's responsibility.
- Restaurant/menu CRUD, customer profiles beyond the registration data, cart/order operations, and their owner-level data checks.

## Build loop

- Follow `workflow.stepReview: "feature"`: complete the small steps, then present one feature-level review packet.
- `workflow.checkpointCommits` is `disabled`; do not create checkpoint commits.
- Run `./mvnw test` for the configured test command. No project-level `Verify` or browser-test command is configured.

## Build steps

- [x] Add Spring Security and the security foundation: required JWT-key configuration, JWT validation, explicit CORS allowlisting without credentials, default-deny API access, and the documented public/role rules. Keep the application compiling and add focused security-configuration tests. **Done when:** unauthenticated protected API requests are rejected, only documented public list/detail and health reads remain accessible, unlisted origins receive no cross-origin access, valid configured JWTs resolve only their signed role, and `./mvnw test` passes.
- [x] Implement account persistence and registration for the two documented roles, using the existing `User`, `Customer`, and `Restaurant` entities; add the required repository and Flyway schema/constraints. Normalize and uniquely persist email, assign the correct role and active profile server-side, and return no password or hash. **Done when:** each documented signup creates the correct linked account, duplicate email including case variants returns `409`, invalid input returns `400`, accounts are active, and tests plus `./mvnw test` pass.
- [x] Implement email/password login and stateless logout for both account types, including BCrypt verification and the 15-minute JWT. Registration must not log the user in. Document the revised login response. **Done when:** valid login returns `200` with the specified JSON token response, invalid login returns a non-enumerating `401`, logout returns `204` without claiming server-side revocation, expiry is verified with a deterministic clock seam, and tests plus `./mvnw test` pass.
- [x] Complete authorization and CORS verification against the endpoint contract, including public signup/login, role-denied requests, allowed-origin preflight and response behavior, and rejection of unlisted origins. **Done when:** protected methods enforce the documented role matrix, browser clients can read responses only from explicitly allowed origins, and `./mvnw test` passes.

## Files / areas

- `pom.xml` - add Spring Security support; JJWT API/implementation/Jackson dependencies already exist.
- `src/main/resources/application.yaml` - bind the required JWT signing key and configured CORS origin allowlist without shipping secrets or permissive defaults.
- `src/main/java/com/rvmc/ordertaker/entity/User.java`, `entity/Customer.java`, `entity/Restaurant.java`, and `entity/enums/Role.java` - existing account models and role values.
- New authentication, security, repository, service, and request DTO classes under `src/main/java/com/rvmc/ordertaker/`.
- New Flyway migration(s) under `src/main/resources/db/migration/` for the account tables and required constraints.
- Existing cart, menu, and order entity references need explicit lazy `ManyToOne` mappings so Hibernate can initialize the application context; keep these foreign-key relationships aligned with the project domain docs.
- Tests under `src/test/java/com/rvmc/ordertaker/`; an existing Spring Boot context-load test is present.
- `docs/endpoint/Endpoint Documentation.pdf` - source for the documented auth fields, response statuses, and endpoint authorization matrix; do not silently rewrite that contract.

## Data / contracts

- `User` currently has `userId` (`Long`), `username`, `email`, `passwordHash`, `role` (`Role` stored as a string), and `createdAt` (`ZonedDateTime`). `Customer` and `Restaurant` each have a one-to-one association with `User`; keep those links consistent when creating profiles.
- Customer signup fields: `username`, `email`, `password`, `customerName`, `address1`, `address2`.
- Restaurant signup fields: `username`, `email`, `password`, `address1`, `address2`, `restaurantName`.
- Login fields for either role: `email`, `password`. Do not accept a role in the login or signup request to select privileges.
- `Role` currently contains `CUSTOMER` and `RESTAURANT`; retain `ROLE_CUSTOMER` and `ROLE_RESTAURANT` authorities through its existing `getPrefixedName()` behavior.
- `Customer.isActive` and `Restaurant.isActive` are nullable `Boolean` fields today; newly registered profiles must persist `true`.
- Bearer access token: `Authorization: Bearer <token>`, 15-minute expiry. Claims include the stable user ID as subject, server-assigned role, issue time, and expiry; never trust unsigned or expired claims.
- Successful signup is `201` with no response body and does not issue a token. Successful login is `200` with `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 900 }`. Logout is `204` and is client-side token disposal only; no server-side revocation is claimed. Invalid credentials use the same `401` response regardless of whether the email exists.
- CORS must use explicit exact origins without credentials or wildcards. No origin values are established in the plans, so cross-origin browser access remains closed until an operator configures the allowlist.
- Auth request bodies must be JSON. Bearer authentication is not ambient browser authority, so no CSRF token protocol is added; protected requests require the explicit Authorization header.
- BCrypt input maximum is 72 UTF-8 bytes. Apply limits before hashing and test multibyte values at the byte boundary.

## Testing

- Add service tests for normalized email uniqueness, role assignment, immediate activation, password hashing and matching, and rejection of passwords below 12 characters or above 72 UTF-8 bytes.
- Add authentication/security tests for successful and failed login, the JSON response contract, JWT role/expiry validation, role-based denial, JSON-only auth requests, and explicit CORS allowlist/preflight behavior.
- Use a deterministic `Clock` seam for token expiry tests; do not sleep or rely on wall-clock timing.
- Run `./mvnw test`. Do not claim browser behavior or live PostgreSQL behavior from unit/security tests. No browser-test command is configured.

## Notes for the AI

- Repository inspection found existing account entities, the `CUSTOMER`/`RESTAURANT` enum, JJWT dependencies, and an endpoint PDF, but no authentication controllers, repositories, Spring Security dependency/configuration, or Flyway migration files.
- The project plan and endpoint PDF specify BCrypt; add only the Spring Security dependency needed for password encoding and request authorization. Reuse JJWT rather than adding another JWT library.
- Build authorization from the existing endpoint PDF's method/path/role matrix. Security authentication identifies the actor; future feature code must still enforce resource ownership in repository/service queries.
- Keep the application fail-closed when JWT key or CORS settings are absent. Do not commit secrets or make cross-origin access work through a wildcard.
- Preserve the user's existing uncommitted onboarding edits in `.gitignore` and `AGENTS.md`; do not stage or include them in this feature.
- The user approved minimal `@AllArgsConstructor` annotations for existing Lombok builder entities that blocked a clean Java compile, and explicit `ManyToOne` joins on cart/menu/order entity references that blocked Hibernate startup. These are prerequisite repairs to make the current app build and boot, not new user-facing feature scope.
- The review decisions for this spec are: stateless JWT, 15-minute expiry, Bearer token in the login JSON response, cross-site browser access through an explicit CORS allowlist, 12-character minimum, BCrypt's 72-byte maximum, normalized case-insensitive email identity, separate login after signup, and immediate activation for both account types. Bearer tokens avoid third-party-cookie restrictions but are readable by client JavaScript; frontend storage and XSS defenses are not part of this backend feature.


<!-- blueprint:completion {"schemaVersion":2,"specBytes":12134,"specSha256":"e0a1da5fcb0d84a60f2f38d1b17810787a23617e5aeecba3a9e53a4c457ca3bf","branch":"refs/heads/feature/authentication-and-role-based-access","head":"edab10e4f391c828086711278ed927449a34f67c","baseRef":"main","baseCommit":"4f9060b03758339c69678e10166d6a29d8fb788e","sourceTree":"ed794786e02d169d2f058f2ddfc1a7397b0d473c","landing":"local-merge","absentOptional":[]} -->

# Independent Review

**Status:** passed
**Target commit:** edab10e4f391c828086711278ed927449a34f67c
**Base commit:** 4f9060b03758339c69678e10166d6a29d8fb788e
**Base ref:** main
**Spec hash:** e0a1da5fcb0d84a60f2f38d1b17810787a23617e5aeecba3a9e53a4c457ca3bf
**Spec snapshot:** blueprint/.state/review-specs/edab10e4f391c828086711278ed927449a34f67c-e0a1da5fcb0d84a60f2f38d1b17810787a23617e5aeecba3a9e53a4c457ca3bf.md
**Prepared by:** copilot
**Builder model:** unknown (runtime did not expose exact model)
**Requested reviewer:** copilot
**Requested model:** runtime default (exact model not known until reviewer starts)
**Requested execution:** automatic
**Requested at:** 2026-10-15T10:23:00Z
**Workflow:** regular
**Check required:** no
**Reviewer adapter:** copilot
**Reviewer model:** gpt-6-luna
**Reviewer context:** fresh session
**Actual execution:** manual
**Reviewed at:** 2026-10-15T10:41:00Z
**Scope:** current
**Lenses:** quality, security, performance, tests
**Verdict:** passed
**Check result:** not-required

## Commands

- `./mvnw test`: passed (19 tests; 0 failures, errors, or skipped).
- `git diff --check 4f9060b03758339c69678e10166d6a29d8fb788e..edab10e4f391c828086711278ed927449a34f67c`: passed.

## Evidence

- Reviewed the complete `main..edab10e4f391c828086711278ed927449a34f67c` delta and the verified feature spec; target, merge base, spec hash, and snapshot hash matched the pending request.
- The configured Maven suite passed, covering auth registration/login, JWT expiry, role denial, logout semantics, and CORS preflight.
- Automatic isolated review was not available in this Copilot runtime; completed from this fresh reviewer session using the allowed manual fallback. The user reported that Auto routed the reviewer to `gpt-6-luna`.

## Findings

- F-01 [P2] Fail-closed security configuration lacks direct tests.

## Remaining risk

- Live PostgreSQL behavior was not exercised; integration tests ran against H2. No browser/E2E verification command is configured.


## Try it now

With the API running via `./mvnw spring-boot:run`, its required JWT key and database configured, and an existing test account available, submit a login request:

```sh
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  --data '{"email":"<existing-test-email>","password":"<existing-test-password>"}'
```

Replace the placeholders with that account's JSON-escaped credentials.

Expected: HTTP 200 and a JSON response containing a non-empty `accessToken`, `tokenType` equal to `Bearer`, and `expiresIn` equal to `900`.

This proves: the configured customer login endpoint returns its documented short-lived token response for a valid account. It does not verify CORS or the full authorization matrix.
