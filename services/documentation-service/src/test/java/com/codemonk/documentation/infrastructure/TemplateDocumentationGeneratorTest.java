package com.codemonk.documentation.infrastructure;

import com.codemonk.documentation.application.GeneratedDocumentation;
import com.codemonk.documentation.domain.DocType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateDocumentationGeneratorTest {

    private final TemplateDocumentationGenerator generator = new TemplateDocumentationGenerator();

    @Test
    void buildsReadableTitleAndMarkdownHeading() {
        GeneratedDocumentation doc = generator.generate("repo-1", DocType.API_OVERVIEW);

        assertThat(doc.title()).isEqualTo("Api Overview for repo-1");
        assertThat(doc.markdownContent()).startsWith("# Api Overview for repo-1\n");
        assertThat(doc.markdownContent()).contains("API_OVERVIEW");
    }

    @ParameterizedTest
    @EnumSource(DocType.class)
    void generatesContentForEveryDocType(DocType docType) {
        GeneratedDocumentation doc = generator.generate("repo-1", docType);

        assertThat(doc.title()).isNotBlank().endsWith("for repo-1");
        assertThat(doc.markdownContent()).isNotBlank();
    }
}
