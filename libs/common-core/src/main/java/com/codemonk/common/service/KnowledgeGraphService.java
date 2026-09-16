package com.codemonk.common.service;

import java.util.*;

public interface KnowledgeGraphService {

    String createNode(String label, String properties);

    String createRelationship(String sourceId, String targetId, String type);

    Optional<GET.Neighborhood> findById(String nodeId);

    GET.Neighborhood findNeighbors(String nodeId);

}
