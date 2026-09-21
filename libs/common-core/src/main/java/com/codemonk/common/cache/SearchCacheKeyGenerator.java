package com.codemonk.common.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Builds the cache keys under which search results are stored.
 *
 * <p>A search result is only reusable when the next request asks the same
 * question, so the key has to fold away everything that does not change the
 * answer: surrounding and repeated whitespace, letter case, and the order in
 * which a caller happens to hand over its filters. Two callers issuing the same
 * search therefore land on the same key, and a key never depends on the shape of
 * the {@link Map} it came from.
 *
 * <p>Keys are laid out as {@code namespace:query} with an optional trailing
 * {@code :name=value|name=value} segment, which keeps them readable in
 * {@code redis-cli} and lets a whole namespace be dropped with a single
 * {@code search:*} scan. The characters {@code :}, {@code |} and {@code =}
 * delimit those segments, so they are replaced by {@code _} wherever they occur
 * inside a query or a filter.
 *
 * <p>The generator holds no state and is safe to share across threads. Pair it
 * with {@link RedisCacheService} to read and write the entries it names.
 */
@Component
public class SearchCacheKeyGenerator {

    /** Namespace used by the overloads that do not take one. */
    public static final String DEFAULT_NAMESPACE = "search";

    private static final String SEGMENT_SEPARATOR = ":";

    private static final String FILTER_SEPARATOR = "|";

    private static final String FILTER_ASSIGNMENT = "=";

    private static final String RESERVED_REPLACEMENT = "_";

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Pattern RESERVED = Pattern.compile("[:|=]");

    /**
     * Builds a key for a query issued without filters, under the
     * {@link #DEFAULT_NAMESPACE}.
     *
     * @param query the search term, neither {@code null} nor blank
     * @return the cache key, for example {@code search:order service}
     * @throws IllegalArgumentException when {@code query} is {@code null} or blank
     */
    public String generate(String query) {
        return generate(DEFAULT_NAMESPACE, query, Map.of());
    }

    /**
     * Builds a key for a query narrowed by filters, under the
     * {@link #DEFAULT_NAMESPACE}.
     *
     * @param query   the search term, neither {@code null} nor blank
     * @param filters the applied filters, {@code null} or empty when the query
     *                stands alone
     * @return the cache key, for example {@code search:order service:lang=java}
     * @throws IllegalArgumentException when {@code query} is {@code null} or blank,
     *                                  or a filter name is {@code null} or blank
     */
    public String generate(String query, Map<String, String> filters) {
        return generate(DEFAULT_NAMESPACE, query, filters);
    }

    /**
     * Builds a key for a query narrowed by filters, under a caller-chosen
     * namespace.
     *
     * <p>A namespace separates result sets that would otherwise share a key, such
     * as the same term searched across repositories rather than across symbols.
     *
     * @param namespace the key prefix, neither {@code null} nor blank
     * @param query     the search term, neither {@code null} nor blank
     * @param filters   the applied filters, {@code null} or empty when the query
     *                  stands alone
     * @return the cache key
     * @throws IllegalArgumentException when {@code namespace} or {@code query} is
     *                                  {@code null} or blank, or a filter name is
     *                                  {@code null} or blank
     */
    public String generate(String namespace, String query, Map<String, String> filters) {
        requireText(namespace, "namespace");
        requireText(query, "query");

        StringBuilder key = new StringBuilder()
                .append(normalize(namespace))
                .append(SEGMENT_SEPARATOR)
                .append(normalize(query));

        String filterSegment = normalizeFilters(filters);
        if (!filterSegment.isEmpty()) {
            key.append(SEGMENT_SEPARATOR).append(filterSegment);
        }
        return key.toString();
    }

    /**
     * Renders the filters as {@code name=value} pairs ordered by name, so that two
     * callers passing the same filters in a different order agree on one key.
     *
     * @return the rendered segment, empty when there is nothing to render
     */
    private String normalizeFilters(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }

        Map<String, String> ordered = new TreeMap<>();
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            requireText(filter.getKey(), "filter name");
            ordered.put(normalize(filter.getKey()), normalize(filter.getValue()));
        }

        List<String> pairs = new ArrayList<>(ordered.size());
        for (Map.Entry<String, String> filter : ordered.entrySet()) {
            pairs.add(filter.getKey() + FILTER_ASSIGNMENT + filter.getValue());
        }
        return String.join(FILTER_SEPARATOR, pairs);
    }

    /**
     * Folds away the differences that do not change what was searched for: case,
     * surrounding and repeated whitespace, and the characters reserved as
     * delimiters.
     *
     * @param value the raw text, {@code null} for an absent filter value
     * @return the normalized text, empty when {@code value} is {@code null}
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String collapsed = WHITESPACE.matcher(value.trim()).replaceAll(" ");
        return RESERVED.matcher(collapsed.toLowerCase(Locale.ROOT)).replaceAll(RESERVED_REPLACEMENT);
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
