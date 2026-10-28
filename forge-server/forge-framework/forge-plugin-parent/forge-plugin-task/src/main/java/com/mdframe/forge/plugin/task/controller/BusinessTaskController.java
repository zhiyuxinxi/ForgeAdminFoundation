package com.mdframe.forge.plugin.task.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.task.api.BusinessTask;
import com.mdframe.forge.task.api.BusinessTaskEngine;
import com.mdframe.forge.task.api.TaskDefinition;
import com.mdframe.forge.task.api.TaskState;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Service-level checks also protect Java consumers; HTTP cannot supply tenant or actor identity. */
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class BusinessTaskController {
    private final BusinessTaskEngine engine;

    @PostMapping("/definition")
    public RespInfo<TaskDefinition> define(@RequestBody BusinessTaskEngine.Define command) {
        return RespInfo.success(engine.define(command));
    }

    @GetMapping("/definitions")
    public RespInfo<List<TaskDefinition>> definitions(@RequestParam String sourceType,
            @RequestParam(required = false) Long afterId, @RequestParam(defaultValue = "50") int limit) {
        return RespInfo.success(engine.definitions(sourceType, afterId, limit));
    }

    @GetMapping("/definition/{id}")
    public RespInfo<TaskDefinition> definition(@PathVariable long id) {
        return RespInfo.success(engine.definition(id));
    }

    @PostMapping("/create")
    public RespInfo<BusinessTask> create(@RequestBody BusinessTaskEngine.Create command) {
        return RespInfo.success(engine.create(command));
    }

    @GetMapping("/{id}")
    public RespInfo<BusinessTask> get(@PathVariable long id) {
        return RespInfo.success(engine.get(id));
    }

    @GetMapping("/assigned")
    public RespInfo<List<BusinessTask>> assigned(@RequestParam(required = false) TaskState state,
            @RequestParam(required = false) Long afterId, @RequestParam(defaultValue = "50") int limit) {
        return RespInfo.success(engine.assigned(state, afterId, limit));
    }

    @PostMapping("/{id}/dispatch")
    public RespInfo<BusinessTask> dispatch(@PathVariable long id) {
        return RespInfo.success(engine.dispatch(id));
    }

    @PostMapping("/{id}/start")
    public RespInfo<BusinessTask> start(@PathVariable long id) {
        return RespInfo.success(engine.start(id));
    }

    @PostMapping("/{id}/complete")
    public RespInfo<BusinessTask> complete(@PathVariable long id, @RequestBody BusinessTaskEngine.Complete command) {
        return RespInfo.success(engine.complete(id, command));
    }

    @PostMapping("/{id}/cancel")
    public RespInfo<BusinessTask> cancel(@PathVariable long id, @RequestBody Cancellation command) {
        return RespInfo.success(engine.cancel(id, command.reason()));
    }

    public record Cancellation(String reason) { }
}
