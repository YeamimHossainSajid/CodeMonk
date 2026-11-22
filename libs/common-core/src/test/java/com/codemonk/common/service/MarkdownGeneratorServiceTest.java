package com.codemonk.common.service;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MarkdownGeneratorServiceTest {

    @Test
    @DisplayName("Should be an interface declaring generateMarkdown(String, String) returning String")
    void shouldDeclareGenerateMarkdown() throws NoSuchMethodException {
        Method method = MarkdownGeneratorService.class
                .getMethod("generateMarkdown", String.class, String.class);

        assertThat(MarkdownGeneratorService.class.isInterface()).isTrue();
        assertThat(method.getReturnType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("Should delegate to the implementation it is given")
    void shouldDelegateToImplementation() {
        MarkdownGeneratorService generator = (repositoryId, docType) -> "# " + docType + " for " + repositoryId;

        assertThat(generator.generateMarkdown("repo-1", "README")).isEqualTo("# README for repo-1");
    }
}
