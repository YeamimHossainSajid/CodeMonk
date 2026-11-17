package com.codemonk.repository.interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemonk.common.exception.InvalidRequestException;
import com.codemonk.common.exception.ResourceNotFoundException;
import com.codemonk.repository.application.RepositoryService;
import com.codemonk.repository.application.dto.RepositoryResponse;

/**
 * REST controller exposing repository management endpoints.
 */
@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Retrieves a repository by its identifier.
     *
     * @param id the repository identifier
     * @return 200 OK with {@link RepositoryResponse} if found
     * @throws InvalidRequestException if {@code id} is blank
     * @throws ResourceNotFoundException if the repository does not exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<RepositoryResponse> getRepositoryById(@PathVariable("id") String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidRequestException("Repository ID must not be blank");
        }
        RepositoryResponse response = repositoryService.getRepositoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", id));
        return ResponseEntity.ok(response);
    }
}
