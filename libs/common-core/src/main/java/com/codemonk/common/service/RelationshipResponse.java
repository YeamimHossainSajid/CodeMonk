package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelationshipResponse(String id,String sourceNodeId,String targetNodeId,String type) {

    public RelationshipResponse{
        id = (id == null || id.isBlank() ?"" :id.trim());
        sourceNodeId = (sourceNodeId == null || sourceNodeId.isBlank()) ?"" : sourceNodeId.trim();
        targetNodeId = (targetNodeId == null || targetNodeId.isBlank()) ?"" : targetNodeId.trim();
        type = (type == null || type.isBlank()) ? "" : type.trim();

    }
}
