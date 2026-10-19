package com.codemonk.common.service;

import java.util.List;
import java.util.Optional;

/**
 * Generates and reads the documentation produced for a repository.
 *
 * <p>Implementations live in the documentation service; common-core only owns the
 * contract so that every caller agrees on the same operations. The contract is
 * expressed in {@link DocumentationResponse} rather than
 * {@link DocumentationEntity}, which keeps the persistence model private to its
 * owning service.
 */
public interface DocumentationService {

    /**
     * Generates documentation of the given type for a repository.
     *
     * @param repositoryId repository the documentation is generated for
     * @param docType      kind of documentation to generate
     * @return the generated documentation
     */
    DocumentationResponse generateDoc(String repositoryId, String docType);

    /**
     * Finds a single documentation entry by its identifier.
     *
     * @param id documentation identifier
     * @return the documentation, or empty when no entry has that identifier
     */
    Optional<DocumentationResponse> getDocById(Long id);

    /**
     * Finds every documentation entry generated for a repository.
     *
     * @param repositoryId repository to look up
     * @return the documentation for that repository, empty when there is none
     */
    List<DocumentationResponse> findByRepositoryId(String repositoryId);
}
