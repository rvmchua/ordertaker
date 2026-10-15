# AGENTS.md

Project guide for OrderTaker, the food-ordering backend.

## Project summary

OrderTaker is a Java 21 Spring Boot application for a food-ordering platform. It exposes a REST API for customer and restaurant flows, including authentication, menu management, cart creation, order placement, and lifecycle tracking.

## Stack

- Java 21
- Spring Boot 4.x
- Spring Web MVC, Spring Data JPA, Spring Security
- PostgreSQL via JDBC / JPA
- Flyway for schema changes
- Maven

## Commands

- Start locally: `./mvnw spring-boot:run`
- Build: `./mvnw clean package`
- Run tests: `./mvnw test`
- Verify: no project-level Verify command is configured yet

## Working conventions

- Keep business rules in the service layer and validation at the API boundary.
- Prefer DTOs over exposing JPA entities directly.
- Use Spring Data repositories for persistence and Flyway for schema evolution.
- Validate request bodies with Bean Validation and return consistent error responses.
- Recompute server-side totals and identity checks instead of trusting client input.
- Keep changes scoped and reviewable.

## Project structure

- `src/main/java` contains the application code
- `src/main/resources` holds configuration and resource files
- `src/test` is where tests belong when the feature needs them

## Verification and quality

- Prefer the smallest command that checks the changed behavior.
- Use `./mvnw test` as the default project test command until a higher-level verify step is added.
- Do not add broad refactors, framework churn, or unrelated feature work to a focused change.
