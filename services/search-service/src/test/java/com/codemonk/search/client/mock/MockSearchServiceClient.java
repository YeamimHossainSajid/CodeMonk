package com.codemonk.search.client.mock;

import com.codemonk.common.exception.ServiceUnavailableException;
import com.codemonk.search.client.SearchDocument;
import com.codemonk.search.client.SearchHit;
import com.codemonk.search.client.SearchServiceClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory {@link SearchServiceClient} used by integration tests in place of a live
 * pgvector backed search index.
 *
 * <p>The client is seeded with a small, fixed corpus so that assertions can be written
 * against known values. Scoring is deterministic: keyword queries score a document by
 * the fraction of distinct query terms found in its content, and vector queries score by
 * cosine similarity against a synthetic embedding derived from the document content.
 * Hits are ordered by descending score, and documents on equal scores keep their
 * insertion order.
 *
 * <p>Tests that need their own fixtures can start from an empty index via {@link #empty()}
 * and add data with {@link #withDocument(SearchDocument)} or {@link #index(SearchDocument)}.
 *
 * <p>Failure handling can be exercised by flipping the client offline with
 * {@link #setHealthy(boolean)}, after which every query and index mutation throws
 * {@link ServiceUnavailableException}.
 *
 * <p>This class is not thread-safe beyond its invocation counter; drive it from a
 * single test thread.
 */
public class MockSearchServiceClient implements SearchServiceClient {

    /** Repository the seeded fixture corpus belongs to. */
    public static final String SEEDED_REPOSITORY_ID = "repo-1";

    /** Identifier of the seeded document describing the order service. */
    public static final String SEEDED_SERVICE_DOCUMENT_ID = "doc-1";

    /** Identifier of the seeded document describing the order repository. */
    public static final String SEEDED_REPOSITORY_DOCUMENT_ID = "doc-2";

    /** Identifier of the seeded document describing the order DTO. */
    public static final String SEEDED_DTO_DOCUMENT_ID = "doc-3";

    /** Length of the synthetic embeddings produced for seeded and indexed documents. */
    public static final int EMBEDDING_DIMENSIONS = 8;

    private final Map<String, SearchDocument> documentsById = new LinkedHashMap<>();
    private final AtomicInteger invocationCount = new AtomicInteger();

    private boolean healthy = true;

    /**
     * Creates a client pre-loaded with the seeded fixture corpus.
     */
    public MockSearchServiceClient() {
        seedDefaultCorpus();
    }

    private MockSearchServiceClient(boolean seed) {
        if (seed) {
            seedDefaultCorpus();
        }
    }

    /**
     * Creates a client with no documents, for tests that supply their own fixtures.
     *
     * @return an empty client
     */
    public static MockSearchServiceClient empty() {
        return new MockSearchServiceClient(false);
    }

    /**
     * Adds or replaces a document in the index without counting an invocation, for use
     * when arranging fixtures.
     *
     * @param document document to register, never {@code null}
     * @return this client, for chaining
     */
    public MockSearchServiceClient withDocument(SearchDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        documentsById.put(document.id(), document);
        return this;
    }

    /**
     * Controls whether queries succeed or fail with {@link ServiceUnavailableException}.
     *
     * @param healthy {@code true} to serve queries, {@code false} to simulate an outage
     * @return this client, for chaining
     */
    public MockSearchServiceClient setHealthy(boolean healthy) {
        this.healthy = healthy;
        return this;
    }

    /**
     * Number of query and mutation methods invoked since construction or the last
     * {@link #reset()}. Health checks and {@link #withDocument(SearchDocument)} are not
     * counted.
     *
     * @return the invocation count
     */
    public int invocationCount() {
        return invocationCount.get();
    }

    /**
     * Number of documents currently held in the index.
     *
     * @return the document count
     */
    public int documentCount() {
        return documentsById.size();
    }

    /**
     * Restores the seeded fixture corpus, clears the invocation counter and marks the
     * client healthy again, so a single instance can be shared across test methods.
     */
    public void reset() {
        documentsById.clear();
        invocationCount.set(0);
        healthy = true;
        seedDefaultCorpus();
    }

    @Override
    public Optional<SearchDocument> findDocument(String documentId) {
        recordInvocation();
        if (documentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(documentsById.get(documentId));
    }

    @Override
    public List<SearchHit> search(String query, int limit) {
        recordInvocation();
        return keywordSearch(null, query, limit);
    }

    @Override
    public List<SearchHit> searchRepository(String repositoryId, String query, int limit) {
        recordInvocation();
        if (repositoryId == null) {
            return List.of();
        }
        return keywordSearch(repositoryId, query, limit);
    }

    @Override
    public List<SearchHit> searchByVector(float[] embedding, int limit) {
        recordInvocation();
        if (embedding == null || limit <= 0) {
            return List.of();
        }
        List<SearchHit> hits = new ArrayList<>();
        for (SearchDocument document : documentsById.values()) {
            double similarity = cosineSimilarity(embedding, embeddingOf(document));
            if (similarity > 0.0d) {
                hits.add(new SearchHit(document, similarity));
            }
        }
        return topHits(hits, limit);
    }

    @Override
    public void index(SearchDocument document) {
        recordInvocation();
        withDocument(document);
    }

    @Override
    public boolean deleteDocument(String documentId) {
        recordInvocation();
        if (documentId == null) {
            return false;
        }
        return documentsById.remove(documentId) != null;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    /**
     * Builds the synthetic embedding the mock assigns to a document, so that tests can
     * query {@link #searchByVector(float[], int)} with a vector known to match.
     *
     * @param document document to derive an embedding for, never {@code null}
     * @return an embedding of {@link #EMBEDDING_DIMENSIONS} components
     */
    public static float[] embeddingOf(SearchDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        float[] embedding = new float[EMBEDDING_DIMENSIONS];
        for (String term : terms(document.content())) {
            embedding[Math.floorMod(term.hashCode(), EMBEDDING_DIMENSIONS)] += 1.0f;
        }
        return embedding;
    }

    private List<SearchHit> keywordSearch(String repositoryId, String query, int limit) {
        List<String> queryTerms = terms(query).stream().distinct().toList();
        if (queryTerms.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<SearchHit> hits = new ArrayList<>();
        for (SearchDocument document : documentsById.values()) {
            if (repositoryId != null && !repositoryId.equals(document.repositoryId())) {
                continue;
            }
            List<String> documentTerms = terms(document.content());
            long matched = queryTerms.stream().filter(documentTerms::contains).count();
            if (matched > 0) {
                hits.add(new SearchHit(document, (double) matched / queryTerms.size()));
            }
        }
        return topHits(hits, limit);
    }

    /**
     * Orders hits by descending score, keeping insertion order among equal scores, and
     * truncates the result to {@code limit}.
     */
    private static List<SearchHit> topHits(List<SearchHit> hits, int limit) {
        hits.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        return List.copyOf(hits.subList(0, Math.min(limit, hits.size())));
    }

    private static List<String> terms(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!token.isEmpty()) {
                terms.add(token);
            }
        }
        return terms;
    }

    private static double cosineSimilarity(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int i = 0; i < length; i++) {
            dot += (double) left[i] * right[i];
        }
        for (float value : left) {
            leftNorm += (double) value * value;
        }
        for (float value : right) {
            rightNorm += (double) value * value;
        }
        if (dot <= 0.0d || leftNorm == 0.0d || rightNorm == 0.0d) {
            return 0.0d;
        }
        // Clamped because floating point rounding can nudge an exact self-match past
        // 1.0, which SearchHit rejects.
        return Math.min(1.0d, dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)));
    }

    private void recordInvocation() {
        if (!healthy) {
            throw new ServiceUnavailableException("search-service", "mock client is offline");
        }
        invocationCount.incrementAndGet();
    }

    private void seedDefaultCorpus() {
        withDocument(new SearchDocument(
                SEEDED_SERVICE_DOCUMENT_ID,
                SEEDED_REPOSITORY_ID,
                "src/main/java/com/codemonk/demo/OrderService.java",
                "public class OrderService places and cancels a customer order",
                "java"));
        withDocument(new SearchDocument(
                SEEDED_REPOSITORY_DOCUMENT_ID,
                SEEDED_REPOSITORY_ID,
                "src/main/java/com/codemonk/demo/OrderRepository.java",
                "public interface OrderRepository loads a stored order by identifier",
                "java"));
        withDocument(new SearchDocument(
                SEEDED_DTO_DOCUMENT_ID,
                SEEDED_REPOSITORY_ID,
                "src/main/java/com/codemonk/demo/OrderDto.java",
                "public record OrderDto carries order totals to the client",
                "java"));
    }
}
