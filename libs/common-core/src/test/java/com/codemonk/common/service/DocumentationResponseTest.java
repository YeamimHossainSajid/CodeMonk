package com.codemonk.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentationResponseTest {

    @Test
    @DisplayName("Should keep the values it is given")
    void shouldKeepItsValues() {
        DocumentationResponse response = new DocumentationResponse(
                7L, "repo-1", "README", "Getting started", "# Getting started", "COMPLETED");

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.repositoryId()).isEqualTo("repo-1");
        assertThat(response.docType()).isEqualTo("README");
        assertThat(response.title()).isEqualTo("Getting started");
        assertThat(response.markdownContent()).isEqualTo("# Getting started");
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should normalize null and blank text to empty strings")
    void shouldNormalizeNullAndBlankText() {
        DocumentationResponse response = new DocumentationResponse(null, null, "   ", null, null, "  ");

        assertThat(response.id()).isNull();
        assertThat(response.repositoryId()).isEmpty();
        assertThat(response.docType()).isEmpty();
        assertThat(response.title()).isEmpty();
        assertThat(response.markdownContent()).isEmpty();
        assertThat(response.status()).isEmpty();
    }

    @Test
    @DisplayName("Should trim surrounding whitespace but keep markdown untouched")
    void shouldTrimTextButKeepMarkdown() {
        DocumentationResponse response = new DocumentationResponse(
                1L, "  repo-1  ", "  API  ", "  Title  ", "  # Title\n", "  PENDING  ");

        assertThat(response.repositoryId()).isEqualTo("repo-1");
        assertThat(response.docType()).isEqualTo("API");
        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.markdownContent()).isEqualTo("  # Title\n");
        assertThat(response.status()).isEqualTo("PENDING");
    }
}
