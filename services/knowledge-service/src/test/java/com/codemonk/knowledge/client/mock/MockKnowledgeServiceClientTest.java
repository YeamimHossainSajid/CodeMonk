package com.codemonk.knowledge.client.mock;

import com.codemonk.common.exception.ServiceUnavailableException;
import com.codemonk.knowledge.client.KnowledgeNode;
import com.codemonk.knowledge.client.KnowledgeRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockKnowledgeServiceClientTest {

    private MockKnowledgeServiceClient client;

    @BeforeEach
    void setUp() {
        client = new MockKnowledgeServiceClient();
    }

    @Test
    void shouldReturnSeededNodeById() {
        Optional<KnowledgeNode> node = client.findNode(MockKnowledgeServiceClient.SEEDED_SERVICE_NODE_ID);

        assertTrue(node.isPresent());
        assertEquals("com.codemonk.demo.OrderService", node.get().name());
        assertEquals("CLASS", node.get().type());
    }

    @Test
    void shouldReturnEmptyForUnknownNode() {
        assertTrue(client.findNode("does-not-exist").isEmpty());
        assertTrue(client.findNode(null).isEmpty());
    }

    @Test
    void shouldListSeededNodesInStableOrder() {
        List<KnowledgeNode> nodes = client.findNodesByRepository(MockKnowledgeServiceClient.SEEDED_REPOSITORY_ID);

        assertEquals(
                List.of(
                        MockKnowledgeServiceClient.SEEDED_SERVICE_NODE_ID,
                        MockKnowledgeServiceClient.SEEDED_REPOSITORY_NODE_ID,
                        MockKnowledgeServiceClient.SEEDED_DTO_NODE_ID),
                nodes.stream().map(KnowledgeNode::id).toList());
        assertEquals(nodes, client.findNodesByRepository(MockKnowledgeServiceClient.SEEDED_REPOSITORY_ID));
    }

    @Test
    void shouldReturnEmptyListForUnknownRepository() {
        assertTrue(client.findNodesByRepository("unknown-repo").isEmpty());
        assertTrue(client.findNodesByRepository(null).isEmpty());
    }

    @Test
    void shouldReturnOutgoingRelationshipsOnly() {
        List<KnowledgeRelationship> outgoing =
                client.findOutgoingRelationships(MockKnowledgeServiceClient.SEEDED_SERVICE_NODE_ID);

        assertEquals(List.of("rel-1", "rel-2"), outgoing.stream().map(KnowledgeRelationship::id).toList());
        assertTrue(client.findOutgoingRelationships(MockKnowledgeServiceClient.SEEDED_DTO_NODE_ID).isEmpty());
    }

    @Test
    void shouldResolveDependenciesFromRelationships() {
        List<KnowledgeNode> dependencies =
                client.findDependencies(MockKnowledgeServiceClient.SEEDED_SERVICE_NODE_ID);

        assertEquals(
                List.of(
                        MockKnowledgeServiceClient.SEEDED_REPOSITORY_NODE_ID,
                        MockKnowledgeServiceClient.SEEDED_DTO_NODE_ID),
                dependencies.stream().map(KnowledgeNode::id).toList());
    }

    @Test
    void shouldSkipDependenciesWithUnresolvedTargets() {
        client.withRelationship(new KnowledgeRelationship(
                "rel-dangling", MockKnowledgeServiceClient.SEEDED_SERVICE_NODE_ID, "missing-node", "CALLS"));

        List<KnowledgeNode> dependencies =
                client.findDependencies(MockKnowledgeServiceClient.SEEDED_SERVICE_NODE_ID);

        assertEquals(2, dependencies.size());
    }

    @Test
    void shouldServeCustomFixturesFromEmptyGraph() {
        MockKnowledgeServiceClient custom = MockKnowledgeServiceClient.empty()
                .withNode(new KnowledgeNode("n-1", "repo-x", "com.example.Alpha", "CLASS"))
                .withNode(new KnowledgeNode("n-2", "repo-x", "com.example.Beta", "CLASS"))
                .withRelationship(new KnowledgeRelationship("r-1", "n-1", "n-2", "CALLS"));

        assertEquals(2, custom.findNodesByRepository("repo-x").size());
        assertEquals(List.of("n-2"), custom.findDependencies("n-1").stream().map(KnowledgeNode::id).toList());
        assertTrue(custom.findNodesByRepository(MockKnowledgeServiceClient.SEEDED_REPOSITORY_ID).isEmpty());
    }

    @Test
    void shouldFailQueriesWhenOffline() {
        client.setHealthy(false);

        assertFalse(client.isHealthy());
        assertThrows(ServiceUnavailableException.class, () -> client.findNode("node-1"));
        assertThrows(ServiceUnavailableException.class, () -> client.findNodesByRepository("repo-1"));
        assertThrows(ServiceUnavailableException.class, () -> client.findOutgoingRelationships("node-1"));
        assertThrows(ServiceUnavailableException.class, () -> client.findDependencies("node-1"));
    }

    @Test
    void shouldCountQueryInvocations() {
        assertEquals(0, client.invocationCount());

        client.findNode("node-1");
        client.findDependencies("node-1");
        client.isHealthy();

        assertEquals(2, client.invocationCount());
    }

    @Test
    void shouldRestoreSeededStateOnReset() {
        client.withNode(new KnowledgeNode("extra", "repo-x", "com.example.Extra", "CLASS"));
        client.setHealthy(false);

        client.reset();

        assertTrue(client.isHealthy());
        assertEquals(0, client.invocationCount());
        assertTrue(client.findNode("extra").isEmpty());
        assertEquals(3, client.findNodesByRepository(MockKnowledgeServiceClient.SEEDED_REPOSITORY_ID).size());
    }
}
