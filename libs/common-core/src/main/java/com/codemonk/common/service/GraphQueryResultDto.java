package com.codemonk.common.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.stereotype.Component;
import java.util.List;

// uses generics (<N, E>) to avoid hardcoding like NodeResponse/RelationshipResponse,
// preventing duplicate DTOs or redundant conversions across services.

@Component
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GraphQueryResultDto<N,E>(List<N> nodes,List<E> edges) {
    
    public GraphQueryResultDto{
        nodes = (nodes==null) ? List.of() : List.copyOf(nodes);
        edges = (edges==null) ? List.of() : List.copyOf(edges);
    }

    public static <N,E> GraphQueryResultDto<N,E> of(List<N> nodes,List<E> edges){
        return  new GraphQueryResultDto<>(nodes,edges);
    }

    public static <N,E> GraphQueryResultDto<N,E> empty(){
        return new GraphQueryResultDto<>(List.of(),List.of());
    }

    public boolean isEmpty(){
        return nodes.isEmpty() && edges.isEmpty();
    }

    public int nodesCount(){
        return nodes.size();
    }

    public int edgesCount(){
        return edges.size();
    }
}

