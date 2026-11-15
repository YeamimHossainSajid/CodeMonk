package com.codemonk.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeNodeTest {

    @Test
    @DisplayName("Should retain all provided values")
    void shouldRetainProvidedValues() {
        CodeNode node = new CodeNode(
                "node-1",
                "OrderService",
                "CLASS",
                "com.codemonk.demo.OrderService",
                "repo-1");

        assertThat(node.nodeId()).isEqualTo("node-1");
        assertThat(node.name()).isEqualTo("OrderService");
        assertThat(node.entityType()).isEqualTo("CLASS");
        assertThat(node.qualifiedName()).isEqualTo("com.codemonk.demo.OrderService");
        assertThat(node.repositoryId()).isEqualTo("repo-1");
    }

    @Test
    @DisplayName("Should normalize null fields to empty values")
    void shouldNormalizeNullFields() {
        CodeNode node = new CodeNode(null, null, null, null, null);

        assertThat(node.nodeId()).isEmpty();
        assertThat(node.name()).isEmpty();
        assertThat(node.entityType()).isEmpty();
        assertThat(node.qualifiedName()).isEmpty();
        assertThat(node.repositoryId()).isEmpty();
    }

    @Test
    @DisplayName("Should trim surrounding whitespace and blank values")
    void shouldTrimAndNormalizeBlankValues() {
        CodeNode node = new CodeNode(
                "  node-1  ",
                "  OrderService  ",
                "  CLASS  ",
                "  com.codemonk.demo.OrderService  ",
                "  repo-1  ");

        assertThat(node.nodeId()).isEqualTo("node-1");
        assertThat(node.name()).isEqualTo("OrderService");
        assertThat(node.entityType()).isEqualTo("CLASS");
        assertThat(node.qualifiedName()).isEqualTo("com.codemonk.demo.OrderService");
        assertThat(node.repositoryId()).isEqualTo("repo-1");

        CodeNode blank = new CodeNode("   ", "\t", "\n", " ", "  ");

        assertThat(blank.nodeId()).isEmpty();
        assertThat(blank.name()).isEmpty();
        assertThat(blank.entityType()).isEmpty();
        assertThat(blank.qualifiedName()).isEmpty();
        assertThat(blank.repositoryId()).isEmpty();
    }

    @Test
    @DisplayName("Nodes with the same values should be equal")
    void shouldBeValueBased() {
        CodeNode a = new CodeNode("node-1", "OrderService", "CLASS", "com.codemonk.demo.OrderService", "repo-1");
        CodeNode b = new CodeNode("node-1", "OrderService", "CLASS", "com.codemonk.demo.OrderService", "repo-1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Nodes with different values should not be equal")
    void shouldNotBeEqualForDifferentValues() {
        CodeNode a = new CodeNode("node-1", "OrderService", "CLASS", "com.codemonk.demo.OrderService", "repo-1");
        CodeNode b = new CodeNode("node-2", "OrderService", "CLASS", "com.codemonk.demo.OrderService", "repo-1");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("Should serialize with the expected field names")
    void shouldSerializeWithExpectedFieldNames() throws Exception {
        CodeNode node = new CodeNode("node-1", "OrderService", "CLASS", "com.codemonk.demo.OrderService", "repo-1");

        String json = new ObjectMapper().writeValueAsString(node);

        assertThat(json).contains("\"nodeId\":\"node-1\"");
        assertThat(json).contains("\"name\":\"OrderService\"");
        assertThat(json).contains("\"entityType\":\"CLASS\"");
        assertThat(json).contains("\"qualifiedName\":\"com.codemonk.demo.OrderService\"");
        assertThat(json).contains("\"repositoryId\":\"repo-1\"");
    }
}
