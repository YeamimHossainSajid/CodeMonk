package com.codemonk.documentation.application;

import com.codemonk.documentation.domain.DocType;

/**
 * Produces the content of a documentation entry.
 *
 * <p>{@link DocumentationServiceImpl} delegates content creation to this port so
 * that the generation strategy can change without touching persistence.
 */
public interface DocumentationGenerator {

    /**
     * Generates documentation of the given type for a repository.
     *
     * @param repositoryId repository the documentation is generated for
     * @param docType      kind of documentation to generate
     * @return the generated title and markdown content
     */
    GeneratedDocumentation generate(String repositoryId, DocType docType);
}
