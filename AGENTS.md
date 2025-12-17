# Repository Guidelines

## Project Structure & Module Organization
Backend source resides under `src/main/java/cn/sztu/questioncloud` using a layered layout: REST controllers live in `web/rest/v1`, application services plus DTO/VO mappers in `application`, infrastructure adapters in `infrastructure`, and shared enums or constants in `common`. Bootstrapping starts at `QuestioncloudApplication.java`. Configuration defaults to `src/main/resources/application.yml`, with profile overrides such as `application-dev.yml`. Place SQL migrations inside `src/main/resources/sql` and static assets under `src/main/resources/static`. Mirror the same packages in `src/test/java` so every controller, service, and mapper has a matching test location.

## Build, Test, and Development Commands
- `./mvnw clean package` – runs the full verification pipeline and assembles the release JAR.
- `./mvnw test` – executes all JUnit 5 suites; run before pushing any change.
- `./mvnw spring-boot:run` – launches the API on Java 21; append `-DskipTests` for quick manual checks.
- `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` – loads local-only settings from `application-dev.yml`.

## Coding Style & Naming Conventions
Indent Java code with 4 spaces and follow Spring naming: `PascalCase` types, `camelCase` fields or methods, and `UPPER_SNAKE_CASE` constants. Controllers must end in `Controller`; services under `application/*/service` should be `*Service` or `*ServiceImpl`. Keep inbound DTOs separate from outbound VOs. Prefer Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) to cut down boilerplate, and annotate application services, mappers, and adapters with the appropriate Spring stereotype.

## Testing Guidelines
Tests rely on `spring-boot-starter-test` with JUnit 5. Name classes `*Tests` (e.g., `UserServiceTests`) and mirror the main package path in `src/test/java`. Add mapper slice tests for MyBatis persistence, plus `@SpringBootTest` flows for cross-layer scenarios. Always run `./mvnw test` locally and avoid committing code that skips or ignores suites without justification.

## Commit & Pull Request Guidelines
Write concise, imperative commit messages under 72 characters (examples: `完善用户模块权限`, `Add file upload validation`). Reference SQL changes when touching `src/main/resources/sql`. Pull requests should summarize scope, link relevant issues, document manual verification (e.g., `./mvnw test`, smoke API calls), and include screenshots for UI or static asset updates.

## Security & Configuration Tips
Do not commit secrets or environment-specific credentials. Prefer environment variables over hard-coded keys, and document any new configuration in the PR or README. Use profile-specific YAML files to keep local overrides isolated, and confirm new external integrations are feature-flagged or guarded by validation rules.
