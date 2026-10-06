package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RelationshipResponseTest {

    @Test
    @DisplayName("Should retain all provided values")
    void shouldRetainProvidedValues() {
        RelationshipResponse response = new RelationshipResponse("42", "n-1", "n-2", "CALLS");

        assertThat(response.id()).isEqualTo("42");
        assertThat(response.sourceNodeId()).isEqualTo("n-1");
        assertThat(response.targetNodeId()).isEqualTo("n-2");
        assertThat(response.type()).isEqualTo("CALLS");
    }

    @Test
    @DisplayName("Should normalize null fields to empty string")
    void shouldNormalizeNullFields() {
        RelationshipResponse response = new RelationshipResponse(null, null, null, null);

        assertThat(response.id()).isEmpty();
        assertThat(response.sourceNodeId()).isEmpty();
        assertThat(response.targetNodeId()).isEmpty();
        assertThat(response.type()).isEmpty();
    }

    @Test
    @DisplayName("Should normalize blank fields to empty string")
    void shouldNormalizeBlankFields() {
        RelationshipResponse response = new RelationshipResponse("   ", " ", "\t", "\n");

        assertThat(response.id()).isEmpty();
        assertThat(response.sourceNodeId()).isEmpty();
        assertThat(response.targetNodeId()).isEmpty();
        assertThat(response.type()).isEmpty();
    }

    @Test
    @DisplayName("Should trim surrounding whitespace from fields")
    void shouldTrimSurroundingWhitespace() {
        RelationshipResponse response = new RelationshipResponse(" 42 ", " n-1", "n-2 ", " CALLS ");

        assertThat(response.id()).isEqualTo("42");
        assertThat(response.sourceNodeId()).isEqualTo("n-1");
        assertThat(response.targetNodeId()).isEqualTo("n-2");
        assertThat(response.type()).isEqualTo("CALLS");
    }

    @Test
    @DisplayName("Records with same values should be equal")
    void shouldBeValueBased() {
        RelationshipResponse a = new RelationshipResponse("1", "n-1", "n-2", "IMPORTS");
        RelationshipResponse b = new RelationshipResponse("1", "n-1", "n-2", "IMPORTS");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Records with different values should not be equal")
    void shouldNotBeEqualForDifferentValues() {
        RelationshipResponse a = new RelationshipResponse("1", "n-1", "n-2", "IMPORTS");
        RelationshipResponse b = new RelationshipResponse("2", "n-3", "n-4", "CALLS");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("Should be annotated with @JsonInclude(NON_NULL)")
    void shouldBeAnnotatedWithJsonIncludeNonNull() {
        JsonInclude include = RelationshipResponse.class.getAnnotation(JsonInclude.class);

        assertThat(include).isNotNull();
        assertThat(include.value()).isEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("Should serialize to JSON with expected field names")
    void shouldSerializeToJson() throws Exception {
        RelationshipResponse response = new RelationshipResponse("42", "n-1", "n-2", "CALLS");

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"id\":\"42\"");
        assertThat(json).contains("\"sourceNodeId\":\"n-1\"");
        assertThat(json).contains("\"targetNodeId\":\"n-2\"");
        assertThat(json).contains("\"type\":\"CALLS\"");
    }

    @Test
    @DisplayName("Should serialize normalized fields (never null) so NON_NULL omits nothing")
    void shouldSerializeAllFieldsWhenOriginallyNull() throws Exception {
        RelationshipResponse response = new RelationshipResponse(null, null, null, null);

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"id\":\"\"");
        assertThat(json).contains("\"sourceNodeId\":\"\"");
        assertThat(json).contains("\"targetNodeId\":\"\"");
        assertThat(json).contains("\"type\":\"\"");
    }
}
