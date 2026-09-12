# Add RedisCacheTest_4 unit test

Closes #375

## Summary

Adds `RedisCacheTest_4`, a unit test in `libs/common-core` for the `RedisCache` component in `com.codemonk.common.cache`. It uses a mocked `RedisTemplate` and follows the same style as the other `RedisCacheTest_N` classes.

## Changes

- **`RedisCacheTest_4`** (`libs/common-core/src/test/java/com/codemonk/common/cache/`), with 6 tests. They cover behaviour the other `RedisCache` tests don't:
  1. **Different prefixes give different keys.** `user:42` and `product:42` don't collide.
  2. **A long TTL is passed through.** A 7-day TTL reaches Redis unchanged.
  3. **A `null` value can be stored.** `put` passes it straight to Redis.
  4. **An overwrite keeps the latest value.** Two `put` calls on the same key happen in order, and `get` returns the second value.
  5. **Reading doesn't evict.** `get` never calls `delete`.
  6. **Evicting a missing key doesn't fail.** `evict` doesn't throw when `delete` returns `false`.

No production code or `pom.xml` changes.

## Testing

```bash
./mvnw test -pl libs/common-core -Dtest=RedisCacheTest_*
```

- `RedisCacheTest_4`: 6 tests, all passing.
- `RedisCacheTest_1` (3), `RedisCacheTest_2` (4) and `RedisCacheTest_5` (6) still pass.
- `./mvnw test -pl libs/common-core`: every test class in the run passed, with 0 failures and 0 errors.

**Note on test discovery:** a plain `./mvnw test -pl libs/common-core` does not run `RedisCacheTest_4`. By default, Surefire only picks up classes whose names end in `Test`, `Tests` or `TestCase`, or start with `Test`. A name ending in `_4` doesn't match, so the class runs only when named with `-Dtest=...`. The other `RedisCacheTest_N` classes have the same problem. Fixing it is outside the scope of this issue.

**Note on JDK 24:** the project targets JDK 21. On JDK 24, the Mockito and Byte Buddy versions that come with Spring Boot can't create mocks, so the tests were run with:

```bash
./mvnw test -pl libs/common-core "-Dtest=RedisCacheTest_*" "-DargLine=-Dnet.bytebuddy.experimental=true"
```

## Checklist

- [x] `RedisCacheTest_4` created in `com.codemonk.common.cache`
- [x] `RedisCacheTest_4` passes
- [x] Existing tests pass

🤖 Generated with [Claude Code](https://claude.com/claude-code)
