package com.codemonk.knowledge.client;

import java.util.Objects;

/**
 * A directed edge between two {@link KnowledgeNode} instances, describing how one
 * code element relates to another.
 *
 * @param id           stable identifier of the relationship within the graph
 * @param sourceNodeId identifier of the node the relationship originates from
 * @param targetNodeId identifier of the node the relationship points to
 * @param type         kind of relationship, for example {@code DEPENDS_ON} or {@code CALLS}
 */
public record KnowledgeRelationship(String id, String sourceNodeId, String targetNodeId, String type) {

    public KnowledgeRelationship {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sourceNodeId, "sourceNodeId must not be null");
        Objects.requireNonNull(targetNodeId, "targetNodeId must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
