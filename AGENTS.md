# Repository Guidelines

## Project Structure & Module Organization
This Spring Boot monolith lives under `src/main/java/cn/sztu/questioncloud`. Controllers belong to `web/rest/v1`, application services plus DTO/VO mappers to `application`, infrastructure adapters to `infrastructure`, and shared enums or constants to `common`. Bootstrapping starts at `QuestioncloudApplication.java`. YAML config resides in `src/main/resources`, with `application.yml` as default and profile overrides such as `application-dev.yml`. Keep SQL migrations inside `src/main/resources/sql` and static assets under `src/main/resources/static`. Tests mirror the production tree inside `src/test/java`, so every controller, service, and mapper keeps matching packages for easy discovery.

## Build, Test, and Development Commands
- `./mvnw clean package` – cleans, runs the full verification pipeline, and produces the release JAR.
- `./mvnw test` – executes all JUnit 5 suites; run it before each push or pull request.
- `./mvnw spring-boot:run` – launches the API on Java 21; add `-DskipTests` for quick smoke checks.
- `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` – starts the app with local-only configuration from `application-dev.yml`.

## Coding Style & Naming Conventions
Use 4-space indentation and Spring naming: `PascalCase` classes, `camelCase` members and methods, `UPPER_SNAKE_CASE` constants. Controllers end with `Controller`, services under `application/*/service` end with `Service` or `ServiceImpl`. Keep inbound DTOs (`*Request`, `*Command`) separate from outbound VOs (`*Response`, `*View`). Annotate services, mappers, and adapters with the correct Spring stereotypes and lean on Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) to limit boilerplate.

## Testing Guidelines
JUnit 5 via `spring-boot-starter-test` is the default stack. Name suites `*Tests` and mirror their production packages (e.g., `src/test/java/.../application/user/service/UserServiceTests`). Provide mapper slice tests for MyBatis persistence plus `@SpringBootTest` coverage for multi-layer flows. Always run `./mvnw test` locally and add focused scenarios when updating business-critical rules or SQL migrations.

## Commit & Pull Request Guidelines
Craft concise, imperative commit messages under 72 characters (e.g., `完善用户模块权限`, `Add file upload validation`). Reference SQL artifacts whenever `src/main/resources/sql` changes. PRs should summarize scope, link relevant issues, list manual verification (`./mvnw test`, smoke API calls), and supply screenshots for UI or static asset updates.

## Security & Configuration Tips
Never commit secrets or profile-specific credentials. Favor environment variables for sensitive values and document new configuration surfaces in the PR or README. Keep profile-specific overrides inside dedicated YAML files and ensure external integrations are guarded by feature flags or validation rules before enabling them by default.
