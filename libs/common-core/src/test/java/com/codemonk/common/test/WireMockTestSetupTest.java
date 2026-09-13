package com.codemonk.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;

import com.github.tomakehurst.wiremock.client.VerificationException;

/**
 * WireMockTestSetupTest
 */
public class WireMockTestSetupTest {

    @RegisterExtension
    static WireMockTestSetup wireMock = new WireMockTestSetup();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("Should start server on a dynamic port")
    void shouldStartServerOnDynamicPort() {

        assertTrue(wireMock.server().isRunning());
        assertTrue(wireMock.port() > 0);
        assertEquals("http://localhost:" + wireMock.port(), wireMock.baseUrl());
    }

    @Test
    @DisplayName("Should serve stubbed GET JSON response")
    void shouldServeStubbedGetJson() throws Exception {

        wireMock.stubGetJson("/api/repos/42", 200, "{\"id\":42}");

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/repos/42")).GET());

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));
        assertEquals("{\"id\":42}", response.body());
        wireMock.verifyCalled("GET", "/api/repos/42", 1);
    }

    @Test
    @DisplayName("Should serve stubbed POST JSON response")
    void shouldServeStubbedPostJson() throws Exception {

        wireMock.stubPostJson("/api/repos", 201, "{\"created\":true}");

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/repos"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"codemonk\"}")));

        assertEquals(201, response.statusCode());
        assertEquals("{\"created\":true}", response.body());
        wireMock.verifyCalled("POST", "/api/repos", 1);
    }

    @Test
    @DisplayName("Should return stubbed error status")
    void shouldReturnStubbedErrorStatus() throws Exception {

        wireMock.stubStatus("GET", "/api/unavailable", 503);

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/unavailable")).GET());

        assertEquals(503, response.statusCode());
    }

    @Test
    @DisplayName("Should return 404 for unstubbed paths")
    void shouldReturnNotFoundForUnstubbedPath() throws Exception {

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/missing")).GET());

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("Should clear stubs and request journal on reset")
    void shouldClearStubsOnReset() throws Exception {

        wireMock.stubGetJson("/api/repos/42", 200, "{\"id\":42}");
        send(HttpRequest.newBuilder(uri("/api/repos/42")).GET());

        wireMock.reset();

        wireMock.verifyCalled("GET", "/api/repos/42", 0);
        assertEquals(404, send(HttpRequest.newBuilder(uri("/api/repos/42")).GET()).statusCode());
    }

    @Test
    @DisplayName("Should fail verification when call count does not match")
    void shouldFailVerificationWhenNotCalled() {

        assertThrows(VerificationException.class, () -> wireMock.verifyCalled("GET", "/api/never", 1));
    }

    @Test
    @DisplayName("Should delay stubbed response to simulate slow services")
    void shouldDelayStubbedResponse() {

        wireMock.stubDelayed("GET", "/api/slow", 2000);

        HttpRequest.Builder request = HttpRequest.newBuilder(uri("/api/slow"))
                .timeout(Duration.ofMillis(200))
                .GET();

        assertThrows(HttpTimeoutException.class, () -> send(request));
    }

    @Test
    @DisplayName("Should register base URL as a dynamic Spring property")
    void shouldRegisterBaseUrlProperty() {

        Map<String, Object> properties = new HashMap<>();
        DynamicPropertyRegistry registry = (name, supplier) -> properties.put(name, supplier.get());

        wireMock.registerBaseUrl(registry, "clients.external.base-url");

        assertEquals(wireMock.baseUrl(), properties.get("clients.external.base-url"));
    }

    private URI uri(String path) {
        return URI.create(wireMock.baseUrl() + path);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
