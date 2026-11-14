package com.codemonk.documentation.application;

import java.util.Objects;

/**
 * Output of a {@link DocumentationGenerator}, before it is persisted.
 *
 * @param title           human-readable title of the documentation
 * @param markdownContent documentation body in markdown
 */
public record GeneratedDocumentation(String title, String markdownContent) {

    public GeneratedDocumentation {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(markdownContent, "markdownContent must not be null");
    }
}
