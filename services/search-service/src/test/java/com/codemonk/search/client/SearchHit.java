package com.codemonk.search.client;

import java.util.Objects;

/**
 * A {@link SearchDocument} returned by a query, together with the relevance score
 * the retrieval strategy assigned to it.
 *
 * @param document the matched document
 * @param score    relevance of the document to the query, between {@code 0.0} and
 *                 {@code 1.0} inclusive, where higher is more relevant
 */
public record SearchHit(SearchDocument document, double score) {

    public SearchHit {
        Objects.requireNonNull(document, "document must not be null");
        if (score < 0.0d || score > 1.0d) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0 but was " + score);
        }
    }
}
