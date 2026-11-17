# GitHub Copilot Repository Instructions

## Codebase Understanding

Use `docs/CODEBASE_MAP.md` as the primary architectural reference.

Before working on tasks involving multiple files, unfamiliar functionality, or architectural changes:

1. Read `docs/CODEBASE_MAP.md`.
2. Identify the relevant classes, methods, and file paths.
3. Inspect the actual source code before proposing or implementing changes.
4. Use workspace search when the documentation does not provide enough information.

Do not repeatedly explore the entire repository when the documentation already identifies the relevant files.

## Development Guidelines

- Follow existing architectural patterns and naming conventions.
- Keep controllers, services, repositories, entities, and DTOs properly separated.
- Avoid introducing unnecessary dependencies.
- Avoid duplicating existing functionality.
- Prefer minimal, targeted modifications.
- Explain significant architectural decisions.

## Documentation Maintenance

Update `docs/CODEBASE_MAP.md` when significant architectural changes occur.

Update `docs/DATABASE_SCHEMA.md` when database entities or relationships change.

Do not regenerate documentation unnecessarily.

## Before Finishing a Task

- Summarize modified files.
- Explain important implementation decisions.
- Run relevant tests when possible.
- Report unresolved issues.
- Identify documentation that needs updating.