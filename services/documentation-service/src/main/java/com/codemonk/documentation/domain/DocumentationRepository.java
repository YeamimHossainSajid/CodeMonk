package com.codemonk.documentation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentationRepository extends JpaRepository<DocumentationEntity, Long> {

    List<DocumentationEntity> findByRepositoryId(String repositoryId);
}
