package com.codemonk.common.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

/**
 * Reusable WireMock helper for stubbing external REST service calls in tests.
 *
 * <p>Register it as a static JUnit 5 extension so a single server runs per test class
 * and stubs are cleared before every test:
 *
 * <pre>{@code
 * @RegisterExtension
 * static WireMockTestSetup wireMock = new WireMockTestSetup();
 *
 * @DynamicPropertySource
 * static void properties(DynamicPropertyRegistry registry) {
 *     wireMock.registerBaseUrl(registry, "clients.github.base-url");
 * }
 * }</pre>
 */
public class WireMockTestSetup implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private final WireMockServer server;

    public WireMockTestSetup() {
        this(options().dynamicPort());
    }

    public WireMockTestSetup(WireMockConfiguration configuration) {
        this.server = new WireMockServer(configuration);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        start();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        start();
        reset();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        stop();
    }

    public void start() {
        if (!server.isRunning()) {
            server.start();
        }
    }

    public void stop() {
        if (server.isRunning()) {
            server.stop();
        }
    }

    /**
     * Removes all stubs and clears the request journal.
     */
    public void reset() {
        server.resetAll();
    }

    public WireMockServer server() {
        return server;
    }

    public int port() {
        return server.port();
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    /**
     * Exposes the mock server URL as a Spring property, e.g. from a {@code @DynamicPropertySource} method.
     */
    public void registerBaseUrl(DynamicPropertyRegistry registry, String propertyName) {
        registry.add(propertyName, this::baseUrl);
    }

    public StubMapping stubJson(String method, String path, int status, String jsonBody) {
        return server.stubFor(request(method, urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(jsonBody)));
    }

    public StubMapping stubGetJson(String path, int status, String jsonBody) {
        return stubJson("GET", path, status, jsonBody);
    }

    public StubMapping stubPostJson(String path, int status, String jsonBody) {
        return stubJson("POST", path, status, jsonBody);
    }

    /**
     * Stubs an empty response with the given status, useful for simulating downstream failures.
     */
    public StubMapping stubStatus(String method, String path, int status) {
        return server.stubFor(request(method, urlPathEqualTo(path))
                .willReturn(aResponse().withStatus(status)));
    }

    /**
     * Stubs a delayed response, useful for exercising client timeouts.
     */
    public StubMapping stubDelayed(String method, String path, int delayMillis) {
        return server.stubFor(request(method, urlPathEqualTo(path))
                .willReturn(aResponse().withStatus(200).withFixedDelay(delayMillis)));
    }

    public void verifyCalled(String method, String path, int times) {
        server.verify(times, RequestPatternBuilder.newRequestPattern(
                RequestMethod.fromString(method), urlPathEqualTo(path)));
    }
}
