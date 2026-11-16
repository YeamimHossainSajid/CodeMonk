package com.codemonk.common.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


@Component
public class NodeMapper {


    public NodeResponse toResponse(CodeNode node) {

        Objects.requireNonNull(node, "node must not be null");

        return new NodeResponse(node.nodeId(), node.name(), node.qualifiedName(), toTags(node.entityType(), node.repositoryId()));
    }


    public CodeNode toDomain(NodeResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        List<String> tags = response.tags();
        String entityType = tags.isEmpty() ? "" : tags.get(0);
        String repositoryId = tags.size() < 2 ? "" : tags.get(1);

        return new CodeNode(response.id(), response.label(), entityType, response.properties(), repositoryId);
    }

    private static List<String> toTags(String entityType, String repositoryId) {
        if (entityType.isEmpty() && repositoryId.isEmpty()) {
            return List.of();
        }

        if (entityType.isEmpty()) {
            return List.of("", repositoryId);
        }

        return repositoryId.isEmpty() ? List.of(entityType) : List.of(entityType, repositoryId);
    }
}
