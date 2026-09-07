package com.codemonk.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphQueryServiceTest {

    private GraphQueryService graphQueryService;

    @BeforeEach
    void setUp() {
        graphQueryService = GraphQueryService.fromAdjacencyMap(Map.of(
                "a", List.of("b", "c"),
                "b", List.of("d"),
                "c", List.of("d"),
                "d", List.of()));
    }

    @Test
    void shouldReturnDirectNeighborsInStableOrder() {
        assertEquals(
                List.of("b", "c"),
                graphQueryService.findDirectNeighbors(" a "));
    }

    @Test
    void shouldReturnEmptyWhenNodeHasNoNeighborsOrIsMissing() {
        assertEquals(
                List.of(),
                graphQueryService.findDirectNeighbors("d"));

        assertEquals(
                List.of(),
                graphQueryService.findDirectNeighbors("missing"));
    }

    @Test
    void shouldReturnEmptyForNullOrBlankNodeIds() {
        assertEquals(
                List.of(),
                graphQueryService.findDirectNeighbors(null));

        assertEquals(
                List.of(),
                graphQueryService.findDirectNeighbors("  "));

        assertEquals(
                List.of(),
                graphQueryService.findPath(null, "a"));

        assertEquals(
                List.of(),
                graphQueryService.findPath("a", " "));
    }

    @Test
    void shouldFindShortestPath() {
        assertEquals(
                List.of("a", "b", "d"),
                graphQueryService.findPath("a", "d"));

        assertEquals(
                List.of("a"),
                graphQueryService.findPath("a", "a"));
    }

    @Test
    void shouldReturnEmptyWhenPathDoesNotExist() {
        assertEquals(
                List.of(),
                graphQueryService.findPath("d", "a"));

        assertEquals(
                List.of(),
                graphQueryService.findPath("missing", "a"));
    }

    @Test
    void shouldTerminateAndReturnPathWhenGraphContainsCycle() {
        GraphQueryService cyclicGraph = GraphQueryService.fromAdjacencyMap(Map.of(
                "a", List.of("b"),
                "b", List.of("a", "c"),
                "c", List.of()));

        assertEquals(
                List.of("a", "b", "c"),
                cyclicGraph.findPath("a", "c"));

        assertEquals(
                List.of(),
                cyclicGraph.findPath("c", "a"));
    }

    @Test
    void shouldReturnImmutableResultsAndRejectNullReader() {
        List<String> neighbors =
                graphQueryService.findDirectNeighbors("a");

        assertThrows(
                UnsupportedOperationException.class,
                () -> neighbors.add("x"));

        assertThrows(
                NullPointerException.class,
                () -> new GraphQueryService((java.util.function.Function<String, List<String>>) null));
    }

    @Test
    void shouldRejectNullAdjacencyMap() {
        assertThrows(
                NullPointerException.class,
                () -> GraphQueryService.fromAdjacencyMap(null));
    }
}