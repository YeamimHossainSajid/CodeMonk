package com.codemonk.knowledge.client;

import java.util.Objects;

/**
 * A single vertex of the code knowledge graph, such as a class, method or module
 * extracted from an analysed repository.
 *
 * @param id           stable identifier of the node within the graph
 * @param repositoryId identifier of the repository the node was extracted from
 * @param name         fully qualified name of the code element
 * @param type         kind of code element, for example {@code CLASS} or {@code METHOD}
 */
public record KnowledgeNode(String id, String repositoryId, String name, String type) {

    public KnowledgeNode {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
