package com.codemonk.common.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;

/**
 * Executes read-only queries against a directed graph.
 *
 * <p>The graph is supplied by the caller so common-core does not depend on a
 * knowledge-service implementation. Neighbor values are read when a query is
 * executed, which keeps the service compatible with remote graph clients.
 */
@Service
public class GraphQueryService {

    private final Function<String, ? extends Collection<String>> outgoingNeighbors;

    /**
     * Creates a query service backed by an outgoing-neighbor reader.
     *
     * @param outgoingNeighbors function returning the outgoing neighbors for a node
     */
    public GraphQueryService(Function<String, ? extends Collection<String>> outgoingNeighbors) {
        this.outgoingNeighbors = Objects.requireNonNull(
                outgoingNeighbors,
                "outgoingNeighbors must not be null");
    }

    /**
     * Creates a query service backed by an adjacency map.
     *
     * <p>This constructor is intended for callers that already have an
     * in-memory adjacency list, such as tests.
     *
     * @param adjacencyMap directed adjacency list
     */
    public static GraphQueryService fromAdjacencyMap(
            Map<String, ? extends Collection<String>> adjacencyMap) {

        Objects.requireNonNull(adjacencyMap, "adjacencyMap must not be null");

        return new GraphQueryService(nodeId -> {
            Collection<String> neighbors = adjacencyMap.get(nodeId);
            return neighbors == null ? List.of() : neighbors;
        });
    }

    /**
     * Returns the direct outgoing neighbors of a node.
     *
     * @param nodeId node identifier
     * @return direct neighbors in reader order, without duplicates
     */
    public List<String> findDirectNeighbors(String nodeId) {
        String normalizedNodeId = normalize(nodeId);

        if (normalizedNodeId.isEmpty()) {
            return List.of();
        }

        Collection<String> neighbors = outgoingNeighbors.apply(normalizedNodeId);

        if (neighbors == null || neighbors.isEmpty()) {
            return List.of();
        }

        return normalizeNeighbors(neighbors);
    }

    /**
     * Finds the shortest directed path from {@code sourceNodeId} to
     * {@code targetNodeId}, including both endpoints.
     *
     * @param sourceNodeId starting node identifier
     * @param targetNodeId destination node identifier
     * @return shortest path, or an empty list when no path exists
     */
    public List<String> findPath(String sourceNodeId, String targetNodeId) {
        String source = normalize(sourceNodeId);
        String target = normalize(targetNodeId);

        if (source.isEmpty() || target.isEmpty()) {
            return List.of();
        }

        if (source.equals(target)) {
            return List.of(source);
        }

        ArrayDeque<List<String>> paths = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        paths.add(List.of(source));
        visited.add(source);

        while (!paths.isEmpty()) {
            List<String> path = paths.removeFirst();
            String current = path.get(path.size() - 1);

            for (String neighbor : findDirectNeighbors(current)) {
                if (!visited.add(neighbor)) {
                    continue;
                }

                List<String> nextPath = new ArrayList<>(path);
                nextPath.add(neighbor);

                if (neighbor.equals(target)) {
                    return List.copyOf(nextPath);
                }

                paths.addLast(List.copyOf(nextPath));
            }
        }

        return List.of();
    }

    private static String normalize(String nodeId) {
        return nodeId == null || nodeId.isBlank() ? "" : nodeId.trim();
    }

    private static List<String> normalizeNeighbors(Collection<String> neighbors) {
        Set<String> normalized = new LinkedHashSet<>();

        for (String neighbor : neighbors) {
            String normalizedNeighbor = normalize(neighbor);

            if (!normalizedNeighbor.isEmpty()) {
                normalized.add(normalizedNeighbor);
            }
        }

        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }
}