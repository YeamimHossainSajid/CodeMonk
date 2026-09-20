package com.codemonk.search.client;

import java.util.Objects;

/**
 * A single indexed code fragment that hybrid search can retrieve, such as a class,
 * method or documentation chunk extracted from an analysed repository.
 *
 * @param id           stable identifier of the document within the index
 * @param repositoryId identifier of the repository the document was extracted from
 * @param path         path of the source file the document was extracted from
 * @param content      indexed text of the fragment, matched by keyword queries
 * @param language     programming language of the fragment, for example {@code java}
 */
public record SearchDocument(String id, String repositoryId, String path, String content, String language) {

    public SearchDocument {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(language, "language must not be null");
    }
}
