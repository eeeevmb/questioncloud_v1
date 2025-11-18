# Repository Guidelines

## Project Structure & Module Organization
Code stays under `src/main/java/cn/sztu/questioncloud`. Controllers live in `web/rest/v1`, application services and DTO/VO mappers in `application`, and persistence plus adapters inside `infrastructure`. Shared enums and constants belong to `common`. `QuestioncloudApplication.java` boots the service. SQL migrations sit in `src/main/resources/sql`, configuration in `src/main/resources/application.yml`, and static assets in `src/main/resources/static`. Mirror the package tree in `src/test/java` for tests.

## Project Progress
- **User module**: Registration, login/logout, avatar upload & retrieval, and basic profile APIs are live. RabbitMQ integration emits `user.registered` events that provision default collections.
- **Question module**: Collection CRUD and LaTeX-based question authoring work end-to-end with MyBatis repositories. Query endpoints remain stubbed (`QuestionQueryController`), and type validation plus collection-item concurrency are TODOs.
- **Infrastructure**: Local file storage, Sa-Token security, and Snowflake ID generation are wired; MyBatis mappers cover user and question entities. Only the skeleton `QuestioncloudV1ApplicationTests` exists, leaving mapper/service coverage outstanding. Exam package is still empty.

## Build, Test, and Development Commands
- `./mvnw clean package` builds the release JAR and runs the full verification pipeline.
- `./mvnw spring-boot:run` starts the API with Java 21; add `-DskipTests` for fast local loops.
- `./mvnw test` runs all automated checks; execute before submitting changes.

## Coding Style & Naming Conventions
Use 4-space indentation and standard Spring naming: classes `PascalCase`, members `camelCase`, constants `UPPER_SNAKE_CASE`. Controllers end with `Controller`; services in `application/*/service` follow `*Service` or `*ServiceImpl`. Prefer Lombok annotations (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) over boilerplate. Keep inbound requests and outbound responses separated by DTO and VO classes.

## Testing Guidelines
Rely on JUnit 5 via `spring-boot-starter-test`. Name test classes with the `Tests` suffix and align package paths with their targets. Add mapper slice tests for MyBatis components, plus `@SpringBootTest` cases when flows touch web, application, and infrastructure layers. Always confirm `./mvnw test` passes before pushing.

## Commit & Pull Request Guidelines
Write concise, imperative commit messages in English or Chinese under 72 characters (e.g., `完善用户模块权限`, `Add file upload validation`). Note SQL changes when editing `src/main/resources/sql`. Pull requests should outline the change scope, record manual verification (`./mvnw test`, smoke requests), and attach screenshots when altering files in `src/main/resources/static`.

## Security & Configuration Tips
Keep secrets out of the repo. Extend `application.yml` with profile files such as `application-dev.yml`, then start locally with `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run`. Document any new external keys or validation rules in the accompanying PR or README updates.
