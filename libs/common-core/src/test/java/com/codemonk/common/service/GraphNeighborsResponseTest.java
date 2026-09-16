package com.codemonk.common.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GraphNeighborsResponseTest {

    @Test
    @DisplayName("Should create response with incoming and outgoing relationship nodes")
    void shouldCreateResponseWithEdges() {
        GraphNeighborsResponse response = GraphNeighborsResponse.of(
                "node-123", List.of("node-A", "node-B"), List.of("node-C"));

        assertThat(response.nodeId()).isEqualTo("node-123");
        assertThat(response.incoming()).containsExactly("node-A", "node-B");
        assertThat(response.outgoing()).containsExactly("node-C");
        assertThat(response.isIsolated()).isFalse();
        assertThat(response.totalDegree()).isEqualTo(3);

    }

    @Test
    @DisplayName("Should create empty response with only nodeId")
    void shouldCreateEmptyResponse(){
        GraphNeighborsResponse response = GraphNeighborsResponse.empty("node-123");

        assertThat(response.nodeId()).isEqualTo("node-123");
        assertThat(response.incoming()).isEmpty();
        assertThat(response.outgoing()).isEmpty();
        assertThat(response.isIsolated()).isTrue();
        assertThat(response.totalDegree()).isZero();
    }


    @Test 
    @DisplayName("Should normalize null or blank nodeId to empty string")
    void shouldHandleNullOrBlankNodeId(){
        assertThat(GraphNeighborsResponse.empty(null).nodeId()).isEmpty();
        assertThat(GraphNeighborsResponse.empty("    ").nodeId()).isEmpty();
    }


    @Test 
    @DisplayName ("Should handle null edge lists safely")
    void shouldhandleNullEdgeLists(){
        GraphNeighborsResponse response =   GraphNeighborsResponse.of("node-123",null,null);

        assertThat(response.incoming()).isEmpty();
        assertThat(response.outgoing()).isEmpty();
        assertThat(response.isIsolated()).isTrue();
    }

    @Test 
    @DisplayName ("Should trim nodeId whitespace")
    void shouldTrimNodeId(){
        GraphNeighborsResponse response = GraphNeighborsResponse.of("   node-xyz    ",List.of(),List.of());
        assertThat(response.nodeId()).isEqualTo("node-xyz");
    }


    @Test 
    @DisplayName("Should defensively copy edge lists") 
    void shouldDefensivelyCopyEdgeLists(){
        List<String> incoming = new java.util.ArrayList<>(List.of("node-A"));
        GraphNeighborsResponse response = GraphNeighborsResponse.of("node-1",incoming,List.of("node-B"));

        incoming.add("node-Evil");
        assertThat(response.incoming()).containsExactly("node-A");
    }
}
