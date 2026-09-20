package com.codemonk.search.client;

import java.util.List;
import java.util.Optional;

/**
 * Contract for querying the code search index owned by the search-service.
 *
 * <p>Integration tests depend on this abstraction instead of a live pgvector backed
 * client so that they can be driven by a deterministic in-memory implementation.
 *
 * @see com.codemonk.search.client.mock.MockSearchServiceClient
 */
public interface SearchServiceClient {

    /**
     * Looks up a single indexed document by its identifier.
     *
     * @param documentId identifier of the document to load
     * @return the document, or {@link Optional#empty()} when the index holds no such document
     */
    Optional<SearchDocument> findDocument(String documentId);

    /**
     * Runs a keyword search across the whole index, scoring documents on how much of
     * their content the query matches.
     *
     * @param query text to match against document content
     * @param limit maximum number of hits to return
     * @return the matching hits ordered by descending score, empty when nothing matches
     */
    List<SearchHit> search(String query, int limit);

    /**
     * Runs a keyword search restricted to a single repository.
     *
     * @param repositoryId identifier of the repository to search within
     * @param query        text to match against document content
     * @param limit        maximum number of hits to return
     * @return the matching hits ordered by descending score, empty when nothing matches
     */
    List<SearchHit> searchRepository(String repositoryId, String query, int limit);

    /**
     * Runs a vector similarity search for the given embedding.
     *
     * @param embedding query vector to compare indexed embeddings against
     * @param limit     maximum number of hits to return
     * @return the nearest hits ordered by descending similarity, empty when the index is empty
     */
    List<SearchHit> searchByVector(float[] embedding, int limit);

    /**
     * Adds or replaces a document in the index.
     *
     * @param document document to index
     */
    void index(SearchDocument document);

    /**
     * Removes a document from the index.
     *
     * @param documentId identifier of the document to remove
     * @return {@code true} when a document was removed, {@code false} when it was not indexed
     */
    boolean deleteDocument(String documentId);

    /**
     * Reports whether the search backend is reachable.
     *
     * @return {@code true} when queries are expected to succeed
     */
    boolean isHealthy();
}
