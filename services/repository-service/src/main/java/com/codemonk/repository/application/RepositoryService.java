package com.codemonk.repository.application;

import java.util.Optional;

import com.codemonk.repository.application.dto.RepositoryResponse;

/**
 * Application service managing repository queries and lifecycle operations.
 */
public interface RepositoryService {

    /**
     * Retrieves repository metadata by identifier.
     *
     * @param id the unique repository identifier
     * @return repository response if found, or empty optional
     */
    Optional<RepositoryResponse> getRepositoryById(String id);
}
