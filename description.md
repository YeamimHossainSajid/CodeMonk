# Add WireMockTestSetup for mocking HTTP APIs

Closes #383

## Summary

Adds `WireMockTestSetup`, a reusable JUnit 5 extension in `libs/common-core` for stubbing calls to external REST services in tests.

## Changes

- **`WireMockTestSetup`** (`libs/common-core/src/test/java/com/codemonk/common/test/`)
  - Starts a WireMock server on a dynamic port.
  - Starts once per test class, resets stubs before each test, and stops after the class.
  - `baseUrl()` / `port()` return the server address.
  - `registerBaseUrl(registry, property)` passes the address to Spring through `@DynamicPropertySource`.
  - Stub helpers: `stubGetJson`, `stubPostJson`, `stubJson`, `stubStatus` (error codes) and `stubDelayed` (timeouts).
  - `verifyCalled(method, path, times)` checks how many times an endpoint was called.
- **`WireMockTestSetupTest`**: 9 unit tests covering the helpers above.
- **Root `pom.xml`**: adds a `wiremock.version` property (3.9.1) and a managed `org.wiremock:wiremock-standalone` test dependency, following the ArchUnit setup.
  - The standalone artifact bundles its own Jetty, so it doesn't clash with the Jetty version Spring Boot manages.
- **`libs/common-core/pom.xml`**: adds the WireMock test dependency.

## Usage

```java
@RegisterExtension
static WireMockTestSetup wireMock = new WireMockTestSetup();

@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    wireMock.registerBaseUrl(registry, "clients.github.base-url");
}

@Test
void fetchesRepository() {
    wireMock.stubGetJson("/repos/42", 200, "{\"id\":42}");
    // call the client under test...
    wireMock.verifyCalled("GET", "/repos/42", 1);
}
```

## Testing

```bash
./mvnw test -pl libs/common-core
```

- `WireMockTestSetupTest`: 9 tests, all passing.
- Whole module: 59 tests, 0 failures, 0 errors, when run with JDK 24 support turned on (see note).

**Note on JDK 24:** without that flag, 23 Mockito-based tests fail on JDK 24. They failed before this change too. The Mockito and Byte Buddy versions that come with Spring Boot 3.3.0 can't create mocks on JDK 24, and the project targets JDK 21. They pass on JDK 21, or on JDK 24 with:

```bash
./mvnw test -pl libs/common-core "-DargLine=-Dnet.bytebuddy.experimental=true"
```

## Checklist

- [x] `WireMockTestSetup` created in `com.codemonk.common.test`
- [x] Unit tests added
- [x] Existing tests pass

🤖 Generated with [Claude Code](https://claude.com/claude-code)
