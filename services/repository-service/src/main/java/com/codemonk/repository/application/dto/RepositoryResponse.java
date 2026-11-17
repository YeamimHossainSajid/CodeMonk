package com.codemonk.repository.application.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object representing repository metadata returned to API clients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositoryResponse(
        String id,
        String name,
        String owner,
        String cloneUrl,
        String defaultBranch,
        String status,
        Instant createdAt
) {
    public RepositoryResponse {
        id = normalize(id);
        name = normalize(name);
        owner = normalize(owner);
        cloneUrl = normalize(cloneUrl);
        defaultBranch = (defaultBranch == null || defaultBranch.isBlank()) ? "main" : defaultBranch.trim();
        status = (status == null || status.isBlank()) ? "PENDING" : status.trim();
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
