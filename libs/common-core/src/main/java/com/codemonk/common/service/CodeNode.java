package com.codemonk.common.service;


public record CodeNode(
        String nodeId,
        String name,
        String entityType,
        String qualifiedName,
        String repositoryId) {

    public CodeNode {
        nodeId = normalize(nodeId);
        name = normalize(name);
        entityType = normalize(entityType);
        qualifiedName = normalize(qualifiedName);
        repositoryId = normalize(repositoryId);
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }
}
