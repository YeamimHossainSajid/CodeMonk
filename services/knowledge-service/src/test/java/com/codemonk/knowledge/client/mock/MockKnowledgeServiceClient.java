package com.codemonk.knowledge.client.mock;

import com.codemonk.common.exception.ServiceUnavailableException;
import com.codemonk.knowledge.client.KnowledgeNode;
import com.codemonk.knowledge.client.KnowledgeRelationship;
import com.codemonk.knowledge.client.KnowledgeServiceClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory {@link KnowledgeServiceClient} used by integration tests in place of a
 * live knowledge graph backend.
 *
 * <p>The client is seeded with a small, fixed graph so that assertions can be written
 * against known values, and every query returns results in a stable insertion order.
 * Tests that need their own fixtures can start from an empty graph via
 * {@link #empty()} and add data with {@link #withNode(KnowledgeNode)} and
 * {@link #withRelationship(KnowledgeRelationship)}.
 *
 * <p>Failure handling can be exercised by flipping the client offline with
 * {@link #setHealthy(boolean)}, after which every query throws
 * {@link ServiceUnavailableException}.
 *
 * <p>This class is not thread-safe beyond its invocation counter; drive it from a
 * single test thread.
 */
public class MockKnowledgeServiceClient implements KnowledgeServiceClient {

    /** Repository the seeded fixture graph belongs to. */
    public static final String SEEDED_REPOSITORY_ID = "repo-1";

    /** Identifier of the seeded node that depends on the other two seeded nodes. */
    public static final String SEEDED_SERVICE_NODE_ID = "node-1";

    /** Identifier of the seeded repository-layer node. */
    public static final String SEEDED_REPOSITORY_NODE_ID = "node-2";

    /** Identifier of the seeded DTO node. */
    public static final String SEEDED_DTO_NODE_ID = "node-3";

    private final Map<String, KnowledgeNode> nodesById = new LinkedHashMap<>();
    private final Map<String, KnowledgeRelationship> relationshipsById = new LinkedHashMap<>();
    private final AtomicInteger invocationCount = new AtomicInteger();

    private boolean healthy = true;

    /**
     * Creates a client pre-loaded with the seeded fixture graph.
     */
    public MockKnowledgeServiceClient() {
        seedDefaultGraph();
    }

    private MockKnowledgeServiceClient(boolean seed) {
        if (seed) {
            seedDefaultGraph();
        }
    }

    /**
     * Creates a client with no nodes or relationships, for tests that supply their own
     * fixtures.
     *
     * @return an empty client
     */
    public static MockKnowledgeServiceClient empty() {
        return new MockKnowledgeServiceClient(false);
    }

    /**
     * Adds or replaces a node in the graph.
     *
     * @param node node to register, never {@code null}
     * @return this client, for chaining
     */
    public MockKnowledgeServiceClient withNode(KnowledgeNode node) {
        Objects.requireNonNull(node, "node must not be null");
        nodesById.put(node.id(), node);
        return this;
    }

    /**
     * Adds or replaces a relationship in the graph. The referenced nodes do not need to
     * exist; {@link #findDependencies(String)} simply skips edges with unresolved targets.
     *
     * @param relationship relationship to register, never {@code null}
     * @return this client, for chaining
     */
    public MockKnowledgeServiceClient withRelationship(KnowledgeRelationship relationship) {
        Objects.requireNonNull(relationship, "relationship must not be null");
        relationshipsById.put(relationship.id(), relationship);
        return this;
    }

    /**
     * Controls whether queries succeed or fail with {@link ServiceUnavailableException}.
     *
     * @param healthy {@code true} to serve queries, {@code false} to simulate an outage
     * @return this client, for chaining
     */
    public MockKnowledgeServiceClient setHealthy(boolean healthy) {
        this.healthy = healthy;
        return this;
    }

    /**
     * Number of query methods invoked since construction or the last {@link #reset()}.
     * Health checks are not counted.
     *
     * @return the invocation count
     */
    public int invocationCount() {
        return invocationCount.get();
    }

    /**
     * Restores the seeded fixture graph, clears the invocation counter and marks the
     * client healthy again, so a single instance can be shared across test methods.
     */
    public void reset() {
        nodesById.clear();
        relationshipsById.clear();
        invocationCount.set(0);
        healthy = true;
        seedDefaultGraph();
    }

    @Override
    public Optional<KnowledgeNode> findNode(String nodeId) {
        recordInvocation();
        if (nodeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    @Override
    public List<KnowledgeNode> findNodesByRepository(String repositoryId) {
        recordInvocation();
        if (repositoryId == null) {
            return List.of();
        }
        List<KnowledgeNode> matches = new ArrayList<>();
        for (KnowledgeNode node : nodesById.values()) {
            if (repositoryId.equals(node.repositoryId())) {
                matches.add(node);
            }
        }
        return List.copyOf(matches);
    }

    @Override
    public List<KnowledgeRelationship> findOutgoingRelationships(String nodeId) {
        recordInvocation();
        return List.copyOf(outgoingRelationships(nodeId));
    }

    @Override
    public List<KnowledgeNode> findDependencies(String nodeId) {
        recordInvocation();
        List<KnowledgeNode> dependencies = new ArrayList<>();
        for (KnowledgeRelationship relationship : outgoingRelationships(nodeId)) {
            KnowledgeNode target = nodesById.get(relationship.targetNodeId());
            if (target != null && !dependencies.contains(target)) {
                dependencies.add(target);
            }
        }
        return List.copyOf(dependencies);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    private Collection<KnowledgeRelationship> outgoingRelationships(String nodeId) {
        if (nodeId == null) {
            return List.of();
        }
        List<KnowledgeRelationship> matches = new ArrayList<>();
        for (KnowledgeRelationship relationship : relationshipsById.values()) {
            if (nodeId.equals(relationship.sourceNodeId())) {
                matches.add(relationship);
            }
        }
        return matches;
    }

    private void recordInvocation() {
        if (!healthy) {
            throw new ServiceUnavailableException("knowledge-service", "mock client is offline");
        }
        invocationCount.incrementAndGet();
    }

    private void seedDefaultGraph() {
        withNode(new KnowledgeNode(
                SEEDED_SERVICE_NODE_ID, SEEDED_REPOSITORY_ID, "com.codemonk.demo.OrderService", "CLASS"));
        withNode(new KnowledgeNode(
                SEEDED_REPOSITORY_NODE_ID, SEEDED_REPOSITORY_ID, "com.codemonk.demo.OrderRepository", "INTERFACE"));
        withNode(new KnowledgeNode(
                SEEDED_DTO_NODE_ID, SEEDED_REPOSITORY_ID, "com.codemonk.demo.OrderDto", "RECORD"));

        withRelationship(new KnowledgeRelationship(
                "rel-1", SEEDED_SERVICE_NODE_ID, SEEDED_REPOSITORY_NODE_ID, "DEPENDS_ON"));
        withRelationship(new KnowledgeRelationship(
                "rel-2", SEEDED_SERVICE_NODE_ID, SEEDED_DTO_NODE_ID, "RETURNS"));
    }
}
