package com.example.myapp_backend.dto;

import io.gorules.zen_engine.JsonBuffer;
import io.gorules.zen_engine.ZenEngineTrace;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SimulationResMapper {

    private final JsonMapper jsonMapper;

    public SimulationRes toSimulationRes(String performance, String resultJson, Map<String, ZenEngineTrace> trace) {
        JsonNode resultNode = jsonMapper.readTree(resultJson); // unchecked JacksonException on failure, no try/catch needed

        Map<String, SimulationRes.TraceEntry> traceMap = new LinkedHashMap<>();
        if (trace != null) {
            trace.forEach((key, t) -> traceMap.put(key, new SimulationRes.TraceEntry(
                    t.id(),
                    t.name(),
                    toJsonNode(t.input()),
                    toJsonNode(t.output()),
                    t.performance(),
                    toJsonNode(t.traceData()),
                    t.order()
            )));
        }

        return new SimulationRes(performance, resultNode, traceMap);
    }

    private JsonNode toJsonNode(JsonBuffer buffer) {
        if (buffer == null) return null;
        return jsonMapper.readTree(buffer.toString()); // ⚠️ still unconfirmed: JsonBuffer.toString()
    }
}