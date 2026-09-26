package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codemonk.common.cache.GraphCacheKeyGenerator.Direction;

class GraphCacheKeyGeneratorTest {

    private GraphCacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new GraphCacheKeyGenerator();
    }

    @Test
    void shouldNamespaceASingleNodeLookup() {
        assertEquals("graph:node:OrderService", keyGenerator.forNode("OrderService"));
    }

    @Test
    void shouldPreserveTheCaseOfANodeIdentifier() {
        assertNotEquals(keyGenerator.forNode("OrderService"), keyGenerator.forNode("orderservice"));
    }

    @Test
    void shouldFoldAwaySurroundingAndRepeatedWhitespaceInANodeIdentifier() {
        assertEquals("graph:node:Order Service", keyGenerator.forNode("  Order    \t Service  "));
    }

    @Test
    void shouldDefaultANeighborTraversalToDirectNeighbors() {
        assertEquals(
                "graph:neighbors:OrderService:outgoing:depth=1",
                keyGenerator.forNeighbors("OrderService", Direction.OUTGOING));
    }

    @Test
    void shouldSeparateTheDirectionsOfATraversal() {
        String incoming = keyGenerator.forNeighbors("OrderService", Direction.INCOMING);
        String outgoing = keyGenerator.forNeighbors("OrderService", Direction.OUTGOING);
        String both = keyGenerator.forNeighbors("OrderService", Direction.BOTH);

        assertEquals("graph:neighbors:OrderService:incoming:depth=1", incoming);
        assertEquals("graph:neighbors:OrderService:both:depth=1", both);
        assertNotEquals(incoming, outgoing);
    }

    @Test
    void shouldSeparateTraversalsOfDifferentDepths() {
        assertEquals(
                "graph:neighbors:OrderService:outgoing:depth=2",
                keyGenerator.forNeighbors("OrderService", Direction.OUTGOING, 2));
        assertNotEquals(
                keyGenerator.forNeighbors("OrderService", Direction.OUTGOING, 1),
                keyGenerator.forNeighbors("OrderService", Direction.OUTGOING, 2));
    }

    @Test
    void shouldKeepTheEndpointsOfAPathInTheOrderGiven() {
        assertEquals(
                "graph:path:OrderService:PaymentService",
                keyGenerator.forPath("OrderService", "PaymentService"));
        assertNotEquals(
                keyGenerator.forPath("OrderService", "PaymentService"),
                keyGenerator.forPath("PaymentService", "OrderService"));
    }

    @Test
    void shouldSeparateOperationsOverTheSameNode() {
        assertNotEquals(
                keyGenerator.forNode("OrderService"),
                keyGenerator.forNeighbors("OrderService", Direction.OUTGOING));
    }

    @Test
    void shouldHonourACallerChosenNamespace() {
        assertEquals("symbols:node:OrderService", keyGenerator.forNode("symbols", "OrderService"));
        assertEquals(
                "symbols:neighbors:OrderService:outgoing:depth=3",
                keyGenerator.forNeighbors("symbols", "OrderService", Direction.OUTGOING, 3));
        assertEquals(
                "symbols:path:OrderService:PaymentService",
                keyGenerator.forPath("symbols", "OrderService", "PaymentService"));
    }

    @Test
    void shouldFoldAwayTheCaseOfANamespaceAndAnOperation() {
        assertEquals(
                "symbols:calls:OrderService",
                keyGenerator.generate("SYMBOLS", "Calls", List.of("OrderService"), Map.of()));
    }

    @Test
    void shouldAppendParametersAsSortedPairs() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("limit", "10");
        parameters.put("edge", "calls");

        assertEquals(
                "graph:traverse:OrderService:edge=calls|limit=10",
                keyGenerator.generate(GraphCacheKeyGenerator.DEFAULT_NAMESPACE, "traverse",
                        List.of("OrderService"), parameters));
    }

    @Test
    void shouldProduceTheSameKeyRegardlessOfParameterOrder() {
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("edge", "calls");
        ordered.put("limit", "10");

        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("limit", "10");
        reversed.put("edge", "calls");

        assertEquals(
                keyGenerator.generate("graph", "traverse", List.of("OrderService"), ordered),
                keyGenerator.generate("graph", "traverse", List.of("OrderService"), reversed));
    }

    @Test
    void shouldProduceTheSameKeyRegardlessOfMapImplementation() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("edge", "calls");
        hashMap.put("limit", "10");

        assertEquals(
                keyGenerator.generate("graph", "traverse", List.of("OrderService"), Map.copyOf(hashMap)),
                keyGenerator.generate("graph", "traverse", List.of("OrderService"), hashMap));
    }

    @Test
    void shouldOmitTheParameterSegmentWhenThereAreNoParameters() {
        String withoutParameters = keyGenerator.generate("graph", "traverse", List.of("OrderService"), Map.of());

        assertEquals("graph:traverse:OrderService", withoutParameters);
        assertEquals(withoutParameters,
                keyGenerator.generate("graph", "traverse", List.of("OrderService"), null));
    }

    @Test
    void shouldReplaceTheCharactersThatDelimitSegments() {
        assertEquals("graph:node:repo_Order_Service", keyGenerator.forNode("repo:Order|Service"));
        assertEquals(
                "graph:traverse:OrderService:edge_kind=_calls",
                keyGenerator.generate("graph", "traverse", List.of("OrderService"),
                        Map.of("edge=kind", "=calls")));
    }

    @Test
    void shouldTreatAMissingParameterValueAsEmpty() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("edge", null);

        assertEquals(
                "graph:traverse:OrderService:edge=",
                keyGenerator.generate("graph", "traverse", List.of("OrderService"), parameters));
    }

    @Test
    void shouldRejectANodeIdentifierThatIsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.forNode(null));
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.forNode("   "));
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.forNeighbors(null, Direction.OUTGOING));
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.forPath("OrderService", null));
    }

    @Test
    void shouldRejectANamespaceThatIsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.forNode(null, "OrderService"));
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.forNode("  ", "OrderService"));
    }

    @Test
    void shouldRejectAnOperationThatIsNullOrBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.generate("graph", null, List.of("OrderService"), Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.generate("graph", "  ", List.of("OrderService"), Map.of()));
    }

    @Test
    void shouldRejectADirectionThatIsNull() {
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.forNeighbors("OrderService", null));
    }

    @Test
    void shouldRejectADepthBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.forNeighbors("OrderService", Direction.OUTGOING, 0));
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.forNeighbors("OrderService", Direction.OUTGOING, -1));
    }

    @Test
    void shouldRejectAQueryThatNamesNoNodes() {
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.generate("graph", "traverse", List.of(), Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.generate("graph", "traverse", null, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.generate("graph", "traverse", Arrays.asList("OrderService", null), Map.of()));
    }

    @Test
    void shouldRejectAParameterNameThatIsBlank() {
        Map<String, String> parameters = Map.of("  ", "calls");

        assertThrows(IllegalArgumentException.class,
                () -> keyGenerator.generate("graph", "traverse", List.of("OrderService"), parameters));
    }

    @Test
    void shouldReturnTheSameKeyForRepeatedCalls() {
        assertEquals(
                keyGenerator.forNeighbors("OrderService", Direction.OUTGOING, 2),
                keyGenerator.forNeighbors("OrderService", Direction.OUTGOING, 2));
    }
}
