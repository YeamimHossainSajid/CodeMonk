# Add ArchUnitArchitectureTests checking layer boundaries

Closes #381

## Summary

Adds `ArchUnitArchitectureTests`, an ArchUnit test in `libs/common-core` that checks clean architecture layer boundaries in `com.codemonk.common`.

The packages are grouped into three rings. Code may only depend on its own ring or rings further in:

| Ring | Packages | Contents |
|---|---|---|
| Foundation | `constant`, `dto` | Constants and DTOs with no framework dependencies |
| Domain | `exception` | Domain exceptions and their HTTP translation |
| Infrastructure | `cache`, `service` | Redis, Kafka and JPA adapters |

## Changes

- **`ArchUnitArchitectureTests`** (`libs/common-core/src/test/java/com/codemonk/common/arch/`), with 6 rules:
  1. **Dependencies only point inwards.** Foundation can't use Domain or Infrastructure. Domain can only use Foundation. Nothing may depend on Infrastructure.
  2. **Adapters are kept apart.** `cache` and `service` must not depend on each other.
  3. **Inner rings don't use infrastructure libraries.** Foundation and Domain can't use Spring Data, Spring DAO, Spring JDBC, Kafka, JPA or Hibernate.
  4. **Foundation doesn't use Spring or Jakarta.**
  5. **Domain exceptions don't know about HTTP.** Subclasses of `DomainException` can't use Spring or `jakarta.servlet`. Only the global exception handler turns them into HTTP responses.
  6. **Only the `exception` package references the `@RestControllerAdvice` class.**

The rules add to the existing architecture tests without repeating them. `ArchitectureQualityTest_12` checks access between individual packages, and `ArchitectureQualityTest_13` checks for package cycles. This test checks the direction of dependencies between rings and keeps the adapters apart.

No production code or `pom.xml` changes. ArchUnit 1.3.0 was already a test dependency of `common-core`.

## Testing

```bash
./mvnw test -pl libs/common-core
```

- `ArchUnitArchitectureTests`: 6 tests, all passing.
- Whole module: 65 tests, 0 failures, 0 errors, when run with JDK 24 support turned on (see note).

**Note on JDK 24:** without that flag, 23 Mockito-based tests fail on JDK 24 (`RedisCacheServiceTest`, `GlobalExceptionHandlerTest`). They failed before this change too. The Mockito and Byte Buddy versions that come with Spring Boot can't create mocks on JDK 24, and the project targets JDK 21. They pass on JDK 21, or on JDK 24 with:

```bash
./mvnw test -pl libs/common-core "-DargLine=-Dnet.bytebuddy.experimental=true"
```

## Checklist

- [x] `ArchUnitArchitectureTests` created in `com.codemonk.common.arch`
- [x] Tests pass
- [x] Existing tests pass

🤖 Generated with [Claude Code](https://claude.com/claude-code)
