package com.codemonk.search.client.mock;

import com.codemonk.common.exception.ServiceUnavailableException;
import com.codemonk.search.client.SearchDocument;
import com.codemonk.search.client.SearchHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockSearchServiceClientTest {

    private MockSearchServiceClient client;

    @BeforeEach
    void setUp() {
        client = new MockSearchServiceClient();
    }

    @Test
    void shouldReturnSeededDocumentById() {
        Optional<SearchDocument> document = client.findDocument(MockSearchServiceClient.SEEDED_SERVICE_DOCUMENT_ID);

        assertTrue(document.isPresent());
        assertEquals(MockSearchServiceClient.SEEDED_REPOSITORY_ID, document.get().repositoryId());
        assertEquals("src/main/java/com/codemonk/demo/OrderService.java", document.get().path());
        assertEquals("java", document.get().language());
    }

    @Test
    void shouldReturnEmptyForUnknownDocument() {
        assertTrue(client.findDocument("does-not-exist").isEmpty());
        assertTrue(client.findDocument(null).isEmpty());
    }

    @Test
    void shouldMatchDocumentsOnKeyword() {
        List<SearchHit> hits = client.search("order", 10);

        assertEquals(
                List.of(
                        MockSearchServiceClient.SEEDED_SERVICE_DOCUMENT_ID,
                        MockSearchServiceClient.SEEDED_REPOSITORY_DOCUMENT_ID,
                        MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID),
                hits.stream().map(hit -> hit.document().id()).toList());
        assertTrue(hits.stream().allMatch(hit -> hit.score() == 1.0d));
    }

    @Test
    void shouldRankMoreCompleteMatchesFirst() {
        List<SearchHit> hits = client.search("stored order identifier", 10);

        assertEquals(MockSearchServiceClient.SEEDED_REPOSITORY_DOCUMENT_ID, hits.get(0).document().id());
        assertEquals(1.0d, hits.get(0).score());
        assertTrue(hits.get(0).score() > hits.get(1).score());
    }

    @Test
    void shouldReturnSameResultsForRepeatedQueries() {
        assertEquals(client.search("order", 10), client.search("order", 10));
    }

    @Test
    void shouldReturnNoHitsForUnmatchedOrBlankQuery() {
        assertTrue(client.search("kubernetes", 10).isEmpty());
        assertTrue(client.search("   ", 10).isEmpty());
        assertTrue(client.search(null, 10).isEmpty());
    }

    @Test
    void shouldRespectQueryLimit() {
        assertEquals(2, client.search("order", 2).size());
        assertTrue(client.search("order", 0).isEmpty());
    }

    @Test
    void shouldRestrictSearchToRequestedRepository() {
        client.index(new SearchDocument("doc-other", "repo-2", "Other.java", "another order handler", "java"));

        List<SearchHit> scoped = client.searchRepository(MockSearchServiceClient.SEEDED_REPOSITORY_ID, "order", 10);

        assertEquals(3, scoped.size());
        assertTrue(scoped.stream().noneMatch(hit -> "doc-other".equals(hit.document().id())));
        assertEquals(1, client.searchRepository("repo-2", "order", 10).size());
    }

    @Test
    void shouldReturnNoHitsForUnknownRepository() {
        assertTrue(client.searchRepository("unknown-repo", "order", 10).isEmpty());
        assertTrue(client.searchRepository(null, "order", 10).isEmpty());
    }

    @Test
    void shouldRankExactEmbeddingMatchFirstOnVectorSearch() {
        SearchDocument target = client.findDocument(MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID).orElseThrow();

        List<SearchHit> hits = client.searchByVector(MockSearchServiceClient.embeddingOf(target), 10);

        assertEquals(MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID, hits.get(0).document().id());
        assertEquals(1.0d, hits.get(0).score());
    }

    @Test
    void shouldReturnNoHitsForEmptyOrNullEmbedding() {
        assertTrue(client.searchByVector(new float[MockSearchServiceClient.EMBEDDING_DIMENSIONS], 10).isEmpty());
        assertTrue(client.searchByVector(null, 10).isEmpty());
    }

    @Test
    void shouldServeIndexedDocuments() {
        client.index(new SearchDocument("doc-new", "repo-1", "Cache.java", "redis cache adapter", "java"));

        assertTrue(client.findDocument("doc-new").isPresent());
        assertEquals(
                List.of("doc-new"),
                client.search("redis cache", 10).stream().map(hit -> hit.document().id()).toList());
    }

    @Test
    void shouldReplaceDocumentOnReindex() {
        client.index(new SearchDocument(
                MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID, "repo-1", "OrderDto.java", "replaced body", "java"));

        assertEquals(3, client.documentCount());
        assertEquals("replaced body", client.findDocument(MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID)
                .orElseThrow()
                .content());
    }

    @Test
    void shouldDeleteIndexedDocument() {
        assertTrue(client.deleteDocument(MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID));

        assertEquals(2, client.documentCount());
        assertTrue(client.findDocument(MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID).isEmpty());
        assertFalse(client.deleteDocument(MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID));
        assertFalse(client.deleteDocument(null));
    }

    @Test
    void shouldServeCustomFixturesFromEmptyIndex() {
        MockSearchServiceClient custom = MockSearchServiceClient.empty()
                .withDocument(new SearchDocument("d-1", "repo-x", "Alpha.java", "alpha parser module", "java"))
                .withDocument(new SearchDocument("d-2", "repo-x", "Beta.py", "beta parser module", "python"));

        assertEquals(2, custom.documentCount());
        assertEquals(2, custom.searchRepository("repo-x", "parser", 10).size());
        assertEquals(
                List.of("d-1"),
                custom.search("alpha", 10).stream().map(hit -> hit.document().id()).toList());
        assertTrue(custom.findDocument(MockSearchServiceClient.SEEDED_DTO_DOCUMENT_ID).isEmpty());
    }

    @Test
    void shouldFailQueriesWhenOffline() {
        client.setHealthy(false);

        assertFalse(client.isHealthy());
        assertThrows(ServiceUnavailableException.class, () -> client.findDocument("doc-1"));
        assertThrows(ServiceUnavailableException.class, () -> client.search("order", 10));
        assertThrows(ServiceUnavailableException.class, () -> client.searchRepository("repo-1", "order", 10));
        assertThrows(
                ServiceUnavailableException.class,
                () -> client.searchByVector(new float[MockSearchServiceClient.EMBEDDING_DIMENSIONS], 10));
        assertThrows(ServiceUnavailableException.class, () -> client.deleteDocument("doc-1"));
        assertThrows(
                ServiceUnavailableException.class,
                () -> client.index(new SearchDocument("doc-x", "repo-1", "X.java", "body", "java")));
    }

    @Test
    void shouldCountQueryInvocations() {
        assertEquals(0, client.invocationCount());

        client.findDocument("doc-1");
        client.search("order", 10);
        client.isHealthy();

        assertEquals(2, client.invocationCount());
    }

    @Test
    void shouldRestoreSeededStateOnReset() {
        client.index(new SearchDocument("extra", "repo-x", "Extra.java", "extra body", "java"));
        client.setHealthy(false);

        client.reset();

        assertTrue(client.isHealthy());
        assertEquals(0, client.invocationCount());
        assertEquals(3, client.documentCount());
        assertTrue(client.findDocument("extra").isEmpty());
    }
}
