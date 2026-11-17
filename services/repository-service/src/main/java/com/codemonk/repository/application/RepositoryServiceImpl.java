package com.codemonk.repository.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemonk.repository.application.dto.RepositoryResponse;
import com.codemonk.repository.domain.RepositoryEntity;
import com.codemonk.repository.infrastructure.RepositoryJpaRepository;

/**
 * Implementation of {@link RepositoryService} backed by {@link RepositoryJpaRepository}.
 */
@Service
@Transactional(readOnly = true)
public class RepositoryServiceImpl implements RepositoryService {

    private final RepositoryJpaRepository jpaRepository;

    public RepositoryServiceImpl(RepositoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RepositoryResponse> getRepositoryById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return jpaRepository.findById(id.trim()).map(RepositoryServiceImpl::toResponse);
    }

    private static RepositoryResponse toResponse(RepositoryEntity entity) {
        return new RepositoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getOwner(),
                entity.getCloneUrl(),
                entity.getDefaultBranch(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
