# Testing and Quality

## Verification Command

The project is verified with IntelliJ's Temurin Java 21 JDK:

```sh
JAVA_HOME="$HOME/.jdks/temurin-21.0.12.1" \
PATH="$HOME/.jdks/temurin-21.0.12.1/bin:$PATH" \
./mvnw test
```

The Maven wrapper can be used in CI with the Temurin 21 distribution. The
GitHub Actions workflow runs `./mvnw test`.

## Current Result

The suite currently contains 130 tests. It passes with 0 failures and 0 errors.

## Test Layers

| Layer | Tools | Purpose |
|---|---|---|
| Unit | JUnit 5, Mockito | Services, domain behavior, authentication |
| MVC | `@WebMvcTest`, MockMvc | Routes, JSON, validation, exception mapping |
| Repository | `@DataJpaTest`, H2 | Spring Data queries and persistence behavior |
| Mapper | JUnit 5 | Entity/DTO conversions |
| Integration | Spring Boot test slices, H2 | Checkout and status workflows |
| Context | Spring Boot test | Application startup and configuration |

## Workflow Coverage

- Customer and restaurant-owner registration and login
- Duplicate-email and password-encoding behavior
- JWT parsing, signing, and authenticated-user resolution
- Cart retrieval, creation, replacement, clearing, overflow, and restaurant rules
- Checkout total calculation, price snapshots, unavailable items, and cart deactivation
- Customer cancellation and restaurant order status transitions
- Menu CRUD, availability, soft deletion, and restaurant ownership
- Successful and rejected seeded API status updates
- Repository derived queries
- H2 application startup independent of local PostgreSQL

## Test-Data Strategy

Service tests use mocks and focused objects. MVC tests verify HTTP behavior
without a full database where mocked dependencies are sufficient. Checkout and
status API integration tests seed H2 entities and exercise requests through
MockMvc to verify persistence behavior.
