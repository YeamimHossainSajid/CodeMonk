package com.codemonk.common.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Builds the cache keys under which knowledge graph query results are stored.
 *
 * <p>A traversal is only reusable when the next request walks the same graph the
 * same way, so the key carries the operation, the nodes it started from and the
 * parameters that change the answer, such as the direction walked or how deep
 * the walk went. What it does not carry is the shape of the call: padding around
 * a node identifier, and the order in which a caller happens to hand over its
 * parameters, are folded away so two callers issuing the same query land on the
 * same key.
 *
 * <p>Unlike {@link SearchCacheKeyGenerator}, letter case is preserved in node
 * identifiers. A search term is prose, and {@code OrderService} asks the same
 * question as {@code orderservice}, but a node identifier is an identity:
 * lowercasing it would let two distinct nodes share one entry and serve each
 * other's neighbors.
 *
 * <p>Keys are laid out as {@code namespace:operation:node}, with a further node
 * for the operations that relate two of them, and an optional trailing
 * {@code :name=value|name=value} segment. That keeps them readable in
 * {@code redis-cli} and lets a whole namespace be dropped with a single
 * {@code graph:*} scan, or one operation with {@code graph:neighbors:*}. The
 * characters {@code :}, {@code |} and {@code =} delimit those segments, so they
 * are replaced by {@code _} wherever they occur inside an identifier or a
 * parameter.
 *
 * <p>The generator holds no state and is safe to share across threads. Pair it
 * with {@link RedisCacheService} to read and write the entries it names.
 */
@Component
public class GraphCacheKeyGenerator {

    /** Namespace used by the overloads that do not take one. */
    public static final String DEFAULT_NAMESPACE = "graph";

    /** Operation segment for a lookup of a single node. */
    public static final String NODE_OPERATION = "node";

    /** Operation segment for a neighbor traversal. */
    public static final String NEIGHBORS_OPERATION = "neighbors";

    /** Operation segment for a path between two nodes. */
    public static final String PATH_OPERATION = "path";

    /** Depth assumed by the neighbor overloads that do not take one. */
    public static final int DEFAULT_DEPTH = 1;

    private static final String DEPTH_PARAMETER = "depth";

    private static final String SEGMENT_SEPARATOR = ":";

    private static final String PARAMETER_SEPARATOR = "|";

    private static final String PARAMETER_ASSIGNMENT = "=";

    private static final String RESERVED_REPLACEMENT = "_";

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Pattern RESERVED = Pattern.compile("[:|=]");

    /** The direction in which a neighbor traversal walks the edges of a node. */
    public enum Direction {

        /** Edges pointing at the node. */
        INCOMING,

        /** Edges leaving the node. */
        OUTGOING,

        /** Edges in either direction. */
        BOTH;

        /**
         * @return the segment this direction contributes to a key
         */
        private String segment() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Builds a key for a single node, under the {@link #DEFAULT_NAMESPACE}.
     *
     * @param nodeId the node identifier, neither {@code null} nor blank
     * @return the cache key, for example {@code graph:node:OrderService}
     * @throws IllegalArgumentException when {@code nodeId} is {@code null} or
     *                                  blank
     */
    public String forNode(String nodeId) {
        return forNode(DEFAULT_NAMESPACE, nodeId);
    }

    /**
     * Builds a key for a single node, under a caller-chosen namespace.
     *
     * @param namespace the key prefix, neither {@code null} nor blank
     * @param nodeId    the node identifier, neither {@code null} nor blank
     * @return the cache key
     * @throws IllegalArgumentException when {@code namespace} or {@code nodeId} is
     *                                  {@code null} or blank
     */
    public String forNode(String namespace, String nodeId) {
        return generate(namespace, NODE_OPERATION, Arrays.asList(nodeId), Map.of());
    }

    /**
     * Builds a key for the direct neighbors of a node, under the
     * {@link #DEFAULT_NAMESPACE}.
     *
     * @param nodeId    the node identifier, neither {@code null} nor blank
     * @param direction the direction walked, not {@code null}
     * @return the cache key, for example
     *         {@code graph:neighbors:OrderService:outgoing:depth=1}
     * @throws IllegalArgumentException when {@code nodeId} is {@code null} or
     *                                  blank, or {@code direction} is {@code null}
     */
    public String forNeighbors(String nodeId, Direction direction) {
        return forNeighbors(DEFAULT_NAMESPACE, nodeId, direction, DEFAULT_DEPTH);
    }

    /**
     * Builds a key for a neighbor traversal of a given depth, under the
     * {@link #DEFAULT_NAMESPACE}.
     *
     * @param nodeId    the node identifier, neither {@code null} nor blank
     * @param direction the direction walked, not {@code null}
     * @param depth     how many hops the walk covers, at least one
     * @return the cache key
     * @throws IllegalArgumentException when {@code nodeId} is {@code null} or
     *                                  blank, {@code direction} is {@code null},
     *                                  or {@code depth} is below one
     */
    public String forNeighbors(String nodeId, Direction direction, int depth) {
        return forNeighbors(DEFAULT_NAMESPACE, nodeId, direction, depth);
    }

    /**
     * Builds a key for a neighbor traversal of a given depth, under a
     * caller-chosen namespace.
     *
     * <p>Direction and depth both sit in the key because both change the answer:
     * the nodes pointing at a node are not the ones it points at, and a two-hop
     * walk reaches further than a one-hop walk.
     *
     * @param namespace the key prefix, neither {@code null} nor blank
     * @param nodeId    the node identifier, neither {@code null} nor blank
     * @param direction the direction walked, not {@code null}
     * @param depth     how many hops the walk covers, at least one
     * @return the cache key
     * @throws IllegalArgumentException when {@code namespace} or {@code nodeId} is
     *                                  {@code null} or blank, {@code direction} is
     *                                  {@code null}, or {@code depth} is below one
     */
    public String forNeighbors(String namespace, String nodeId, Direction direction, int depth) {
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be at least 1");
        }

        return generate(
                namespace,
                NEIGHBORS_OPERATION,
                Arrays.asList(nodeId, direction.segment()),
                Map.of(DEPTH_PARAMETER, Integer.toString(depth)));
    }

    /**
     * Builds a key for the path between two nodes, under the
     * {@link #DEFAULT_NAMESPACE}.
     *
     * @param sourceNodeId the starting node identifier, neither {@code null} nor
     *                     blank
     * @param targetNodeId the destination node identifier, neither {@code null}
     *                     nor blank
     * @return the cache key, for example
     *         {@code graph:path:OrderService:PaymentService}
     * @throws IllegalArgumentException when either identifier is {@code null} or
     *                                  blank
     */
    public String forPath(String sourceNodeId, String targetNodeId) {
        return forPath(DEFAULT_NAMESPACE, sourceNodeId, targetNodeId);
    }

    /**
     * Builds a key for the path between two nodes, under a caller-chosen
     * namespace.
     *
     * <p>The endpoints keep the order they were given, because the path from one
     * node to another is not the path back: the graph is directed.
     *
     * @param namespace    the key prefix, neither {@code null} nor blank
     * @param sourceNodeId the starting node identifier, neither {@code null} nor
     *                     blank
     * @param targetNodeId the destination node identifier, neither {@code null}
     *                     nor blank
     * @return the cache key
     * @throws IllegalArgumentException when {@code namespace} or either identifier
     *                                  is {@code null} or blank
     */
    public String forPath(String namespace, String sourceNodeId, String targetNodeId) {
        return generate(namespace, PATH_OPERATION, Arrays.asList(sourceNodeId, targetNodeId), Map.of());
    }

    /**
     * Builds a key for a graph query the named overloads do not cover.
     *
     * @param namespace  the key prefix, neither {@code null} nor blank
     * @param operation  the query being cached, neither {@code null} nor blank
     * @param nodeIds    the identifiers the query starts from, in the order that
     *                   matters to it; at least one, none {@code null} or blank
     * @param parameters anything else that changes the answer, {@code null} or
     *                   empty when the nodes stand alone
     * @return the cache key
     * @throws IllegalArgumentException when {@code namespace}, {@code operation},
     *                                  a node identifier or a parameter name is
     *                                  {@code null} or blank, or when
     *                                  {@code nodeIds} is {@code null} or empty
     */
    public String generate(
            String namespace,
            String operation,
            List<String> nodeIds,
            Map<String, String> parameters) {

        requireText(namespace, "namespace");
        requireText(operation, "operation");
        if (nodeIds == null || nodeIds.isEmpty()) {
            throw new IllegalArgumentException("nodeIds must not be null or empty");
        }

        StringBuilder key = new StringBuilder()
                .append(normalize(namespace))
                .append(SEGMENT_SEPARATOR)
                .append(normalize(operation));

        for (String nodeId : nodeIds) {
            requireText(nodeId, "node id");
            key.append(SEGMENT_SEPARATOR).append(normalizeIdentifier(nodeId));
        }

        String parameterSegment = normalizeParameters(parameters);
        if (!parameterSegment.isEmpty()) {
            key.append(SEGMENT_SEPARATOR).append(parameterSegment);
        }
        return key.toString();
    }

    /**
     * Renders the parameters as {@code name=value} pairs ordered by name, so that
     * two callers passing the same parameters in a different order agree on one
     * key.
     *
     * @return the rendered segment, empty when there is nothing to render
     */
    private String normalizeParameters(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }

        Map<String, String> ordered = new TreeMap<>();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            requireText(parameter.getKey(), "parameter name");
            ordered.put(normalize(parameter.getKey()), normalize(parameter.getValue()));
        }

        List<String> pairs = new ArrayList<>(ordered.size());
        for (Map.Entry<String, String> parameter : ordered.entrySet()) {
            pairs.add(parameter.getKey() + PARAMETER_ASSIGNMENT + parameter.getValue());
        }
        return String.join(PARAMETER_SEPARATOR, pairs);
    }

    /**
     * Folds away the differences that do not change which node was asked for:
     * surrounding and repeated whitespace, and the characters reserved as
     * delimiters. Case is left alone, because it tells one node from another.
     *
     * @param value the raw identifier, already known to be non-blank
     * @return the normalized identifier
     */
    private String normalizeIdentifier(String value) {
        String collapsed = WHITESPACE.matcher(value.trim()).replaceAll(" ");
        return RESERVED.matcher(collapsed).replaceAll(RESERVED_REPLACEMENT);
    }

    /**
     * Normalizes the parts of a key that name a query rather than a node, where
     * case carries no meaning and is folded away along with whitespace and the
     * reserved delimiters.
     *
     * @param value the raw text, {@code null} for an absent parameter value
     * @return the normalized text, empty when {@code value} is {@code null}
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return normalizeIdentifier(value).toLowerCase(Locale.ROOT);
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
