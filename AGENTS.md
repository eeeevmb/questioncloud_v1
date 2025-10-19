# Repository Guidelines

## Project Structure & Module Organization
Honor the layered packages under `src/main/java/cn/sztu/questioncloud`: `web/rest/v1` exposes controllers, `application` hosts use-case services plus DTO/VO mapping, and `infrastructure` handles persistence, adapters, and file storage. Shared enums and constants stay in `common`. `QuestioncloudApplication.java` boots the service. Keep SQL schemas in `src/main/resources/sql`, configuration in `application.yml`, and static assets in `src/main/resources/static`. Place new tests under `src/test/java` using matching packages.

## Build, Test & Development Commands
Use the Maven Wrapper with Java 21. `./mvnw clean package` produces a release JAR, `./mvnw spring-boot:run` starts the API locally, and `./mvnw test` runs all automated checks. For quicker loops you may call `./mvnw spring-boot:run -DskipTests`, but always finish with `./mvnw test`. Restart the process after changing configuration files.

## Coding Style & Naming Conventions
Stick to 4-space indentation and standard Spring naming: classes `PascalCase`, members `camelCase`, and constants `UPPER_SNAKE_CASE`. Controllers end with `Controller`; services in `application/*/service` use `*Service` or `*ServiceImpl`. Prefer Lombok annotations (`@Getter`, `@Builder`) instead of hand-written boilerplate, and create DTO/VO classes to separate inbound and outbound payloads.

## Testing Guidelines
JUnit 5 arrives via `spring-boot-starter-test`, with MyBatis helpers for mapper tests. Name classes `*Tests` and mirror package structure under `src/test/java`. Add slice tests for mappers and `@SpringBootTest` for flows that cross service and web layers, covering security configuration and file handling boundaries. Confirm `./mvnw test` before opening pull requests.

## Commit & Pull Request Guidelines
History shows short English or Chinese summaries; keep the first line imperative and ≤72 characters (for example `完善用户模块权限` or `Add file upload validation`). Mention SQL changes when editing `src/main/resources/sql` and reference related issues in the body. Pull requests should outline the change, describe manual verification (`./mvnw test`, API smoke checks), and attach screenshots when updating static assets.

## Configuration & Security Tips
Never commit credentials. Extend `application.yml` with a profile-specific file such as `application-dev.yml`, then run `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run`. Document any new external keys or validation rules alongside the feature.
