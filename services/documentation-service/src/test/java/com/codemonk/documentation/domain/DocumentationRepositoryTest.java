package com.codemonk.documentation.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DocumentationRepositoryTest {

    @Autowired
    private DocumentationRepository repository;

    @Test
    void savesEntityAndAssignsId() {
        DocumentationEntity saved = repository.save(
                new DocumentationEntity("repo-1", "ARCHITECTURE", "Architecture", "# Architecture", "GENERATED"));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(DocumentationEntity::getRepositoryId, DocumentationEntity::getTitle)
                .containsExactly("repo-1", "Architecture");
    }

    @Test
    void findByRepositoryIdReturnsOnlyThatRepositorysDocs() {
        repository.save(new DocumentationEntity("repo-1", "ARCHITECTURE", "A", "# A", "GENERATED"));
        repository.save(new DocumentationEntity("repo-1", "API_OVERVIEW", "B", "# B", "GENERATED"));
        repository.save(new DocumentationEntity("repo-2", "ARCHITECTURE", "C", "# C", "GENERATED"));

        List<DocumentationEntity> docs = repository.findByRepositoryId("repo-1");

        assertThat(docs)
                .extracting(DocumentationEntity::getTitle)
                .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void storesMarkdownContentLongerThanDefaultColumnLength() {
        String longContent = "# Architecture\n\n" + "x".repeat(10_000);

        DocumentationEntity saved = repository.saveAndFlush(
                new DocumentationEntity("repo-1", "ARCHITECTURE", "Architecture", longContent, "GENERATED"));

        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(DocumentationEntity::getMarkdownContent)
                .isEqualTo(longContent);
    }

    @Test
    void findByRepositoryIdReturnsEmptyListForUnknownRepository() {
        assertThat(repository.findByRepositoryId("missing")).isEmpty();
    }
}
