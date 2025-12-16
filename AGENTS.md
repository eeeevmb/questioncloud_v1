# Repository Guidelines

## Project Structure & Module Organization
Source lives under `src/main/java/cn/sztu/questioncloud` and mirrors the layered layout: controllers stay in `web/rest/v1`, application services plus DTO/VO mappers in `application`, infrastructure adapters and persistence code in `infrastructure`, and shared enums/constants beneath `common`. Bootstrapping happens in `QuestioncloudApplication.java`. Keep configuration in `src/main/resources/application.yml`, SQL migrations in `src/main/resources/sql`, and static assets in `src/main/resources/static`. Tests should mirror the main tree inside `src/test/java`, reusing the same package hierarchy for controllers, services, and mappers.

## Build, Test, and Development Commands
- `./mvnw clean package` — runs the full verification pipeline and produces the release JAR.
- `./mvnw test` — executes all JUnit 5 suites; run before sending patches.
- `./mvnw spring-boot:run` — launches the API with Java 21; add `-DskipTests` for faster local loops.
Set `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` to pick up `application-dev.yml`.

## Coding Style & Naming Conventions
Stick to 4-space indentation and standard Spring naming: `PascalCase` classes, `camelCase` members, `UPPER_SNAKE_CASE` constants. Controllers end with `Controller`, services live in `application/*/service` as `*Service` or `*ServiceImpl`. Separate inbound DTOs from outbound VOs. Prefer Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) over boilerplate; annotate mappers and services with Spring stereotypes where appropriate.

## Testing Guidelines
Use JUnit 5 via `spring-boot-starter-test`. Mirror package paths in `src/test/java` and suffix classes with `Tests` (e.g., `UserServiceTests`). Add mapper slice tests for MyBatis repositories and `@SpringBootTest` coverage for flows that cross web, application, and infrastructure layers. Ensure `./mvnw test` passes locally before opening a PR.

## Commit & Pull Request Guidelines
Write concise, imperative commit messages under 72 characters (e.g., `完善用户模块权限`, `Add file upload validation`). Mention SQL changes when touching `src/main/resources/sql`. Pull requests should summarize scope, note manual verification (such as `./mvnw test`, smoke calls), and include screenshots for static asset updates.

## Security & Configuration Tips
Never commit secrets. Extend `application.yml` with profile-specific files like `application-dev.yml` for local overrides. Document any new external keys or validation rules in the PR or README updates, and prefer environment variables for sensitive configuration.
