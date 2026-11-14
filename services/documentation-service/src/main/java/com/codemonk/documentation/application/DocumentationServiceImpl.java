package com.codemonk.documentation.application;

import com.codemonk.common.exception.InvalidRequestException;
import com.codemonk.common.service.DocumentationResponse;
import com.codemonk.common.service.DocumentationService;
import com.codemonk.documentation.domain.DocType;
import com.codemonk.documentation.domain.DocumentationEntity;
import com.codemonk.documentation.domain.DocumentationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@link DocumentationService} backed by a {@link DocumentationGenerator} for content
 * and a {@link DocumentationRepository} for storage.
 */
@Service
@Transactional(readOnly = true)
public class DocumentationServiceImpl implements DocumentationService {

    static final String STATUS_GENERATED = "GENERATED";

    private final DocumentationGenerator generator;
    private final DocumentationRepository repository;

    public DocumentationServiceImpl(DocumentationGenerator generator, DocumentationRepository repository) {
        this.generator = generator;
        this.repository = repository;
    }

    @Override
    @Transactional
    public DocumentationResponse generateDoc(String repositoryId, String docType) {
        String normalizedRepositoryId = requireRepositoryId(repositoryId);
        DocType type = parseDocType(docType);

        GeneratedDocumentation generated = generator.generate(normalizedRepositoryId, type);
        DocumentationEntity saved = repository.save(new DocumentationEntity(
                normalizedRepositoryId, type.name(), generated.title(), generated.markdownContent(), STATUS_GENERATED));

        return toResponse(saved);
    }

    @Override
    public Optional<DocumentationResponse> getDocById(Long id) {
        if (id == null) {
            throw new InvalidRequestException("id must not be null");
        }
        return repository.findById(id).map(DocumentationServiceImpl::toResponse);
    }

    @Override
    public List<DocumentationResponse> findByRepositoryId(String repositoryId) {
        return repository.findByRepositoryId(requireRepositoryId(repositoryId)).stream()
                .map(DocumentationServiceImpl::toResponse)
                .toList();
    }

    private static String requireRepositoryId(String repositoryId) {
        if (repositoryId == null || repositoryId.isBlank()) {
            throw new InvalidRequestException("repositoryId must not be blank");
        }
        return repositoryId.trim();
    }

    private static DocType parseDocType(String docType) {
        if (docType == null || docType.isBlank()) {
            throw new InvalidRequestException("docType must not be blank");
        }
        try {
            return DocType.valueOf(docType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Unsupported docType '" + docType + "'; expected one of "
                    + Arrays.toString(DocType.values()), ex);
        }
    }

    private static DocumentationResponse toResponse(DocumentationEntity entity) {
        return new DocumentationResponse(
                entity.getId(),
                entity.getRepositoryId(),
                entity.getDocType(),
                entity.getTitle(),
                entity.getMarkdownContent(),
                entity.getStatus());
    }
}
