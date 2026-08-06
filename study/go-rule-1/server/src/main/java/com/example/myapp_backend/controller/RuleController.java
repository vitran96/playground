package com.example.myapp_backend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.myapp_backend.entity.Rule;

import io.gorules.zen_engine.JsonBuffer;
import io.gorules.zen_engine.ZenDecision;
import io.gorules.zen_engine.ZenEngine;
import io.gorules.zen_engine.ZenEvaluateOptions;
import io.gorules.zen_engine.ZenException;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Slf4j
public class RuleController {
    private final ZenEngine zenEngine;
    private final List<Rule> rules = new ArrayList<>(); // empty by default

    @GetMapping
    public List<Rule> getAll() {
        return rules;
    }

    @GetMapping("/{id}")
    public Rule getOne(@PathVariable long id) {
        return findByIdOrThrow(id);
    }

    @PostMapping
    public Rule create(@RequestBody Rule rule) {
        long id = rules.size(); // index as ID
        rule.setId(id);
        rules.add(rule);
        return rule;
    }

    @PutMapping("/{id}")
    public Rule update(@PathVariable long id, @RequestBody Rule updated) {
        Rule existing = findByIdOrThrow(id);
        existing.setName(updated.getName());
        existing.setDecision(updated.getDecision());
        return existing;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        Rule existing = findByIdOrThrow(id);
        rules.remove(existing);
    }

    @PostMapping("/{id}/evaluate")
    public CompletableFuture<String> evaluate(@PathVariable long id, @RequestBody String inputJsonStr) {
        try {
            Rule rule = findByIdOrThrow(id);
            JsonBuffer decisionJsonBuffer = new JsonBuffer(rule.getDecision());
            ZenDecision decision = zenEngine.createDecision(decisionJsonBuffer);

            JsonBuffer inputJsonBuffer = new JsonBuffer(inputJsonStr);
            ZenEvaluateOptions zenEvalOptions = new ZenEvaluateOptions(null, false);

            return decision.evaluate(inputJsonBuffer, zenEvalOptions)
                .thenApply(response -> response.result().toString());
        } catch (ZenException e) {
            log.error("Error when evaluate", e);
            throw new RuntimeException(e);
        }
    }

    private Rule findByIdOrThrow(long id) {
        Optional<Rule> found = rules.stream().filter(r -> r.getId() == id).findFirst();
        return found.orElseThrow(() -> new RuntimeException("Rule not found: " + id));
    }
}
