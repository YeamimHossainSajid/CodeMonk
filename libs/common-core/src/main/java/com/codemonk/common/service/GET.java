package com.codemonk.common.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;

@Service
public class GET {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Neighborhood(String nodeId, List<String> incoming, List<String> outgoing) {

        public Neighborhood(String nodeId, List<String> incoming, List<String> outgoing) {
            this.nodeId = (nodeId == null || nodeId.isBlank()) ? "" : nodeId.trim();
            this.incoming = (incoming == null) ? List.of() : List.copyOf(incoming);
            this.outgoing = (outgoing == null) ? List.of() : List.copyOf(outgoing);
        }

        public static Neighborhood empty(String nodeId) {
            return new Neighborhood(nodeId, List.of(), List.of());
        }

        public boolean isIsolated() {
            return incoming.isEmpty() && outgoing.isEmpty();
        }

        public int totalDegree() {
            return incoming.size() + outgoing.size();
        }
    }

    public Neighborhood neighbors(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return Neighborhood.empty("");
        }
        return Neighborhood.empty(nodeId.trim());
    }

    public Neighborhood neighbors(String nodeId, List<String> incoming, List<String> outgoing) {
        return new Neighborhood(nodeId, incoming, outgoing);
    }

}
