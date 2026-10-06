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
    @DisplayName("toResponse should map all fields from CodeNode to NodeResponse")
    public void toResponse_shouldMapAllFields(){
        CodeNode node = new CodeNode("n-1","MyClass","{\"pkg\":\"com.example\"}", List.of("java", "service"));

        NodeResponse response = mapper.toResponse(node);

        assertEquals("n-1",response.id());
        assertEquals("MyClass", response.label());
        assertEquals("{\"pkg\":\"com.example\"}", response.properties());
        assertEquals(List.of("java", "service"), response.tags());
    }

    @Test
    @DisplayName("toDomain should map all fields from NodeResponse to CodeNode")
    public void toDomain_shouldMapAllFields(){
        NodeResponse response = new NodeResponse("n-2","SomeInterface","{}",List.of("api"));

        CodeNode node = mapper.toDomain(response);

        assertEquals("n-2", node.id());
        assertEquals("SomeInterface", node.label());
        assertEquals("{}", node.properties());
        assertEquals(List.of("api"), node.tags());
    }

    @Test
    @DisplayName("toResponse then toDomain round-trip should preserve all values")
    public void roundTrip_toResponseThenToDomain() {
        CodeNode original = new CodeNode("n-3", "Util", "{}", List.of("util"));
        CodeNode result = mapper.toDomain(mapper.toResponse(original));
        assertEquals(original, result);
    }

    @Test
    @DisplayName("toDomain then toResponse round-trip should preserve all values")
    public void roundTrip_toDomainThenToResponse() {
        NodeResponse original = new NodeResponse("n-4", "Repo", "{}", List.of("data"));
        NodeResponse result = mapper.toResponse(mapper.toDomain(original));
        assertEquals(original, result);
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
