package com.codemonk.common.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class NodeMapperTest {

    private NodeMapper mapper;

    @BeforeEach
    void setUp(){
        mapper = new NodeMapper();
    }

    @Test
    @DisplayName("toResponse should map node fields onto the NodeResponse DTO")
    public void toResponse_shouldMapAllFields(){
        CodeNode node = new CodeNode(
                "n-1",
                "MyClass",
                "CLASS",
                "com.example.MyClass",
                "repo-1");

        NodeResponse response = mapper.toResponse(node);

        assertEquals("n-1", response.id());
        assertEquals("MyClass", response.label());
        assertEquals("com.example.MyClass", response.properties());
        assertEquals(List.of("CLASS", "repo-1"), response.tags());
    }

    @Test
    @DisplayName("toDomain should map NodeResponse fields onto the CodeNode entity")
    public void toDomain_shouldMapAllFields(){
        NodeResponse response = new NodeResponse(
                "n-2",
                "SomeInterface",
                "com.example.SomeInterface",
                List.of("INTERFACE", "repo-2"));

        CodeNode node = mapper.toDomain(response);

        assertEquals("n-2", node.nodeId());
        assertEquals("SomeInterface", node.name());
        assertEquals("INTERFACE", node.entityType());
        assertEquals("com.example.SomeInterface", node.qualifiedName());
        assertEquals("repo-2", node.repositoryId());
    }

    @Test
    @DisplayName("toResponse then toDomain round-trip should preserve all values")
    public void roundTrip_toResponseThenToDomain() {
        CodeNode original = new CodeNode(
                "n-3",
                "Util",
                "CLASS",
                "com.example.Util",
                "repo-3");

        CodeNode result = mapper.toDomain(mapper.toResponse(original));

        assertEquals(original, result);
    }

    @Test
    @DisplayName("toDomain then toResponse round-trip should preserve all values")
    public void roundTrip_toDomainThenToResponse() {
        NodeResponse original = new NodeResponse(
                "n-4",
                "Repo",
                "com.example.Repo",
                List.of("CLASS", "repo-4"));

        NodeResponse result = mapper.toResponse(mapper.toDomain(original));

        assertEquals(original, result);
    }

    @Test
    @DisplayName("toResponse should omit blank entityType and repositoryId from tags")
    public void toResponse_shouldOmitBlankTagValues() {
        NodeResponse response = mapper.toResponse(new CodeNode("n-5", "Util", "", "", ""));

        assertEquals(List.of(), response.tags());
    }

    @Test
    @DisplayName("toResponse should keep tag positions when entityType is blank")
    public void toResponse_shouldKeepRepositoryIdPosition() {
        NodeResponse response = mapper.toResponse(new CodeNode("n-6", "Util", "", "com.example.Util", "repo-6"));

        assertEquals(List.of("", "repo-6"), response.tags());
    }

    @Test
    @DisplayName("toDomain should tolerate a response without tags")
    public void toDomain_shouldTolerateMissingTags() {
        CodeNode node = mapper.toDomain(new NodeResponse("n-7", "Util", "com.example.Util", List.of()));

        assertEquals("", node.entityType());
        assertEquals("", node.repositoryId());
    }

    @Test
    @DisplayName("toResponse should throw NullPointerException for null input")
    public void toResponse_shouldThrowOnNull() {
        assertThrows(NullPointerException.class, () -> mapper.toResponse(null));
    }

    @Test
    @DisplayName("toDomain should throw NullPointerException for null input")
    public void toDomain_shouldThrowOnNull() {
        assertThrows(NullPointerException.class, () -> mapper.toDomain(null));
    }

    @Test
    @DisplayName("NodeMapper should be annotated with @Component")
    public void shouldBeAnnotatedWithComponent() {
        assertTrue(NodeMapper.class.isAnnotationPresent(Component.class));
    }

}
