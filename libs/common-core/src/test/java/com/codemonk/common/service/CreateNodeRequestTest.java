package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateNodeRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Should retain all provided values")
    void shouldRetainProvidedValues() {
        CreateNodeRequest request = new CreateNodeRequest("UserService", "{\"a\":1}", List.of("api", "data"));

        assertThat(request.label()).isEqualTo("UserService");
        assertThat(request.properties()).isEqualTo("{\"a\":1}");
        assertThat(request.tags()).containsExactly("api", "data");
    }

    @Test
    @DisplayName("Should normalize null optional fields")
    void shouldNormalizeNullOptionalFields() {
        CreateNodeRequest request = new CreateNodeRequest(null, null, null);

        assertThat(request.label()).isNull();
        assertThat(request.properties()).isEmpty();
        assertThat(request.tags()).isEmpty();
    }

    @Test
    @DisplayName("Should trim label but keep properties as-is")
    void shouldTrimLabelOnly() {
        CreateNodeRequest request = new CreateNodeRequest("  UserService  ", " padded ", List.of("api"));

        assertThat(request.label()).isEqualTo("UserService");
        assertThat(request.properties()).isEqualTo(" padded ");
    }

    @Test
    @DisplayName("Should defensively copy the tags list")
    void shouldDefensivelyCopyTags() {
        List<String> mutableTags = new ArrayList<>(List.of("api"));
        CreateNodeRequest request = new CreateNodeRequest("UserService", "{}", mutableTags);

        mutableTags.add("mutated");

        assertThat(request.tags()).containsExactly("api");
        assertThat(request.tags()).isUnmodifiable();
    }

    @Test
    @DisplayName("Should be annotated with @JsonInclude(NON_NULL)")
    void shouldBeAnnotatedWithJsonIncludeNonNull() {
        JsonInclude include = CreateNodeRequest.class.getAnnotation(JsonInclude.class);

        assertThat(include).isNotNull();
        assertThat(include.value()).isEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("Should serialize to JSON with expected field names")
    void shouldSerializeToJson() throws Exception {
        CreateNodeRequest request = new CreateNodeRequest("UserService", "{\"a\":1}", List.of("api"));

        String json = new ObjectMapper().writeValueAsString(request);

        assertThat(json).contains("\"label\":\"UserService\"");
        assertThat(json).contains("\"properties\":\"{\\\"a\\\":1}\"");
        assertThat(json).contains("\"tags\":[\"api\"]");
    }

    @Test
    @DisplayName("Should reject a blank label")
    void shouldRejectBlankLabel() {
        assertThat(validator.validate(new CreateNodeRequest("   ", "{}", List.of()))).isNotEmpty();
    }

    @Test
    @DisplayName("Should reject a null label")
    void shouldRejectNullLabel() {
        assertThat(validator.validate(new CreateNodeRequest(null, "{}", List.of()))).isNotEmpty();
    }

    @Test
    @DisplayName("Should reject a blank tag")
    void shouldRejectBlankTag() {
        assertThat(validator.validate(new CreateNodeRequest("UserService", "{}", List.of(" ")))).isNotEmpty();
    }

    @Test
    @DisplayName("Should accept a valid request")
    void shouldAcceptValidRequest() {
        assertThat(validator.validate(new CreateNodeRequest("UserService", "{}", List.of("api")))).isEmpty();
    }
}