package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;


import java.util.ArrayList;
import java.util.List;


public class GraphQueryResultDtoTest {

    @Test
    @DisplayName("of() should populate nodes and edges")
    void ofShouldPopulateNodesAndEdges(){
        GraphQueryResultDto<String,String> result = GraphQueryResultDto.of(List.of("a","b"),List.of("a->b"));

        assertThat(result.nodes()).containsExactly("a","b");
        assertThat(result.edges()).containsExactly("a->b");
        assertThat(result.nodesCount()).isEqualTo(2);
        assertThat(result.edgesCount()).isEqualTo(1);
        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("of() should normalize null nodes and edges to empty lists")
    void ofShouldNormalizeNulls(){
        GraphQueryResultDto<String,String> result = GraphQueryResultDto.of(null,null);

        assertThat(result.nodes()).isEmpty();
        assertThat(result.edges()).isEmpty();
        assertThat(result.nodesCount()).isZero();
        assertThat(result.edgesCount()).isZero();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("empty() should return a DTO with no nodes or edges")
    void emptyShouldReturnEmptyDto() {
        GraphQueryResultDto<String, String> result = GraphQueryResultDto.empty();

        assertThat(result.nodes()).isEmpty();
        assertThat(result.edges()).isEmpty();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty() should be false when only nodes are present")
    void isEmptyShouldBeFalseWithOnlyNodes() {
        GraphQueryResultDto<String, String> result =
                new GraphQueryResultDto<>(List.of("a"), List.of());

        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty() should be false when only edges are present")
    void isEmptyShouldBeFalseWithOnlyEdges() {
        GraphQueryResultDto<String, String> result =
                new GraphQueryResultDto<>(List.of(), List.of("a->b"));

        assertThat(result.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("counts should reflect the number of nodes and edges")
    void countsShouldReflectSizes() {
        GraphQueryResultDto<String, String> result =
                GraphQueryResultDto.of(List.of("a", "b", "c"), List.of("a->b", "b->c"));

        assertThat(result.nodesCount()).isEqualTo(3);
        assertThat(result.edgesCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should defensively copy nodes and edges")
    void shouldDefensivelyCopyLists() {
        List<String> nodes = new ArrayList<>(List.of("a"));
        List<String> edges = new ArrayList<>(List.of("a->b"));

        GraphQueryResultDto<String, String> result = GraphQueryResultDto.of(nodes, edges);

        nodes.add("evil");
        edges.add("evil->edge");

        assertThat(result.nodes()).containsExactly("a");
        assertThat(result.edges()).containsExactly("a->b");
    }

    @Test
    @DisplayName("Should expose immutable node and edge lists")
    void shouldExposeImmutableLists() {
        GraphQueryResultDto<String, String> result =
                GraphQueryResultDto.of(List.of("a"), List.of("a->b"));

        assertThatThrownBy(() -> result.nodes().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.edges().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should support arbitrary node and edge types via generics")
    void shouldSupportGenericTypes() {
        GraphQueryResultDto<Integer, String> result =
                GraphQueryResultDto.of(List.of(1, 2, 3), List.of("1->2"));

        assertThat(result.nodes()).containsExactly(1, 2, 3);
        assertThat(result.edges()).containsExactly("1->2");
    }

    @Test
    @DisplayName("Should be annotated with @Component")
    void shouldBeAnnotatedWithComponent() {
        assertThat(GraphQueryResultDto.class.isAnnotationPresent(Component.class)).isTrue();
    }

    @Test
    @DisplayName("Should be annotated with @JsonInclude(NON_NULL)")
    void shouldBeAnnotatedWithJsonIncludeNonNull() {
        JsonInclude include = GraphQueryResultDto.class.getAnnotation(JsonInclude.class);

        assertThat(include).isNotNull();
        assertThat(include.value()).isEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("Should serialize nodes and edges as JSON")
    void shouldSerializeToJson() throws Exception {
        GraphQueryResultDto<String, String> result =
                GraphQueryResultDto.of(List.of("a", "b"), List.of("a->b"));

        String json = new ObjectMapper().writeValueAsString(result);

        assertThat(json).contains("\"nodes\":[\"a\",\"b\"]");
        assertThat(json).contains("\"edges\":[\"a->b\"]");
    }


}
