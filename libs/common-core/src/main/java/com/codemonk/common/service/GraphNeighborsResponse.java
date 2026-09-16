package com.codemonk.common.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;

@Component 
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GraphNeighborsResponse(String nodeId,List<String> incoming,List<String> outgoing) {

    public GraphNeighborsResponse{
        nodeId = (nodeId == null || nodeId.isBlank()) ? "" : nodeId.trim();
        incoming = (incoming == null) ? List.of() : List.copyOf(incoming);
        outgoing = (outgoing == null) ? List.of() : List.copyOf(outgoing);
    }

    public static GraphNeighborsResponse of (String nodeId,List<String> incoming,List<String> outgoing){
        return new GraphNeighborsResponse(nodeId, incoming, outgoing);
    }

    public static GraphNeighborsResponse empty(String nodeId){
        return new GraphNeighborsResponse(nodeId, List.of(), List.of());
    }

    public boolean isIsolated(){
        return incoming.isEmpty() && outgoing.isEmpty();
    }

    public int totalDegree(){
        return incoming.size() + outgoing.size();
    }
}
