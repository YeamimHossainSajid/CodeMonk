package com.codemonk.repository.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codemonk.repository.domain.RepositoryEntity;

/**
 * Spring Data JPA repository for {@link RepositoryEntity}.
 */
@Repository
public interface RepositoryJpaRepository extends JpaRepository<RepositoryEntity, String> {
}
