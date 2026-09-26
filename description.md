# Create MockSearchServiceClient for test environments

Closes #384

## Summary

Adds `MockSearchServiceClient`, an in-memory stand-in for the search index used by integration tests in `services/search-service`, so tests can run without a live pgvector backend.

It follows the existing `MockKnowledgeServiceClient` in `services/knowledge-service` closely: same test-tree layout, same seeded-fixture approach, and the same `empty()` / `setHealthy()` / `invocationCount()` / `reset()` ergonomics, so anyone who has used one will recognise the other.

## Changes

All new files, under `services/search-service/src/test/java/com/codemonk/search/client/`:

- **`SearchServiceClient`** — the client contract: `findDocument`, `search`, `searchRepository`, `searchByVector`, `index`, `deleteDocument` and `isHealthy`.
- **`SearchDocument`** and **`SearchHit`** — records for an indexed fragment and a scored match. Both validate their components; `SearchHit` rejects a score outside `0.0`–`1.0`.
- **`mock/MockSearchServiceClient`** — the mock itself, seeded with three fixture documents in `repo-1`, exposed as `SEEDED_*` constants so assertions can reference known values.
- **`mock/MockSearchServiceClientTest`** — 18 tests covering lookup, keyword ranking, repository scoping, vector search, indexing and deletion, custom fixtures, offline failures, invocation counting and `reset()`.

Responses are deterministic, as the issue asks:

- Keyword queries score a document by the fraction of distinct query terms found in its content.
- Vector queries score by cosine similarity against a synthetic embedding derived from the document content. `embeddingOf(document)` is public so a test can build a vector guaranteed to match a given document.
- Hits sort by descending score, and documents on equal scores keep insertion order (`LinkedHashMap`), so repeated queries return identical lists.

`setHealthy(false)` makes every query and mutation throw `ServiceUnavailableException`, for exercising failure paths.

No production code or `pom.xml` changes.

## Testing

```bash
./mvnw test -pl services/search-service
```

- 19 tests, 0 failures, 0 errors. Surefire confirms 18 in `MockSearchServiceClientTest` and the pre-existing `SearchApplicationTests.contextLoads` still passing.
- `./mvnw test -pl services/knowledge-service` re-run as well, since this change reads from that module's pattern: 12 tests, all passing, unaffected.

## Notes for reviewers

**The client interface is new.** `SearchServiceClient` did not exist — the name appeared nowhere in the repo, and `search-service`'s `main` tree is currently just `SearchApplication` plus four empty `package-info.java` layer markers. The issue says to "implement target client interface," so this PR authors the contract alongside the mock.

**It lives in the test tree,** matching `knowledge-service`, where `KnowledgeServiceClient` and its DTOs are also under `src/test`. When a real production search client lands, this interface is the natural thing to promote to `main` and have both implementations share.

**The method surface is inferred** from the module's `package-info` descriptions (hybrid search orchestration, vector embeddings, pgvector adapters). If a client contract is already specified somewhere this PR missed, the signatures should be reconciled against it — the mock's internals would need little change.

## Checklist

- [x] `MockSearchServiceClient` created in `com.codemonk.search.client.mock`
- [x] Existing tests pass

🤖 Generated with [Claude Code](https://claude.com/claude-code)
