# OrderTaker

OrderTaker is a Java 21 REST API for a local restaurant pickup workflow:
customers register and authenticate, browse restaurants and menus, maintain a
cart, checkout, and cancel eligible orders; restaurant owners manage their menu
and advance or cancel incoming orders.

The project is designed as a portfolio and capstone-ready backend, with a
layered Spring architecture, relational persistence, state-transition rules,
JWT security, integration tests, Docker Compose deployment, and explicit
documentation of implemented and deferred behavior.

## Documentation

- [Project and capstone documentation](docs/PROJECT_DOCUMENTATION.md) — source of truth
- [Architecture and codebase map](docs/CODEBASE_MAP.md)
- [Database schema](docs/DATABASE_SCHEMA.md)
- [API contract](docs/API.md)
- [Testing strategy](docs/TESTING.md)
- [Roadmap and known gaps](docs/ROADMAP.md)
- [Entity relationship diagram](docs/uml/ERD.puml)
- [Order state diagram](docs/uml/State%20Diagram.puml)

## Technology

- Java 21 and Spring Boot 4.1.1
- Spring MVC, Spring Security, and Spring Data JPA
- PostgreSQL 17 and Flyway
- JJWT 0.13.0
- MapStruct 1.6.3 and Lombok
- JUnit 5, Mockito, MockMvc, H2, and Maven
- Docker and Docker Compose
- Springdoc OpenAPI

## Quick Start

Prerequisites: JDK 21, Maven (the included wrapper may be used), PostgreSQL, and
Docker if Compose is preferred.

Set PostgreSQL values:

```sh
export POSTGRES_URL="jdbc:postgresql://localhost:5432/ordertaker"
export POSTGRES_USER="ordertaker"
read -r -s -p "PostgreSQL password: " POSTGRES_PASS
printf '\n'
export POSTGRES_PASS
```

Use the project JDK and run the application:

```sh
JAVA_HOME="$HOME/.jdks/temurin-21.0.12.1" \
PATH="$HOME/.jdks/temurin-21.0.12.1/bin:$PATH" \
./mvnw spring-boot:run
```

Run all tests:

```sh
JAVA_HOME="$HOME/.jdks/temurin-21.0.12.1" \
PATH="$HOME/.jdks/temurin-21.0.12.1/bin:$PATH" \
./mvnw test
```

The current suite contains 130 tests and passes. Once running locally, the
health endpoint is at `/actuator/health`, OpenAPI JSON at `/v3/api-docs`, and
Swagger UI at `/swagger-ui.html`.

## Docker Compose

Secrets are supplied by the shell and must not be committed:

```sh
export POSTGRES_USER=ordertaker
read -r -s -p "PostgreSQL password: " POSTGRES_PASS
printf '\n'
export POSTGRES_PASS
read -r -s -p "JWT signing secret (at least 32 characters): " JWT_SECRET
printf '\n'
export JWT_SECRET

docker compose up --build -d
curl --fail http://localhost:8080/actuator/health
```

## Repository Layout

```text
src/main/java/com/rvmc/ordertaker/
  controller/       HTTP routing
  service/          business workflows
  repository/       Spring Data persistence
  entity/           JPA domain model
  dto/              API records
  mapper/           MapStruct conversions
  security/         JWT and authenticated-user handling
  exception/        domain exceptions and HTTP mappings
src/main/resources/
  db/migration/     versioned Flyway schema changes
src/test/java/      unit and integration tests
docs/               maintained project documentation
```
