package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Documentation as it is handed to callers of {@link DocumentationService}.
 *
 * <p>{@link DocumentationEntity} stays inside the service that persists it, so
 * the contract shared through common-core carries this response instead.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentationResponse(
        Long id,
        String repositoryId,
        String docType,
        String title,
        String markdownContent,
        String status) {

    public DocumentationResponse {
        repositoryId = normalize(repositoryId);
        docType = normalize(docType);
        title = normalize(title);
        markdownContent = markdownContent == null ? "" : markdownContent;
        status = normalize(status);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
