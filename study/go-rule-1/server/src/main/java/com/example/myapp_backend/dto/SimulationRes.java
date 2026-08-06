package com.example.myapp_backend.dto;

import tools.jackson.databind.JsonNode;

import java.util.Map;

public record SimulationRes(
    String performance,
    JsonNode result,
    Map<String, TraceEntry> trace
) {
    public record TraceEntry(
        String id,
        String name,
        JsonNode input,
        JsonNode output,
        String performance,
        JsonNode traceData,
        Integer order
    ) {}
}
