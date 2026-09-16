package com.codemonk.knowledge.client;

import java.util.List;
import java.util.Optional;

/**
 * Read-only contract for querying the code knowledge graph owned by the
 * knowledge-service.
 *
 * <p>Integration tests depend on this abstraction instead of a live Neo4j backed
 * client so that they can be driven by a deterministic in-memory implementation.
 *
 * @see com.codemonk.knowledge.client.mock.MockKnowledgeServiceClient
 */
public interface KnowledgeServiceClient {

    /**
     * Looks up a single graph node by its identifier.
     *
     * @param nodeId identifier of the node to load
     * @return the node, or {@link Optional#empty()} when the graph holds no such node
     */
    Optional<KnowledgeNode> findNode(String nodeId);

    /**
     * Lists every node extracted from the given repository.
     *
     * @param repositoryId identifier of the repository
     * @return the matching nodes, empty when the repository is unknown
     */
    List<KnowledgeNode> findNodesByRepository(String repositoryId);

    /**
     * Lists the relationships originating from the given node.
     *
     * @param nodeId identifier of the source node
     * @return the outgoing relationships, empty when the node has none
     */
    List<KnowledgeRelationship> findOutgoingRelationships(String nodeId);

    /**
     * Resolves the nodes the given node directly depends on by following its
     * outgoing relationships.
     *
     * @param nodeId identifier of the source node
     * @return the resolved target nodes, empty when the node has no dependencies
     */
    List<KnowledgeNode> findDependencies(String nodeId);

    /**
     * Reports whether the knowledge graph backend is reachable.
     *
     * @return {@code true} when queries are expected to succeed
     */
    boolean isHealthy();
}
