package com.codemonk.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    @DisplayName("Should format ResourceNotFoundException message correctly")
    void shouldFormatResourceNotFoundException() {
        ResourceNotFoundException ex1 = new ResourceNotFoundException("Repository", "repo-123");
        assertThat(ex1.getMessage()).isEqualTo("Repository with identifier 'repo-123' was not found");

        ResourceNotFoundException ex2 = new ResourceNotFoundException("CodeEntity", "name", "AuthService");
        assertThat(ex2.getMessage()).isEqualTo("CodeEntity with name 'AuthService' was not found");
    }

    @Test
    @DisplayName("Should format DuplicateResourceException message correctly")
    void shouldFormatDuplicateResourceException() {
        DuplicateResourceException ex = new DuplicateResourceException("Repository", "url", "https://github.com/org/repo");
        assertThat(ex.getMessage()).isEqualTo("Repository with url 'https://github.com/org/repo' already exists");
    }

    @Test
    @DisplayName("Should format ServiceUnavailableException message correctly")
    void shouldFormatServiceUnavailableException() {
        ServiceUnavailableException ex = new ServiceUnavailableException("ai-service", "Connection timed out");
        assertThat(ex.getMessage()).isEqualTo("Service 'ai-service' is currently unavailable: Connection timed out");
    }
}
