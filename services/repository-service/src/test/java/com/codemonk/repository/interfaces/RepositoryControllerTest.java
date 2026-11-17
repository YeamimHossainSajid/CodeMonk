package com.codemonk.repository.interfaces;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.codemonk.common.exception.GlobalExceptionHandler;
import com.codemonk.repository.application.RepositoryService;
import com.codemonk.repository.application.dto.RepositoryResponse;

@WebMvcTest(controllers = RepositoryController.class)
@Import(GlobalExceptionHandler.class)
class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;

    @Test
    @DisplayName("GET /api/v1/repositories/{id} returns 200 OK and response payload when repository exists")
    void shouldReturn200AndRepositoryDetailsWhenFound() throws Exception {
        Instant now = Instant.parse("2026-11-17T10:00:00Z");
        RepositoryResponse response = new RepositoryResponse(
                "repo-1",
                "CodeMonk",
                "YeamimHossainSajid",
                "https://github.com/YeamimHossainSajid/CodeMonk.git",
                "main",
                "ACTIVE",
                now
        );

        given(repositoryService.getRepositoryById("repo-1")).willReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/repositories/{id}", "repo-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("repo-1"))
                .andExpect(jsonPath("$.name").value("CodeMonk"))
                .andExpect(jsonPath("$.owner").value("YeamimHossainSajid"))
                .andExpect(jsonPath("$.cloneUrl").value("https://github.com/YeamimHossainSajid/CodeMonk.git"))
                .andExpect(jsonPath("$.defaultBranch").value("main"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-17T10:00:00Z"));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} returns 404 NOT FOUND with ErrorResponse when repository does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        given(repositoryService.getRepositoryById("missing-repo")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/repositories/{id}", "missing-repo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Repository with identifier 'missing-repo' was not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/repositories/missing-repo"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} returns 400 BAD REQUEST with ErrorResponse when id is whitespace")
    void shouldReturn400WhenIdIsWhitespace() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{id}", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Repository ID must not be blank"))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
