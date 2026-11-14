package com.codemonk.documentation.application;

import com.codemonk.common.exception.InvalidRequestException;
import com.codemonk.common.service.DocumentationResponse;
import com.codemonk.documentation.domain.DocType;
import com.codemonk.documentation.domain.DocumentationEntity;
import com.codemonk.documentation.domain.DocumentationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentationServiceImplTest {

    @Mock
    private DocumentationGenerator generator;

    @Mock
    private DocumentationRepository repository;

    @InjectMocks
    private DocumentationServiceImpl service;

    @Test
    void generateDocDelegatesToGeneratorAndPersistsResult() {
        when(generator.generate("repo-1", DocType.ARCHITECTURE))
                .thenReturn(new GeneratedDocumentation("Architecture", "# Architecture"));
        when(repository.save(any(DocumentationEntity.class))).thenAnswer(invocation -> {
            DocumentationEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return entity;
        });

        DocumentationResponse response = service.generateDoc("repo-1", "ARCHITECTURE");

        ArgumentCaptor<DocumentationEntity> saved = ArgumentCaptor.forClass(DocumentationEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRepositoryId()).isEqualTo("repo-1");
        assertThat(saved.getValue().getDocType()).isEqualTo("ARCHITECTURE");
        assertThat(saved.getValue().getTitle()).isEqualTo("Architecture");
        assertThat(saved.getValue().getMarkdownContent()).isEqualTo("# Architecture");
        assertThat(saved.getValue().getStatus()).isEqualTo(DocumentationServiceImpl.STATUS_GENERATED);

        assertThat(response).isEqualTo(new DocumentationResponse(
                42L, "repo-1", "ARCHITECTURE", "Architecture", "# Architecture", "GENERATED"));
    }

    @Test
    void generateDocNormalizesRepositoryIdAndDocType() {
        when(generator.generate("repo-1", DocType.API_OVERVIEW))
                .thenReturn(new GeneratedDocumentation("API", "# API"));
        when(repository.save(any(DocumentationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentationResponse response = service.generateDoc("  repo-1 ", " api_overview ");

        assertThat(response.repositoryId()).isEqualTo("repo-1");
        assertThat(response.docType()).isEqualTo("API_OVERVIEW");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void generateDocRejectsBlankRepositoryId(String repositoryId) {
        assertThatThrownBy(() -> service.generateDoc(repositoryId, "ARCHITECTURE"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("repositoryId");
        verifyNoInteractions(generator, repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "UNKNOWN_TYPE"})
    void generateDocRejectsMissingOrUnknownDocType(String docType) {
        assertThatThrownBy(() -> service.generateDoc("repo-1", docType))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("docType");
        verifyNoInteractions(generator, repository);
    }

    @Test
    void getDocByIdMapsStoredEntity() {
        DocumentationEntity entity = new DocumentationEntity("repo-1", "ARCHITECTURE", "Arch", "# Arch", "GENERATED");
        entity.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        assertThat(service.getDocById(7L)).contains(new DocumentationResponse(
                7L, "repo-1", "ARCHITECTURE", "Arch", "# Arch", "GENERATED"));
    }

    @Test
    void getDocByIdReturnsEmptyWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.getDocById(99L)).isEmpty();
    }

    @Test
    void getDocByIdRejectsNullId() {
        assertThatThrownBy(() -> service.getDocById(null)).isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void findByRepositoryIdMapsEveryEntity() {
        DocumentationEntity first = new DocumentationEntity("repo-1", "ARCHITECTURE", "A", "# A", "GENERATED");
        first.setId(1L);
        DocumentationEntity second = new DocumentationEntity("repo-1", "API_OVERVIEW", "B", "# B", "GENERATED");
        second.setId(2L);
        when(repository.findByRepositoryId("repo-1")).thenReturn(List.of(first, second));

        List<DocumentationResponse> docs = service.findByRepositoryId(" repo-1 ");

        assertThat(docs).extracting(DocumentationResponse::id).containsExactly(1L, 2L);
        assertThat(docs).extracting(DocumentationResponse::docType).containsExactly("ARCHITECTURE", "API_OVERVIEW");
    }

    @Test
    void findByRepositoryIdReturnsEmptyListWhenNothingStored() {
        when(repository.findByRepositoryId("repo-1")).thenReturn(List.of());

        assertThat(service.findByRepositoryId("repo-1")).isEmpty();
    }

    @Test
    void findByRepositoryIdRejectsBlankRepositoryId() {
        assertThatThrownBy(() -> service.findByRepositoryId(" ")).isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(repository);
    }
}
