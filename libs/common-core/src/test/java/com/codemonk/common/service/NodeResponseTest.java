package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class NodeResponseTest {


    @Test
    @DisplayName("Should retain all provided values")
    void shouldRetainProvideValues(){
        NodeResponse response = new NodeResponse("n-1","UserService","{\"a\":1}", List.of("api","data"));

        assertThat(response.id()).isEqualTo("n-1");
        assertThat(response.label()).isEqualTo("UserService");
        assertThat(response.properties()).isEqualTo("{\"a\":1}");
        assertThat(response.tags()).containsExactly("api","data");
    }

    @Test
    @DisplayName("Should normalize null fields to empty values")
    void shouldNormalizeNullFields(){
        NodeResponse response = new NodeResponse(null,null,null,null);

        assertThat(response.id()).isEmpty();
        assertThat(response.label()).isEmpty();
        assertThat(response.properties()).isEmpty();
        assertThat(response.tags()).isEmpty();
    }

    @Test
    @DisplayName("Should normalize blank id and label to empty strings")
    void shouldNormalizeBlankFields(){
        NodeResponse response = new NodeResponse("   ","\t",null,null);

        assertThat(response.id()).isEmpty();
        assertThat(response.label()).isEmpty();
    }

    @Test
    @DisplayName("Should trim surrounding whitespace from id and label")
    void shouldTrimSurroundingWhitespace(){
        NodeResponse response = new NodeResponse(" n-1 "," UserService ","{\"a\":1}",List.of("api"));

        assertThat(response.id()).isEqualTo("n-1");
        assertThat(response.label()).isEqualTo("UserService");

    }

    @Test
    @DisplayName("Should keep properties as-is without trimming")
    void shouldKeepPropertiesAsIs(){
        NodeResponse  response = new NodeResponse("n-1","Label"," padded  ",List.of());

        assertThat(response.properties()).isEqualTo(" padded  ");
    }

    @Test
    @DisplayName("Should defensively copy the tags list")
    void shouldDefensivelyCopyTags(){
        List<String> mutableTags = new ArrayList<>(List.of("api"));
        NodeResponse response = new NodeResponse("n-1","Label","{}",mutableTags);

        mutableTags.add("mutated");

        assertThat(response.tags()).containsExactly("api");
        assertThat(response.tags()).isUnmodifiable();
    }

    @Test
    @DisplayName("Records with same values should be equal")
    void shouldBeValueBased(){
        NodeResponse a = new NodeResponse("n-1","Label","{}",List.of("api"));
        NodeResponse b = new NodeResponse("n-1","Label","{}",List.of("api"));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Records with different values should not be equal")
    void shouldNotBeEqualForDifferentValues(){
        NodeResponse a = new NodeResponse("n-1","Label","{}",List.of("api"));
        NodeResponse b = new NodeResponse("n-2","Other","{}",List.of("data"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("Should be annotated with @JsonInclude(NON_NULL)")
    void shouldBeAnnotatedWithJsonIncludeNonNull(){
        JsonInclude include = NodeResponse.class.getAnnotation(JsonInclude.class);

        assertThat(include).isNotNull();
        assertThat(include.value()).isEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("Should serialize to JSON with expected field names")
    void shouldSerializeToJson() throws Exception {
        NodeResponse response = new NodeResponse("n-1", "UserService", "{\"a\":1}", List.of("api"));

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"id\":\"n-1\"");
        assertThat(json).contains("\"label\":\"UserService\"");
        assertThat(json).contains("\"properties\":\"{\\\"a\\\":1}\"");
        assertThat(json).contains("\"tags\":[\"api\"]");
    }

    @Test
    @DisplayName("Should serialize normalized fields (never null) so NON_NULL omits nothing")
    void shouldSerializeAllFieldsWhenOriginallyNull() throws Exception {
        NodeResponse response = new NodeResponse(null, null, null, null);

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"id\":\"\"");
        assertThat(json).contains("\"label\":\"\"");
        assertThat(json).contains("\"properties\":\"\"");
        assertThat(json).contains("\"tags\":[]");
    }
}
