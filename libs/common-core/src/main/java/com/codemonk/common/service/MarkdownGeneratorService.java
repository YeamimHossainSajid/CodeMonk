package com.codemonk.common.service;

/**
 * Renders the markdown body of a documentation entry.
 *
 * <p>Implementations live in the documentation service; common-core only owns the
 * contract so that every caller agrees on the same operation. The documentation
 * type is passed as a plain string, matching {@link DocumentationService}, so the
 * contract does not depend on the documentation service's own types.
 */
@FunctionalInterface
public interface MarkdownGeneratorService {

    /**
     * Generates markdown documentation of the given type for a repository.
     *
     * @param repositoryId repository the markdown is generated for
     * @param docType      kind of documentation to generate
     * @return the generated markdown content
     */
    String generateMarkdown(String repositoryId, String docType);
}
