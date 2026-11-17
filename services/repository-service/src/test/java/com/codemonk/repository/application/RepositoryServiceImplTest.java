package com.codemonk.repository.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemonk.repository.application.dto.RepositoryResponse;
import com.codemonk.repository.domain.RepositoryEntity;
import com.codemonk.repository.infrastructure.RepositoryJpaRepository;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceImplTest {

    @Mock
    private RepositoryJpaRepository jpaRepository;

    @InjectMocks
    private RepositoryServiceImpl service;

    @Test
    @DisplayName("getRepositoryById maps entity to response when entity exists")
    void getRepositoryByIdMapsEntityToResponseWhenFound() {
        Instant now = Instant.now();
        RepositoryEntity entity = new RepositoryEntity(
                "repo-1",
                "CodeMonk",
                "YeamimHossainSajid",
                "https://github.com/YeamimHossainSajid/CodeMonk.git",
                "main",
                "ACTIVE",
                now
        );
        when(jpaRepository.findById("repo-1")).thenReturn(Optional.of(entity));

        Optional<RepositoryResponse> result = service.getRepositoryById("repo-1");

        assertThat(result).isPresent();
        RepositoryResponse response = result.get();
        assertThat(response.id()).isEqualTo("repo-1");
        assertThat(response.name()).isEqualTo("CodeMonk");
        assertThat(response.owner()).isEqualTo("YeamimHossainSajid");
        assertThat(response.cloneUrl()).isEqualTo("https://github.com/YeamimHossainSajid/CodeMonk.git");
        assertThat(response.defaultBranch()).isEqualTo("main");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdAt()).isEqualTo(now);
        verify(jpaRepository).findById("repo-1");
    }

    @Test
    @DisplayName("getRepositoryById returns empty Optional when entity does not exist")
    void getRepositoryByIdReturnsEmptyWhenNotFound() {
        when(jpaRepository.findById("missing-repo")).thenReturn(Optional.empty());

        Optional<RepositoryResponse> result = service.getRepositoryById("missing-repo");

        assertThat(result).isEmpty();
        verify(jpaRepository).findById("missing-repo");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("getRepositoryById returns empty Optional for null or blank id without querying database")
    void getRepositoryByIdReturnsEmptyForNullOrBlankId(String id) {
        Optional<RepositoryResponse> result = service.getRepositoryById(id);

        assertThat(result).isEmpty();
        verifyNoInteractions(jpaRepository);
    }

    @Test
    @DisplayName("getRepositoryById trims whitespace from lookup id")
    void getRepositoryByIdTrimsWhitespace() {
        Instant now = Instant.now();
        RepositoryEntity entity = new RepositoryEntity(
                "repo-1",
                "CodeMonk",
                "YeamimHossainSajid",
                "https://github.com/YeamimHossainSajid/CodeMonk.git",
                "main",
                "ACTIVE",
                now
        );
        when(jpaRepository.findById("repo-1")).thenReturn(Optional.of(entity));

        Optional<RepositoryResponse> result = service.getRepositoryById("   repo-1   ");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("repo-1");
        verify(jpaRepository).findById("repo-1");
    }
}
