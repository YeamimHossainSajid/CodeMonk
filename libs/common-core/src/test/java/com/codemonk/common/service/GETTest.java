package com.codemonk.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GETTest
 */
public class GETTest {

    private GET getService;

    @BeforeEach
    void setUp() {
        getService = new GET();
    }

    @Test
    @DisplayName("Should return empty neighborhood when only nodeId is provided")
    void shouldReturnEmptyNeighborhoodForNodeId() {

        GET.Neighborhood result = getService.neighbors("node-123");

        assertEquals("node-123", result.nodeId());
        assertTrue(result.incoming().isEmpty());
        assertTrue(result.outgoing().isEmpty());
        assertTrue(result.isIsolated());
        assertEquals(0, result.totalDegree());
    }

    @Test
    @DisplayName("Should handle null or blank nodeId gracefully")
    void shouldHandleNullOrBlankedNodeId() {

        GET.Neighborhood nullResult = getService.neighbors(null);
        assertEquals("", nullResult.nodeId());
        assertTrue(nullResult.isIsolated());

        GET.Neighborhood blankResult = getService.neighbors(" ");
        assertEquals("", blankResult.nodeId());
        assertTrue(blankResult.isIsolated());

    }

    @Test
    @DisplayName("Should create neighborhood with incoming and outgoing edges")
    void shouldCreateNeighborhoodWithIncomingAndOutgoingEdges(){
        
        List<String> incoming = List.of("node-A","node-B");
        List<String> outcoming = List.of("node-C");

        GET.Neighborhood result = getService.neighbors("node-123", incoming, outcoming);

        assertEquals("node-123",result.nodeId());
        assertEquals(2, result.incoming().size());
        assertEquals(List.of("node-A","node-B"), result.incoming());
        assertEquals(List.of("node-C"), result.outgoing());
        assertFalse(result.isIsolated());
        assertEquals(3, result.totalDegree());
    }

    @Test
    @DisplayName("Should handle null incoming and outgoing edge lists safely")
    void shouldHandleNullEdgeListsSafely(){

        GET.Neighborhood result = getService.neighbors("node-123",null,null);
        assertEquals("node-123", result.nodeId());
        assertTrue(result.incoming().isEmpty());
        assertTrue(result.outgoing().isEmpty());
        assertTrue(result.isIsolated());
        assertEquals(0, result.totalDegree());
    }

    @Test
    @DisplayName("Should trim nodeId whitespace properly")
    void shouldTrimNodeId(){
        GET.Neighborhood result = getService.neighbors("  node-xyz  ");
        assertEquals("node-xyz", result.nodeId());
    }
}