package com.codemonk.common.service;

import org.springframework.stereotype.Component;

import java.util.Objects;

//this mirrors NodeMapper.java
@Component
public class RelationshipMapper {

    public RelationshipResponse toResponse(RelationshipEntity entity){
        Objects.requireNonNull(entity,"entity must not be null");
        String id = entity.getId() == null ? "":String.valueOf(entity.getId());

        return new RelationshipResponse(id,entity.getSourceNodeId(),entity.getTargetNodeId(),entity.getType());
    }

    public RelationshipEntity toDomain(RelationshipResponse response){
        Objects.requireNonNull(response,"response must not be null");
        RelationshipEntity entity = new RelationshipEntity(response.sourceNodeId(), response.targetNodeId(), response.type());
        if(!response.id().isBlank()){
            entity.setId(Long.valueOf(response.id()));
        }
        
        return entity;
    }
}
